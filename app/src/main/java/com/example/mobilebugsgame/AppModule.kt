package com.example.mobilebugsgame

import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    single { AppDatabase.getInstance(androidContext()) }

    single<GameRepository> {
        val database = get<AppDatabase>()
        GameRepository(
            database.playerDao(),
            database.gameSettingsDao(),
            database.gameResultsDao()
        )
    }

    viewModel { GameViewModel(get()) }
    viewModel { GameStateViewModel() }

    single { CbrRepository() }
}