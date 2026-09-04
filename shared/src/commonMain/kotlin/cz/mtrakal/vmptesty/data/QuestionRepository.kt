package cz.mtrakal.vmptesty.data

import org.jetbrains.compose.resources.ExperimentalResourceApi
import vmptesty.shared.generated.resources.Res

/** Načte otázky z bundlovaného resource. Data jsou statická, stačí načíst jednou. */
class QuestionRepository {

    private var cached: List<Question>? = null

    @OptIn(ExperimentalResourceApi::class)
    suspend fun load(): List<Question> = cached ?: run {
        val tsv = Res.readBytes(QUESTIONS_PATH).decodeToString()
        QuestionParser.parse(tsv).also { cached = it }
    }

    private companion object {
        const val QUESTIONS_PATH = "files/questions.tsv"
    }
}
