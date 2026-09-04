package cz.mtrakal.vmptesty

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import cz.mtrakal.vmptesty.quiz.QuizUiState
import cz.mtrakal.vmptesty.quiz.QuizViewModel
import cz.mtrakal.vmptesty.ui.COMPACT_WIDTH_THRESHOLD
import cz.mtrakal.vmptesty.ui.LocalCompactWidth
import cz.mtrakal.vmptesty.ui.QuestionScreen
import cz.mtrakal.vmptesty.ui.ResultScreen
import cz.mtrakal.vmptesty.ui.SetupScreen
import cz.mtrakal.vmptesty.ui.VmpTheme

@Composable
fun App(viewModel: QuizViewModel = viewModel { QuizViewModel() }) {
    VmpTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
        ) {
            // Šířka okna rozhoduje o velikosti odznaků a teček — na telefonu
            // se zvětšují, aby se daly pohodlně trefit prstem.
            BoxWithConstraints(Modifier.fillMaxSize()) {
                CompositionLocalProvider(
                    LocalCompactWidth provides (maxWidth < COMPACT_WIDTH_THRESHOLD),
                ) {
                    Column(Modifier.fillMaxSize().safeContentPadding()) {
                        CurrentScreen(viewModel)
                    }
                }
            }
        }
    }
}

@Composable
private fun CurrentScreen(viewModel: QuizViewModel) {
    when (val state = viewModel.uiState) {
        QuizUiState.Loading -> CenteredMessage { CircularProgressIndicator() }

        is QuizUiState.Failed -> CenteredMessage {
            Text(
                text = state.message,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
            )
            Button(onClick = viewModel::retryLoad) { Text("Zkusit znovu") }
        }

        is QuizUiState.Setup -> SetupScreen(
            state = state,
            onToggleSet = viewModel::toggleSet,
            onLengthChange = viewModel::setLength,
            onStart = viewModel::start,
        )

        is QuizUiState.Running -> QuestionScreen(
            session = state.session,
            onSelect = viewModel::select,
            onNext = viewModel::next,
        )

        is QuizUiState.Finished -> ResultScreen(
            session = state.session,
            onRestart = viewModel::backToSetup,
        )
    }
}

@Composable
private fun CenteredMessage(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
    ) {
        content()
    }
}
