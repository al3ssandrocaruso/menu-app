package com.alessandrocaruso.menuapp.viewmodel

import com.alessandrocaruso.menuapp.course
import com.alessandrocaruso.menuapp.data.ApiException
import com.alessandrocaruso.menuapp.data.MenuDataSource
import com.alessandrocaruso.menuapp.data.MenuParseException
import com.alessandrocaruso.menuapp.data.favourites.FavouriteRestaurant
import com.alessandrocaruso.menuapp.data.favourites.FavouriteRestaurantDao
import com.alessandrocaruso.menuapp.data.favourites.FavouritesRepository
import com.alessandrocaruso.menuapp.model.Course
import com.alessandrocaruso.menuapp.model.CourseCategory
import com.alessandrocaruso.menuapp.model.Menu
import com.alessandrocaruso.menuapp.model.MenuSection
import com.alessandrocaruso.menuapp.model.RestaurantPreview
import com.alessandrocaruso.menuapp.ui.ErrorKind
import com.alessandrocaruso.menuapp.ui.Screen
import com.alessandrocaruso.menuapp.ui.UiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * State transitions of the screen-level state holder: loading/success/error, navigation and the
 * cross-screen cart. None of this was reachable from a test before, because the same logic lived
 * inside composable functions.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MenuViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val dataSource = FakeMenuDataSource()
    private val dao = FakeFavouriteDao()

    private fun viewModel() = MenuViewModel(dataSource, FavouritesRepository(dao))

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    // ---- Loading states ---------------------------------------------------------------------

    @Test
    fun `previews go Loading then Success`() = runTest(dispatcher) {
        dataSource.previewsResult = Result.success(listOf(preview("1")))
        val vm = viewModel()

        assertEquals(UiState.Loading, vm.previews.value)
        vm.loadPreviews()
        advanceUntilIdle()

        val state = vm.previews.value
        assertTrue(state is UiState.Success)
        assertEquals(listOf("1"), (state as UiState.Success).data.map { it.id })
    }

    @Test
    fun `a transport failure surfaces as a typed error, not a blank screen`() = runTest(dispatcher) {
        dataSource.previewsResult = Result.failure(ApiException(ApiException.Kind.NoConnection))
        val vm = viewModel()

        vm.loadPreviews()
        advanceUntilIdle()

        assertEquals(UiState.Error(ErrorKind.NoConnection), vm.previews.value)
    }

    @Test
    fun `a malformed response is reported as a parse error`() = runTest(dispatcher) {
        dataSource.previewsResult = Result.failure(MenuParseException("bad"))
        val vm = viewModel()

        vm.loadPreviews()
        advanceUntilIdle()

        assertEquals(UiState.Error(ErrorKind.Malformed), vm.previews.value)
    }

    @Test
    fun `repeated loads are coalesced instead of refetching on every call`() = runTest(dispatcher) {
        dataSource.previewsResult = Result.success(listOf(preview("1")))
        val vm = viewModel()

        repeat(5) { vm.loadPreviews() }
        advanceUntilIdle()
        repeat(5) { vm.loadPreviews() }
        advanceUntilIdle()

        assertEquals(1, dataSource.previewCalls)
    }

    @Test
    fun `retry after a failure issues a fresh request`() = runTest(dispatcher) {
        dataSource.previewsResult = Result.failure(ApiException(ApiException.Kind.Timeout))
        val vm = viewModel()

        vm.loadPreviews()
        advanceUntilIdle()
        assertEquals(UiState.Error(ErrorKind.Timeout), vm.previews.value)

        dataSource.previewsResult = Result.success(listOf(preview("2")))
        vm.loadPreviews(forceRefresh = true)
        advanceUntilIdle()

        assertEquals(2, dataSource.previewCalls)
        assertTrue(vm.previews.value is UiState.Success)
    }

    @Test
    fun `opening a menu fetches it exactly once`() = runTest(dispatcher) {
        val vm = viewModel()

        vm.openMenu("1")
        advanceUntilIdle()

        assertEquals(1, dataSource.menuCalls)
        assertEquals(Screen.MenuDetail("1"), vm.currentScreen.value)
        assertTrue(vm.menu.value is UiState.Success)
    }

    @Test
    fun `reopening the same menu reuses the loaded one`() = runTest(dispatcher) {
        val vm = viewModel()

        vm.openMenu("1")
        advanceUntilIdle()
        vm.navigateBack()
        vm.openMenu("1")
        advanceUntilIdle()

        assertEquals(1, dataSource.menuCalls)
    }

    @Test
    fun `opening a different menu refetches`() = runTest(dispatcher) {
        val vm = viewModel()

        vm.openMenu("1")
        advanceUntilIdle()
        vm.navigateBack()
        vm.openMenu("2")
        advanceUntilIdle()

        assertEquals(2, dataSource.menuCalls)
        assertEquals("2", (vm.menu.value as UiState.Success).data.restaurantId)
    }

    // ---- Navigation -------------------------------------------------------------------------

    @Test
    fun `navigation keeps a back stack instead of a hardcoded destination`() = runTest(dispatcher) {
        val vm = viewModel()

        vm.openMenu("1")
        advanceUntilIdle()
        vm.navigateTo(Screen.Cart)
        assertEquals(Screen.Cart, vm.currentScreen.value)

        assertTrue(vm.navigateBack())
        assertEquals(Screen.MenuDetail("1"), vm.currentScreen.value)
        assertTrue(vm.navigateBack())
        assertEquals(Screen.Home, vm.currentScreen.value)
    }

    @Test
    fun `back from the cart opened at home returns home, not to a menu`() = runTest(dispatcher) {
        // The original code always routed cart-back to the menu screen, even when the user had
        // opened the cart straight from home.
        val vm = viewModel()

        vm.navigateTo(Screen.Cart)
        assertTrue(vm.navigateBack())

        assertEquals(Screen.Home, vm.currentScreen.value)
    }

    @Test
    fun `back at the root is refused so the activity can finish`() = runTest(dispatcher) {
        val vm = viewModel()

        assertFalse(vm.navigateBack())
        assertEquals(Screen.Home, vm.currentScreen.value)
    }

    // ---- Cart across screens ----------------------------------------------------------------

    @Test
    fun `cart quantity is shared, so a cart change is visible to the menu`() = runTest(dispatcher) {
        val vm = viewModel()
        val nachos = course(name = "Nachos", price = 4.5, restaurantId = "1")

        vm.addCourse(nachos)
        vm.addCourse(nachos)
        assertEquals(2, vm.cart.value.quantityOf(nachos))

        vm.navigateTo(Screen.Cart)
        vm.removeCourse(nachos)

        // Same source of truth the menu screen reads.
        assertEquals(1, vm.cart.value.quantityOf(nachos))
        assertEquals(4.5, vm.cart.value.subtotal, 0.001)
    }

    @Test
    fun `adding from another restaurant raises a prompt and leaves the cart alone`() = runTest(dispatcher) {
        val vm = viewModel()
        val fromOne = course(name = "Nachos", restaurantId = "1")
        val fromTwo = course(name = "Pasta", restaurantId = "2")

        vm.addCourse(fromOne)
        vm.addCourse(fromTwo)

        assertEquals(fromTwo, vm.pendingRestaurantSwitch.value)
        assertEquals(1, vm.cart.value.itemCount)
        assertEquals("1", vm.cart.value.restaurantId)
    }

    @Test
    fun `confirming the prompt starts a new order`() = runTest(dispatcher) {
        val vm = viewModel()
        val fromOne = course(name = "Nachos", restaurantId = "1")
        val fromTwo = course(name = "Pasta", price = 9.0, restaurantId = "2")

        vm.addCourse(fromOne)
        vm.addCourse(fromTwo)
        vm.confirmRestaurantSwitch()

        assertNull(vm.pendingRestaurantSwitch.value)
        assertEquals("2", vm.cart.value.restaurantId)
        assertEquals(1, vm.cart.value.itemCount)
        assertEquals(9.0, vm.cart.value.subtotal, 0.001)
    }

    @Test
    fun `dismissing the prompt keeps the original order`() = runTest(dispatcher) {
        val vm = viewModel()

        vm.addCourse(course(name = "Nachos", restaurantId = "1"))
        vm.addCourse(course(name = "Pasta", restaurantId = "2"))
        vm.dismissRestaurantSwitch()

        assertNull(vm.pendingRestaurantSwitch.value)
        assertEquals("1", vm.cart.value.restaurantId)
    }

    @Test
    fun `the delivery fee comes from the loaded restaurant, not a hardcoded constant`() =
        runTest(dispatcher) {
            dataSource.previewsResult = Result.success(
                listOf(preview("1", deliveryFee = 1.50), preview("2", deliveryFee = 3.00))
            )
            val vm = viewModel()
            vm.loadPreviews()
            advanceUntilIdle()

            vm.addCourse(course(name = "Nachos", price = 4.50, restaurantId = "1"))

            assertEquals(1.50, vm.cart.value.deliveryFee, 0.001)
            assertEquals(6.00, vm.cart.value.total, 0.001)
        }

    // ---- Favourites -------------------------------------------------------------------------

    @Test
    fun `toggling a favourite writes and then clears it`() = runTest(dispatcher) {
        val vm = viewModel()

        vm.toggleFavourite("3")
        advanceUntilIdle()
        assertEquals(setOf("3"), dao.ids())

        vm.toggleFavourite("3")
        advanceUntilIdle()
        assertTrue(dao.ids().isEmpty())
    }

    // ---- Test doubles -----------------------------------------------------------------------

    private fun preview(id: String, deliveryFee: Double = 2.0) = RestaurantPreview(
        id = id,
        name = "Restaurant $id",
        type = "",
        poster = "",
        price = "",
        address = "",
        city = "",
        phone = "",
        deliveryFee = deliveryFee,
    )

    private class FakeMenuDataSource : MenuDataSource {
        var previewsResult: Result<List<RestaurantPreview>> = Result.success(emptyList())
        var previewCalls = 0
        var menuCalls = 0

        override suspend fun loadPreviews(): Result<List<RestaurantPreview>> {
            previewCalls++
            return previewsResult
        }

        override suspend fun loadMenu(restaurantId: String): Result<Menu> {
            menuCalls++
            return Result.success(
                Menu(
                    restaurantId = restaurantId,
                    restaurantName = "Restaurant $restaurantId",
                    sections = listOf(
                        MenuSection(
                            CourseCategory.STARTERS,
                            listOf(
                                Course("Nachos", 4.5, "", "", restaurantId),
                            ),
                        )
                    ),
                )
            )
        }
    }

    /** In-memory stand-in for the Room DAO; the DAO is an interface precisely so this is possible. */
    private class FakeFavouriteDao : FavouriteRestaurantDao {
        private val rows = MutableStateFlow<List<FavouriteRestaurant>>(emptyList())

        fun ids(): Set<String> = rows.value.map { it.id }.toSet()

        override fun observeAll(): Flow<List<FavouriteRestaurant>> = rows.map { it }

        override suspend fun isFavourite(id: String): Boolean = rows.value.any { it.id == id }

        override suspend fun insert(favourite: FavouriteRestaurant) {
            if (rows.value.none { it.id == favourite.id }) rows.value = rows.value + favourite
        }

        override suspend fun deleteById(id: String) {
            rows.value = rows.value.filterNot { it.id == id }
        }
    }
}
