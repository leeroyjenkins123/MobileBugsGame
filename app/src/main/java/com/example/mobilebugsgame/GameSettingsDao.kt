package com.example.mobilebugsgame

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import androidx.room.Delete
import kotlinx.coroutines.flow.Flow

@Dao
interface GameSettingsDao {
    @Query("SELECT * FROM game_settings")
    fun getAllSettings(): Flow<List<GameSettingsEntity>>

    @Query("SELECT * FROM game_settings WHERE playerId = :playerId")
    fun getSettingsByPlayer(playerId: Long): Flow<GameSettingsEntity?>

    @Insert
    suspend fun insertSettings(settings: GameSettingsEntity): Long

    @Update
    suspend fun updateSettings(settings: GameSettingsEntity)

    @Delete
    suspend fun deleteSettings(settings: GameSettingsEntity)
}