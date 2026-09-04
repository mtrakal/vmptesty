package cz.mtrakal.vmptesty.data

import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.ExperimentalResourceApi
import vmptesty.shared.generated.resources.Res
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Ověřuje, že se bundlované resources dají za běhu skutečně načíst.
 *
 * Cesty do `composeResources/files` jsou stringy, takže je překladač
 * nezkontroluje — kdyby se soubor přesunul nebo přejmenoval, projevilo by se
 * to až prázdnou obrazovkou v běžící aplikaci. Tenhle test to zachytí dřív.
 *
 * Obrázky se tady jen čtou, ne dekódují: `decodeToImageBitmap()` potřebuje
 * nativní skiko, které na test classpath modulu `shared` není. Test tedy
 * pokrývá cestu k souboru a jeho obsah, samotné dekódování obstará Compose.
 */
@OptIn(ExperimentalResourceApi::class)
class ResourceLoadingTest {

    @Test
    fun `repository nacte otazky z bundlovaneho resource`() = runBlocking {
        val questions = QuestionRepository().load()

        assertEquals(792, questions.size)
        assertEquals(
            setOf(Zpusobilost.S, Zpusobilost.C, Zpusobilost.M),
            questions.map { it.zpusobilost }.toSet(),
        )
    }

    @Test
    fun `obrazek u otazky je ctitelny jpeg`() = runBlocking {
        val question = QuestionRepository().load().first { it.image != null }

        assertJpeg(Res.readBytes("files/images/${question.image}"), question.image!!)
    }

    @Test
    fun `obrazek u odpovedi je ctitelny jpeg`() = runBlocking {
        val answer = QuestionRepository().load()
            .flatMap { it.answers }
            .first { it.image != null }

        assertJpeg(Res.readBytes("files/images/${answer.image}"), answer.image!!)
    }

    @Test
    fun `vsechny odkazovane obrazky jsou ctitelne`() = runBlocking {
        val names = QuestionRepository().load()
            .flatMap { question -> question.answers.mapNotNull { it.image } + listOfNotNull(question.image) }
            .distinct()

        assertEquals(242, names.size)
        names.forEach { name -> assertJpeg(Res.readBytes("files/images/$name"), name) }
    }

    private fun assertJpeg(bytes: ByteArray, name: String) {
        assertTrue(bytes.size > MIN_JPEG_SIZE, "$name: jen ${bytes.size} B")
        // JPEG začíná SOI markerem FF D8 a končí EOI markerem FF D9.
        assertTrue(
            bytes[0] == JPEG_SOI_FIRST && bytes[1] == JPEG_SOI_SECOND,
            "$name: chybí JPEG SOI marker",
        )
    }

    private companion object {
        const val MIN_JPEG_SIZE = 100
        const val JPEG_SOI_FIRST = 0xFF.toByte()
        const val JPEG_SOI_SECOND = 0xD8.toByte()
    }
}
