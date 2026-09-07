package com.alessandrocaruso.menuapp

import android.app.Application
import com.android.volley.RequestQueue
import com.android.volley.toolbox.Volley
import com.alessandrocaruso.menuapp.data.MenuRepository
import com.alessandrocaruso.menuapp.data.RestaurantApi
import com.alessandrocaruso.menuapp.data.favourites.FavouritesDatabase
import com.alessandrocaruso.menuapp.data.favourites.FavouritesRepository
import com.alessandrocaruso.menuapp.domain.MenuQrPayload

/**
 * Process-wide dependency holder.
 *
 * A hand-written service locator rather than a DI framework: the graph is five objects deep, so
 * Hilt/Koin would add a plugin, annotation processing and build time without solving a problem
 * this app has. Everything is `by lazy`, so nothing is built until first use.
 *
 * The single [RequestQueue] replaces `Volley.newRequestQueue(context)` being called once per HTTP
 * request, which spun up a fresh four-thread pool and disk cache each time.
 */
class MenuApplication : Application() {

    val requestQueue: RequestQueue by lazy { Volley.newRequestQueue(this) }

    val menuRepository: MenuRepository by lazy {
        MenuRepository(RestaurantApi(requestQueue, BuildConfig.DATA_BASE_URL))
    }

    val favouritesRepository: FavouritesRepository by lazy {
        FavouritesRepository(FavouritesDatabase.getInstance(this).favouriteRestaurantDao())
    }

    val qrPayload: MenuQrPayload by lazy { MenuQrPayload(BuildConfig.MENU_PDF_BASE_URL) }
}
