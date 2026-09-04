package cz.mtrakal.vmptesty.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cz.mtrakal.vmptesty.data.Answer
import cz.mtrakal.vmptesty.quiz.QuizItem
import cz.mtrakal.vmptesty.quiz.QuizSession

private val ANSWER_LABELS = listOf("a", "b", "c")

/** Aktuální otázka s odpověďmi a okamžitou zpětnou vazbou. */
@Composable
fun QuestionScreen(
    session: QuizSession,
    onSelect: (Int) -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val item = session.current ?: return
    val scrollState = rememberScrollState()

    // Nová otázka začíná odshora, jinak by zůstal scroll z předchozí.
    LaunchedEffect(session.index) { scrollState.scrollTo(0) }

    Column(modifier = modifier.fillMaxSize()) {
        QuizHeader(session)

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Spacer(Modifier.height(4.dp))

            Text(
                text = item.question.text,
                style = MaterialTheme.typography.titleMedium,
            )

            item.question.image?.let { QuestionImage(name = it) }

            item.answers.forEachIndexed { index, answer ->
                AnswerCard(
                    label = ANSWER_LABELS.getOrElse(index) { "?" },
                    answer = answer,
                    state = answerState(session, item, index),
                    onClick = { onSelect(index) },
                )
            }

            Spacer(Modifier.height(8.dp))
        }

        AnswerFooter(session = session, onNext = onNext)
    }
}

/** Jak se má odpověď vykreslit po odpovězení. */
private enum class AnswerState { Idle, Correct, Wrong, Muted }

private fun answerState(session: QuizSession, item: QuizItem, index: Int): AnswerState = when {
    !session.isAnswered -> AnswerState.Idle
    index == item.correctIndex -> AnswerState.Correct
    index == session.selectedIndex -> AnswerState.Wrong
    else -> AnswerState.Muted
}

@Composable
private fun QuizHeader(session: QuizSession) {
    Surface(color = MaterialTheme.colorScheme.surfaceContainer) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Otázka ${session.questionNumber}/${session.items.size}",
                    style = MaterialTheme.typography.labelLarge,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ScoreChip("✓ ${session.score.correct}", correctColor())
                    ScoreChip("✗ ${session.score.wrong}", MaterialTheme.colorScheme.error)
                }
            }
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { session.index.toFloat() / session.items.size },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun ScoreChip(text: String, color: Color) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Bold,
        color = color,
    )
}

@Composable
private fun AnswerCard(
    label: String,
    answer: Answer,
    state: AnswerState,
    onClick: () -> Unit,
) {
    val container by animateColorAsState(
        when (state) {
            AnswerState.Idle -> MaterialTheme.colorScheme.surfaceContainerLow
            AnswerState.Correct -> correctContainerColor()
            AnswerState.Wrong -> MaterialTheme.colorScheme.errorContainer
            AnswerState.Muted -> MaterialTheme.colorScheme.surfaceContainerLow
        }
    )
    val border = when (state) {
        AnswerState.Correct -> correctColor()
        AnswerState.Wrong -> MaterialTheme.colorScheme.error
        else -> null
    }

    Card(
        onClick = onClick,
        // Po odpovědi jsou odpovědi zamčené, další kliknutí už skóre nemění.
        enabled = state == AnswerState.Idle,
        colors = CardDefaults.cardColors(containerColor = container),
        border = border?.let { BorderStroke(2.dp, it) },
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            AnswerBadge(label = label, state = state)
            Column(
                modifier = Modifier.padding(start = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (answer.hasText) {
                    Text(text = answer.text, style = MaterialTheme.typography.bodyLarge)
                }
                // Odpověď může být jen obrázková, karta pak stojí na obrázku.
                answer.image?.let { QuestionImage(name = it, maxHeight = 160) }
            }
        }
    }
}

@Composable
private fun AnswerBadge(label: String, state: AnswerState) {
    val color = when (state) {
        AnswerState.Correct -> correctColor()
        AnswerState.Wrong -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.outline
    }
    Surface(
        shape = CircleShape,
        color = color,
        modifier = Modifier.size(28.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = when (state) {
                    AnswerState.Correct -> "✓"
                    AnswerState.Wrong -> "✗"
                    else -> label
                },
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.surface,
            )
        }
    }
}

@Composable
private fun AnswerFooter(session: QuizSession, onNext: () -> Unit) {
    if (!session.isAnswered) return

    val isLast = session.index == session.items.lastIndex
    Surface(color = MaterialTheme.colorScheme.surfaceContainer) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Text(
                text = if (session.isSelectionCorrect) "Správně" else "Špatně",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (session.isSelectionCorrect) {
                    correctColor()
                } else {
                    MaterialTheme.colorScheme.error
                },
            )
            Spacer(Modifier.height(8.dp))
            Button(onClick = onNext, modifier = Modifier.fillMaxWidth()) {
                Text(if (isLast) "Zobrazit výsledek" else "Další otázka")
            }
        }
    }
}
