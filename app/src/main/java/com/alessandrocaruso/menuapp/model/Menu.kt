package com.alessandrocaruso.menuapp.model

/**
 * The seven menu sections the app renders, in presentation order.
 *
 * [jsonKeys] lists every spelling seen in the remote data set. The feed uses camelCase
 * (`firstCourses`), while earlier revisions used all-lowercase (`firstcourses`); accepting both
 * keeps the app working across data revisions instead of silently rendering an empty section.
 */
enum class CourseCategory(val jsonKeys: List<String>) {
    STARTERS(listOf("starters")),
    FIRST_COURSES(listOf("firstCourses", "firstcourses")),
    SECOND_COURSES(listOf("secondCourses", "secondcourses")),
    SIDES(listOf("sides")),
    FRUITS(listOf("fruits")),
    DESSERTS(listOf("desserts")),
    DRINKS(listOf("drinks")),
}

/** One rendered section of a menu. */
data class MenuSection(
    val category: CourseCategory,
    val courses: List<Course>,
)

/**
 * A restaurant's full menu. [sections] only contains categories that are present and non-empty
 * in the feed, so the UI never draws an empty titled row.
 */
data class Menu(
    val restaurantId: String,
    val restaurantName: String,
    val sections: List<MenuSection>,
) {
    val isEmpty: Boolean get() = sections.isEmpty()

    val allCourses: List<Course> get() = sections.flatMap { it.courses }
}
