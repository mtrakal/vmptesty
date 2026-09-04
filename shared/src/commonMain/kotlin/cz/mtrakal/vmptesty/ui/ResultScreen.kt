package cz.mtrakal.vmptesty.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cz.mtrakal.vmptesty.data.Zpusobilost
import cz.mtrakal.vmptesty.quiz.QuizSession
import cz.mtrakal.vmptesty.quiz.Score

/** Souhrn po dojezdu sady. */
@Composable
fun ResultScreen(
    session: QuizSession,
    onRestart: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val score = session.score

    Column(
        modifier = modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = "Hotovo",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )

        Card(Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "${score.percent} %",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = if (score.percent >= PASS_PERCENT) {
                        correctColor()
                    } else {
                        MaterialTheme.colorScheme.error
                    },
                )
                Text(
                    text = "${score.correct} z ${score.answered} správně",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        if (session.scoreBySet.size > 1) {
            Text(
                text = "Podle sad",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Card(Modifier.fillMaxWidth()) {
                Column {
                    val rows = Zpusobilost.entries.mapNotNull { set ->
                        session.scoreBySet[set]?.let { set to it }
                    }
                    rows.forEachIndexed { index, (set, setScore) ->
                        if (index > 0) HorizontalDivider()
                        SetScoreRow(set, setScore)
                    }
                }
            }
        }

        Spacer(Modifier.height(4.dp))

        Button(onClick = onRestart, modifier = Modifier.fillMaxWidth()) {
            Text("Zkusit znovu")
        }
    }
}

@Composable
private fun SetScoreRow(set: Zpusobilost, score: Score) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "${set.code} — ${set.label}",
            style = MaterialTheme.typography.bodyMedium,
        )
        Text(
            text = "${score.correct}/${score.answered} · ${score.percent} %",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
        )
    }
}

/**
 * Od kolika procent se výsledek obarví zeleně. Jen vizuální vodítko —
 * není to oficiální hranice úspěšnosti zkoušky.
 */
private const val PASS_PERCENT = 75
