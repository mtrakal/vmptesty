package cz.mtrakal.vmptesty.wear

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.CircularProgressIndicator
import androidx.wear.compose.material3.FilledTonalButton
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text

/** Zelená pro "správně" — Wear Material 3 nemá pro úspěch vlastní barvu. */
internal val CorrectColor = Color(0xFF7BD88F)

/** Podklad karty se správnou odpovědí. */
internal val CorrectContainerColor = Color(0xFF17371F)

/**
 * Kořen hodinkové aplikace.
 *
 * V celé aplikaci je jen jeden [AppScaffold]; každá obrazovka si drží vlastní
 * [ScreenScaffold], aby jí fungoval scroll indikátor a TimeText.
 */
@Composable
fun WearApp(viewModel: WearQuizViewModel = viewModel { WearQuizViewModel() }) {
    MaterialTheme {
        AppScaffold {
            when (val state = viewModel.uiState) {
                WearUiState.Loading -> MessageScreen { CircularProgressIndicator() }

                is WearUiState.Failed -> MessageScreen {
                    Text(
                        text = state.message,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                    )
                    FilledTonalButton(onClick = viewModel::load) { Text("Zkusit znovu") }
                }

                is WearUiState.Picking -> PickSetScreen(
                    counts = state.counts,
                    onPick = viewModel::start,
                )

                is WearUiState.Running -> WearQuestionScreen(
                    session = state.session,
                    phase = state.phase,
                    onSelect = viewModel::select,
                    onNext = viewModel::next,
                )

                is WearUiState.Finished -> WearResultScreen(
                    session = state.session,
                    onRestart = viewModel::backToPicking,
                )
            }
        }
    }
}

/** Krátká zpráva na střed — nikdy nescrolluje, takže stačí [Column]. */
@Composable
private fun MessageScreen(content: @Composable () -> Unit) {
    ScreenScaffold {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
        ) {
            content()
        }
    }
}
