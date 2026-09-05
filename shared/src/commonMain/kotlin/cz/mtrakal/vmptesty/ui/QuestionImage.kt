package cz.mtrakal.vmptesty.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp

/**
 * Obrázek k otázce nebo odpovědi na telefonu, desktopu a webu.
 *
 * Načítání řeší [QuestionImages]; na webu se díky tomu obrázky stahují lazy
 * až u dané otázky. Hodinky mají vlastní, menší variantu.
 *
 * @param name název souboru, např. `221.jpg`
 */
@Composable
fun QuestionImage(
    name: String,
    modifier: Modifier = Modifier,
    maxHeight: Int = 260,
) {
    val bitmap = rememberQuestionImage(name)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = PLACEHOLDER_HEIGHT.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHighest),
        contentAlignment = Alignment.Center,
    ) {
        when (bitmap) {
            null -> Box(Modifier.height(PLACEHOLDER_HEIGHT.dp))
            else -> Image(
                bitmap = bitmap,
                contentDescription = "Obrázek k otázce",
                modifier = Modifier.fillMaxWidth().heightIn(max = maxHeight.dp),
                contentScale = ContentScale.Fit,
            )
        }
    }
}

/** Načte obrázek mimo kompozici; do prvního snímku vrátí to, co už je v cache. */
@Composable
fun rememberQuestionImage(name: String): ImageBitmap? {
    var bitmap by remember(name) { mutableStateOf(QuestionImages.cached(name)) }

    LaunchedEffect(name) {
        if (bitmap == null) bitmap = QuestionImages.load(name)
    }
    return bitmap
}

private const val PLACEHOLDER_HEIGHT = 120
