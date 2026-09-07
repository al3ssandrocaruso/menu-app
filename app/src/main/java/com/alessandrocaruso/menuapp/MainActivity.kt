package com.alessandrocaruso.menuapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.alessandrocaruso.menuapp.domain.MenuQrPayload
import com.alessandrocaruso.menuapp.ui.Screen
import com.alessandrocaruso.menuapp.ui.components.NewRestaurantDialog
import com.alessandrocaruso.menuapp.ui.layout.CartScreen
import com.alessandrocaruso.menuapp.ui.layout.HomeScreen
import com.alessandrocaruso.menuapp.ui.layout.MenuScreen
import com.alessandrocaruso.menuapp.ui.layout.QrCodeScannerScreen
import com.alessandrocaruso.menuapp.ui.theme.MenuAppTheme
import com.alessandrocaruso.menuapp.viewmodel.MenuViewModel

/**
 * The app's only Activity. It hosts the Compose tree and nothing else — all state lives in
 * [MenuViewModel], so rotation no longer discards the loaded menu and the cart.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val qrPayload = (application as MenuApplication).qrPayload
        setContent {
            MenuAppTheme {
                MenuApp(qrPayload = qrPayload)
            }
        }
    }
}

/**
 * Routes between screens and wires each one to the ViewModel.
 *
 * Screens receive plain state and callbacks rather than the ViewModel itself, which keeps them
 * independently previewable and testable.
 */
@Composable
fun MenuApp(
    qrPayload: MenuQrPayload,
    viewModel: MenuViewModel = viewModel(factory = MenuViewModel.Factory),
) {
    val screen by viewModel.currentScreen.collectAsStateWithLifecycle()
    val previews by viewModel.previews.collectAsStateWithLifecycle()
    val menu by viewModel.menu.collectAsStateWithLifecycle()
    val cart by viewModel.cart.collectAsStateWithLifecycle()
    val favouriteIds by viewModel.favouriteIds.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val showFavouritesOnly by viewModel.showFavouritesOnly.collectAsStateWithLifecycle()
    val pendingSwitch by viewModel.pendingRestaurantSwitch.collectAsStateWithLifecycle()

    // Fetch once per ViewModel, not once per recomposition.
    LaunchedEffect(Unit) { viewModel.loadPreviews() }

    // Back is handled by the ViewModel's stack everywhere except Home, where the system default
    // (finishing the Activity) is correct.
    BackHandler(enabled = screen != Screen.Home) { viewModel.navigateBack() }

    if (pendingSwitch != null) {
        NewRestaurantDialog(
            onConfirm = viewModel::confirmRestaurantSwitch,
            onDismiss = viewModel::dismissRestaurantSwitch,
        )
    }

    when (val current = screen) {
        is Screen.Home -> HomeScreen(
            state = previews,
            searchQuery = searchQuery,
            showFavouritesOnly = showFavouritesOnly,
            favouriteIds = favouriteIds,
            onSearchQueryChange = viewModel::onSearchQueryChange,
            onToggleFavouritesFilter = viewModel::toggleFavouritesFilter,
            onOpenMenu = viewModel::openMenu,
            onOpenCart = { viewModel.navigateTo(Screen.Cart) },
            onOpenScanner = { viewModel.navigateTo(Screen.Scanner) },
            onRetry = { viewModel.loadPreviews(forceRefresh = true) },
        )

        is Screen.MenuDetail -> MenuScreen(
            state = menu,
            cart = cart,
            isFavourite = current.restaurantId in favouriteIds,
            onAddCourse = viewModel::addCourse,
            onRemoveCourse = viewModel::removeCourse,
            onToggleFavourite = { viewModel.toggleFavourite(current.restaurantId) },
            onOpenCart = { viewModel.navigateTo(Screen.Cart) },
            onRetry = { viewModel.retryMenu(current.restaurantId) },
            qrPayloadFor = qrPayload::encode,
        )

        is Screen.Cart -> CartScreen(
            cart = cart,
            onAddCourse = viewModel::addCourse,
            onRemoveCourse = viewModel::removeCourse,
        )

        is Screen.Scanner -> QrCodeScannerScreen(
            qrPayload = qrPayload,
            onOpenMenu = viewModel::openScannedMenu,
        )
    }
}
