package com.alessandrocaruso.menuapp

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.alessandrocaruso.menuapp.domain.Cart
import com.alessandrocaruso.menuapp.model.Course
import com.alessandrocaruso.menuapp.ui.formatPrice
import com.alessandrocaruso.menuapp.ui.layout.CartScreen
import com.alessandrocaruso.menuapp.ui.theme.MenuAppTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * A small number of UI tests covering what unit tests cannot: that the composed cart renders the
 * state it is handed and reports the user's taps back.
 *
 * Expected amounts are produced with the app's own [formatPrice] so the assertions verify the
 * values flowing through the screen rather than the device's locale.
 */
@RunWith(AndroidJUnit4::class)
class CartScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val nachos = Course("Nachos", 4.50, "", "Tortillas di mais", "1")

    @Test
    fun emptyCartShowsAMessageAndChargesNoDelivery() {
        composeRule.setContent {
            MenuAppTheme {
                CartScreen(cart = Cart(deliveryFee = 2.0), onAddCourse = {}, onRemoveCourse = {})
            }
        }

        composeRule.onNodeWithText("Your cart is empty.").assertIsDisplayed()
        // Subtotal, delivery and total are all zero until something is selected.
        composeRule.onAllNodesWithText(formatPrice(0.0)).assertCountEquals(3)
    }

    @Test
    fun cartRendersLineTotalsSubtotalAndTotal() {
        val cart = Cart(deliveryFee = 1.50).plus(nachos).plus(nachos)

        composeRule.setContent {
            MenuAppTheme { CartScreen(cart = cart, onAddCourse = {}, onRemoveCourse = {}) }
        }

        composeRule.onNodeWithText("Nachos").assertIsDisplayed()
        // Line total and subtotal are both 9.00; the total adds the 1.50 delivery fee.
        composeRule.onAllNodesWithText(formatPrice(9.00)).assertCountEquals(2)
        composeRule.onNodeWithText(formatPrice(10.50)).assertIsDisplayed()
    }

    @Test
    fun tappingMinusReportsTheDishToRemove() {
        val cart = Cart().plus(nachos)
        var removed: Course? = null

        composeRule.setContent {
            MenuAppTheme {
                CartScreen(cart = cart, onAddCourse = {}, onRemoveCourse = { removed = it })
            }
        }

        composeRule.onNodeWithContentDescription("Remove one").performClick()

        assertEquals(nachos, removed)
    }

    private fun Cart.plus(course: Course): Cart =
        (add(course) as Cart.AddResult.Updated).cart
}
