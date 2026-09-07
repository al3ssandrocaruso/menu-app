package com.alessandrocaruso.menuapp

import com.alessandrocaruso.menuapp.model.Course

/**
 * Loads a recorded API response from `src/test/resources/fixtures`.
 *
 * Tests read captured payloads rather than calling the live GitHub-hosted data set, so the suite
 * is deterministic and runs offline — a requirement for CI.
 */
fun fixture(name: String): String =
    checkNotNull(object {}.javaClass.getResourceAsStream("/fixtures/$name")) {
        "Missing test fixture: $name"
    }.bufferedReader().use { it.readText() }

fun course(
    name: String = "Nachos",
    price: Double = 4.5,
    restaurantId: String = "1",
): Course = Course(
    name = name,
    price = price,
    poster = "",
    description = "",
    restaurantId = restaurantId,
)
