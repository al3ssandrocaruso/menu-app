package com.alessandrocaruso.menuapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.alessandrocaruso.menuapp.MenuApplication
import com.alessandrocaruso.menuapp.data.MenuDataSource
import com.alessandrocaruso.menuapp.data.favourites.FavouritesRepository
import com.alessandrocaruso.menuapp.domain.Cart
import com.alessandrocaruso.menuapp.model.Course
import com.alessandrocaruso.menuapp.model.Menu
import com.alessandrocaruso.menuapp.model.RestaurantPreview
import com.alessandrocaruso.menuapp.ui.Screen
import com.alessandrocaruso.menuapp.ui.UiState
import com.alessandrocaruso.menuapp.ui.toErrorKind
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Owns every piece of state that outlives a single composition.
 *
 * Because this is a real [ViewModel], the restaurant list, the loaded menu, the cart and the
 * current screen all survive rotation and other configuration changes — previously they lived in
 * `remember { ... }` inside `setContent` and were rebuilt (and refetched) from scratch.
 *
 * All I/O runs in [viewModelScope]: it is cancelled when the ViewModel is cleared, so no request
 * outlives the screen that asked for it.
 */
class MenuViewModel(
    private val menuRepository: MenuDataSource,
    private val favouritesRepository: FavouritesRepository,
) : ViewModel() {

    // ---- Navigation -------------------------------------------------------------------------

    private val backStack = mutableListOf<Screen>(Screen.Home)

    private val _currentScreen = MutableStateFlow<Screen>(Screen.Home)
    val currentScreen: StateFlow<Screen> = _currentScreen.asStateFlow()

    fun navigateTo(screen: Screen) {
        if (_currentScreen.value == screen) return
        backStack += screen
        _currentScreen.value = screen
    }

    /** Pops the back stack. Returns false when already at the root so the Activity can finish. */
    fun navigateBack(): Boolean {
        if (backStack.size <= 1) return false
        backStack.removeAt(backStack.lastIndex)
        _currentScreen.value = backStack.last()
        return true
    }

    // ---- Home: restaurant previews ----------------------------------------------------------

    private val _previews = MutableStateFlow<UiState<List<RestaurantPreview>>>(UiState.Loading)
    val previews: StateFlow<UiState<List<RestaurantPreview>>> = _previews.asStateFlow()

    private var previewsJob: Job? = null

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _showFavouritesOnly = MutableStateFlow(false)
    val showFavouritesOnly: StateFlow<Boolean> = _showFavouritesOnly.asStateFlow()

    /**
     * Loads the restaurant list. Safe to call repeatedly: an in-flight load is reused rather than
     * duplicated. The original code called the API directly from a composable body, firing a new
     * request on *every recomposition*.
     */
    fun loadPreviews(forceRefresh: Boolean = false) {
        if (!forceRefresh && previewsJob?.isActive == true) return
        if (!forceRefresh && _previews.value is UiState.Success) return

        previewsJob?.cancel()
        previewsJob = viewModelScope.launch {
            _previews.value = UiState.Loading
            _previews.value = menuRepository.loadPreviews().fold(
                onSuccess = { UiState.Success(it) },
                onFailure = { UiState.Error(it.toErrorKind()) },
            )
        }
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
        // Searching and the favourites filter are mutually exclusive views of the same list.
        if (query.isNotEmpty()) _showFavouritesOnly.value = false
    }

    fun toggleFavouritesFilter() {
        _showFavouritesOnly.value = !_showFavouritesOnly.value
        if (_showFavouritesOnly.value) _searchQuery.value = ""
    }

    // ---- Menu -------------------------------------------------------------------------------

    private val _menu = MutableStateFlow<UiState<Menu>>(UiState.Loading)
    val menu: StateFlow<UiState<Menu>> = _menu.asStateFlow()

    private var menuJob: Job? = null

    /** Fetches [restaurantId]'s menu with a single request and navigates to it. */
    fun openMenu(restaurantId: String) {
        navigateTo(Screen.MenuDetail(restaurantId))
        loadMenu(restaurantId)
    }

    fun loadMenu(restaurantId: String) {
        val loaded = _menu.value
        if (loaded is UiState.Success && loaded.data.restaurantId == restaurantId) return

        menuJob?.cancel()
        // Clear synchronously, before the coroutine is dispatched: otherwise the menu screen
        // renders the previously loaded restaurant's dishes for a frame after navigating.
        _menu.value = UiState.Loading
        menuJob = viewModelScope.launch {
            _menu.value = menuRepository.loadMenu(restaurantId).fold(
                onSuccess = { UiState.Success(it) },
                onFailure = { UiState.Error(it.toErrorKind()) },
            )
        }
    }

    fun retryMenu(restaurantId: String) {
        menuJob?.cancel()
        _menu.value = UiState.Loading
        loadMenu(restaurantId)
    }

    // ---- Cart -------------------------------------------------------------------------------

    private val _cart = MutableStateFlow(Cart())

    /**
     * The single source of truth for dish selection. Both the menu screen and the cart screen
     * read quantities from here, which is what makes deselecting a dish in the cart update the
     * menu counter immediately — previously each card kept its own `rememberSaveable` copy that
     * the other screen could not see.
     */
    val cart: StateFlow<Cart> = _cart.asStateFlow()

    /** Set when adding a dish would mix restaurants; the UI asks the user before clearing. */
    private val _pendingRestaurantSwitch = MutableStateFlow<Course?>(null)
    val pendingRestaurantSwitch: StateFlow<Course?> = _pendingRestaurantSwitch.asStateFlow()

    fun addCourse(course: Course) {
        when (val result = _cart.value.add(course, deliveryFeeFor(course.restaurantId))) {
            is Cart.AddResult.Updated -> _cart.value = result.cart
            is Cart.AddResult.RequiresNewRestaurant -> _pendingRestaurantSwitch.value = course
        }
    }

    fun removeCourse(course: Course) {
        _cart.value = _cart.value.remove(course)
    }

    /** Confirms the "dishes from another restaurant" prompt: empties the cart and adds the dish. */
    fun confirmRestaurantSwitch() {
        val course = _pendingRestaurantSwitch.value ?: return
        _cart.value = _cart.value.startNewOrder(course, deliveryFeeFor(course.restaurantId))
        _pendingRestaurantSwitch.value = null
    }

    fun dismissRestaurantSwitch() {
        _pendingRestaurantSwitch.value = null
    }

    /**
     * Delivery fee advertised by the restaurant in the feed, falling back to the default when the
     * previews have not loaded yet.
     */
    private fun deliveryFeeFor(restaurantId: String): Double {
        val previews = (_previews.value as? UiState.Success)?.data ?: return _cart.value.deliveryFee
        return previews.firstOrNull { it.id == restaurantId }?.deliveryFee
            ?: RestaurantPreview.DEFAULT_DELIVERY_FEE
    }

    // ---- Favourites -------------------------------------------------------------------------

    val favouriteIds: StateFlow<Set<String>> = favouritesRepository.favouriteIds
        .catch { emit(emptySet()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptySet())

    fun toggleFavourite(restaurantId: String) {
        viewModelScope.launch {
            favouritesRepository.toggle(restaurantId)
        }
    }

    // ---- QR scanning ------------------------------------------------------------------------

    /** Opens a menu that arrived from a scanned QR code, leaving the scanner screen behind. */
    fun openScannedMenu(restaurantId: String) {
        navigateBack()
        openMenu(restaurantId)
    }

    companion object {
        private const val STOP_TIMEOUT_MS = 5_000L

        /** Builds the ViewModel from the dependencies held by [MenuApplication]. */
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = checkNotNull(
                    this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]
                ) as MenuApplication
                MenuViewModel(app.menuRepository, app.favouritesRepository)
            }
        }
    }
}
