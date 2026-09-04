package cz.mtrakal.vmptesty.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cz.mtrakal.vmptesty.data.Zpusobilost
import cz.mtrakal.vmptesty.quiz.QuizLength
import cz.mtrakal.vmptesty.quiz.QuizUiState

/** Výběr sad otázek a délky testu. */
@Composable
fun SetupScreen(
    state: QuizUiState.Setup,
    onToggleSet: (Zpusobilost) -> Unit,
    onLengthChange: (QuizLength) -> Unit,
    onStart: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = "Vůdce malého plavidla",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )

        SectionLabel("Které sady se chci učit")
        Card {
            Column {
                Zpusobilost.entries.forEach { set ->
                    SetRow(
                        set = set,
                        count = state.counts[set] ?: 0,
                        checked = set in state.selectedSets,
                        onToggle = { onToggleSet(set) },
                    )
                }
            }
        }

        SectionLabel("Kolik otázek")
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            QuizLength.entries.forEachIndexed { index, length ->
                SegmentedButton(
                    selected = state.length == length,
                    onClick = { onLengthChange(length) },
                    shape = SegmentedButtonDefaults.itemShape(index, QuizLength.entries.size),
                ) {
                    Text(length.label)
                }
            }
        }

        Spacer(Modifier.height(4.dp))

        Button(
            onClick = onStart,
            enabled = state.canStart,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                if (state.canStart) {
                    "Začít — ${state.plannedCount} ${questionWord(state.plannedCount)}"
                } else {
                    "Vyber aspoň jednu sadu"
                }
            )
        }
    }
}

@Composable
private fun SetRow(
    set: Zpusobilost,
    count: Int,
    checked: Boolean,
    onToggle: () -> Unit,
) {
    ClickableRow(onClick = onToggle) {
        Checkbox(checked = checked, onCheckedChange = { onToggle() })
        Column(Modifier.padding(start = 4.dp)) {
            Text(
                text = "${set.code} — ${set.label}",
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                text = "$count ${questionWord(count)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

/** Celá řádka je klikatelná, ne jen checkbox — na mobilu se to lépe trefuje. */
@Composable
private fun ClickableRow(
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        content()
    }
}

/** Skloňování "otázka" podle počtu. */
internal fun questionWord(count: Int): String = when {
    count == 1 -> "otázka"
    count in 2..4 -> "otázky"
    else -> "otázek"
}
