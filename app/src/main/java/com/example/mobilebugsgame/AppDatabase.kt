package com.example.mobilebugsgame

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import android.content.Context

@Database(
    entities = [PlayerEntity::class, GameSettingsEntity::class, GameResultEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun playerDao(): PlayerDao
    abstract fun gameSettingsDao(): GameSettingsDao
    abstract fun gameResultsDao(): GameResultDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "game_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}