package com.example.mobilebugsgame

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class GameStateViewModel : ViewModel() {

    private val _gameState = MutableStateFlow(GameState())
    val gameState = _gameState.asStateFlow()

    private val _activeInsects = MutableStateFlow<List<InsectData>>(emptyList())
    val activeInsects = _activeInsects.asStateFlow()

    fun updateScore(points: Int) {
        _gameState.value = _gameState.value.copy(
            score = _gameState.value.score + points
        )
    }

    fun decreaseLives() {
        _gameState.value = _gameState.value.copy(
            lives = _gameState.value.lives - 1,
            score = (_gameState.value.score - 2).coerceAtLeast(0)
        )
    }

    fun resetGame() {
        _gameState.value = GameState()
        _activeInsects.value = emptyList()
    }

    fun initializeGame(player: Player, settings: Settings) {
        _gameState.value = GameState(
            player = player,
            settings = settings,
            lives = 5,
            score = 0
        )
    }

    fun addInsect(insect: InsectData) {
        _activeInsects.value = _activeInsects.value + insect
    }

    fun removeInsect(insectId: String) {
        _activeInsects.value = _activeInsects.value.filter { it.id != insectId }
    }

    fun clearInsects() {
        _activeInsects.value = emptyList()
    }

    fun setIsGameActive(isActive: Boolean) {
        _gameState.value = _gameState.value.copy(isGameActive = isActive)
    }

    fun getCurrentState(): GameState {
        return _gameState.value
    }
}

data class GameState(
    val player: Player? = null,
    val settings: Settings? = null,
    val score: Int = 0,
    val lives: Int = 5,
    val isGameActive: Boolean = true
)

data class InsectType(
    val drawableRes: Int,
    val points: Int,
    val speedMultiplier: Float
)

data class InsectData(
    val id: String,
    val type: InsectType,
    val x: Float,
    val y: Float
)

object InsectTypes {
    val COCKROACH = InsectType(R.drawable.ic_cockroach, 10, 1f)
    val BEETLE = InsectType(R.drawable.ic_beetle, 5, 0.5f)
    val FLY = InsectType(R.drawable.ic_fly, 15, 1.5f)
    val GOLD_BONUS = InsectType(R.drawable.ic_gold_bonus, 0, 0.8f)
    val BONUS = InsectType(R.drawable.ic_bonus, 0, 1f)

    val REGULAR_INSECTS = listOf(COCKROACH, BEETLE, FLY)

    // Метод для создания золотого насекомого с динамическими очками
    fun createGoldInsect(points: Int): InsectType {
        return InsectType(R.drawable.ic_gold_bonus, points, 0.8f)
    }
}