package com.example.mobilebugsgame

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull

class GameRepository(
    private val playerDao: PlayerDao,
    private val gameSettingsDao: GameSettingsDao,
    private val gameResultDao: GameResultDao
) {
    // Работа с игроками
    fun getAllPlayers(): Flow<List<PlayerEntity>> = playerDao.getAllPlayers()

    fun getPlayerById(playerId: Long): Flow<PlayerEntity?> = playerDao.getPlayerById(playerId)

    fun getPlayerByName(fullName: String): Flow<PlayerEntity?> = playerDao.getPlayerByName(fullName)

    suspend fun insertPlayer(player: PlayerEntity): Long {
        return playerDao.insertPlayer(player)
    }

    suspend fun updatePlayer(player: PlayerEntity) {
        playerDao.updatePlayer(player)
    }

    suspend fun deletePlayer(player: PlayerEntity) {
        playerDao.deletePlayer(player)
    }

    // Работа с настройками
    fun getSettingsByPlayer(playerId: Long): Flow<GameSettingsEntity?> =
        gameSettingsDao.getSettingsByPlayer(playerId)

    suspend fun insertSettings(settings: GameSettingsEntity): Long {
        return gameSettingsDao.insertSettings(settings)
    }

    suspend fun updateSettings(settings: GameSettingsEntity) {
        gameSettingsDao.updateSettings(settings)
    }

    suspend fun deleteSettings(settings: GameSettingsEntity) {
        gameSettingsDao.deleteSettings(settings)
    }

    // Работа с результатами
    fun getResultsByPlayer(playerId: Long): Flow<List<GameResultEntity>> =
        gameResultDao.getResultsByPlayer(playerId)

    fun getTopScores(): Flow<List<GameResultEntity>> = gameResultDao.getTopScores()

    suspend fun insertResult(result: GameResultEntity): Long {
        return gameResultDao.insertResult(result)
    }

    // Регистрация игрока и настроек
    suspend fun registerPlayerAndSettings(
        player: PlayerEntity,
        settings: GameSettingsEntity
    ): Pair<Long, Long> {
        val playerId = playerDao.insertPlayer(player)
        val updatedSettings = settings.copy(playerId = playerId)
        val settingsId = gameSettingsDao.insertSettings(updatedSettings)
        return Pair(playerId, settingsId)
    }
}