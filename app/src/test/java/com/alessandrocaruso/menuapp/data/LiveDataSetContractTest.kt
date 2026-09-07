package com.alessandrocaruso.menuapp.data

import com.alessandrocaruso.menuapp.domain.MenuQrPayload
import com.alessandrocaruso.menuapp.fixture
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Contract test over the *entire* published data set, recorded from
 * `github.com/al3ssandrocaruso/restaurantsappdata` and checked in under
 * `src/test/resources/fixtures`.
 *
 * The fixtures are what keep this offline and deterministic in CI while still asserting that the
 * app can read the real feed. Refresh them with `scripts/refresh-fixtures.sh` when the data
 * changes; a failure here means the app and its data have drifted apart — exactly the class of
 * bug that left the original release rendering an empty home screen.
 */
class LiveDataSetContractTest {

    private val previews = MenuJsonParser.parseRestaurantPreviews(fixture("allpreviews.json"))

    @Test
    fun `every advertised restaurant parses`() {
        assertEquals(4, previews.size)
        assertTrue(previews.all { it.id.isNotBlank() && it.name.isNotBlank() })
        assertTrue(previews.all { it.deliveryFee > 0 })
    }

    @Test
    fun `every advertised restaurant has a readable menu`() {
        previews.forEach { preview ->
            val menu = MenuJsonParser.parseMenu(fixture("menu_${preview.id}.json"), preview.id)

            assertEquals(preview.id, menu.restaurantId)
            assertTrue("${preview.name} has no sections", menu.sections.isNotEmpty())
            assertTrue("${preview.name} has no courses", menu.allCourses.isNotEmpty())
        }
    }

    @Test
    fun `the whole data set parses to the expected number of dishes`() {
        val total = previews.sumOf {
            MenuJsonParser.parseMenu(fixture("menu_${it.id}.json"), it.id).allCourses.size
        }

        // 22 + 25 + 26 + 23 across the four published restaurants.
        assertEquals(96, total)
    }

    @Test
    fun `no dish is dropped for an unreadable price`() {
        previews.forEach { preview ->
            val menu = MenuJsonParser.parseMenu(fixture("menu_${preview.id}.json"), preview.id)
            assertTrue(
                "${preview.name} has a dish priced at zero or less",
                menu.allCourses.all { it.price > 0 },
            )
        }
    }

    @Test
    fun `a shareable QR code round-trips for every restaurant`() {
        val payload = MenuQrPayload(
            "https://github.com/al3ssandrocaruso/restaurantsappdata/raw/main/menus/PDFsMenu/"
        )

        previews.forEach { preview ->
            val decoded = payload.decode(payload.encode(preview.id))

            assertTrue(decoded is MenuQrPayload.ScanResult.MenuLink)
            assertEquals(preview.id, (decoded as MenuQrPayload.ScanResult.MenuLink).restaurantId)
        }
    }
}
