package com.alessandrocaruso.menuapp.data.favourites

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

/** A restaurant the user marked as favourite. The id matches `RestaurantPreview.id` from the feed. */
@Entity(tableName = "favourite_restaurant")
data class FavouriteRestaurant(
    @PrimaryKey val id: String,
)

@Dao
interface FavouriteRestaurantDao {

    /**
     * Emits the current favourites and every subsequent change. Room runs the query on its own
     * executor, so collecting this never touches the database from the main thread.
     */
    @Query("SELECT * FROM favourite_restaurant ORDER BY id")
    fun observeAll(): Flow<List<FavouriteRestaurant>>

    @Query("SELECT EXISTS(SELECT 1 FROM favourite_restaurant WHERE id = :id)")
    suspend fun isFavourite(id: String): Boolean

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(favourite: FavouriteRestaurant)

    @Query("DELETE FROM favourite_restaurant WHERE id = :id")
    suspend fun deleteById(id: String)
}

/**
 * Local store for favourites.
 *
 * The original build shipped a pre-packaged `FavouriteRest.db` asset whose only table was
 * `Preferiti` with `user_version = 0`, while the entity mapped to a different table at schema
 * version 1 — Room rejected it at first access. The asset held no rows, so it is gone and Room
 * creates the (empty) table itself, which is both correct and one less binary in the repo.
 */
@Database(entities = [FavouriteRestaurant::class], version = 1, exportSchema = false)
abstract class FavouritesDatabase : RoomDatabase() {

    abstract fun favouriteRestaurantDao(): FavouriteRestaurantDao

    companion object {
        @Volatile
        private var instance: FavouritesDatabase? = null

        /** Double-checked locking; the previous unsynchronised version could build two databases. */
        fun getInstance(context: Context): FavouritesDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    FavouritesDatabase::class.java,
                    "favourites.db",
                ).build().also { instance = it }
            }
    }
}
