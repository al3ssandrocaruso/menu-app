package com.alessandrocaruso.menuapp.ui

import java.util.Locale

/**
 * Formats a price for display.
 *
 * The locale is passed explicitly rather than relying on `String.format`'s implicit default, so
 * the choice is visible at the call site and the behaviour is the same in tests.
 */
fun formatPrice(value: Double, locale: Locale = Locale.getDefault()): String =
    "€ " + String.format(locale, "%.2f", value)
