package cz.mtrakal.vmptesty.data

/** Způsobilost k vedení malého plavidla — jedna sada otázek. */
enum class Zpusobilost(val code: String, val label: String) {
    M("M", "Motorové plavidlo"),
    S("S", "Plachetnice"),
    C("C", "Námořní plavba"),
    ;

    companion object {
        fun ofCode(code: String): Zpusobilost? = entries.firstOrNull { it.code == code }
    }
}

/**
 * Jedna nabízená odpověď.
 *
 * Ve zdroji existují tři varianty: jen text, text + obrázek, a jen obrázek
 * (u obrázkových otázek typu "která bóje má tento vrcholový znak"). Aspoň
 * jedno z [text] a [image] je vždy neprázdné.
 *
 * @param image název souboru v `composeResources/files/images`, nebo null
 */
data class Answer(
    val text: String,
    val image: String? = null,
) {
    val hasText: Boolean get() = text.isNotEmpty()
}

/**
 * Testová otázka se třemi odpověďmi.
 *
 * Identitu tvoří pár [zpusobilost] + [id] — čísla otázek se mezi sadami opakují.
 *
 * @param podsada zkratka souboru otázek ve zdroji (např. `N3`, `PP2 2015`)
 * @param image název souboru obrázku k otázce, nebo null
 * @param correctIndex index správné odpovědi v [answers]
 */
data class Question(
    val id: Int,
    val zpusobilost: Zpusobilost,
    val podsada: String,
    val text: String,
    val image: String?,
    val answers: List<Answer>,
    val correctIndex: Int,
) {
    val key: String get() = "${zpusobilost.code}-$id"

    val correctAnswer: Answer get() = answers[correctIndex]

    init {
        require(answers.size == ANSWER_COUNT) {
            "otázka $key má ${answers.size} odpovědí, očekávány $ANSWER_COUNT"
        }
        require(correctIndex in answers.indices) {
            "otázka $key má correctIndex $correctIndex mimo rozsah ${answers.indices}"
        }
    }

    companion object {
        const val ANSWER_COUNT = 3
    }
}
