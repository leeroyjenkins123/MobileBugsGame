package com.example.mobilebugsgame

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import androidx.room.Delete
import kotlinx.coroutines.flow.Flow

@Dao
interface GameResultDao {
    @Query("SELECT * FROM game_results")
    fun getAllResults(): Flow<List<GameResultEntity>>

    @Query("SELECT * FROM game_results WHERE playerId = :playerId")
    fun getResultsByPlayer(playerId: Long): Flow<List<GameResultEntity>>

    @Query("SELECT * FROM game_results WHERE settingsId = :settingsId")
    fun getResultsBySettings(settingsId: Long): Flow<List<GameResultEntity>>

    @Insert
    suspend fun insertResult(result: GameResultEntity): Long

    @Update
    suspend fun updateResult(result: GameResultEntity)

    @Delete
    suspend fun deleteResult(result: GameResultEntity)

    @Query("SELECT * FROM game_results ORDER BY score DESC LIMIT 10")
    fun getTopScores(): Flow<List<GameResultEntity>>
}
