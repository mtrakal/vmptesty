package cz.mtrakal.vmptesty.wear

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberOverscrollEffect
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.Card
import androidx.wear.compose.material3.CardDefaults
import androidx.wear.compose.material3.EdgeButton
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.ListHeaderDefaults
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.SurfaceTransformation
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.lazy.rememberTransformationSpec
import androidx.wear.compose.material3.lazy.transformedHeight
import cz.mtrakal.vmptesty.data.Answer
import cz.mtrakal.vmptesty.quiz.QuizSession
import cz.mtrakal.vmptesty.ui.fittedHeight
import cz.mtrakal.vmptesty.ui.rememberQuestionImage

private val ANSWER_LABELS = listOf("a", "b", "c")

/**
 * Otázka na hodinkách. Cílem je rychlý dril, takže se chová jinak než telefon:
 *
 * - správná odpověď na okamžik zezelená ([WearUiState.Phase.CorrectFlash])
 *   a pak sama přeskočí na další otázku, bez potvrzování
 * - chybná odpověď na okamžik zčervená ([WearUiState.Phase.WrongFlash]) a pak
 *   na obrazovce zůstane jen ta správná ([WearUiState.Phase.Correction])
 *   s tlačítkem dál
 *
 * Díky tomu je jedno klepnutí za otázku, když ji umíš, a dvě, když ne. V opravné
 * fázi posune dál klepnutí kamkoliv, ne jen na tlačítko — [EdgeButton] u spodní
 * hrany se u delších otázek dostane pod viditelnou plochu a museli bys k němu
 * scrollovat.
 */
@Composable
fun WearQuestionScreen(
    session: QuizSession,
    phase: WearUiState.Phase,
    onSelect: (Int) -> Unit,
    onNext: () -> Unit,
) {
    val item = session.current ?: return
    val listState = rememberTransformingLazyColumnState()
    val transformationSpec = rememberTransformationSpec()

    // V opravné fázi zůstává na obrazovce jen správná odpověď.
    val shownAnswers = if (phase == WearUiState.Phase.Correction) {
        listOf(item.correctIndex)
    } else {
        item.answers.indices.toList()
    }

    ScreenScaffold(
        scrollState = listState,
        edgeButton = {
            if (phase == WearUiState.Phase.Correction) {
                EdgeButton(
                    onClick = onNext,
                    // Kdyby uživatel začal scrollovat prstem z tlačítka.
                    modifier = Modifier.scrollable(
                        listState,
                        orientation = Orientation.Vertical,
                        reverseDirection = true,
                        overscrollEffect = rememberOverscrollEffect(),
                    ),
                ) {
                    Text(if (session.index == session.items.lastIndex) "Výsledek" else "Další")
                }
            }
        },
    ) { contentPadding ->
        TransformingLazyColumn(
            state = listState,
            contentPadding = contentPadding,
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (phase == WearUiState.Phase.Correction) {
                        // Bez indikace, aby klepnutí kamkoliv nevypadalo jako stisk karty.
                        Modifier.clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onNext,
                        )
                    } else {
                        Modifier
                    },
                ),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Pořadí a skóre jako ListHeader, aby dostalo horní odsazení
            // a neschovalo se pod TimeText.
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
                    QuestionProgress(session)
                }
            }

            item {
                Text(
                    text = item.question.text,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                )
            }

            item.question.image?.let { image ->
                item { QuestionPicture(name = image, maxHeight = QUESTION_IMAGE_HEIGHT) }
            }

            items(shownAnswers.size) { position ->
                val index = shownAnswers[position]
                AnswerCard(
                    label = ANSWER_LABELS.getOrElse(index) { "?" },
                    answer = item.answers[index],
                    state = answerState(session, phase, item.correctIndex, index),
                    // V opravné fázi je i karta jen dalším místem, kde jde pokračovat.
                    onClick = if (phase == WearUiState.Phase.Correction) {
                        onNext
                    } else {
                        { onSelect(index) }
                    },
                    // SurfaceTransformation je extension na scope položky seznamu,
                    // musí se tedy vytvářet až tady, ne mimo TransformingLazyColumn.
                    transformation = SurfaceTransformation(transformationSpec),
                    modifier = Modifier
                        .fillMaxWidth()
                        .minimumVerticalContentPadding(
                            CardDefaults.minimumVerticalListContentPadding,
                        )
                        .transformedHeight(this, transformationSpec),
                )
            }
        }
    }
}

/** Jak se má odpověď vykreslit. */
private enum class AnswerState { Idle, Correct, Wrong }

private fun answerState(
    session: QuizSession,
    phase: WearUiState.Phase,
    correctIndex: Int,
    index: Int,
): AnswerState = when (phase) {
    WearUiState.Phase.Asking -> AnswerState.Idle
    WearUiState.Phase.CorrectFlash ->
        if (index == session.selectedIndex) AnswerState.Correct else AnswerState.Idle
    // Během červeného bliknutí svítí jen chybná volba, správná se ukáže až v opravě.
    WearUiState.Phase.WrongFlash ->
        if (index == session.selectedIndex) AnswerState.Wrong else AnswerState.Idle
    WearUiState.Phase.Correction ->
        if (index == correctIndex) AnswerState.Correct else AnswerState.Idle
}

@Composable
private fun QuestionProgress(session: QuizSession) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "${session.questionNumber}/${session.items.size}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        ScoreDot(session.score.correct, CorrectColor)
        ScoreDot(session.score.wrong, MaterialTheme.colorScheme.error)
    }
}

/**
 * Počítadlo jako barevná tečka a číslo — stejně jako na telefonu, tedy bez
 * glyfů ✓/✗, na které se u fontu nedá spolehnout.
 */
@Composable
private fun ScoreDot(count: Int, color: Color) {
    Row(
        modifier = Modifier.padding(start = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(8.dp).background(color = color, shape = CircleShape))
        Text(
            text = "$count",
            style = MaterialTheme.typography.labelSmall,
            color = color,
            modifier = Modifier.padding(start = 3.dp),
        )
    }
}

/**
 * Obrázek k otázce. Zdrojové obrázky jsou malé (medián 150×151 px), takže se
 * musí zvětšit — výška se počítá z poměru stran, aby dostal celou šířku a
 * nezůstal v původní velikosti uprostřed rámečku.
 */
@Composable
private fun QuestionPicture(name: String, maxHeight: Int, modifier: Modifier = Modifier) {
    val bitmap = rememberQuestionImage(name) ?: return

    Box(
        modifier = modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center,
    ) {
        BoxWithConstraints {
            Image(
                bitmap = bitmap,
                contentDescription = "Obrázek k otázce",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(
                        fittedHeight(
                            imageWidth = bitmap.width,
                            imageHeight = bitmap.height,
                            availableWidth = maxWidth.value,
                            maxHeight = maxHeight,
                        ).dp,
                    ),
                contentScale = ContentScale.Fit,
                filterQuality = FilterQuality.High,
            )
        }
    }
}

@Composable
private fun AnswerCard(
    label: String,
    answer: Answer,
    state: AnswerState,
    onClick: () -> Unit,
    transformation: SurfaceTransformation,
    modifier: Modifier = Modifier,
) {
    val container = when (state) {
        AnswerState.Idle -> MaterialTheme.colorScheme.surfaceContainer
        AnswerState.Correct -> CorrectContainerColor
        AnswerState.Wrong -> MaterialTheme.colorScheme.errorContainer
    }
    val accent = when (state) {
        AnswerState.Idle -> MaterialTheme.colorScheme.onSurfaceVariant
        AnswerState.Correct -> CorrectColor
        AnswerState.Wrong -> MaterialTheme.colorScheme.error
    }

    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = container),
        transformation = transformation,
        modifier = modifier,
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = accent,
                modifier = Modifier.padding(end = 8.dp),
            )
            Column(Modifier.fillMaxWidth()) {
                if (answer.hasText) {
                    Text(text = answer.text, style = MaterialTheme.typography.bodySmall)
                }
                // Odpověď může být jen obrázková, karta pak stojí na obrázku.
                answer.image?.let { image ->
                    QuestionPicture(
                        name = image,
                        maxHeight = ANSWER_IMAGE_HEIGHT,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
        }
    }
}

private const val QUESTION_IMAGE_HEIGHT = 100
private const val ANSWER_IMAGE_HEIGHT = 56
