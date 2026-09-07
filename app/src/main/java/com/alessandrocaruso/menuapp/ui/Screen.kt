package com.alessandrocaruso.menuapp.ui

/**
 * The screens the app can show.
 *
 * Replaces the previous `ScreenRouter` global (`object` holding a `MutableState<Int>`): a process
 * -wide singleton meant the destination was a magic number, survived Activity destruction with
 * stale values, and could not be tested or restored. Destinations are now a closed set held by
 * the ViewModel alongside a real back stack.
 */
sealed interface Screen {
    data object Home : Screen
    data class MenuDetail(val restaurantId: String) : Screen
    data object Cart : Screen
    data object Scanner : Screen
}
