package com.alessandrocaruso.menuapp.data

import com.alessandrocaruso.menuapp.model.Menu
import com.alessandrocaruso.menuapp.model.RestaurantPreview
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * What the ViewModel needs from the data layer.
 *
 * The seam exists so state-transition tests can drive the ViewModel with scripted successes and
 * failures instead of standing up Volley and a live HTTP endpoint.
 */
interface MenuDataSource {
    suspend fun loadPreviews(): Result<List<RestaurantPreview>>
    suspend fun loadMenu(restaurantId: String): Result<Menu>
}

/**
 * The app's single entry point for restaurant data.
 *
 * Responsibilities kept here (and out of the UI): choosing the endpoint, moving JSON parsing off
 * the main thread, and converting both transport and parse failures into a [Result] the
 * ViewModel can map to a UI state. Composables never see Volley or Gson types.
 */
class MenuRepository(
    private val api: RestaurantApi,
    private val parsingDispatcher: CoroutineDispatcher = Dispatchers.Default,
) : MenuDataSource {

    override suspend fun loadPreviews(): Result<List<RestaurantPreview>> = runCatchingData {
        val body = api.fetchPreviews()
        withContext(parsingDispatcher) { MenuJsonParser.parseRestaurantPreviews(body) }
    }

    override suspend fun loadMenu(restaurantId: String): Result<Menu> = runCatchingData {
        val body = api.fetchMenu(restaurantId)
        withContext(parsingDispatcher) { MenuJsonParser.parseMenu(body, restaurantId) }
    }

    /**
     * Catches the failures this layer is expected to produce and leaves everything else alone.
     * Notably it does not swallow [kotlinx.coroutines.CancellationException], so a cancelled load
     * stays cancelled instead of being reported to the user as an error.
     */
    private inline fun <T> runCatchingData(block: () -> T): Result<T> = try {
        Result.success(block())
    } catch (e: ApiException) {
        Result.failure(e)
    } catch (e: MenuParseException) {
        Result.failure(e)
    }
}
