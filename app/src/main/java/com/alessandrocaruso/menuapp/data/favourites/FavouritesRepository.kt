package com.alessandrocaruso.menuapp.data.favourites

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Favourites as a set of restaurant ids.
 *
 * Writes are `suspend` and run on the caller's coroutine (the ViewModel scope), so they are tied
 * to a lifecycle instead of the previous fire-and-forget `CoroutineScope(Dispatchers.IO).launch`,
 * which outlived the screen and could never report failure.
 */
class FavouritesRepository(private val dao: FavouriteRestaurantDao) {

    val favouriteIds: Flow<Set<String>> =
        dao.observeAll().map { rows -> rows.mapTo(mutableSetOf()) { it.id } }

    suspend fun add(restaurantId: String) = dao.insert(FavouriteRestaurant(restaurantId))

    suspend fun remove(restaurantId: String) = dao.deleteById(restaurantId)

    /**
     * Flips the favourite flag for [restaurantId].
     *
     * The current value is read from the database rather than from [favouriteIds], because that
     * flow only carries data while the UI is collecting it — deciding from a cached, possibly
     * unsubscribed value made the toggle a no-op in exactly the situations tests reproduce.
     */
    suspend fun toggle(restaurantId: String) {
        if (dao.isFavourite(restaurantId)) remove(restaurantId) else add(restaurantId)
    }
}
