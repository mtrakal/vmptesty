#!/usr/bin/env python3
"""Vytáhne testové otázky VMP ze stránek Státní plavební správy.

Zapisuje bundlované resources pro shared modul:
  shared/src/commonMain/composeResources/files/questions.json
  shared/src/commonMain/composeResources/files/images/*.jpg

Použití:
  python tools/scrape.py                  # stáhne HTML i obrázky ze sítě
  python tools/scrape.py --cache DIR      # HTML bere z DIR/{s,c,m}.html, jinak stáhne
"""
from __future__ import annotations

import argparse
import html
import json
import os
import re
import sys
import urllib.parse
import urllib.request
from concurrent.futures import ThreadPoolExecutor

BASE = "http://www.spspraha.cz/zkousky/"
SOURCES = {"S": "S 2015", "C": "C", "M": "M 2015"}
EXPECTED = {"S": 170, "C": 215, "M": 407}

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
OUT_DIR = os.path.join(ROOT, "shared", "src", "commonMain", "composeResources", "files")
JSON_PATH = os.path.join(OUT_DIR, "questions.json")
IMG_DIR = os.path.join(OUT_DIR, "images")

SCHEMA_VERSION = 1

RE_ID = re.compile(r"č\.\s*<span[^>]*>\s*(\d+)\s*</span>")
RE_SUBSET = re.compile(r"<i>Zkratka souboru otázek:</i>\s*</span>\s*([^<]*)")
RE_IMG = re.compile(r'<img[^>]*\bsrc="([^"]+)"')
RE_TAG = re.compile(r"<[^>]+>")
RE_WS = re.compile(r"\s+")


def fetch(url: str) -> bytes:
    req = urllib.request.Request(url, headers={"User-Agent": "vmptesty-scraper"})
    with urllib.request.urlopen(req, timeout=60) as resp:
        return resp.read()


def load_page(code: str, cache: str | None) -> str:
    if cache:
        path = os.path.join(cache, code.lower() + ".html")
        if os.path.exists(path):
            with open(path, encoding="utf-8") as fh:
                return fh.read()
    url = BASE + "otazky.asp?zp=" + urllib.parse.quote(SOURCES[code])
    print("  stahuji " + url)
    return fetch(url).decode("utf-8")


def cell(block: str, label: str) -> tuple[str, str]:
    """Vrátí (text, nazev_obrazku) pro řádek s daným popiskem."""
    match = re.search(r"<th[^>]*>" + label + r"</th>(.*?)</tr>", block, re.S)
    if match is None:
        raise ValueError("chybí řádek " + repr(label))
    raw = match.group(1)

    image = ""
    img = RE_IMG.search(raw)
    if img:
        image = os.path.basename(urllib.parse.urlparse(img.group(1)).path)

    text = html.unescape(RE_TAG.sub(" ", raw)).replace(chr(160), " ")
    return RE_WS.sub(" ", text).strip(), image


def parse_page(code: str, page: str) -> list[dict]:
    questions: list[dict] = []
    for block in re.split(r'<tr class="bg">', page)[1:]:
        ident = RE_ID.search(block)
        subset = RE_SUBSET.search(block)
        if ident is None or subset is None:
            raise ValueError(code + ": blok bez čísla nebo zkratky sady")

        q_text, q_img = cell(block, "Otázka")
        a_text, a_img = cell(block, r"Správná odpověď&nbsp;a\)")
        b_text, b_img = cell(block, r"Odpověď&nbsp;b\)")
        c_text, c_img = cell(block, r"Odpověď&nbsp;c\)")

        where = code + " č." + ident.group(1)
        if not q_text:
            raise ValueError(where + ": prázdný text otázky")

        # Ve zdroji je správná odpověď vždy a). V JSON to nese příznak "correct"
        # u odpovědi, takže se dají odpovědi v souboru libovolně přeuspořádat,
        # aniž by se rozbila správnost.
        answers = []
        for label, text, image, correct in (
            ("a", a_text, a_img, True),
            ("b", b_text, b_img, False),
            ("c", c_text, c_img, False),
        ):
            if not text and not image:
                raise ValueError(where + ": odpověď " + label + " je prázdná")
            answer: dict = {"text": text}
            if image:
                answer["image"] = image
            if correct:
                answer["correct"] = True
            answers.append(answer)

        question: dict = {
            "id": int(ident.group(1)),
            "set": code,
            "subset": subset.group(1).strip(),
            "text": q_text,
        }
        if q_img:
            question["image"] = q_img
        question["answers"] = answers
        questions.append(question)
    return questions


def download_images(names: list[str]) -> None:
    os.makedirs(IMG_DIR, exist_ok=True)
    todo = [n for n in names if not os.path.exists(os.path.join(IMG_DIR, n))]
    print("  obrázky: " + str(len(names)) + " celkem, " + str(len(todo)) + " ke stažení")

    def one(name: str) -> str | None:
        try:
            data = fetch(BASE + "images/" + name)
        except Exception as exc:  # noqa: BLE001 - chceme jméno souboru v hlášce
            return name + ": " + str(exc)
        if not data:
            return name + ": prázdná odpověď"
        with open(os.path.join(IMG_DIR, name), "wb") as fh:
            fh.write(data)
        return None

    with ThreadPoolExecutor(max_workers=8) as pool:
        errors = [e for e in pool.map(one, todo) if e]
    if errors:
        raise SystemExit("nepodařilo se stáhnout obrázky: " + "; ".join(errors))


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--cache", help="adresář s předstaženými {s,c,m}.html")
    args = parser.parse_args()

    all_questions: list[dict] = []
    for code in SOURCES:
        print("sada " + code + ":")
        questions = parse_page(code, load_page(code, args.cache))
        if len(questions) != EXPECTED[code]:
            raise SystemExit(
                "sada " + code + ": očekáváno " + str(EXPECTED[code])
                + " otázek, nalezeno " + str(len(questions))
                + ". Zdroj se změnil - zkontroluj parser i konstanty EXPECTED."
            )
        print("  " + str(len(questions)) + " otázek")
        all_questions.extend(questions)

    images = sorted(
        {
            image
            for question in all_questions
            for image in [question.get("image")] + [a.get("image") for a in question["answers"]]
            if image
        }
    )
    bad = [i for i in images if not i.endswith(".jpg")]
    if bad:
        raise SystemExit("neočekávané přípony obrázků: " + str(bad))

    document = {
        "version": SCHEMA_VERSION,
        "source": BASE,
        "questions": all_questions,
    }

    os.makedirs(OUT_DIR, exist_ok=True)
    # Odsazený JSON s neescapovanou diakritikou - soubor má jít přečíst i ručně
    # a diff po regeneraci má zůstat čitelný.
    with open(JSON_PATH, "w", encoding="utf-8", newline="\n") as fh:
        json.dump(document, fh, ensure_ascii=False, indent=2)
        fh.write("\n")
    print("zapsáno " + str(len(all_questions)) + " otázek do " + os.path.relpath(JSON_PATH, ROOT))

    download_images(images)
    print(str(len(images)) + " unikátních obrázků v " + os.path.relpath(IMG_DIR, ROOT))
    return 0


if __name__ == "__main__":
    sys.exit(main())
