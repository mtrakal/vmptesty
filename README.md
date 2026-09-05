# VMP Testy

Procvičování testových otázek ke zkoušce **Vůdce malého plavidla** — způsobilosti
**M** (motorové plavidlo), **S** (plachetnice) a **C** (námořní plavba).

Kotlin Multiplatform + Compose Multiplatform, běží na Androidu, desktopu (JVM)
a na webu (Kotlin/Wasm). Otázky jsou bundlované, aplikace funguje offline.

## Jak to funguje

1. Vybereš, které sady se chceš učit (lze víc než jednu) a kolik otázek — 20, 50, nebo vše.
2. Aplikace vygeneruje zamíchanou sadu a pouští ji po jedné otázce.
3. Po zaškrtnutí odpovědi se hned ukáže, jestli byla správně. U chybné odpovědi
   se zároveň zvýrazní ta správná.
4. V hlavičce průběžně běží počítadlo `✓ správně · ✗ špatně`, na konci je souhrn
   s úspěšností a rozpadem podle sad.

Skóre platí jen pro aktuální běh aplikace — nic se neukládá.

## Data

792 otázek (S 170, C 215, M 407), každá se třemi odpověďmi, a 242 obrázků.
Zdrojem je [Státní plavební správa](http://www.spspraha.cz/zkousky/).

Data jsou bundlovaná jako resource:

```
shared/src/commonMain/composeResources/files/questions.json
shared/src/commonMain/composeResources/files/images/*.jpg
```

Dvě vlastnosti zdroje, které stojí za zmínku:

- **Správná odpověď je na webu SPS vždy `a)`**, takže aplikace odpovědi míchá —
  jinak by se dala správná odpověď uhádnout podle pozice.
- Obrázek může být u otázky i u jednotlivých odpovědí, a některé odpovědi jsou
  **jen obrázkové, úplně bez textu**.

### Regenerace dat

Když Státní plavební správa otázky změní:

```bash
python tools/scrape.py
```

Skript stáhne HTML, přepíše JSON a doplní chybějící obrázky. Padne, pokud
nenajde očekávané počty otázek — tichá změna zdroje se tak neprojeví až
za běhu. Po regeneraci spusť testy, `QuestionDataTest` ověří integritu dat.

## Spuštění

```bash
./gradlew :desktopApp:run
```

```bash
./gradlew :androidApp:assembleDebug
```

```bash
./gradlew :webApp:wasmJsBrowserDevelopmentRun
```

Web dev server běží na <http://localhost:8080>. Produkční build webu:
`./gradlew :webApp:wasmJsBrowserDistribution` (výstup v `webApp/build/dist`).

## Testy

```bash
./gradlew :shared:jvmTest
```

- `QuestionParserTest` — parser JSON včetně všech variant obrázků a chybových stavů
- `QuizSessionTest` — filtrace sad, míchání, skóre, přechody mezi otázkami
- `QuestionDataTest` — integrita reálných dat (počty, existence všech obrázků)
- `ResourceLoadingTest` — že se bundlované resources za běhu skutečně načtou

## Struktura

| Cesta | Co tam je |
|---|---|
| `shared/…/data` | model otázky, parser JSON, načtení resource |
| `shared/…/quiz` | `QuizSession` (čistá logika bez Compose) a `QuizViewModel` |
| `shared/…/ui` | obrazovky Setup / Question / Result, theme, načítání obrázků |
| `androidApp`, `desktopApp`, `webApp` | jen vstupní body, které volají `App()` |
| `tools/scrape.py` | regenerátor dat ze stránek SPS |
| `docs/superpowers/specs/` | návrhový dokument |

`QuizSession` a `QuestionParser` záměrně nezávisí na Compose, takže je pokrývají
běžné unit testy.

## Poznámka k buildu

`gradle.properties` nastavuje `kotlin.user.home=.kotlin-home`, aby Kotlin držel
npm tooling v projektu. Bez toho ho instaluje do `~/.kotlin`, kde si yarn 1.x
najde v domovském adresáři cizí `package.json` s polem `packageManager`
a odmítne pokračovat — wasm build pak spadne na `kotlinWasmToolingSetup`.
