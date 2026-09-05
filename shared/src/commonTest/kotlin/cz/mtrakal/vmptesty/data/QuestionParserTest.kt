package cz.mtrakal.vmptesty.data

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class QuestionParserTest {

    @Test
    fun `parsuje otazku bez obrazku`() {
        val question = QuestionParser.parse(document(TEXT_ONLY)).single()

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
        val question = QuestionParser.parse(document(QUESTION_IMAGE)).single()

        assertEquals("221.jpg", question.image)
        assertTrue(question.answers.all { it.image == null })
    }

    @Test
    fun `parsuje obrazky u odpovedi`() {
        val question = QuestionParser.parse(document(ANSWER_IMAGES)).single()

        assertNull(question.image)
        assertEquals(listOf("N03a.jpg", "N07.jpg", "N08.jpg"), question.answers.map { it.image })
        assertTrue(question.answers.all { it.hasText })
    }

    @Test
    fun `parsuje odpovedi jen s obrazkem bez textu`() {
        val question = QuestionParser.parse(document(IMAGE_ONLY_ANSWERS)).single()

        assertEquals(listOf("N16.jpg", "N17.jpg", "N18.jpg"), question.answers.map { it.image })
        assertTrue(question.answers.none { it.hasText })
    }

    @Test
    fun `spravnou odpoved urcuje priznak, ne poradi`() {
        // Stejná otázka, ale správná odpověď je až třetí v souboru.
        val question = QuestionParser.parse(document(CORRECT_LAST)).single()

        assertEquals(2, question.correctIndex)
        assertEquals("správná až na konci.", question.correctAnswer.text)
    }

    @Test
    fun `parsuje vic otazek`() {
        val questions = QuestionParser.parse(document(TEXT_ONLY, QUESTION_IMAGE))

        assertEquals(listOf(412, 221), questions.map { it.id })
    }

    @Test
    fun `ignoruje neznama pole`() {
        val json = """
            {"version":1,"neznamy":"klic","questions":[$TEXT_ONLY]}
        """.trimIndent()

        assertEquals(1, QuestionParser.parse(json).size)
    }

    @Test
    fun `spadne na nepodporovane verzi schematu`() {
        val json = """{"version":99,"questions":[$TEXT_ONLY]}"""

        val error = assertFailsWith<IllegalArgumentException> { QuestionParser.parse(json) }

        assertTrue("verzi 99" in error.message.orEmpty(), error.message.orEmpty())
    }

    @Test
    fun `spadne na nezname zpusobilosti`() {
        assertFailsWith<IllegalArgumentException> {
            QuestionParser.parse(document(TEXT_ONLY.replace("\"set\": \"S\"", "\"set\": \"X\"")))
        }
    }

    @Test
    fun `spadne na prazdnem textu otazky`() {
        assertFailsWith<IllegalArgumentException> {
            QuestionParser.parse(
                document(TEXT_ONLY.replace("\"Výraz plachetnice označuje:\"", "\"\"")),
            )
        }
    }

    @Test
    fun `spadne kdyz zadna odpoved neni spravna`() {
        val error = assertFailsWith<IllegalArgumentException> {
            QuestionParser.parse(document(TEXT_ONLY.replace("\"correct\": true", "\"correct\": false")))
        }

        assertTrue("nalezeno 0" in error.message.orEmpty(), error.message.orEmpty())
    }

    @Test
    fun `spadne kdyz je spravnych odpovedi vic`() {
        val error = assertFailsWith<IllegalArgumentException> {
            QuestionParser.parse(document(TWO_CORRECT))
        }

        assertTrue("nalezeno 2" in error.message.orEmpty(), error.message.orEmpty())
    }

    @Test
    fun `spadne na odpovedi bez textu i obrazku`() {
        val error = assertFailsWith<IllegalArgumentException> {
            QuestionParser.parse(document(EMPTY_ANSWER))
        }

        assertTrue("bez textu i obrázku" in error.message.orEmpty(), error.message.orEmpty())
    }

    private fun document(vararg questions: String) =
        """{"version":1,"source":"test","questions":[${questions.joinToString(",")}]}"""

    private companion object {

        val TEXT_ONLY = """
            {
              "id": 412, "set": "S", "subset": "P1 2015",
              "text": "Výraz plachetnice označuje:",
              "answers": [
                {"text": "loď pro plavbu pomocí plachet.", "correct": true},
                {"text": "jakékoli plavidlo hnané větrem."},
                {"text": "plavidlo využívající plochy lodního tělesa."}
              ]
            }
        """.trimIndent()

        val QUESTION_IMAGE = """
            {
              "id": 221, "set": "M", "subset": "PP2 2015",
              "text": "Tato signalizační světla nese:",
              "image": "221.jpg",
              "answers": [
                {"text": "plavidlo s vlečnou soupravou.", "correct": true},
                {"text": "plavidlo stojící na mělčině."},
                {"text": "plavidlo provádějící práce."}
              ]
            }
        """.trimIndent()

        val ANSWER_IMAGES = """
            {
              "id": 127, "set": "C", "subset": "N3",
              "text": "Červené válcové bóje mohou mít jako vrcholový znak:",
              "answers": [
                {"text": "červený válec.", "image": "N03a.jpg", "correct": true},
                {"text": "červenou kouli.", "image": "N07.jpg"},
                {"text": "červený kužel.", "image": "N08.jpg"}
              ]
            }
        """.trimIndent()

        val IMAGE_ONLY_ANSWERS = """
            {
              "id": 134, "set": "C", "subset": "N4",
              "text": "Bóje má jako vrcholový znak:",
              "answers": [
                {"text": "", "image": "N16.jpg", "correct": true},
                {"text": "", "image": "N17.jpg"},
                {"text": "", "image": "N18.jpg"}
              ]
            }
        """.trimIndent()

        val CORRECT_LAST = """
            {
              "id": 1, "set": "M", "subset": "PP1 2015",
              "text": "Otázka:",
              "answers": [
                {"text": "první."},
                {"text": "druhá."},
                {"text": "správná až na konci.", "correct": true}
              ]
            }
        """.trimIndent()

        val TWO_CORRECT = """
            {
              "id": 2, "set": "M", "subset": "PP1 2015",
              "text": "Otázka:",
              "answers": [
                {"text": "první.", "correct": true},
                {"text": "druhá.", "correct": true},
                {"text": "třetí."}
              ]
            }
        """.trimIndent()

        val EMPTY_ANSWER = """
            {
              "id": 3, "set": "M", "subset": "PP1 2015",
              "text": "Otázka:",
              "answers": [
                {"text": "první.", "correct": true},
                {"text": ""},
                {"text": "třetí."}
              ]
            }
        """.trimIndent()
    }
}
