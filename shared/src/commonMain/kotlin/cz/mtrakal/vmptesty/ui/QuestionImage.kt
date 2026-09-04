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
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.decodeToImageBitmap
import vmptesty.shared.generated.resources.Res

/**
 * Obrázek k otázce nebo odpovědi.
 *
 * Obrázky se nenačítají jako typované `Res.drawable.*` — názvy souborů jako
 * `221.jpg` nebo `261BA.jpg` nejsou platné Kotlin identifikátory. Načítají se
 * proto dynamicky z `files/images` podle názvu z dat. Na webu se díky tomu
 * stahují lazy až u dané otázky.
 *
 * @param name název souboru, např. `221.jpg`
 */
@Composable
fun QuestionImage(
    name: String,
    modifier: Modifier = Modifier,
    maxHeight: Int = 260,
) {
    val bitmap = rememberImageBitmap(name)

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

@Composable
private fun rememberImageBitmap(name: String): ImageBitmap? {
    var bitmap by remember(name) { mutableStateOf(imageCache[name]) }

    LaunchedEffect(name) {
        if (bitmap == null) {
            bitmap = loadImage(name)?.also { imageCache[name] = it }
        }
    }
    return bitmap
}

@OptIn(ExperimentalResourceApi::class)
private suspend fun loadImage(name: String): ImageBitmap? = runCatching {
    Res.readBytes("files/images/$name").decodeToImageBitmap()
}.getOrNull()

/**
 * Dekódované obrázky se drží v paměti — sada obrázků je malá (5 MB zdrojových
 * JPEG) a při procvičování se stejné obrázky opakují.
 */
private val imageCache = mutableMapOf<String, ImageBitmap>()

private const val PLACEHOLDER_HEIGHT = 120
