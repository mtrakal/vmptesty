package cz.mtrakal.vmptesty.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.runtime.key
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

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        // Obrázek smí zabrat nejvýš třetinu výšky, aby se pod něj vešlo zadání
        // i odpovědi bez scrollování.
        val imageMaxHeight = (maxHeight.value * IMAGE_HEIGHT_FRACTION).toInt()
        // Obrázkové odpovědi jsou tři pod sebou, dostanou proto třetinu z toho.
        val answerImageMaxHeight = imageMaxHeight / 3

        Column(modifier = Modifier.fillMaxSize()) {
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

                item.question.image?.let { QuestionImage(name = it, maxHeight = imageMaxHeight) }

                // Klíč na index otázky: bez něj by animateColorAsState dofadeovávalo
                // zvýraznění z předchozí otázky přes novou, takže by chvíli svítila
                // "správná" odpověď, na kterou se nikdo neptal.
                key(session.index) {
                    item.answers.forEachIndexed { index, answer ->
                        AnswerCard(
                            label = ANSWER_LABELS.getOrElse(index) { "?" },
                            answer = answer,
                            state = answerState(session, item, index),
                            onClick = { onSelect(index) },
                            imageMaxHeight = answerImageMaxHeight,
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))
            }

            AnswerFooter(session = session, onNext = onNext)
        }
    }
}

/** Obrázek u otázky zabere nejvýš třetinu výšky obrazovky. */
private const val IMAGE_HEIGHT_FRACTION = 1f / 3f

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
                    ScoreChip(session.score.correct, correctColor())
                    ScoreChip(session.score.wrong, MaterialTheme.colorScheme.error)
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

/**
 * Počítadlo jako barevná tečka a číslo.
 *
 * Záměrně bez glyfů ✓/✗ — výchozí font Skii na webu je nemá a vykreslily by
 * se jako prázdné rámečky. Tečka je nakreslený tvar, ten se vykreslí vždy.
 */
@Composable
private fun ScoreChip(count: Int, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(Dimens.scoreDot)
                .background(color = color, shape = CircleShape)
        )
        Text(
            text = "$count",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = color,
            modifier = Modifier.padding(start = 6.dp),
        )
    }
}

@Composable
private fun AnswerCard(
    label: String,
    answer: Answer,
    state: AnswerState,
    onClick: () -> Unit,
    imageMaxHeight: Int,
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
        colors = CardDefaults.cardColors(containerColor = container),
        border = border?.let { BorderStroke(2.dp, it) },
        // Zámek řeší clickable, ne Card(enabled = false) - ten by ztmavil obsah
        // disabled alphou právě ve chvíli, kdy si chceš správnou odpověď přečíst.
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = state == AnswerState.Idle, onClick = onClick),
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
                answer.image?.let {
                    QuestionImage(name = it, maxHeight = imageMaxHeight)
                }
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
        modifier = Modifier.size(Dimens.answerBadge),
    ) {
        Box(contentAlignment = Alignment.Center) {
            // Vždy písmeno odpovědi - správnost už nese barva odznaku i rámeček.
            Text(
                text = label,
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
