package cz.mtrakal.vmptesty.data

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class QuestionParserTest {

    @Test
    fun `parsuje otazku bez obrazku`() {
        val question = QuestionParser.parse(row(TEXT_ONLY)).single()

        assertEquals(412, question.id)
        assertEquals(Zpusobilost.S, question.zpusobilost)
        assertEquals("P1 2015", question.podsada)
        assertEquals("Výraz plachetnice označuje:", question.text)
        assertNull(question.image)
        assertEquals(0, question.correctIndex)
        assertEquals("loď pro plavbu pomocí plachet.", question.correctAnswer.text)
        assertEquals(listOf(null, null, null), question.answers.map { it.image })
    }

    @Test
    fun `parsuje obrazek u otazky`() {
        val question = QuestionParser.parse(row(QUESTION_IMAGE)).single()

        assertEquals("221.jpg", question.image)
        assertTrue(question.answers.all { it.image == null })
    }

    @Test
    fun `parsuje obrazky u odpovedi`() {
        val question = QuestionParser.parse(row(ANSWER_IMAGES)).single()

        assertNull(question.image)
        assertEquals(listOf("N03a.jpg", "N07.jpg", "N08.jpg"), question.answers.map { it.image })
        assertTrue(question.answers.all { it.hasText })
    }

    @Test
    fun `parsuje odpovedi jen s obrazkem bez textu`() {
        val question = QuestionParser.parse(row(IMAGE_ONLY_ANSWERS)).single()

        assertEquals(listOf("N16.jpg", "N17.jpg", "N18.jpg"), question.answers.map { it.image })
        assertTrue(question.answers.none { it.hasText })
    }

    @Test
    fun `preskoci prazdne radky`() {
        val tsv = row(TEXT_ONLY) + "\n\n" + row(QUESTION_IMAGE) + "\n"

        assertEquals(2, QuestionParser.parse(tsv).size)
    }

    @Test
    fun `spadne na spatnem poctu sloupcu`() {
        val error = assertFailsWith<IllegalArgumentException> {
            QuestionParser.parse("412\tS\tP1 2015")
        }

        assertTrue("3 sloupců" in error.message.orEmpty(), error.message.orEmpty())
    }

    @Test
    fun `spadne na nezname zpusobilosti`() {
        assertFailsWith<IllegalArgumentException> {
            QuestionParser.parse(row(TEXT_ONLY).replace("\tS\t", "\tX\t"))
        }
    }

    @Test
    fun `spadne na necislenem id`() {
        assertFailsWith<IllegalArgumentException> {
            QuestionParser.parse(row(TEXT_ONLY).replaceFirst("412", "ctyristadvanact"))
        }
    }

    @Test
    fun `spadne na prazdnem textu otazky`() {
        assertFailsWith<IllegalArgumentException> {
            QuestionParser.parse(row(TEXT_ONLY).replace("Výraz plachetnice označuje:", ""))
        }
    }

    @Test
    fun `spadne na odpovedi bez textu i obrazku`() {
        val columns = TEXT_ONLY.toMutableList()
        columns[7] = ""

        val error = assertFailsWith<IllegalArgumentException> {
            QuestionParser.parse(row(columns))
        }

        assertTrue("odpověď b" in error.message.orEmpty(), error.message.orEmpty())
    }

    private fun row(columns: List<String>) = columns.joinToString("\t")

    private companion object {
        //          id     zp   podsada     qImg   qText  aText  aImg  bText  bImg  cText  cImg
        val TEXT_ONLY = listOf(
            "412", "S", "P1 2015", "",
            "Výraz plachetnice označuje:",
            "loď pro plavbu pomocí plachet.", "",
            "jakékoli plavidlo hnané větrem.", "",
            "plavidlo využívající plochy lodního tělesa.", "",
        )
        val QUESTION_IMAGE = listOf(
            "221", "M", "PP2 2015", "221.jpg",
            "Tato signalizační světla nese:",
            "plavidlo s vlečnou soupravou.", "",
            "plavidlo stojící na mělčině.", "",
            "plavidlo provádějící práce.", "",
        )
        val ANSWER_IMAGES = listOf(
            "127", "C", "N3", "",
            "Červené válcové bóje mohou mít jako vrcholový znak:",
            "červený válec.", "N03a.jpg",
            "červenou kouli.", "N07.jpg",
            "červený kužel.", "N08.jpg",
        )
        val IMAGE_ONLY_ANSWERS = listOf(
            "134", "C", "N4", "",
            "Bóje má jako vrcholový znak:",
            "", "N16.jpg",
            "", "N17.jpg",
            "", "N18.jpg",
        )
    }
}
