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
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.MediaPlayer
import android.util.Log
import kotlin.random.Random

class GameActivity : AppCompatActivity() {

    private lateinit var player: Player
    private lateinit var settings: Settings
    private lateinit var viewModel: GameViewModel
    private var playerId: Long = 0
    private var settingsId: Long = 0

    private lateinit var cbrRepository: CbrRepository
    private var gameTimer: CountDownTimer? = null
    private var spawnTimer: CountDownTimer? = null
    private var goldTimer: CountDownTimer? = null
    private var bonusTimer: CountDownTimer? = null

    private var isGameActive = true
    private var isTiltControlActive = false
    private lateinit var sensorManager: SensorManager
    private var tiltX = 0f
    private var tiltY = 0f
    private var screamSound: MediaPlayer? = null

    private var score = 0
    private var lives = 5

    private lateinit var scoreView: TextView
    private lateinit var timerView: TextView
    private lateinit var livesView: TextView
    private lateinit var gameContainer: ViewGroup
    private lateinit var gameOverText: TextView

    private val activeInsects = mutableListOf<ImageView>()

    private companion object {
        const val BASE_SPAWN_INTERVAL = 1300L
        const val BASE_INSECTS_PER_SPAWN = 1
    }

    private data class InsectType(
        val drawableRes: Int,
        val points: Int,
        val speedMultiplier: Float
    )

    private val insectTypes = listOf(
        InsectType(R.drawable.ic_cockroach, 10, 1f),
        InsectType(R.drawable.ic_beetle, 5, 0.5f),
        InsectType(R.drawable.ic_fly, 15, 1.5f)
    )

    private val goldInsectType = InsectType(R.drawable.ic_gold_bonus, 0, 0.8f)

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

        cbrRepository = CbrRepository()

        val database = AppDatabase.getInstance(this)
        val repository = GameRepository(
            database.playerDao(),
            database.gameSettingsDao(),
            database.gameResultsDao()
        )
        viewModel = ViewModelProvider(this, GameViewModelFactory(repository))[GameViewModel::class.java]

        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        screamSound = MediaPlayer.create(this, R.raw.bug_scream)

        gameContainer.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_DOWN && isGameActive) handleMiss()
            true
        }

        updateUI()
        startGame()
        startGoldTimer()
        startBonusTimer()
    }

    private fun startGame() {
        gameOverText.visibility = TextView.GONE
        timerView.visibility = TextView.VISIBLE

        gameTimer = object : CountDownTimer(settings.roundDuration * 1000L, 1000L) {
            override fun onTick(millisUntilFinished: Long) {
                timerView.text = "Время: ${millisUntilFinished / 1000}"
            }

            override fun onFinish() {
                endGame("Время вышло!")
            }
        }.start()

        spawnTimer = object : CountDownTimer(settings.roundDuration * 1000L, getScaledSpawnInterval()) {
            override fun onTick(millisUntilFinished: Long) {
                if (!isGameActive) return
                val spawnCount = getScaledSpawnCount().coerceAtMost(settings.maxInsects - activeInsects.size)
                repeat(spawnCount) { spawnInsect() }
            }

            override fun onFinish() {}
        }.start()
    }

    private fun getScaledSpawnCount(): Int =
        (BASE_INSECTS_PER_SPAWN * settings.gameSpeed).coerceIn(1, settings.maxInsects.coerceAtLeast(1))

    private fun getScaledSpawnInterval(): Long =
        (BASE_SPAWN_INTERVAL / (settings.gameSpeed * 2)).coerceIn(200L, 2000L)

    private fun spawnInsect() {
        if (activeInsects.size >= settings.maxInsects) return
        val type = insectTypes.random()
        val insect = createMovingImageView(type.drawableRes, 120, 120, type.points) {
            score += type.points
            removeInsect(it)
            showPointsSplash(it.x, it.y, type.points)
            updateUI()
            spawnInsect() // respawn
        }
        startMovement(insect, type.speedMultiplier * settings.gameSpeed * 5)
    }

    private fun startGoldTimer() {
        goldTimer = object : CountDownTimer(settings.roundDuration * 1000L, 20000L) {
            override fun onTick(millisUntilFinished: Long) {
                if (!isGameActive || activeInsects.size >= settings.maxInsects) return
                lifecycleScope.launch(Dispatchers.IO) {
                    try {
                        // Используем основной метод с текущей датой
                        val goldRate = cbrRepository.getCurrentGoldRate()
                        val points = goldRate.toInt().coerceAtLeast(10)
                        Log.d("GameActivity", "Gold insect points: $points (current rate: $goldRate)")
                        launch(Dispatchers.Main) { spawnGoldInsect(points) }
                    } catch (e: Exception) {
                        Log.e("GameActivity", "Gold timer error: ${e.message}")
                        // Спавним золотого насекомого со стандартными очками
                        launch(Dispatchers.Main) { spawnGoldInsect(75) }
                    }
                }
            }
            override fun onFinish() {
                Log.d("GameActivity", "Gold timer finished")
            }
        }.start()
        Log.d("GameActivity", "Gold timer started with current date")
    }

    private fun spawnGoldInsect(points: Int) {
        val insect = createMovingImageView(R.drawable.ic_gold_bonus, 140, 140, points) {
            score += points
            removeInsect(it)
            showPointsSplash(it.x, it.y, points, Color.YELLOW)
            updateUI()
        }
        startMovement(insect, goldInsectType.speedMultiplier * settings.gameSpeed * 5)
    }

    private fun startBonusTimer() {
        val intervalMs = (settings.bonusInterval * 1000L).coerceAtLeast(5000L)
        bonusTimer = object : CountDownTimer(settings.roundDuration * 1000L, intervalMs) {
            override fun onTick(millisUntilFinished: Long) = showBonus()
            override fun onFinish() {}
        }.start()
    }

    private fun showBonus() {
        val bonus = createMovingImageView(R.drawable.ic_bonus, 160, 160, 0) {
            activateTiltControl()
            gameContainer.removeView(it)
        }
        startMovement(bonus, 5f)
        bonus.postDelayed({ if (bonus.parent != null) gameContainer.removeView(bonus) }, 5000)
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
            tiltX = -event.values[0] / 2
            tiltY = event.values[1] / 2
            if (isTiltControlActive) {
                activeInsects.forEach { insect ->
                    insect.x = (insect.x + tiltX).coerceIn(0f, gameContainer.width - insect.width.toFloat())
                    insect.y = (insect.y + tiltY).coerceIn(0f, gameContainer.height - insect.height.toFloat())
                }
            }
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    }

    private fun handleMiss() {
        if (!isGameActive) return
        lives--
        score = (score - 2).coerceAtLeast(0)
        updateUI()
        gameContainer.animate().scaleX(0.95f).scaleY(0.95f).setDuration(100L)
            .withEndAction { gameContainer.animate().scaleX(1f).scaleY(1f).setDuration(100L).start() }.start()
        if (lives <= 0) gameOver()
    }

    private fun removeInsect(insect: ImageView) {
        insect.clearAnimation()
        insect.setOnClickListener(null)
        activeInsects.remove(insect)
        gameContainer.removeView(insect)
    }

    private fun createMovingImageView(
        drawableRes: Int,
        width: Int,
        height: Int,
        points: Int,
        onClick: ((ImageView) -> Unit)? = null
    ): ImageView {
        val imageView = ImageView(this).apply {
            setImageResource(drawableRes)
            layoutParams = ViewGroup.LayoutParams(width, height)
            x = Random.nextInt(50, gameContainer.width - width).toFloat()
            y = Random.nextInt(50, gameContainer.height - height).toFloat()
            tag = points
            alpha = 0f
            onClick?.let { listener -> setOnClickListener { listener(this) } }
        }
        gameContainer.addView(imageView)
        activeInsects.add(imageView)
        imageView.animate().alpha(1f).setDuration(300).start()
        return imageView
    }

    private fun startMovement(view: ImageView, speed: Float) {
        var dx = if (Random.nextBoolean()) speed else -speed
        var dy = if (Random.nextBoolean()) speed else -speed
        val stepTime = 16L
        val moveRunnable = object : Runnable {
            override fun run() {
                if (!isGameActive || view.parent == null) return
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
        val killSplash = ImageView(this).apply { setImageResource(R.drawable.ic_kill_splash)
            layoutParams = ViewGroup.LayoutParams(120, 120)
            this.x = x
            this.y = y
            alpha = 0f
            animate().alpha(0.8f) .setDuration(200) .withEndAction {
                animate().alpha(0f)
                    .setDuration(200)
                    .withEndAction {
                        gameContainer.removeView(this) }
                    .start() }
                .start()
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
        scoreView.text = "Очки: $score"
        livesView.text = "Жизни: $lives"
    }

    private fun gameOver() {
        isGameActive = false
        gameTimer?.cancel()
        spawnTimer?.cancel()
        goldTimer?.cancel()
        bonusTimer?.cancel()
        clearActiveInsects()
        endGame("Жизни закончились!")
    }

    private fun clearActiveInsects() {
        activeInsects.forEach {
            it.clearAnimation()
            it.setOnClickListener(null)
            gameContainer.removeView(it)
        }
        activeInsects.clear()
    }

    private fun endGame(message: String) {
        timerView.visibility = TextView.GONE
        gameOverText.text = message
        gameOverText.visibility = TextView.VISIBLE

        if (playerId != 0L && settingsId != 0L) {
            viewModel.saveGameResult(playerId, settingsId, score)
        }

        gameContainer.postDelayed({
            gameContainer.removeAllViews()
            showGameResultDialog()
        }, 1000)
    }

    private fun showGameResultDialog() {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Игра окончена!")
            .setMessage("""
                Ваш результат: $score очков
                Уровень сложности: ${player.difficulty}
                Скорость игры: ${settings.gameSpeed}
                Игрок: ${player.fullName}
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
        isGameActive = false
        sensorManager.unregisterListener(tiltListener)
        screamSound?.release()
    }
}