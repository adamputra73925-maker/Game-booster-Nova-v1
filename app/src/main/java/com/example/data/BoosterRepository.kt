package com.example.data

import kotlinx.coroutines.flow.Flow

/**
 * Repository to bridge DAOs and ViewModels.
 */
class BoosterRepository(private val db: AppDatabase) {
    private val boostedAppDao = db.boostedAppDao()
    private val gameSessionDao = db.gameSessionDao()

    // --- Boosted Apps ---
    val allBoostedApps: Flow<List<BoostedApp>> = boostedAppDao.getAllBoostedApps()

    suspend fun getBoostedApp(packageName: String): BoostedApp? {
        return boostedAppDao.getBoostedAppByPackage(packageName)
    }

    suspend fun addBoostedApp(app: BoostedApp) {
        boostedAppDao.insertBoostedApp(app)
    }

    suspend fun removeBoostedApp(app: BoostedApp) {
        boostedAppDao.deleteBoostedApp(app)
    }

    suspend fun removeBoostedAppByPackage(packageName: String) {
        boostedAppDao.deleteByPackageName(packageName)
    }

    // --- Game Sessions ---
    val allSessions: Flow<List<GameSession>> = gameSessionDao.getAllSessions()

    fun getSessionsForApp(packageName: String): Flow<List<GameSession>> {
        return gameSessionDao.getSessionsForApp(packageName)
    }

    suspend fun addSession(session: GameSession) {
        gameSessionDao.insertSession(session)
    }

    suspend fun clearAllSessions() {
        gameSessionDao.clearAllSessions()
    }
}
