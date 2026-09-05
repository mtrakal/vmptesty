package cz.mtrakal.vmptesty.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.ExperimentalResourceApi
import vmptesty.shared.generated.resources.Res

/** Načte otázky z bundlovaného resource. Data jsou statická, stačí načíst jednou. */
class QuestionRepository {

    private var cached: List<Question>? = null

    /**
     * Čtení i parsování běží mimo hlavní vlákno — 400 kB JSON se 792 otázkami
     * by na hodinkách při startu viditelně zaseklo UI.
     */
    @OptIn(ExperimentalResourceApi::class)
    suspend fun load(): List<Question> = cached ?: withContext(Dispatchers.Default) {
        val text = Res.readBytes(QUESTIONS_PATH).decodeToString()
        QuestionParser.parse(text)
    }.also { cached = it }

    private companion object {
        const val QUESTIONS_PATH = "files/questions.json"
    }
}
