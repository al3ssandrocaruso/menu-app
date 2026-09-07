package com.alessandrocaruso.menuapp.domain

import com.alessandrocaruso.menuapp.course
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The dish-selection rules. These were previously spread across two composables and a shared
 * mutable `Course.count`, and could not be exercised without an emulator.
 */
class CartTest {

    private val nachos = course(name = "Nachos", price = 4.50, restaurantId = "1")
    private val burger = course(name = "Smokey Smashed", price = 8.00, restaurantId = "1")
    private val pasta = course(name = "Tagliatelle", price = 9.00, restaurantId = "2")

    @Test
    fun `a new cart is empty and costs nothing`() {
        val cart = Cart()

        assertTrue(cart.isEmpty)
        assertEquals(0, cart.itemCount)
        assertEquals(0.0, cart.subtotal, 0.001)
        assertEquals(0.0, cart.total, 0.001)
        assertNull(cart.restaurantId)
    }

    @Test
    fun `adding a dish records the restaurant and one unit`() {
        val cart = Cart().addOrThrow(nachos)

        assertEquals("1", cart.restaurantId)
        assertEquals(1, cart.quantityOf(nachos))
        assertEquals(4.50, cart.subtotal, 0.001)
    }

    @Test
    fun `adding the same dish twice increments the line instead of duplicating it`() {
        val cart = Cart().addOrThrow(nachos).addOrThrow(nachos)

        assertEquals(1, cart.lines.size)
        assertEquals(2, cart.quantityOf(nachos))
        assertEquals(9.00, cart.subtotal, 0.001)
    }

    @Test
    fun `subtotal sums across different dishes`() {
        val cart = Cart().addOrThrow(nachos).addOrThrow(burger).addOrThrow(burger)

        assertEquals(3, cart.itemCount)
        assertEquals(20.50, cart.subtotal, 0.001)
    }

    @Test
    fun `total adds the delivery fee only once the cart is non-empty`() {
        val empty = Cart(deliveryFee = 1.50)
        assertEquals(0.0, empty.total, 0.001)

        val filled = empty.addOrThrow(nachos)
        assertEquals(6.00, filled.total, 0.001)
    }

    @Test
    fun `removing the last unit drops the line and clears the restaurant`() {
        val cart = Cart().addOrThrow(nachos).remove(nachos)

        assertTrue(cart.isEmpty)
        assertNull(cart.restaurantId)
        assertEquals(0.0, cart.subtotal, 0.001)
    }

    @Test
    fun `removing one of several units decrements the line`() {
        val cart = Cart().addOrThrow(nachos).addOrThrow(nachos).remove(nachos)

        assertEquals(1, cart.quantityOf(nachos))
        assertEquals("1", cart.restaurantId)
    }

    @Test
    fun `removing a dish that is not in the cart changes nothing`() {
        val cart = Cart().addOrThrow(nachos)

        assertEquals(cart, cart.remove(burger))
    }

    @Test
    fun `quantity can never go negative`() {
        val cart = Cart().remove(nachos).remove(nachos)

        assertEquals(0, cart.quantityOf(nachos))
        assertTrue(cart.isEmpty)
    }

    @Test
    fun `adding a dish from another restaurant is refused, not silently mixed`() {
        val cart = Cart().addOrThrow(nachos)

        val result = cart.add(pasta)

        assertTrue(result is Cart.AddResult.RequiresNewRestaurant)
        result as Cart.AddResult.RequiresNewRestaurant
        assertEquals("1", result.current)
        assertEquals("2", result.requested)
        // The cart itself is untouched until the user confirms.
        assertEquals(1, cart.itemCount)
    }

    @Test
    fun `confirming a restaurant switch empties the cart and starts over`() {
        val cart = Cart().addOrThrow(nachos).addOrThrow(burger).startNewOrder(pasta)

        assertEquals("2", cart.restaurantId)
        assertEquals(1, cart.lines.size)
        assertEquals(1, cart.quantityOf(pasta))
        assertEquals(0, cart.quantityOf(nachos))
        assertEquals(9.00, cart.subtotal, 0.001)
    }

    @Test
    fun `emptying the cart allows a different restaurant without a prompt`() {
        val cart = Cart().addOrThrow(nachos).remove(nachos)

        val result = cart.add(pasta)

        assertTrue(result is Cart.AddResult.Updated)
        assertEquals("2", (result as Cart.AddResult.Updated).cart.restaurantId)
    }

    @Test
    fun `the delivery fee follows the restaurant being ordered from`() {
        val cart = Cart(deliveryFee = 2.0).addOrThrow(nachos, deliveryFee = 1.50)

        assertEquals(1.50, cart.deliveryFee, 0.001)
        assertEquals(6.00, cart.total, 0.001)
    }

    @Test
    fun `dishes are identified by restaurant and name, not by object identity`() {
        val cart = Cart().addOrThrow(nachos)

        // A separately constructed but equivalent Course refers to the same line.
        val sameDish = course(name = "Nachos", price = 4.50, restaurantId = "1")
        assertEquals(1, cart.quantityOf(sameDish))
        assertTrue(cart.remove(sameDish).isEmpty)
    }

    @Test
    fun `clear empties the cart but keeps the delivery fee`() {
        val cart = Cart(deliveryFee = 3.0).addOrThrow(nachos).clear()

        assertTrue(cart.isEmpty)
        assertEquals(3.0, cart.deliveryFee, 0.001)
    }

    private fun Cart.addOrThrow(course: com.alessandrocaruso.menuapp.model.Course, deliveryFee: Double = this.deliveryFee): Cart =
        when (val result = add(course, deliveryFee)) {
            is Cart.AddResult.Updated -> result.cart
            is Cart.AddResult.RequiresNewRestaurant ->
                throw AssertionError("Unexpected restaurant conflict: $result")
        }
}
