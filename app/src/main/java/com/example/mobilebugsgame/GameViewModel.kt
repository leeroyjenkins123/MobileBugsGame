package com.example.mobilebugsgame

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class GameViewModel(private val repository: GameRepository) : ViewModel() {

    // Работа с игроками
    fun getAllPlayers(): Flow<List<PlayerEntity>> = repository.getAllPlayers()

    fun getPlayerById(playerId: Long): Flow<PlayerEntity?> = repository.getPlayerById(playerId)

    fun getPlayerByName(fullName: String): Flow<PlayerEntity?> = repository.getPlayerByName(fullName)

    fun insertPlayer(player: PlayerEntity) = viewModelScope.launch {
        repository.insertPlayer(player)
    }

    fun updatePlayer(player: PlayerEntity) = viewModelScope.launch {
        repository.updatePlayer(player)
    }

    fun deletePlayer(player: PlayerEntity) = viewModelScope.launch {
        repository.deletePlayer(player)
    }

    // Работа с настройками
    fun getSettingsByPlayer(playerId: Long): Flow<GameSettingsEntity?> =
        repository.getSettingsByPlayer(playerId)

    fun insertSettings(settings: GameSettingsEntity) = viewModelScope.launch {
        repository.insertSettings(settings)
    }

    fun updateSettings(settings: GameSettingsEntity) = viewModelScope.launch {
        repository.updateSettings(settings)
    }

    fun deleteSettings(settings: GameSettingsEntity) = viewModelScope.launch {
        repository.deleteSettings(settings)
    }

    // Работа с результатами
    fun getResultsByPlayer(playerId: Long): Flow<List<GameResultEntity>> =
        repository.getResultsByPlayer(playerId)

    fun getTopScores(): Flow<List<GameResultEntity>> = repository.getTopScores()

    fun insertResult(result: GameResultEntity) = viewModelScope.launch {
        repository.insertResult(result)
    }

    // Регистрация игрока и настроек
    fun registerPlayerAndSettings(
        player: PlayerEntity,
        settings: GameSettingsEntity,
        onSuccess: (playerId: Long, settingsId: Long) -> Unit = { _, _ -> },
        onError: (Exception) -> Unit = {}
    ) = viewModelScope.launch {
        try {
            val (playerId, settingsId) = repository.registerPlayerAndSettings(player, settings)
            onSuccess(playerId, settingsId)
        } catch (e: Exception) {
            onError(e)
        }
    }

    // Сохранение результата игры
    fun saveGameResult(
        playerId: Long,
        settingsId: Long,
        score: Int,
        onSuccess: (Long) -> Unit = {},
        onError: (Exception) -> Unit = {}
    ) = viewModelScope.launch {
        try {
            val result = GameResultEntity(
                playerId = playerId,
                settingsId = settingsId,
                score = score,
                gameDate = System.currentTimeMillis()
            )
            val resultId = repository.insertResult(result)
            onSuccess(resultId)
        } catch (e: Exception) {
            onError(e)
        }
    }
}