package com.alessandrocaruso.menuapp.domain

import com.alessandrocaruso.menuapp.model.Course
import com.alessandrocaruso.menuapp.model.RestaurantPreview

/** One line of the order: a dish and how many of it were selected. */
data class CartLine(
    val course: Course,
    val quantity: Int,
) {
    val lineTotal: Double get() = course.price * quantity
}

/**
 * The order being composed. Pure Kotlin and immutable: every mutation returns a new [Cart],
 * so the whole selection model is unit-testable without Android and cannot drift between screens.
 *
 * Invariant: a cart only ever holds dishes from a single restaurant ([restaurantId]). Adding a
 * dish from another restaurant is reported as [AddResult.RequiresNewRestaurant] and left for the
 * caller to confirm, rather than silently mixing menus.
 */
data class Cart(
    val restaurantId: String? = null,
    val lines: List<CartLine> = emptyList(),
    val deliveryFee: Double = RestaurantPreview.DEFAULT_DELIVERY_FEE,
) {
    val isEmpty: Boolean get() = lines.isEmpty()

    val itemCount: Int get() = lines.sumOf { it.quantity }

    val subtotal: Double get() = lines.sumOf { it.lineTotal }

    /** Delivery is only charged once something has actually been selected. */
    val total: Double get() = if (isEmpty) 0.0 else subtotal + deliveryFee

    fun quantityOf(course: Course): Int =
        lines.firstOrNull { it.course.id == course.id }?.quantity ?: 0

    /** Outcome of [add]: either the updated cart, or a request to confirm switching restaurant. */
    sealed interface AddResult {
        data class Updated(val cart: Cart) : AddResult
        data class RequiresNewRestaurant(val current: String, val requested: String) : AddResult
    }

    /**
     * Adds one unit of [course]. If the cart already holds another restaurant's dishes the add is
     * refused and [AddResult.RequiresNewRestaurant] is returned; call [startNewOrder] to accept.
     */
    fun add(course: Course, deliveryFee: Double = this.deliveryFee): AddResult {
        val current = restaurantId
        if (current != null && !isEmpty && current != course.restaurantId) {
            return AddResult.RequiresNewRestaurant(current, course.restaurantId)
        }
        return AddResult.Updated(increment(course, deliveryFee))
    }

    /** Empties the cart and starts a fresh order with one unit of [course]. */
    fun startNewOrder(course: Course, deliveryFee: Double = this.deliveryFee): Cart =
        Cart(restaurantId = course.restaurantId, deliveryFee = deliveryFee).increment(course, deliveryFee)

    /**
     * Removes one unit of [course]. Dropping the last unit removes the line; emptying the cart
     * clears [restaurantId] so the next add can come from any restaurant.
     */
    fun remove(course: Course): Cart {
        val existing = lines.firstOrNull { it.course.id == course.id } ?: return this
        val updated = if (existing.quantity <= 1) {
            lines.filterNot { it.course.id == course.id }
        } else {
            lines.map { if (it.course.id == course.id) it.copy(quantity = it.quantity - 1) else it }
        }
        return copy(
            lines = updated,
            restaurantId = if (updated.isEmpty()) null else restaurantId,
        )
    }

    fun clear(): Cart = Cart(deliveryFee = deliveryFee)

    private fun increment(course: Course, deliveryFee: Double): Cart {
        val existing = lines.firstOrNull { it.course.id == course.id }
        val updated = if (existing == null) {
            lines + CartLine(course, 1)
        } else {
            lines.map { if (it.course.id == course.id) it.copy(quantity = it.quantity + 1) else it }
        }
        return copy(
            restaurantId = course.restaurantId,
            lines = updated,
            deliveryFee = deliveryFee,
        )
    }
}
