package cz.mtrakal.vmptesty.ui

import androidx.compose.ui.graphics.ImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.decodeToImageBitmap
import vmptesty.shared.generated.resources.Res

/**
 * Načítání bundlovaných obrázků k otázkám podle názvu souboru.
 *
 * Obrázky se nenačítají jako typované `Res.drawable.*` — názvy souborů jako
 * `221.jpg` nebo `261BA.jpg` nejsou platné Kotlin identifikátory. Čtou se proto
 * dynamicky z `files/images` podle názvu z dat.
 *
 * Sdílené mezi telefonním/desktopovým/webovým UI a hodinkami, aby se cesta
 * k resource neopisovala na dvou místech.
 */
object QuestionImages {

    /**
     * Dekódované obrázky se drží v paměti — sada je malá (5 MB zdrojových JPEG)
     * a při procvičování se stejné obrázky opakují.
     */
    private val cache = mutableMapOf<String, ImageBitmap>()

    /** Už dekódovaný obrázek, nebo null. Nesahá na disk, hodí se pro první snímek. */
    fun cached(name: String): ImageBitmap? = cache[name]

    /**
     * Načte a dekóduje obrázek. Vrací null, když soubor chybí nebo se nepovede
     * dekódovat — chybějící obrázek nemá shodit běžící test.
     *
     * Dekódování JPEG běží mimo hlavní vlákno. Volá se z `LaunchedEffect`, ten
     * běží na hlavním dispatcheru, takže bez přepnutí by se UI zaseklo u každé
     * obrázkové otázky — na hodinkách výrazně.
     *
     * @param name název souboru, např. `221.jpg`
     */
    @OptIn(ExperimentalResourceApi::class)
    suspend fun load(name: String): ImageBitmap? =
        cache[name] ?: withContext(Dispatchers.Default) {
            runCatching { Res.readBytes("$IMAGE_DIR/$name").decodeToImageBitmap() }.getOrNull()
        }?.also { cache[name] = it }

    private const val IMAGE_DIR = "files/images"
}
