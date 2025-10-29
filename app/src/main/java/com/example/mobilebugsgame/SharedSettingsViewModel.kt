package com.example.mobilebugsgame

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class SharedSettingsViewModel : ViewModel() {
    private val _settings = MutableStateFlow(Settings(1, 10, 5, 60))
    val settings: StateFlow<Settings> = _settings

    fun updateSettings(newSettings: Settings) {
        _settings.value = newSettings
    }

    // Для автоматического сохранения
    private val _shouldSaveSettings = MutableStateFlow(false)
    val shouldSaveSettings: StateFlow<Boolean> = _shouldSaveSettings

    fun enableAutoSave() {
        _shouldSaveSettings.value = true
    }

    fun disableAutoSave() {
        _shouldSaveSettings.value = false
    }
}