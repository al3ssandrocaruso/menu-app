package com.alessandrocaruso.menuapp.model

/**
 * Summary card shown on the home screen. [id] is the key used to fetch the full menu.
 *
 * [deliveryFee] comes from the feed rather than being hardcoded in the cart, so each restaurant
 * charges its own advertised fee.
 */
data class RestaurantPreview(
    val id: String,
    val name: String,
    val type: String,
    val poster: String,
    val price: String,
    val address: String,
    val city: String,
    val phone: String,
    val deliveryFee: Double,
) {
    companion object {
        /** Used when the feed omits a delivery fee; matches the value the cart previously hardcoded. */
        const val DEFAULT_DELIVERY_FEE = 2.0
    }
}
