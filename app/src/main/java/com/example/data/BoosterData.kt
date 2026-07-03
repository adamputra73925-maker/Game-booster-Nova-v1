package com.example.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

/**
 * Entity representing an application added to the "Boosted Apps" list.
 */
@Entity(tableName = "boosted_apps")
data class BoostedApp(
    @PrimaryKey val packageName: String,
    val appName: String,
    val boostMode: String = "BALANCED", // "PERFORMANCE", "BATTERY", "BALANCED"
    val addedAt: Long = System.currentTimeMillis()
)

/**
 * Entity representing a tracking session when an app is boosted.
 */
@Entity(tableName = "game_sessions")
data class GameSession(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val packageName: String,
    val appName: String,
    val startTime: Long,
    val durationMs: Long,
    val ramCleanedMb: Int,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Data Access Object for Boosted Apps.
 */
@Dao
interface BoostedAppDao {
    @Query("SELECT * FROM boosted_apps ORDER BY addedAt DESC")
    fun getAllBoostedApps(): Flow<List<BoostedApp>>

    @Query("SELECT * FROM boosted_apps WHERE packageName = :packageName LIMIT 1")
    suspend fun getBoostedAppByPackage(packageName: String): BoostedApp?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBoostedApp(app: BoostedApp)

    @Delete
    suspend fun deleteBoostedApp(app: BoostedApp)

    @Query("DELETE FROM boosted_apps WHERE packageName = :packageName")
    suspend fun deleteByPackageName(packageName: String)
}

/**
 * Data Access Object for Tracking Statistics / Game Sessions.
 */
@Dao
interface GameSessionDao {
    @Query("SELECT * FROM game_sessions ORDER BY timestamp DESC")
    fun getAllSessions(): Flow<List<GameSession>>

    @Query("SELECT * FROM game_sessions WHERE packageName = :packageName ORDER BY timestamp DESC")
    fun getSessionsForApp(packageName: String): Flow<List<GameSession>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: GameSession)

    @Query("DELETE FROM game_sessions")
    suspend fun clearAllSessions()
}

/**
 * The main Room Database instance for Game Booster.
 */
@Database(entities = [BoostedApp::class, GameSession::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun boostedAppDao(): BoostedAppDao
    abstract fun gameSessionDao(): GameSessionDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "game_booster_db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
