package cz.mtrakal.vmptesty.quiz

import cz.mtrakal.vmptesty.data.Answer
import cz.mtrakal.vmptesty.data.Question
import cz.mtrakal.vmptesty.data.Zpusobilost
import kotlin.random.Random

/**
 * Kolik otázek má vygenerovaná sada mít. Pořadí konstant určuje pořadí v UI.
 *
 * [EXAM] je 35 otázek, což odpovídá rozsahu ostrého testu.
 */
enum class QuizLength(val count: Int?, val label: String) {
    SHORT(20, "20"),
    EXAM(35, "35"),
    MEDIUM(50, "50"),
    ALL(null, "Vše"),
}

/**
 * Otázka připravená k zobrazení — odpovědi už jsou zamíchané.
 *
 * Ve zdroji je správná odpověď vždy `a)`, takže bez zamíchání by se dala
 * uhádnout podle pozice. Míchá se jednou při stavbě sady, aby se pořadí
 * neměnilo při každé rekompozici.
 */
data class QuizItem(
    val question: Question,
    val answers: List<Answer>,
    val correctIndex: Int,
) {
    val correctAnswer: Answer get() = answers[correctIndex]
}

/** Skóre za běh — správně, špatně, úspěšnost. */
data class Score(val correct: Int = 0, val wrong: Int = 0) {
    val answered: Int get() = correct + wrong
    val percent: Int get() = if (answered == 0) 0 else correct * 100 / answered

    fun plus(wasCorrect: Boolean): Score =
        if (wasCorrect) copy(correct = correct + 1) else copy(wrong = wrong + 1)
}

/**
 * Stav jednoho průchodu sadou otázek. Immutable — každá akce vrací nový stav.
 *
 * Skóre se počítá jen z první odpovědi na otázku; opakované kliknutí
 * (nebo kliknutí po odpovědi) je no-op.
 */
data class QuizSession(
    val items: List<QuizItem>,
    val index: Int = 0,
    val selectedIndex: Int? = null,
    val score: Score = Score(),
    val scoreBySet: Map<Zpusobilost, Score> = emptyMap(),
) {
    val isFinished: Boolean get() = index >= items.size
    val current: QuizItem? get() = items.getOrNull(index)
    val isAnswered: Boolean get() = selectedIndex != null

    /** Číslo aktuální otázky pro zobrazení, tedy od 1. */
    val questionNumber: Int get() = (index + 1).coerceAtMost(items.size)

    /** True, když už je odpovězeno a odpověď byla správná. */
    val isSelectionCorrect: Boolean
        get() = selectedIndex != null && selectedIndex == current?.correctIndex

    /** Zaznamená odpověď na aktuální otázku. Po odpovědi už se nedá přepsat. */
    fun select(answerIndex: Int): QuizSession {
        val item = current ?: return this
        if (isAnswered) return this
        require(answerIndex in item.answers.indices) {
            "index odpovědi $answerIndex je mimo rozsah ${item.answers.indices}"
        }

        val wasCorrect = answerIndex == item.correctIndex
        val set = item.question.zpusobilost
        return copy(
            selectedIndex = answerIndex,
            score = score.plus(wasCorrect),
            scoreBySet = scoreBySet + (set to (scoreBySet[set] ?: Score()).plus(wasCorrect)),
        )
    }

    /** Posun na další otázku. Bez odpovědi se přeskočit nedá. */
    fun next(): QuizSession {
        if (isFinished || !isAnswered) return this
        return copy(index = index + 1, selectedIndex = null)
    }

    companion object {

        /**
         * Postaví zamíchanou sadu z otázek vybraných způsobilostí.
         *
         * Když je dostupných otázek méně, než požaduje [length], použije se
         * všechno, co je.
         */
        fun build(
            pool: List<Question>,
            sets: Set<Zpusobilost>,
            length: QuizLength,
            random: Random = Random.Default,
        ): QuizSession {
            val shuffled = pool.filter { it.zpusobilost in sets }.shuffled(random)
            val limited = length.count?.let(shuffled::take) ?: shuffled
            return QuizSession(items = limited.map { it.toItem(random) })
        }

        private fun Question.toItem(random: Random): QuizItem {
            val order = answers.indices.shuffled(random)
            return QuizItem(
                question = this,
                answers = order.map(answers::get),
                correctIndex = order.indexOf(correctIndex),
            )
        }
    }
}
