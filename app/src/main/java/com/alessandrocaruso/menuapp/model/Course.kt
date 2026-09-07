package com.alessandrocaruso.menuapp.model

/**
 * A single dish on a restaurant menu.
 *
 * The model is immutable: the quantity a user has selected is *cart* state, not dish state,
 * and lives in [com.alessandrocaruso.menuapp.domain.Cart]. Keeping it out of here is what makes
 * the same [Course] safely shareable between the menu screen and the cart screen.
 *
 * [price] is parsed once at the data-layer boundary, so the UI never re-parses strings while
 * recomposing and a malformed price cannot crash a screen.
 */
data class Course(
    val name: String,
    val price: Double,
    val poster: String,
    val description: String,
    val restaurantId: String,
) {
    /** Stable identity for list keys and cart lookups. Names are unique within a restaurant menu. */
    val id: String get() = "$restaurantId/$name"
}
