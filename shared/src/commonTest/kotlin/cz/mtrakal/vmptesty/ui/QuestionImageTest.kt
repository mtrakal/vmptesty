package cz.mtrakal.vmptesty.ui

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Rozmery obrazku k otazce.
 *
 * Zdrojove obrazky ze spspraha.cz jsou male (median 150x151 px), takze se musi
 * vyrazne zvetsit. Bez toho zustanou v puvodni velikosti a nejde na nich nic
 * rozeznat - presne to byla nahlasena vada.
 */
class QuestionImageTest {

    @Test
    fun `ctvercovy obrazek dostane celou sirku`() {
        // 150x151 je median zdrojovych obrazku, 337 dp je sirka obsahu na telefonu.
        val height = fittedHeight(imageWidth = 150, imageHeight = 151, availableWidth = 337f, maxHeight = 340)

        assertEquals(339.2f, height, absoluteTolerance = 0.5f)
    }

    @Test
    fun `siroky obrazek je nizsi nez sirka`() {
        val height = fittedHeight(imageWidth = 300, imageHeight = 65, availableWidth = 337f, maxHeight = 340)

        assertEquals(73.0f, height, absoluteTolerance = 0.5f)
    }

    @Test
    fun `vysoky uzky obrazek se strop uplatni`() {
        // 42x150 je nejuzsi obrazek v sade; bez stropu by vysel pres 1200 dp.
        val height = fittedHeight(imageWidth = 42, imageHeight = 150, availableWidth = 337f, maxHeight = 340)

        assertEquals(340f, height)
    }

    @Test
    fun `strop plati i pro odpovedi s nizsim limitem`() {
        val height = fittedHeight(imageWidth = 150, imageHeight = 151, availableWidth = 337f, maxHeight = 160)

        assertEquals(160f, height)
    }

    @Test
    fun `nesmyslne rozmery spadnou na strop misto deleni nulou`() {
        assertEquals(340f, fittedHeight(0, 0, 337f, 340))
        assertEquals(340f, fittedHeight(150, 0, 337f, 340))
    }
}
