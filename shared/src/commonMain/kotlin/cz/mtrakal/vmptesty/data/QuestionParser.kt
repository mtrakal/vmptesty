package cz.mtrakal.vmptesty.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Parser bundlovaného JSON s otázkami (`composeResources/files/questions.json`).
 *
 * Formát:
 * ```json
 * {
 *   "version": 1,
 *   "source": "http://www.spspraha.cz/zkousky/",
 *   "questions": [
 *     {
 *       "id": 412, "set": "S", "subset": "P1 2015",
 *       "text": "Výraz plachetnice označuje:",
 *       "image": "221.jpg",
 *       "answers": [
 *         { "text": "…", "correct": true },
 *         { "text": "…" },
 *         { "text": "", "image": "N16.jpg" }
 *       ]
 *     }
 *   ]
 * }
 * ```
 *
 * Správnou odpověď určuje příznak `correct`, ne pozice — odpovědi se dají
 * v souboru libovolně přeuspořádat, aniž by se rozbila správnost. V aplikaci
 * se stejně míchají za běhu.
 *
 * Data generuje `tools/scrape.py`. Parser je záměrně striktní: nekonzistentní
 * data mají spadnout hned při startu, ne se projevit jako podivná otázka
 * uprostřed testu.
 */
object QuestionParser {

    /** Verze schématu, kterou tenhle parser umí. */
    const val SUPPORTED_VERSION = 1

    private val json = Json {
        ignoreUnknownKeys = true
    }

    /** @throws IllegalArgumentException když je [text] poškozený */
    fun parse(text: String): List<Question> {
        val document = json.decodeFromString<QuestionsDocument>(text)
        require(document.version == SUPPORTED_VERSION) {
            "questions.json má verzi ${document.version}, podporována je $SUPPORTED_VERSION"
        }
        return document.questions.map(QuestionDto::toQuestion)
    }

    @Serializable
    private data class QuestionsDocument(
        val version: Int,
        val source: String = "",
        val questions: List<QuestionDto>,
    )

    @Serializable
    private data class QuestionDto(
        val id: Int,
        @SerialName("set") val zpusobilost: String,
        val subset: String = "",
        val text: String,
        val image: String? = null,
        val answers: List<AnswerDto>,
    ) {
        fun toQuestion(): Question {
            val where = "$zpusobilost č.$id"
            val set = requireNotNull(Zpusobilost.ofCode(zpusobilost)) {
                "$where: neznámá způsobilost '$zpusobilost'"
            }
            require(text.isNotEmpty()) { "$where: prázdný text otázky" }

            val correctIndices = answers.indices.filter { answers[it].correct }
            require(correctIndices.size == 1) {
                "$where: očekávána právě jedna správná odpověď, nalezeno ${correctIndices.size}"
            }

            return Question(
                id = id,
                zpusobilost = set,
                podsada = subset,
                text = text,
                image = image?.ifEmpty { null },
                answers = answers.map { it.toAnswer(where) },
                correctIndex = correctIndices.single(),
            )
        }
    }

    @Serializable
    private data class AnswerDto(
        val text: String = "",
        val image: String? = null,
        val correct: Boolean = false,
    ) {
        /** Odpověď smí být jen obrázková (bez textu), ale prázdná být nesmí. */
        fun toAnswer(where: String): Answer {
            require(text.isNotEmpty() || !image.isNullOrEmpty()) {
                "$where: odpověď bez textu i obrázku"
            }
            return Answer(text = text, image = image?.ifEmpty { null })
        }
    }
}
