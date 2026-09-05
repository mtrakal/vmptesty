package cz.mtrakal.vmptesty.wear

import cz.mtrakal.vmptesty.data.Answer
import cz.mtrakal.vmptesty.data.Question
import cz.mtrakal.vmptesty.data.Zpusobilost
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Stavový automat rychlého drilu na hodinkách.
 *
 * Chování se liší od telefonu (správná odpověď se sama posune dál), takže si
 * zaslouží vlastní testy — přes UI se 100 ms blik ověřit nedá.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class WearQuizViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `po nacteni nabidne sady i s pocty`() = runTest(dispatcher) {
        val viewModel = viewModel()
        advanceUntilIdle()

        val state = assertIs<WearUiState.Picking>(viewModel.uiState)
        assertEquals(2, state.counts[WearPick.Single(Zpusobilost.M)])
        assertEquals(1, state.counts[WearPick.Single(Zpusobilost.S)])
        assertEquals(3, state.counts[WearPick.All])
    }

    @Test
    fun `chyba pri nacteni skonci ve stavu Failed`() = runTest(dispatcher) {
        val viewModel = WearQuizViewModel { error("nelze načíst") }
        advanceUntilIdle()

        assertEquals("nelze načíst", assertIs<WearUiState.Failed>(viewModel.uiState).message)
    }

    @Test
    fun `spravna odpoved zablikne zelene a pak sama posune dal`() = runTest(dispatcher) {
        val viewModel = started()
        val session = running(viewModel).session

        viewModel.select(session.current!!.correctIndex)

        // Nejdriv zelene bliknuti na stejne otazce.
        val flashing = running(viewModel)
        assertEquals(WearUiState.Phase.CorrectFlash, flashing.phase)
        assertEquals(0, flashing.session.index)
        assertEquals(1, flashing.session.score.correct)

        // Az po uplynuti bliknuti se posune.
        advanceTimeBy(WearQuizViewModelTestValues.FLASH_MS + 1)
        val advanced = running(viewModel)
        assertEquals(1, advanced.session.index)
        assertEquals(WearUiState.Phase.Asking, advanced.phase)
    }

    @Test
    fun `chybna odpoved zablikne cervene a pak zustane oprava`() = runTest(dispatcher) {
        val viewModel = started()
        val item = running(viewModel).session.current!!

        viewModel.select((item.correctIndex + 1) % item.answers.size)

        val flashing = running(viewModel)
        assertEquals(WearUiState.Phase.WrongFlash, flashing.phase)
        assertEquals(1, flashing.session.score.wrong)

        advanceTimeBy(WearQuizViewModelTestValues.FLASH_MS + 1)
        val correcting = running(viewModel)
        assertEquals(WearUiState.Phase.Correction, correcting.phase)
        // Oprava se sama neposune, ceka na uzivatele.
        assertEquals(0, correcting.session.index)
    }

    @Test
    fun `oprava se posune az po next`() = runTest(dispatcher) {
        val viewModel = started()
        val item = running(viewModel).session.current!!

        viewModel.select((item.correctIndex + 1) % item.answers.size)
        advanceTimeBy(WearQuizViewModelTestValues.FLASH_MS + 1)
        viewModel.next()

        val advanced = running(viewModel)
        assertEquals(1, advanced.session.index)
        assertEquals(WearUiState.Phase.Asking, advanced.phase)
    }

    @Test
    fun `druhe klepnuti na odpoved skore nemeni`() = runTest(dispatcher) {
        val viewModel = started()
        val item = running(viewModel).session.current!!

        viewModel.select(item.correctIndex)
        viewModel.select((item.correctIndex + 1) % item.answers.size)

        assertEquals(1, running(viewModel).session.score.correct)
        assertEquals(0, running(viewModel).session.score.wrong)
    }

    @Test
    fun `po posledni otazce prijde vysledek`() = runTest(dispatcher) {
        val viewModel = viewModel()
        advanceUntilIdle()
        viewModel.start(WearPick.Single(Zpusobilost.S))

        val item = running(viewModel).session.current!!
        viewModel.select(item.correctIndex)
        advanceTimeBy(WearQuizViewModelTestValues.FLASH_MS + 1)

        val finished = assertIs<WearUiState.Finished>(viewModel.uiState)
        assertTrue(finished.session.isFinished)
        assertEquals(1, finished.session.score.answered)
    }

    @Test
    fun `zpet na vyber zahodi skore`() = runTest(dispatcher) {
        val viewModel = started()
        viewModel.select(running(viewModel).session.current!!.correctIndex)
        advanceTimeBy(WearQuizViewModelTestValues.FLASH_MS + 1)

        viewModel.backToPicking()

        assertIs<WearUiState.Picking>(viewModel.uiState)
    }

    private fun running(viewModel: WearQuizViewModel) =
        assertIs<WearUiState.Running>(viewModel.uiState)

    private fun viewModel() = WearQuizViewModel { POOL }

    private suspend fun kotlinx.coroutines.test.TestScope.started(): WearQuizViewModel {
        val viewModel = viewModel()
        advanceUntilIdle()
        viewModel.start(WearPick.Single(Zpusobilost.M))
        return viewModel
    }

    private object WearQuizViewModelTestValues {
        /** Musí odpovídat WearQuizViewModel.FLASH_MS. */
        const val FLASH_MS = 100L
    }

    private companion object {
        val POOL = listOf(
            question(1, Zpusobilost.M),
            question(2, Zpusobilost.M),
            question(3, Zpusobilost.S),
        )

        fun question(id: Int, set: Zpusobilost) = Question(
            id = id,
            zpusobilost = set,
            podsada = "${set.code}1",
            text = "${set.code} otázka $id",
            image = null,
            answers = listOf(
                Answer("správná $id"),
                Answer("špatná b $id"),
                Answer("špatná c $id"),
            ),
            correctIndex = 0,
        )
    }
}
