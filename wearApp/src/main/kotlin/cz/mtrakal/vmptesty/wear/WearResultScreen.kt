package cz.mtrakal.vmptesty.wear

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.EdgeButton
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.ListHeaderDefaults
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.SurfaceTransformation
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.lazy.rememberTransformationSpec
import androidx.wear.compose.material3.lazy.transformedHeight
import cz.mtrakal.vmptesty.data.Zpusobilost
import cz.mtrakal.vmptesty.quiz.QuizSession

/** Souhrn po dojetí sady. */
@Composable
fun WearResultScreen(
    session: QuizSession,
    onRestart: () -> Unit,
) {
    val listState = rememberTransformingLazyColumnState()
    val transformationSpec = rememberTransformationSpec()
    val score = session.score

    ScreenScaffold(
        scrollState = listState,
        edgeButton = {
            EdgeButton(onClick = onRestart) { Text("Znovu") }
        },
    ) { contentPadding ->
        TransformingLazyColumn(
            state = listState,
            contentPadding = contentPadding,
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            item {
                ListHeader(
                    modifier = Modifier
                        .minimumVerticalContentPadding(
                            ListHeaderDefaults.minimumTopListContentPadding,
                            ListHeaderDefaults.minimumBottomListContentPadding,
                        )
                        .transformedHeight(this, transformationSpec),
                    transformation = SurfaceTransformation(transformationSpec),
                ) {
                    Text("Hotovo")
                }
            }

            item {
                Text(
                    text = "${score.percent} %",
                    style = MaterialTheme.typography.displaySmall,
                    color = if (score.percent >= GREEN_FROM_PERCENT) {
                        CorrectColor
                    } else {
                        MaterialTheme.colorScheme.error
                    },
                )
            }

            item {
                Text(
                    text = "${score.correct} z ${score.answered} správně",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                )
            }

            // Rozpad podle sad dává smysl jen když jich bylo víc než jedna.
            if (session.scoreBySet.size > 1) {
                items(Zpusobilost.entries.size) { index ->
                    val set = Zpusobilost.entries[index]
                    session.scoreBySet[set]?.let { setScore ->
                        Text(
                            text = "${set.code}: ${setScore.correct}/${setScore.answered} · ${setScore.percent} %",
                            style = MaterialTheme.typography.labelSmall,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                        )
                    }
                }
            }
        }
    }
}

/**
 * Od kolika procent se výsledek obarví zeleně. Jen vizuální vodítko —
 * není to oficiální hranice úspěšnosti zkoušky.
 */
private const val GREEN_FROM_PERCENT = 75
