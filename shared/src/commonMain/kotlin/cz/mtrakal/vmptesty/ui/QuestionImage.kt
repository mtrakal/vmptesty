package cz.mtrakal.vmptesty.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp

/**
 * Obrázek k otázce nebo odpovědi na telefonu, desktopu a webu.
 *
 * Obrázky ze zdroje jsou malé — medián 150×151 px, nejmenší 42×150 — takže se
 * musí výrazně zvětšit, na telefonu klidně sedmkrát. Kdyby se jen nechaly
 * vycentrovat v širokém rámečku, zůstanou v původní velikosti a nejde na nich
 * nic rozeznat.
 *
 * Výška se proto počítá z poměru stran obrázku: šířka je vždy celá dostupná,
 * výška ji následuje a stropí se na [maxHeight]. U velmi vysokých a úzkých
 * obrázků se pak uplatní výška a obrázek zůstane užší — jinak by jeden pruh
 * zabral celou obrazovku.
 *
 * @param name název souboru, např. `221.jpg`
 */
@Composable
fun QuestionImage(
    name: String,
    modifier: Modifier = Modifier,
    maxHeight: Int = 340,
) {
    val bitmap = rememberQuestionImage(name)

    BoxWithConstraints(
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
                // Zvetsuje se nekolikanasobne, s vychozi kvalitou by to kostickovalo.
                filterQuality = FilterQuality.High,
            )
        }
    }
}

/**
 * Výška, na kterou obrázek vyjde, když dostane celou dostupnou šířku.
 *
 * Zachovává poměr stran a stropí se na [maxHeight], aby vysoký úzký obrázek
 * nezabral celou obrazovku. Čistá funkce, aby šla otestovat bez vykreslování.
 */
fun fittedHeight(
    imageWidth: Int,
    imageHeight: Int,
    availableWidth: Float,
    maxHeight: Int,
): Float {
    if (imageWidth <= 0 || imageHeight <= 0) return maxHeight.toFloat()
    val ratio = imageWidth.toFloat() / imageHeight.toFloat()
    return (availableWidth / ratio).coerceAtMost(maxHeight.toFloat())
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
