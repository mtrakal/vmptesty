# VMP Testy — návrh

Datum: 2026-09-04

Aplikace pro procvičování testových otázek ke zkoušce Vůdce malého plavidla
(způsobilosti **S**, **C**, **M**). Kotlin Multiplatform + Compose Multiplatform,
cíle: Android, desktop (JVM), web (wasmJs/js).

## Cíl

Rychlé drilování otázek offline. Uživatel vybere způsobilosti, dostane
zamíchanou sadu, po každé odpovědi okamžitě vidí, zda odpověděl správně, a
během běhu aplikace se počítá skóre.

## Zdroj dat

Otázky pocházejí ze Státní plavební správy:

| Sada | URL | Počet otázek |
|---|---|---|
| S | `http://www.spspraha.cz/zkousky/otazky.asp?zp=S%202015` | 170 |
| C | `http://www.spspraha.cz/zkousky/otazky.asp?zp=C` | 215 |
| M | `http://www.spspraha.cz/zkousky/otazky.asp?zp=M%202015` | 407 |

Celkem **792 otázek**, **242 obrázků** (5,1 MB, vše `.jpg`).

Zjištěné vlastnosti HTML:

- Každá otázka má přesně **3 odpovědi**.
- Správná odpověď je **vždy `a)`** → v aplikaci se odpovědi **musí míchat**.
- Obrázek může být **u otázky i u jednotlivých odpovědí**. U otázky nejvýše
  jeden (224 otázek: C 62, M 162). V sadě C je navíc **16 otázek, kde má
  obrázek každá ze tří odpovědí** (obrázkové varianty, např. vrcholové znaky
  bójí). U 7 z těchto 16 otázek jsou odpovědi **jen obrázkové, úplně bez
  textu** (21 takových odpovědí) — karta odpovědi tedy musí zvládnout tři
  varianty: jen text, text + obrázek, jen obrázek. Celkem 272 odkazů na 242
  unikátních souborů (některé obrázky se opakují u více otázek).
- Každá otázka nese zkratku podsady (S: `P1 2015`–`P4 2015`; C: `M1`, `MP1`–`MP4`,
  `N1`–`N4`, `Z1`; M: `PP1 2015`–`PP4 2015`, `TZ 2015`, `ZP 2015`).
- Pole `Poznámka` je ve zdroji zakomentované → ignoruje se.

## Rozhodnutí o datech

Otázky se **negenerují jako Kotlin mapa**, ale jako **bundlovaný TSV resource**.
Důvod: 792 otázek jako Kotlin literály znamená ~300 KB generovaného kódu a
riziko překročení 64 KB limitu velikosti metody na JVM/Androidu u velkých
inicializátorů. TSV je navíc čitelný v diffu při regeneraci.

```
shared/src/commonMain/composeResources/files/questions.tsv
shared/src/commonMain/composeResources/files/images/*.jpg
```

Sloupce TSV (tab-separated, jeden řádek = jedna otázka, 11 sloupců):

```
id  zpusobilost  podsada  qImage  qText  aText  aImage  bText  bImage  cText  cImage
```

- `zpusobilost` ∈ `S` | `C` | `M`
- `*Image` je název souboru (např. `221.jpg`) nebo prázdný string
- `aText`/`aImage` je vždy **správná** odpověď (míchá se až v aplikaci)
- v textech se nevyskytují taby ani nové řádky (parser je normalizuje na mezery)
- `*Text` odpovědi smí být prázdný, pokud má odpověď obrázek; text otázky
  prázdný být nesmí
- ve zdrojových buňkách se nevyskytují žádné inline tagy kromě `<img>` ani HTML
  entity, takže čištění textu je triviální

**Regenerátor `tools/scrape.py`** se commituje do repozitáře, aby šlo data
znovu vytáhnout, když SPS otázky změní. Stahuje HTML, parsuje regexem,
zapisuje TSV a stahuje obrázky.

Obrázky se **nenačítají jako typované `Res.drawable.*`** — názvy jako `221.jpg`
nebo `261BA.jpg` nejsou spolehlivé Kotlin identifikátory a generované accessory
by byly nepředvídatelné. Místo toho dynamicky přes
`Res.readBytes("files/images/$nazev")` + `decodeToImageBitmap()`. Na webu se
obrázky díky tomu stahují lazy až u dané otázky, takže initial bundle zůstane malý.

## Architektura

Veškerá logika i UI v `shared/src/commonMain`, aplikační moduly
(`androidApp`, `desktopApp`, `webApp`) jen volají `App()`.

| Soubor | Odpovědnost |
|---|---|
| `data/Question.kt` | modely: `Answer(text, image: String?)` a `Question(id, zpusobilost, podsada, text, image: String?, answers: List<Answer>, correctIndex)`. Identita otázky je pár `(zpusobilost, id)` — čísla se mezi sadami opakují. |
| `data/QuestionParser.kt` | čistá funkce `parseTsv(String): List<Question>` |
| `data/QuestionRepository.kt` | načtení TSV z composeResources → `List<Question>` |
| `quiz/QuizSession.kt` | čistá logika: filtrace poolu, zamíchání otázek i odpovědí, posun, skóre |
| `quiz/QuizViewModel.kt` | drží stav pro UI, obsluhuje load a akce |
| `ui/SetupScreen.kt` | checkboxy S/C/M + délka sady + Start |
| `ui/QuestionScreen.kt` | text, obrázek, odpovědi, feedback, Další, počítadlo |
| `ui/ResultScreen.kt` | výsledek + Znovu |
| `App.kt` | přepínání `Setup → Running → Finished` |

`QuizSession` a `QuestionParser` nemají závislost na Compose → plně testovatelné
v `commonTest`.

### Stavový automat

```
Loading ──► Setup ──(Start)──► Running ──(poslední otázka)──► Finished
                ▲                                                 │
                └──────────────────(Znovu)────────────────────────┘
```

## Chování

### Setup

- Tři checkboxy: `S (170)`, `C (215)`, `M (407)`. Lze vybrat více.
- Délka sady: `20` / `50` / `Vše` (segmented button). Když je vybraných otázek
  méně než požadovaná délka, použije se počet dostupných.
- Start je disabled, dokud není vybraná aspoň jedna sada.

### Otázka

- Hlavička: `otázka 5/20` a počítadlo `✓ 3 · ✗ 1`.
- Text otázky, pod ním obrázek (pokud otázka obrázek má).
- Tři odpovědi jako klikatelné karty **v zamíchaném pořadí**; karta zobrazí
  text a pod ním obrázek, pokud ho odpověď má. U obrázkových odpovědí bez
  textu se zobrazí jen obrázek (karta nesmí zkolabovat na nulovou výšku).
- Pořadí otázek i odpovědí se zamíchá jednou při stavbě běhu (nemění se při
  rekompozici).

### Zpětná vazba po odpovědi

- Vybraná správná odpověď → zelené zvýraznění.
- Vybraná chybná odpověď → červené zvýraznění **a zároveň zelené zvýraznění
  správné odpovědi**.
- Odpovědi se zamknou, další kliknutí skóre nemění.
- Objeví se tlačítko „Další otázka".

### Výsledek

- `17/20 správně (85 %)`, rozpad podle způsobilostí.
- Tlačítko „Znovu" → zpět na Setup se zachovaným výběrem.

### Statistika

Počítá se jen za běh aplikace, **nic se nepersistuje**. Restart appky = nula.

## Testy (commonTest)

1. `parseTsv` vrátí 792 otázek, z toho 170 `S`, 215 `C`, 407 `M`.
2. Každá otázka má přesně 3 neprázdné odpovědi a `correctIndex` v rozsahu 0..2.
   Odpověď má neprázdný text NEBO obrázek. Existuje aspoň jedna otázka
   s obrázkem u otázky, aspoň jedna s obrázky u odpovědí a aspoň jedna
   s odpovědí bez textu (regrese proti tomu, že by scraper obrázky
   odpovědí zahodil).
3. Filtrace poolu podle vybraných způsobilostí vrací správné počty; pár
   `(zpusobilost, id)` je unikátní (samotné `id` není — mezi sadami se opakuje).
4. Zamíchání se seedovaným `Random` je deterministické a zachová všechny otázky.
5. Zamíchání odpovědí zachová `correctIndex` ukazující na původní správný text.
6. Skóre: správná odpověď zvýší `correct`, chybná `wrong`, druhé kliknutí na
   stejnou otázku skóre nemění.
7. Počet unikátních názvů obrázků v TSV je 242 a každý název končí `.jpg`
   (existenci souborů ověřuje `tools/scrape.py` při generování, `commonTest`
   neumí přenositelně listovat resource adresář).

## Co záměrně není součástí

Perzistence skóre, opakovací kolo chybných otázek, filtrování na úrovni podsad
(data je nesou, UI je nefiltruje), účty, timer, vysvětlivky k odpovědím,
iOS target.
