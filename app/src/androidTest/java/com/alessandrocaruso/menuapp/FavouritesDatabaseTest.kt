package com.alessandrocaruso.menuapp

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.alessandrocaruso.menuapp.data.favourites.FavouritesDatabase
import com.alessandrocaruso.menuapp.data.favourites.FavouritesRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Exercises the real Room schema.
 *
 * This is the test that would have caught the original defect: the app shipped a pre-packaged
 * `FavouriteRest.db` whose only table was `Preferiti` at `user_version = 0`, while the entity
 * mapped to a different table at schema version 1, so Room threw on first access.
 */
@RunWith(AndroidJUnit4::class)
class FavouritesDatabaseTest {

    private lateinit var database: FavouritesDatabase
    private lateinit var repository: FavouritesRepository

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            FavouritesDatabase::class.java,
        ).build()
        repository = FavouritesRepository(database.favouriteRestaurantDao())
    }

    @After
    fun tearDown() = database.close()

    @Test
    fun favouritesRoundTripThroughTheDatabase() = runBlocking {
        assertTrue(repository.favouriteIds.first().isEmpty())

        repository.add("1")
        repository.add("3")
        assertEquals(setOf("1", "3"), repository.favouriteIds.first())

        repository.remove("1")
        assertEquals(setOf("3"), repository.favouriteIds.first())
    }

    @Test
    fun addingTheSameFavouriteTwiceIsIdempotent() = runBlocking {
        repository.add("2")
        repository.add("2")

        assertEquals(setOf("2"), repository.favouriteIds.first())
    }

    @Test
    fun toggleFlipsStateBasedOnWhatIsStored() = runBlocking {
        repository.toggle("5")
        assertEquals(setOf("5"), repository.favouriteIds.first())

        repository.toggle("5")
        assertTrue(repository.favouriteIds.first().isEmpty())
    }
}
