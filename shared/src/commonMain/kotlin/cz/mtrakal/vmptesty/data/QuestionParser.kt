package cz.mtrakal.vmptesty.data

/**
 * Parser bundlovaného TSV s otázkami (`composeResources/files/questions.tsv`).
 *
 * Formát je jeden řádek na otázku, 11 sloupců oddělených tabem:
 * ```
 * id  zpusobilost  podsada  qImage  qText  aText  aImage  bText  bImage  cText  cImage
 * ```
 * Sloupec `aText`/`aImage` je vždy správná odpověď — míchá se až za běhu
 * v [cz.mtrakal.vmptesty.quiz.QuizSession], protože ve zdroji na spspraha.cz
 * je správná odpověď vždy `a)`.
 *
 * Data generuje `tools/scrape.py`. Parser je záměrně striktní: nekonzistentní
 * data mají spadnout hned při startu, ne se projevit jako podivná otázka
 * uprostřed testu.
 */
object QuestionParser {

    private const val COLUMN_COUNT = 11

    /** @throws IllegalArgumentException když je [tsv] poškozené */
    fun parse(tsv: String): List<Question> =
        tsv.lineSequence()
            .withIndex()
            .filter { (_, line) -> line.isNotBlank() }
            .map { (index, line) -> parseLine(line, lineNumber = index + 1) }
            .toList()

    private fun parseLine(line: String, lineNumber: Int): Question {
        val columns = line.split(SEPARATOR)
        require(columns.size == COLUMN_COUNT) {
            "řádek $lineNumber: ${columns.size} sloupců, očekáváno $COLUMN_COUNT"
        }

        val id = requireNotNull(columns[0].toIntOrNull()) {
            "řádek $lineNumber: '${columns[0]}' není číslo otázky"
        }
        val zpusobilost = requireNotNull(Zpusobilost.ofCode(columns[1])) {
            "řádek $lineNumber: neznámá způsobilost '${columns[1]}'"
        }
        val text = columns[4]
        require(text.isNotEmpty()) { "řádek $lineNumber: prázdný text otázky" }

        // Správná odpověď je ve zdroji vždy první, proto correctIndex = 0.
        val answers = listOf(
            answer(columns[5], columns[6], "a", lineNumber),
            answer(columns[7], columns[8], "b", lineNumber),
            answer(columns[9], columns[10], "c", lineNumber),
        )

        return Question(
            id = id,
            zpusobilost = zpusobilost,
            podsada = columns[2],
            text = text,
            image = columns[3].ifEmpty { null },
            answers = answers,
            correctIndex = 0,
        )
    }

    private fun answer(text: String, image: String, label: String, lineNumber: Int): Answer {
        require(text.isNotEmpty() || image.isNotEmpty()) {
            "řádek $lineNumber: odpověď $label je prázdná (bez textu i obrázku)"
        }
        return Answer(text = text, image = image.ifEmpty { null })
    }

    private const val SEPARATOR = '\t'
}
