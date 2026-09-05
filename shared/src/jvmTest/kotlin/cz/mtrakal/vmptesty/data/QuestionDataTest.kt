package cz.mtrakal.vmptesty.data

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Integrita reálných bundlovaných dat.
 *
 * Běží jen na JVM, protože potřebuje filesystem — díky tomu umí ověřit i to,
 * že každý obrázek odkazovaný z dat skutečně existuje jako soubor. Tenhle test
 * je záchytná síť proti tomu, aby regenerace přes `tools/scrape.py` tiše
 * rozbila data.
 */
class QuestionDataTest {

    private val questions: List<Question> = QuestionParser.parse(dataFile().readText())

    @Test
    fun `obsahuje vsech 792 otazek se spravnymi pocty na sadu`() {
        assertEquals(792, questions.size)
        assertEquals(
            mapOf(Zpusobilost.S to 170, Zpusobilost.C to 215, Zpusobilost.M to 407),
            questions.groupingBy { it.zpusobilost }.eachCount(),
        )
    }

    @Test
    fun `kazda otazka ma tri pouzitelne odpovedi`() {
        questions.forEach { question ->
            assertEquals(3, question.answers.size, question.key)
            assertTrue(question.correctIndex in question.answers.indices, question.key)
            question.answers.forEach { answer ->
                assertTrue(
                    answer.hasText || answer.image != null,
                    "${question.key}: odpověď bez textu i obrázku",
                )
            }
        }
    }

    @Test
    fun `par zpusobilost a id je unikatni`() {
        val duplicates = questions.groupingBy { it.key }.eachCount().filterValues { it > 1 }

        assertEquals(emptyMap(), duplicates)
    }

    @Test
    fun `zachovava vsechny tri varianty obrazku`() {
        assertEquals(224, questions.count { it.image != null }, "otázek s obrázkem")
        assertEquals(
            16,
            questions.count { question -> question.answers.any { it.image != null } },
            "otázek s obrázky u odpovědí",
        )
        assertEquals(
            21,
            questions.sumOf { question -> question.answers.count { !it.hasText } },
            "odpovědí jen s obrázkem",
        )
    }

    @Test
    fun `vsechny odkazovane obrazky existuji jako soubory`() {
        val referenced = questions
            .flatMap { question -> question.answers.mapNotNull { it.image } + listOfNotNull(question.image) }
            .toSortedSet()
        val onDisk = imagesDir().listFiles().orEmpty().map { it.name }.toSortedSet()

        assertEquals(242, referenced.size, "unikátních odkazů na obrázky")
        assertEquals(emptySet(), referenced - onDisk, "odkazované, ale chybějící obrázky")
        assertEquals(emptySet(), onDisk - referenced, "obrázky, které žádná otázka nepoužívá")
        assertTrue(referenced.all { it.endsWith(".jpg") }, "neočekávaná přípona")
    }

    private companion object {

        /** Pracovní adresář testu se mezi Gradle a IDE liší, resource se proto hledá nahoru. */
        fun resourcesDir(): File {
            val relative = "shared/src/commonMain/composeResources/files"
            var dir: File? = File(".").absoluteFile
            while (dir != null) {
                File(dir, relative).takeIf { it.isDirectory }?.let { return it }
                File(dir, relative.removePrefix("shared/")).takeIf { it.isDirectory }?.let { return it }
                dir = dir.parentFile
            }
            error("nenalezen adresář $relative")
        }

        fun dataFile(): File = File(resourcesDir(), "questions.json")

        fun imagesDir(): File = File(resourcesDir(), "images")
    }
}
