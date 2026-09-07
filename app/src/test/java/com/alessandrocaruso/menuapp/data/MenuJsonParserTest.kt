package com.alessandrocaruso.menuapp.data

import com.alessandrocaruso.menuapp.fixture
import com.alessandrocaruso.menuapp.model.CourseCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Parsing tests run against payloads recorded from the live data set, which is how the
 * section-name mismatch that emptied every menu was found: the app asked for `firstcourses`
 * while the feed has always served `firstCourses`.
 */
class MenuJsonParserTest {

    // ---- Previews ---------------------------------------------------------------------------

    @Test
    fun `parses every restaurant from the recorded previews response`() {
        val previews = MenuJsonParser.parseRestaurantPreviews(fixture("allpreviews.json"))

        assertEquals(4, previews.size)
        assertEquals(listOf("0", "1", "2", "3"), previews.map { it.id })

        val smashBurger = previews.first { it.id == "1" }
        assertEquals("Smash Burger", smashBurger.name)
        assertEquals("Aus Burger", smashBurger.type)
        assertEquals("Roma", smashBurger.city)
        assertEquals(1.50, smashBurger.deliveryFee, 0.001)
    }

    @Test
    fun `previews parse without a totalResults field`() {
        // The original code read `totalResults` before anything else and aborted the whole parse
        // when it was absent. It is absent from the live feed, so the home screen never populated.
        val json = """{"Restaurants":[{"id":"7","name":"Solo"}]}"""

        val previews = MenuJsonParser.parseRestaurantPreviews(json)

        assertEquals(1, previews.size)
        assertEquals("Solo", previews[0].name)
    }

    @Test
    fun `preview without a delivery fee falls back to the default`() {
        val json = """{"Restaurants":[{"id":"7","name":"Solo"}]}"""

        val fee = MenuJsonParser.parseRestaurantPreviews(json)[0].deliveryFee

        assertEquals(2.0, fee, 0.001)
    }

    @Test
    fun `entries missing an id or a name are skipped, valid neighbours survive`() {
        val json = """
            {"Restaurants":[
              {"name":"No id"},
              {"id":"2"},
              {"id":"3","name":"Good"}
            ]}
        """.trimIndent()

        val previews = MenuJsonParser.parseRestaurantPreviews(json)

        assertEquals(listOf("3"), previews.map { it.id })
    }

    @Test
    fun `duplicate ids are collapsed so Compose list keys stay unique`() {
        // A duplicate key in a LazyRow is a hard crash, so the data layer guarantees uniqueness.
        val json = """{"Restaurants":[
              {"id":"1","name":"First"},
              {"id":"1","name":"Repeat"},
              {"id":"2","name":"Second"}
            ]}"""

        val previews = MenuJsonParser.parseRestaurantPreviews(json)

        assertEquals(listOf("1", "2"), previews.map { it.id })
        assertEquals("First", previews[0].name)
    }

    @Test
    fun `duplicate dish names within a section are collapsed`() {
        val json = """{"id":"1","starters":[
              {"name":"Nachos","price":"4.50"},
              {"name":"Nachos","price":"5.00"}
            ]}"""

        val courses = MenuJsonParser.parseMenu(json, requestedId = "1").allCourses

        assertEquals(1, courses.size)
        assertEquals(4.50, courses[0].price, 0.001)
    }

    @Test(expected = MenuParseException::class)
    fun `previews response without the Restaurants array is an error`() {
        MenuJsonParser.parseRestaurantPreviews("""{"something":"else"}""")
    }

    @Test(expected = MenuParseException::class)
    fun `previews response that is not JSON is an error`() {
        MenuJsonParser.parseRestaurantPreviews("404: Not Found")
    }

    @Test(expected = MenuParseException::class)
    fun `previews response that is a JSON array is an error`() {
        MenuJsonParser.parseRestaurantPreviews("""[{"id":"1"}]""")
    }

    // ---- Menus ------------------------------------------------------------------------------

    @Test
    fun `parses all seven sections from the recorded menu response`() {
        val menu = MenuJsonParser.parseMenu(fixture("menu_1.json"), requestedId = "1")

        assertEquals("1", menu.restaurantId)
        assertEquals("Smash Burger", menu.restaurantName)
        assertEquals(
            listOf(
                CourseCategory.STARTERS,
                CourseCategory.FIRST_COURSES,
                CourseCategory.SECOND_COURSES,
                CourseCategory.SIDES,
                CourseCategory.FRUITS,
                CourseCategory.DESSERTS,
                CourseCategory.DRINKS,
            ),
            menu.sections.map { it.category },
        )
        assertEquals(25, menu.allCourses.size)
    }

    @Test
    fun `reads the camelCase section keys the live feed actually serves`() {
        val menu = MenuJsonParser.parseMenu(fixture("menu_1.json"), requestedId = "1")

        val firstCourses = menu.sections.single { it.category == CourseCategory.FIRST_COURSES }
        assertEquals(5, firstCourses.courses.size)
        assertEquals("Smokey Smashed", firstCourses.courses[0].name)
    }

    @Test
    fun `also reads the all-lowercase section keys used by older data revisions`() {
        val json = """
            {"id":"9","ristorante":"Legacy",
             "firstcourses":[{"name":"Pasta","price":"7.00"}],
             "secondcourses":[{"name":"Pesce","price":"12.00"}]}
        """.trimIndent()

        val menu = MenuJsonParser.parseMenu(json, requestedId = "9")

        assertEquals(2, menu.sections.size)
        assertEquals("Pasta", menu.sections[0].courses[0].name)
    }

    @Test
    fun `prices are parsed to numbers once, at the data boundary`() {
        val menu = MenuJsonParser.parseMenu(fixture("menu_1.json"), requestedId = "1")

        val nachos = menu.allCourses.single { it.name == "Nachos" }
        assertEquals(4.50, nachos.price, 0.001)
    }

    @Test
    fun `every parsed course carries the restaurant id`() {
        val menu = MenuJsonParser.parseMenu(fixture("menu_1.json"), requestedId = "1")

        assertTrue(menu.allCourses.all { it.restaurantId == "1" })
    }

    @Test
    fun `a course with an unparseable price is skipped, the rest of the section survives`() {
        val json = """
            {"id":"1","starters":[
              {"name":"Good","price":"3.50"},
              {"name":"Bad","price":"free"},
              {"name":"Nameless price","price":"4.00"}
            ]}
        """.trimIndent()

        val courses = MenuJsonParser.parseMenu(json, requestedId = "1").allCourses

        assertEquals(listOf("Good", "Nameless price"), courses.map { it.name })
    }

    @Test
    fun `a comma decimal separator is accepted`() {
        val json = """{"id":"1","starters":[{"name":"Bruschette","price":"5,50"}]}"""

        val course = MenuJsonParser.parseMenu(json, requestedId = "1").allCourses.single()

        assertEquals(5.50, course.price, 0.001)
    }

    @Test
    fun `empty sections are dropped so the UI never renders an empty heading`() {
        val json = """{"id":"1","starters":[{"name":"X","price":"1.00"}],"desserts":[]}"""

        val menu = MenuJsonParser.parseMenu(json, requestedId = "1")

        assertEquals(listOf(CourseCategory.STARTERS), menu.sections.map { it.category })
    }

    @Test
    fun `the requested id is used when the payload omits its own`() {
        val json = """{"starters":[{"name":"X","price":"1.00"}]}"""

        val menu = MenuJsonParser.parseMenu(json, requestedId = "42")

        assertEquals("42", menu.restaurantId)
        assertEquals("42", menu.allCourses.single().restaurantId)
    }

    @Test(expected = MenuParseException::class)
    fun `a menu with no readable sections is an error rather than a blank screen`() {
        MenuJsonParser.parseMenu("""{"id":"1","ristorante":"Empty"}""", requestedId = "1")
    }

    @Test
    fun `course identity combines restaurant and dish name`() {
        val menu = MenuJsonParser.parseMenu(fixture("menu_1.json"), requestedId = "1")

        val ids = menu.allCourses.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
        assertNotNull(ids.firstOrNull { it == "1/Nachos" })
        assertNull(ids.firstOrNull { it.startsWith("2/") })
    }
}
