package cz.mtrakal.vmptesty.wear

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cz.mtrakal.vmptesty.data.Question
import cz.mtrakal.vmptesty.data.QuestionRepository
import cz.mtrakal.vmptesty.data.Zpusobilost
import cz.mtrakal.vmptesty.quiz.QuizLength
import cz.mtrakal.vmptesty.quiz.QuizSession
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Co si na hodinkách vybíráš. Buď jedna způsobilost, nebo všechno dohromady.
 *
 * Na kulatém displeji nedává smysl skládat výběr z checkboxů — jedno klepnutí
 * rovnou startuje procvičování.
 */
sealed interface WearPick {

    val label: String

    data class Single(val set: Zpusobilost) : WearPick {
        override val label: String get() = "${set.code} — ${set.label}"
    }

    data object All : WearPick {
        override val label: String get() = "Všechny sady"
    }

    /** Které způsobilosti výběr pokrývá. */
    fun sets(): Set<Zpusobilost> = when (this) {
        is Single -> setOf(set)
        All -> Zpusobilost.entries.toSet()
    }

    companion object {
        val entries: List<WearPick> = Zpusobilost.entries.map(::Single) + All
    }
}

/** Stav, který vidí hodinkové UI. */
sealed interface WearUiState {

    data object Loading : WearUiState

    data class Failed(val message: String) : WearUiState

    /** Nabídka sad i s počty otázek. */
    data class Picking(val counts: Map<WearPick, Int>) : WearUiState

    /**
     * Fáze otázky. Na hodinkách jde o rychlost drilu, takže se liší od telefonu:
     * správná odpověď rovnou přeskočí dál, u chybné se krátce zablikne červeně
     * a pak zůstane na obrazovce jen ta správná.
     */
    enum class Phase { Asking, WrongFlash, Correction }

    data class Running(val session: QuizSession, val phase: Phase = Phase.Asking) : WearUiState

    data class Finished(val session: QuizSession) : WearUiState
}

/**
 * Stav procvičování na hodinkách.
 *
 * Sdílí [QuizSession] s telefonem — míchání otázek i odpovědí, skóre a přechody
 * jsou tedy stejné a pokryté stejnými testy. Liší se jen výběr: hodinky vždy
 * projedou celou vybranou sadu ([QuizLength.ALL]).
 */
class WearQuizViewModel(
    private val repository: QuestionRepository = QuestionRepository(),
) : ViewModel() {

    private var questions: List<Question> = emptyList()

    var uiState: WearUiState by mutableStateOf(WearUiState.Loading)
        private set

    init {
        load()
    }

    fun load() {
        uiState = WearUiState.Loading
        viewModelScope.launch {
            uiState = runCatching { repository.load() }.fold(
                onSuccess = { loaded ->
                    questions = loaded
                    pickingState()
                },
                onFailure = { error ->
                    WearUiState.Failed(error.message ?: "Otázky se nepodařilo načíst.")
                },
            )
        }
    }

    fun start(pick: WearPick) {
        val session = QuizSession.build(questions, pick.sets(), QuizLength.ALL)
        if (!session.isFinished) uiState = WearUiState.Running(session)
    }

    /**
     * Zaznamená odpověď.
     *
     * Správná odpověď posune rovnou na další otázku — potvrzením je naskočené
     * počítadlo, žádné další klepnutí není potřeba. Chybná odpověď se na
     * [WRONG_FLASH_MS] zvýrazní červeně a pak se ukáže jen správná odpověď
     * s tlačítkem dál.
     */
    fun select(answerIndex: Int) {
        val running = uiState as? WearUiState.Running ?: return
        if (running.session.isAnswered) return

        val answered = running.session.select(answerIndex)
        if (answered.isSelectionCorrect) {
            uiState = WearUiState.Running(answered)
            next()
            return
        }

        uiState = WearUiState.Running(answered, WearUiState.Phase.WrongFlash)
        viewModelScope.launch {
            delay(WRONG_FLASH_MS)
            val current = uiState
            // Mezitim uz mohl uzivatel klepnout na "dalsi"; prepisovat by bylo spatne.
            if (current is WearUiState.Running && current.session.index == answered.index) {
                uiState = WearUiState.Running(current.session, WearUiState.Phase.Correction)
            }
        }
    }

    fun next() {
        val running = uiState as? WearUiState.Running ?: return
        val advanced = running.session.next()
        uiState = if (advanced.isFinished) {
            WearUiState.Finished(advanced)
        } else {
            WearUiState.Running(advanced, WearUiState.Phase.Asking)
        }
    }

    fun backToPicking() {
        uiState = pickingState()
    }

    private companion object {
        /** Jak dlouho blikne chybně zvolená odpověď, než se ukáže ta správná. */
        const val WRONG_FLASH_MS = 100L
    }

    private fun pickingState() = WearUiState.Picking(
        counts = WearPick.entries.associateWith { pick ->
            questions.count { it.zpusobilost in pick.sets() }
        },
    )
}
