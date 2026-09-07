package com.alessandrocaruso.menuapp.data

import com.alessandrocaruso.menuapp.model.Course
import com.alessandrocaruso.menuapp.model.CourseCategory
import com.alessandrocaruso.menuapp.model.Menu
import com.alessandrocaruso.menuapp.model.MenuSection
import com.alessandrocaruso.menuapp.model.RestaurantPreview
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.JsonSyntaxException

/** Raised when a response is not valid JSON or is missing the structure the app requires. */
class MenuParseException(message: String, cause: Throwable? = null) : Exception(message, cause)

/**
 * Turns the raw JSON feed into domain models.
 *
 * Deliberately built on Gson's tree API and plain Kotlin only — no `org.json`, no Android types —
 * so the whole parsing layer runs in ordinary JVM unit tests against recorded fixtures.
 *
 * Parsing policy:
 *  - a document that is not a JSON object, or that lacks the required root array, is an error;
 *  - an individual entry that is malformed (missing name, unparseable price) is skipped rather
 *    than failing the whole response, because one bad dish should not empty a restaurant's menu;
 *  - unknown/extra fields are ignored, and both camelCase and lowercase section keys are accepted.
 */
object MenuJsonParser {

    private const val RESTAURANTS_KEY = "Restaurants"

    /** Parses the `restaurants/allpreviews` document into home-screen cards. */
    fun parseRestaurantPreviews(json: String): List<RestaurantPreview> {
        val root = readObject(json)
        val array = root.getAsJsonArrayOrNull(RESTAURANTS_KEY)
            ?: throw MenuParseException("Missing '$RESTAURANTS_KEY' array in previews response")

        // Distinct by id: these become Compose list keys, and a duplicate key is a hard crash.
        return array.mapNotNull { element ->
            val obj = element as? JsonObject ?: return@mapNotNull null
            val id = obj.string("id") ?: return@mapNotNull null
            val name = obj.string("name") ?: return@mapNotNull null
            RestaurantPreview(
                id = id,
                name = name,
                type = obj.string("type").orEmpty(),
                poster = obj.string("poster").orEmpty(),
                price = obj.string("price").orEmpty(),
                address = obj.string("address").orEmpty(),
                city = obj.string("city").orEmpty(),
                phone = obj.string("phone").orEmpty(),
                deliveryFee = obj.string("delivery")?.toDoubleOrNull()
                    ?: RestaurantPreview.DEFAULT_DELIVERY_FEE,
            )
        }.distinctBy { it.id }
    }

    /**
     * Parses a `menus/{id}` document.
     *
     * [requestedId] is the id the menu was fetched with; it is used when the payload omits its own
     * `id`, and it is what stamps every [Course] so the cart can tell menus apart.
     */
    fun parseMenu(json: String, requestedId: String): Menu {
        val root = readObject(json)
        val restaurantId = root.string("id")?.takeIf { it.isNotBlank() } ?: requestedId
        val restaurantName = root.string("ristorante") ?: root.string("restaurant") ?: ""

        val sections = CourseCategory.entries.mapNotNull { category ->
            val array = category.jsonKeys.firstNotNullOfOrNull { root.getAsJsonArrayOrNull(it) }
                ?: return@mapNotNull null
            val courses = array
                .mapNotNull { element -> parseCourse(element, restaurantId) }
                .distinctBy { it.id }
            if (courses.isEmpty()) null else MenuSection(category, courses)
        }

        if (sections.isEmpty()) {
            throw MenuParseException("Menu '$restaurantId' contains no readable course sections")
        }
        return Menu(restaurantId = restaurantId, restaurantName = restaurantName, sections = sections)
    }

    private fun parseCourse(element: JsonElement, restaurantId: String): Course? {
        val obj = element as? JsonObject ?: return null
        val name = obj.string("name")?.takeIf { it.isNotBlank() } ?: return null
        val price = obj.string("price")?.replace(',', '.')?.toDoubleOrNull() ?: return null
        if (price < 0) return null
        return Course(
            name = name,
            price = price,
            poster = obj.string("poster").orEmpty(),
            description = obj.string("description").orEmpty(),
            restaurantId = restaurantId,
        )
    }

    private fun readObject(json: String): JsonObject = try {
        JsonParser.parseString(json) as? JsonObject
            ?: throw MenuParseException("Expected a JSON object at the document root")
    } catch (e: JsonSyntaxException) {
        throw MenuParseException("Response is not valid JSON", e)
    } catch (e: IllegalStateException) {
        throw MenuParseException("Response is not valid JSON", e)
    }

    /** Reads a string field, tolerating numeric JSON values. Returns null if absent or a non-primitive. */
    private fun JsonObject.string(key: String): String? {
        val element = get(key) ?: return null
        if (!element.isJsonPrimitive) return null
        return element.asString
    }

    private fun JsonObject.getAsJsonArrayOrNull(key: String) =
        get(key)?.takeIf { it.isJsonArray }?.asJsonArray
}
