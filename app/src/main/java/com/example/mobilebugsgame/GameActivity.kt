package com.example.mobilebugsgame

import android.graphics.Color
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.view.MotionEvent
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.MediaPlayer
import android.view.Surface
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import kotlin.random.Random

class GameActivity : AppCompatActivity() {

    private val gameStateViewModel: GameStateViewModel by viewModel()
    private val  gameViewModel: GameViewModel by viewModel()

    private lateinit var player: Player
    private lateinit var settings: Settings
    private var playerId: Long = 0
    private var settingsId: Long = 0

    private val  cbrRepository: CbrRepository by inject()
    private var gameTimer: CountDownTimer? = null
    private var spawnTimer: CountDownTimer? = null
    private var goldTimer: CountDownTimer? = null
    private var bonusTimer: CountDownTimer? = null

    private var isTiltControlActive = false
    private lateinit var sensorManager: SensorManager
    private var tiltX = 0f
    private var tiltY = 0f
    private var screamSound: MediaPlayer? = null

    private lateinit var scoreView: TextView
    private lateinit var timerView: TextView
    private lateinit var livesView: TextView
    private lateinit var gameContainer: ViewGroup
    private lateinit var gameOverText: TextView

    private val activeInsects = mutableListOf<ImageView>()
    private val activeGoldInsects = mutableListOf<ImageView>()
    private val activeBonusInsects = mutableListOf<ImageView>()

    private companion object {
        const val BASE_SPAWN_INTERVAL = 1300L
        const val BASE_INSECTS_PER_SPAWN = 1
    }

    private val insectTypes = InsectTypes.REGULAR_INSECTS
    private val goldInsectType = InsectTypes.GOLD_BONUS

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game)

        player = intent.getParcelableExtra("player", Player::class.java)!!
        settings = intent.getParcelableExtra("settings", Settings::class.java)!!
        playerId = intent.getLongExtra("playerId", 0)
        settingsId = intent.getLongExtra("settingsId", 0)

        scoreView = findViewById(R.id.textScore)
        timerView = findViewById(R.id.textTimer)
        livesView = findViewById(R.id.textLives)
        gameContainer = findViewById(R.id.gameContainer)
        gameOverText = findViewById(R.id.textGameOver)


        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        screamSound = MediaPlayer.create(this, R.raw.bug_scream)

        // Инициализируем игру в ViewModel
        if (gameStateViewModel.getCurrentState().player == null) {
            gameStateViewModel.initializeGame(player, settings)
        }

        setupObservers()

        gameContainer.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_DOWN && gameStateViewModel.getCurrentState().isGameActive) handleMiss()
            true
        }

        updateUI()
        startGame()
        startGoldTimer()
        startBonusTimer()
    }

    private fun setupObservers() {
        lifecycleScope.launch {
            gameStateViewModel.gameState.collectLatest { state ->
                updateUI(state)
                if (state.lives <= 0 && state.isGameActive) {
                    gameOver()
                }
            }
        }
    }

    private fun startGame() {
        gameOverText.visibility = TextView.GONE
        timerView.visibility = TextView.VISIBLE

        val state = gameStateViewModel.getCurrentState()
        val roundDuration = state.settings?.roundDuration ?: 60

        gameTimer = object : CountDownTimer(roundDuration * 1000L, 1000L) {
            override fun onTick(millisUntilFinished: Long) {
                timerView.text = "Время: ${millisUntilFinished / 1000}"
            }

            override fun onFinish() {
                endGame("Время вышло!")
            }
        }.start()

        spawnTimer = object : CountDownTimer(roundDuration * 1000L, getScaledSpawnInterval()) {
            override fun onTick(millisUntilFinished: Long) {
                if (!gameStateViewModel.getCurrentState().isGameActive) return
                val spawnCount = getScaledSpawnCount().coerceAtMost(getMaxInsects() - activeInsects.size)
                repeat(spawnCount) { spawnInsect() }
            }

            override fun onFinish() {}
        }.start()
    }

    private fun getScaledSpawnCount(): Int {
        val state = gameStateViewModel.getCurrentState()
        val gameSpeed = state.settings?.gameSpeed ?: 1
        val maxInsects = state.settings?.maxInsects ?: 10
        return (BASE_INSECTS_PER_SPAWN * gameSpeed).coerceIn(1, maxInsects.coerceAtLeast(1))
    }

    private fun getScaledSpawnInterval(): Long {
        val state = gameStateViewModel.getCurrentState()
        val gameSpeed = state.settings?.gameSpeed ?: 1
        return (BASE_SPAWN_INTERVAL / (gameSpeed * 2)).coerceIn(200L, 2000L)
    }

    private fun getMaxInsects(): Int {
        return gameStateViewModel.getCurrentState().settings?.maxInsects ?: 10
    }

    private fun spawnInsect() {
        if (activeInsects.size >= getMaxInsects()) return

        val type = insectTypes.random()
        val insectId = System.currentTimeMillis().toString() + Random.nextInt(1000)

        val insectData = InsectData(
            id = insectId,
            type = type,
            x = Random.nextInt(50, gameContainer.width - 120).toFloat(),
            y = Random.nextInt(50, gameContainer.height - 120).toFloat()
        )
        gameStateViewModel.addInsect(insectData)

        val insect = createMovingImageView(type.drawableRes, 120, 120, type.points, insectId, true) {
            gameStateViewModel.updateScore(type.points)
            gameStateViewModel.removeInsect(insectId)
            removeInsect(it, true)
            showPointsSplash(it.x, it.y, type.points)
            spawnInsect() // respawn
        }
        startMovement(insect, type.speedMultiplier * getGameSpeed() * 5)
    }

    private fun startGoldTimer() {
        val state = gameStateViewModel.getCurrentState()
        val roundDuration = state.settings?.roundDuration ?: 60

        goldTimer = object : CountDownTimer(roundDuration * 1000L, 20000L) {
            override fun onTick(millisUntilFinished: Long) {
                if (!gameStateViewModel.getCurrentState().isGameActive || activeInsects.size >= getMaxInsects()) return
                lifecycleScope.launch(Dispatchers.IO) {
                    try {
                        val points = cbrRepository.getCurrentGoldRate().toInt().coerceAtLeast(10)
                        launch(Dispatchers.Main) { spawnGoldInsect(points) }
                    } catch (_: Exception) {}
                }
            }

            override fun onFinish() {}
        }.start()
    }

    private fun spawnGoldInsect(points: Int) {
        val insectId = "gold_" + System.currentTimeMillis() + Random.nextInt(1000)
        val goldType = InsectTypes.createGoldInsect(points)

        val insectData = InsectData(
            id = insectId,
            type = goldType,
            x = Random.nextInt(50, gameContainer.width - 140).toFloat(),
            y = Random.nextInt(50, gameContainer.height - 140).toFloat()
        )
        gameStateViewModel.addInsect(insectData)

        val insect = createMovingImageView(R.drawable.ic_gold_bonus, 140, 140, points, insectId, false) {
            gameStateViewModel.updateScore(points)
            gameStateViewModel.removeInsect(insectId)
            removeInsect(it, false)
            showPointsSplash(it.x, it.y, points, Color.YELLOW)
        }
        startMovement(insect, goldInsectType.speedMultiplier * getGameSpeed() * 5)
    }

    private fun startBonusTimer() {
        val state = gameStateViewModel.getCurrentState()
        val roundDuration = state.settings?.roundDuration ?: 60
        val bonusInterval = state.settings?.bonusInterval ?: 10
        val intervalMs = (bonusInterval * 1000L).coerceAtLeast(5000L)

        bonusTimer = object : CountDownTimer(roundDuration * 1000L, intervalMs) {
            override fun onTick(millisUntilFinished: Long) = showBonus()
            override fun onFinish() {}
        }.start()
    }

    private fun showBonus() {
        val bonus = createMovingImageView(R.drawable.ic_bonus, 160, 160, 0, "bonus_${System.currentTimeMillis()}", false) {
            activateTiltControl()
            removeInsect(it, false)
        }
        startMovement(bonus, 5f)

        bonus.postDelayed({
            if (bonus.parent != null) removeInsect(bonus, false)
        }, 5000)
    }

    private fun activateTiltControl() {
        screamSound?.start()
        if (isTiltControlActive) return
        isTiltControlActive = true
        sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)?.also { sensor ->
            sensorManager.registerListener(tiltListener, sensor, SensorManager.SENSOR_DELAY_GAME)
        }
        gameContainer.postDelayed({
            isTiltControlActive = false
            sensorManager.unregisterListener(tiltListener)
        }, 10000)
    }

    private val tiltListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            val rotation = windowManager.defaultDisplay.rotation

            var x = event.values[0]
            var y = event.values[1]

            when (rotation) {
                Surface.ROTATION_0 -> {
                    tiltX = -x / 2
                    tiltY = y / 2
                }
                Surface.ROTATION_90 -> {
                    tiltX = y / 2
                    tiltY = x / 2
                }
                Surface.ROTATION_180 -> {
                    tiltX = x / 2
                    tiltY = -y / 2
                }
                Surface.ROTATION_270 -> {
                    tiltX = -y / 2
                    tiltY = -x / 2
                }
            }

            if (isTiltControlActive) {
                (activeInsects + activeGoldInsects + activeBonusInsects).forEach { insect ->
                    insect.x = (insect.x + tiltX).coerceIn(0f, gameContainer.width - insect.width.toFloat())
                    insect.y = (insect.y + tiltY).coerceIn(0f, gameContainer.height - insect.height.toFloat())
                }
            }
        }
        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    }

    private fun handleMiss() {
        if (!gameStateViewModel.getCurrentState().isGameActive) return
        gameStateViewModel.decreaseLives()
        gameContainer.animate().scaleX(0.95f).scaleY(0.95f).setDuration(100L)
            .withEndAction { gameContainer.animate().scaleX(1f).scaleY(1f).setDuration(100L).start() }.start()
    }

    private fun removeInsect(insect: ImageView, isRegular: Boolean) {
        insect.clearAnimation()
        insect.setOnClickListener(null)

        when {
            isRegular -> activeInsects.remove(insect)
            insect.drawable.constantState?.equals(resources.getDrawable(R.drawable.ic_gold_bonus, null)?.constantState) == true ->
                activeGoldInsects.remove(insect)
            else -> activeBonusInsects.remove(insect)
        }

        gameContainer.removeView(insect)
    }

    private fun createMovingImageView(
        drawableRes: Int,
        width: Int,
        height: Int,
        points: Int,
        insectId: String,
        isRegular: Boolean,
        onClick: ((ImageView) -> Unit)? = null
    ): ImageView {
        val imageView = ImageView(this).apply {
            setImageResource(drawableRes)
            layoutParams = ViewGroup.LayoutParams(width, height)
            x = Random.nextInt(50, gameContainer.width - width).toFloat()
            y = Random.nextInt(50, gameContainer.height - height).toFloat()
            tag = insectId
            alpha = 0f
            onClick?.let { listener -> setOnClickListener { listener(this) } }
        }
        gameContainer.addView(imageView)

        when {
            isRegular -> activeInsects.add(imageView)
            drawableRes == R.drawable.ic_gold_bonus -> activeGoldInsects.add(imageView)
            else -> activeBonusInsects.add(imageView)
        }

        imageView.animate().alpha(1f).setDuration(300).start()
        return imageView
    }


    private fun startMovement(view: ImageView, speed: Float) {
        var dx = if (Random.nextBoolean()) speed else -speed
        var dy = if (Random.nextBoolean()) speed else -speed
        val stepTime = 16L
        val moveRunnable = object : Runnable {
            override fun run() {
                if (!gameStateViewModel.getCurrentState().isGameActive || view.parent == null) return
                view.x = (view.x + dx).coerceIn(0f, gameContainer.width - view.width.toFloat())
                view.y = (view.y + dy).coerceIn(0f, gameContainer.height - view.height.toFloat())
                if (view.x <= 0 || view.x + view.width >= gameContainer.width) dx = -dx
                if (view.y <= 0 || view.y + view.height >= gameContainer.height) dy = -dy
                view.postDelayed(this, stepTime)
            }
        }
        view.post(moveRunnable)
    }

    private fun showPointsSplash(x: Float, y: Float, points: Int, color: Int = Color.YELLOW) {
        val killSplash = ImageView(this).apply {
            setImageResource(R.drawable.ic_kill_splash)
            layoutParams = ViewGroup.LayoutParams(120, 120)
            this.x = x
            this.y = y
            alpha = 0f
            animate().alpha(0.8f).setDuration(200).withEndAction {
                animate().alpha(0f)
                    .setDuration(200)
                    .withEndAction {
                        gameContainer.removeView(this)
                    }
                    .start()
            }.start()
        }
        gameContainer.addView(killSplash)

        val pointsText = TextView(this).apply {
            text = "+$points"
            setTextColor(color)
            textSize = 20f
            typeface = Typeface.DEFAULT_BOLD
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT )
            this.x = x + 40
            this.y = y
            alpha = 0f
            animate().alpha(1f)
                .translationYBy(-100f)
                .setDuration(1000)
                .withEndAction {
                    gameContainer.removeView(this)
                }
                .start()
        }
        gameContainer.addView(pointsText)
    }

    private fun updateUI() {
        val state = gameStateViewModel.getCurrentState()
        scoreView.text = "Очки: ${state.score}"
        livesView.text = "Жизни: ${state.lives}"
    }

    private fun updateUI(state: GameState) {
        scoreView.text = "Очки: ${state.score}"
        livesView.text = "Жизни: ${state.lives}"
    }

    private fun getGameSpeed(): Int {
        return gameStateViewModel.getCurrentState().settings?.gameSpeed ?: 1
    }

    private fun gameOver() {
        gameStateViewModel.setIsGameActive(false)
        gameTimer?.cancel()
        spawnTimer?.cancel()
        goldTimer?.cancel()
        bonusTimer?.cancel()
        clearActiveInsects()
        endGame("Жизни закончились!")
    }

    private fun clearActiveInsects() {
        listOf(activeInsects, activeGoldInsects, activeBonusInsects).forEach { list ->
            list.forEach {
                it.clearAnimation()
                it.setOnClickListener(null)
                gameContainer.removeView(it)
            }
            list.clear()
        }
        gameStateViewModel.clearInsects()
    }

    private fun endGame(message: String) {
        timerView.visibility = TextView.GONE
        gameOverText.text = message
        gameOverText.visibility = TextView.VISIBLE

        if (playerId != 0L && settingsId != 0L) {
            val state = gameStateViewModel.getCurrentState()
            gameViewModel.saveGameResult(playerId, settingsId, state.score)
        }

        gameContainer.postDelayed({
            gameContainer.removeAllViews()
            showGameResultDialog()
        }, 1000)
    }

    private fun showGameResultDialog() {
        val state = gameStateViewModel.getCurrentState()
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Игра окончена!")
            .setMessage("""
                Ваш результат: ${state.score} очков
                Уровень сложности: ${state.player?.difficulty}
                Скорость игры: ${state.settings?.gameSpeed}
                Игрок: ${state.player?.fullName}
            """.trimIndent())
            .setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
                finish()
            }
            .setCancelable(false)
            .show()
    }

    override fun onDestroy() {
        super.onDestroy()
        gameTimer?.cancel()
        spawnTimer?.cancel()
        goldTimer?.cancel()
        bonusTimer?.cancel()
        gameStateViewModel.setIsGameActive(false)
        sensorManager.unregisterListener(tiltListener)
        screamSound?.release()
    }
}