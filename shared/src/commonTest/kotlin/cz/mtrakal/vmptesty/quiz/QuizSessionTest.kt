package cz.mtrakal.vmptesty.quiz

import cz.mtrakal.vmptesty.data.Answer
import cz.mtrakal.vmptesty.data.Question
import cz.mtrakal.vmptesty.data.Zpusobilost
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class QuizSessionTest {

    @Test
    fun `filtruje pool podle vybranych zpusobilosti`() {
        val session = QuizSession.build(pool, setOf(Zpusobilost.S), QuizLength.ALL, seeded())

        assertEquals(S_COUNT, session.items.size)
        assertTrue(session.items.all { it.question.zpusobilost == Zpusobilost.S })
    }

    @Test
    fun `spoji vice vybranych zpusobilosti`() {
        val session = QuizSession.build(pool, setOf(Zpusobilost.S, Zpusobilost.M), QuizLength.ALL, seeded())

        assertEquals(S_COUNT + M_COUNT, session.items.size)
        assertEquals(
            setOf(Zpusobilost.S, Zpusobilost.M),
            session.items.map { it.question.zpusobilost }.toSet(),
        )
    }

    @Test
    fun `prazdny vyber da prazdnou sadu`() {
        val session = QuizSession.build(pool, emptySet(), QuizLength.ALL, seeded())

        assertEquals(0, session.items.size)
        assertTrue(session.isFinished)
    }

    @Test
    fun `zkrati sadu na pozadovanou delku`() {
        val session = QuizSession.build(pool, allSets, QuizLength.SHORT, seeded())

        assertEquals(20, session.items.size)
    }

    @Test
    fun `delka odpovida zkousce ma 35 otazek`() {
        val session = QuizSession.build(pool, allSets, QuizLength.EXAM, seeded())

        assertEquals(35, QuizLength.EXAM.count)
        assertEquals(35, session.items.size)
    }

    @Test
    fun `nabizene delky jsou serazene vzestupne a Vse je posledni`() {
        val counts = QuizLength.entries.map { it.count }

        assertEquals(listOf(20, 35, 50, null), counts)
    }

    @Test
    fun `kdyz je otazek malo pouzije vsechny dostupne`() {
        val session = QuizSession.build(pool, setOf(Zpusobilost.C), QuizLength.MEDIUM, seeded())

        assertEquals(C_COUNT, session.items.size, "C má jen $C_COUNT otázek, MEDIUM chce 50")
    }

    @Test
    fun `michani je pro stejny seed deterministicke a nic nezahodi`() {
        val first = QuizSession.build(pool, allSets, QuizLength.ALL, seeded())
        val second = QuizSession.build(pool, allSets, QuizLength.ALL, seeded())

        assertEquals(first.items.map { it.question.key }, second.items.map { it.question.key })
        assertEquals(pool.map { it.key }.toSet(), first.items.map { it.question.key }.toSet())
    }

    @Test
    fun `michani skutecne meni poradi otazek`() {
        val session = QuizSession.build(pool, allSets, QuizLength.ALL, seeded())

        assertTrue(
            session.items.map { it.question.key } != pool.map { it.key },
            "sada má stejné pořadí jako pool - míchání nefunguje",
        )
    }

    @Test
    fun `zamichane odpovedi zachovaji spravnou odpoved`() {
        val session = QuizSession.build(pool, allSets, QuizLength.ALL, seeded())

        session.items.forEach { item ->
            assertEquals(
                item.question.answers.toSet(),
                item.answers.toSet(),
                "${item.question.key}: zamícháním se ztratila odpověď",
            )
            assertEquals(
                item.question.correctAnswer,
                item.correctAnswer,
                "${item.question.key}: correctIndex neukazuje na správnou odpověď",
            )
        }
    }

    @Test
    fun `spravna odpoved neni vzdy na stejne pozici`() {
        val session = QuizSession.build(pool, allSets, QuizLength.ALL, seeded())

        assertTrue(
            session.items.map { it.correctIndex }.toSet().size > 1,
            "správná odpověď je pořád na stejném indexu",
        )
    }

    @Test
    fun `spravna odpoved zvysi correct`() {
        val session = startedSession()

        val answered = session.select(session.current!!.correctIndex)

        assertEquals(Score(correct = 1, wrong = 0), answered.score)
        assertTrue(answered.isAnswered)
        assertTrue(answered.isSelectionCorrect)
    }

    @Test
    fun `chybna odpoved zvysi wrong`() {
        val session = startedSession()
        val wrongIndex = (session.current!!.correctIndex + 1) % 3

        val answered = session.select(wrongIndex)

        assertEquals(Score(correct = 0, wrong = 1), answered.score)
        assertFalse(answered.isSelectionCorrect)
    }

    @Test
    fun `druhe kliknuti na stejnou otazku skore nemeni`() {
        val session = startedSession()
        val correctIndex = session.current!!.correctIndex

        val answered = session.select(correctIndex)
        val again = answered.select((correctIndex + 1) % 3)

        assertEquals(Score(correct = 1, wrong = 0), again.score)
        assertEquals(correctIndex, again.selectedIndex, "výběr se nesmí přepsat")
    }

    @Test
    fun `select mimo rozsah spadne`() {
        assertFailsWith<IllegalArgumentException> { startedSession().select(3) }
    }

    @Test
    fun `next bez odpovedi neposune`() {
        val session = startedSession()

        assertEquals(0, session.next().index)
    }

    @Test
    fun `next po odpovedi posune a zapomene vyber`() {
        val session = startedSession().select(0).next()

        assertEquals(1, session.index)
        assertNull(session.selectedIndex)
        assertFalse(session.isAnswered)
    }

    @Test
    fun `po posledni otazce je sada dokoncena`() {
        var session = QuizSession.build(pool, setOf(Zpusobilost.S), QuizLength.ALL, seeded())
        repeat(S_COUNT) { session = session.select(0).next() }

        assertTrue(session.isFinished)
        assertNull(session.current)
        assertEquals(S_COUNT, session.score.answered)
    }

    @Test
    fun `dokoncenou sadou uz nejde hybat`() {
        var session = QuizSession.build(pool, setOf(Zpusobilost.S), QuizLength.ALL, seeded())
        repeat(S_COUNT) { session = session.select(0).next() }

        assertEquals(session, session.next())
        assertEquals(session, session.select(0))
    }

    @Test
    fun `skore se rozpada podle zpusobilosti`() {
        var session = QuizSession.build(pool, allSets, QuizLength.ALL, seeded())
        val expected = mutableMapOf<Zpusobilost, Score>()

        while (!session.isFinished) {
            val item = session.current!!
            // Správně odpovíme jen na otázky ze sady M, jinak schválně mimo.
            val correct = item.question.zpusobilost == Zpusobilost.M
            val pick = if (correct) item.correctIndex else (item.correctIndex + 1) % 3
            expected[item.question.zpusobilost] =
                (expected[item.question.zpusobilost] ?: Score()).plus(correct)
            session = session.select(pick).next()
        }

        assertEquals(expected, session.scoreBySet)
        assertEquals(M_COUNT, session.score.correct)
        assertEquals(S_COUNT + C_COUNT, session.score.wrong)
    }

    @Test
    fun `procenta se pocitaji z odpovezenych`() {
        assertEquals(0, Score().percent)
        assertEquals(85, Score(correct = 17, wrong = 3).percent)
        assertEquals(100, Score(correct = 4, wrong = 0).percent)
    }

    private fun startedSession() =
        QuizSession.build(pool, allSets, QuizLength.SHORT, seeded())

    private companion object {
        const val S_COUNT = 30
        const val C_COUNT = 25
        const val M_COUNT = 40

        val allSets = Zpusobilost.entries.toSet()

        /** Fixní seed, aby byly testy míchání reprodukovatelné. */
        fun seeded() = Random(20260904)

        val pool: List<Question> = buildList {
            addAll(questions(Zpusobilost.S, S_COUNT))
            addAll(questions(Zpusobilost.C, C_COUNT))
            addAll(questions(Zpusobilost.M, M_COUNT))
        }

        fun questions(set: Zpusobilost, count: Int) = (1..count).map { number ->
            Question(
                id = number,
                zpusobilost = set,
                podsada = "${set.code}1",
                text = "${set.code} otázka $number",
                image = if (number % 5 == 0) "$number.jpg" else null,
                answers = listOf(
                    Answer("správná $number"),
                    Answer("špatná b $number"),
                    Answer(text = "", image = "$number-c.jpg"),
                ),
                correctIndex = 0,
            )
        }
    }
}
