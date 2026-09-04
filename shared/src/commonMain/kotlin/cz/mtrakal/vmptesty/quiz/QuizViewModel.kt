package cz.mtrakal.vmptesty.quiz

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cz.mtrakal.vmptesty.data.Question
import cz.mtrakal.vmptesty.data.QuestionRepository
import cz.mtrakal.vmptesty.data.Zpusobilost
import kotlinx.coroutines.launch

/** Stav, který vidí UI. */
sealed interface QuizUiState {

    data object Loading : QuizUiState

    data class Failed(val message: String) : QuizUiState

    /** Výběr sad a délky testu. */
    data class Setup(
        val counts: Map<Zpusobilost, Int>,
        val selectedSets: Set<Zpusobilost>,
        val length: QuizLength,
    ) : QuizUiState {
        val availableCount: Int get() = selectedSets.sumOf { counts[it] ?: 0 }
        val plannedCount: Int get() = length.count?.coerceAtMost(availableCount) ?: availableCount
        val canStart: Boolean get() = availableCount > 0
    }

    data class Running(val session: QuizSession) : QuizUiState

    data class Finished(val session: QuizSession) : QuizUiState
}

/**
 * Drží stav učení. Jako ViewModel proto, aby rotace displeje na Androidu
 * nezahodila rozjetý test i s počítadlem.
 */
class QuizViewModel(
    private val repository: QuestionRepository = QuestionRepository(),
) : ViewModel() {

    private var questions: List<Question> = emptyList()
    private var selectedSets: Set<Zpusobilost> = setOf(Zpusobilost.M)
    private var length: QuizLength = QuizLength.SHORT

    var uiState: QuizUiState by mutableStateOf(QuizUiState.Loading)
        private set

    init {
        load()
    }

    private fun load() {
        uiState = QuizUiState.Loading
        viewModelScope.launch {
            uiState = runCatching { repository.load() }.fold(
                onSuccess = { loaded ->
                    questions = loaded
                    setupState()
                },
                onFailure = { error ->
                    QuizUiState.Failed(error.message ?: "Otázky se nepodařilo načíst.")
                },
            )
        }
    }

    fun retryLoad() = load()

    fun toggleSet(set: Zpusobilost) {
        selectedSets = if (set in selectedSets) selectedSets - set else selectedSets + set
        uiState = setupState()
    }

    fun setLength(newLength: QuizLength) {
        length = newLength
        uiState = setupState()
    }

    fun start() {
        val session = QuizSession.build(questions, selectedSets, length)
        uiState = if (session.isFinished) setupState() else QuizUiState.Running(session)
    }

    fun select(answerIndex: Int) {
        val running = uiState as? QuizUiState.Running ?: return
        uiState = QuizUiState.Running(running.session.select(answerIndex))
    }

    fun next() {
        val running = uiState as? QuizUiState.Running ?: return
        val advanced = running.session.next()
        uiState = if (advanced.isFinished) {
            QuizUiState.Finished(advanced)
        } else {
            QuizUiState.Running(advanced)
        }
    }

    /** Zpět na výběr sad — výběr i délka zůstávají, skóre se zahodí. */
    fun backToSetup() {
        uiState = setupState()
    }

    private fun setupState() = QuizUiState.Setup(
        counts = questions.groupingBy { it.zpusobilost }.eachCount(),
        selectedSets = selectedSets,
        length = length,
    )
}
