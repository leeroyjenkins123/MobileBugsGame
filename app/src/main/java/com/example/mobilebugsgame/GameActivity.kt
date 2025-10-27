package com.example.mobilebugsgame

import android.animation.ObjectAnimator
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
import kotlin.random.Random

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.MediaPlayer


class GameActivity : AppCompatActivity(){
    private lateinit var player: Player
    private lateinit var settings: Settings
    private lateinit var viewModel: GameViewModel
    private var playerId: Long = 0
    private var settingsId: Long = 0

    // === BONUS FEATURE START ===
    private var bonusTimer: CountDownTimer? = null
    private var isTiltControlActive = false
    private lateinit var sensorManager: SensorManager
    private var tiltX = 0f
    private var tiltY = 0f
    private var screamSound: MediaPlayer? = null
    // === BONUS FEATURE END ===


    private var score = 0
    private var lives = 5
    private lateinit var scoreView: TextView
    private lateinit var timerView: TextView
    private lateinit var livesView: TextView
    private lateinit var gameContainer: ViewGroup
    private lateinit var gameOverText: TextView

    private companion object{
        const val BASE_SPAWN_INTERVAL = 1300L // (1 секунда)
        const val BASE_INSECTS_PER_SPAWN = 1
    }

    private data class InsectType(
        val drawableRes: Int,
        val points: Int,
        val baseSpeedMultiplier: Float,
        val spawnChance: Float
    )

    private val insectTypes = listOf(
        InsectType(R.drawable.ic_cockroach, 10, 1.0f, 0.5f),  // Нормальная скорость
        InsectType(R.drawable.ic_beetle, 5, 0.5f, 0.5f),         // Медленнее
        InsectType(R.drawable.ic_fly, 15, 1.5f, 0.5f)             // Быстрее
    )

    private var gameTimer: CountDownTimer? = null
    private var insectSpawnTimer : CountDownTimer? = null
    private var isGameActive = true
    private var activeInsects = mutableListOf<ImageView>()

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate(savedInstanceState: Bundle?){
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

        gameContainer.setOnTouchListener { _, event ->
            if(event.action == MotionEvent.ACTION_DOWN && isGameActive){
                handleMiss()
            }
            true
        }

        val database = AppDatabase.getInstance(this)
        val repository = GameRepository(
            database.playerDao(),
            database.gameSettingsDao(),
            database.gameResultsDao()
        )
        viewModel = ViewModelProvider(this, GameViewModelFactory(repository))[GameViewModel::class.java]

        updateUI()
        startGame()

        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        screamSound = MediaPlayer.create(this, R.raw.bug_scream) // добавь в res/raw/bug_scream.mp3

        startBonusTimer()
    }

    private fun startGame(){
        gameOverText.visibility = TextView.GONE
        timerView.visibility = TextView.VISIBLE

        gameTimer = object : CountDownTimer(settings.roundDuration * 1000L, 1000L){
            override fun onTick(millisUntilFinished: Long) {
                timerView.text = "Время: ${millisUntilFinished / 1000}"
            }

            override fun onFinish() {
                endGame("Время вышло!")
            }
        }.start()

        insectSpawnTimer = object : CountDownTimer(settings.roundDuration * 1000L, getScaledSpawnInterval()){
            override fun onTick(millisUntilFinished: Long) {
                if(isGameActive){
                    if(activeInsects.size < settings.maxInsects){
                        val insectsToSpawn = getScaledSpawnPerSpawn().coerceAtMost(settings.maxInsects - activeInsects.size)
                        if(insectsToSpawn > 0){
                            repeat(insectsToSpawn){
                                spawnInsect()
                            }
                        }
                    }
                }
            }
            override fun onFinish() {}
        }.start()
    }

    private fun getScaledSpawnPerSpawn() : Int{
        val scaledCount = (BASE_INSECTS_PER_SPAWN * settings.gameSpeed)
        return scaledCount.coerceIn(1, settings.maxInsects.coerceAtLeast(1))
    }

    private fun getScaledSpawnInterval() : Long{
        val scaledInterval = (BASE_SPAWN_INTERVAL / (settings.gameSpeed * 2))
        return scaledInterval.coerceIn(200L, 2000L)
    }

    private fun spawnInsect(){
        if(activeInsects.size >= settings.maxInsects ) return

        val insectType = getRandomInsectType()
        val insect = ImageView(this).apply {
            setImageResource(insectType.drawableRes)
            layoutParams = ViewGroup.LayoutParams(120, 120)
            x = Random.nextInt(50, gameContainer.width - 120).toFloat()
            y = Random.nextInt(50, gameContainer.width - 120).toFloat()
            tag = insectType

            setOnClickListener {
                if(isGameActive){
                    score += insectType.points
                    updateUI()
                    removeInsect(this)

                    showKillSplash(x, y)

                    animate().scaleX(0f)
                        .scaleY(0f)
                        .alpha(0f)
                        .setDuration(200)
                        .withEndAction {
                            gameContainer.removeView(this)
                            if(isGameActive && activeInsects.size < settings.maxInsects){
                                spawnInsect()
                            }
                        }
                        .start()
                }
            }
        }
        gameContainer.addView(insect)
        activeInsects.add(insect)
        startInsectMovement(insect, insectType)
    }

    private fun showKillSplash(x: Float, y: Float){
        val killSplash = ImageView(this).apply {
            setImageResource(R.drawable.ic_kill_splash)
            layoutParams = ViewGroup.LayoutParams(120, 120)
            this.x = x
            this.y = y
            alpha = 0f

            animate()
                .alpha(0.8f)
                .setDuration(200)
                .withEndAction {
                    animate()
                        .alpha(0f)
                        .setDuration(200)
                        .withEndAction {
                            gameContainer.removeView(this)
                        }
                        .start()
                }
                .start()
        }
        gameContainer.addView(killSplash)
    }

    private fun startInsectMovement(insect : ImageView, insectType: InsectType){
        val speed = (5 * insectType.baseSpeedMultiplier * settings.gameSpeed).toInt().coerceAtLeast(2)

        var dx = if (Random.nextBoolean()) speed else -speed
        var dy = if (Random.nextBoolean()) speed else -speed

        val stepTime = 16L

        val moveRunnable = object : Runnable {
            override fun run() {
                if (!isGameActive || insect.parent == null) return

                var newX = insect.x + dx
                var newY = insect.y + dy

                if (newX <= 0 || newX + insect.width >= gameContainer.width) {
                    dx = -dx
                    newX = insect.x + dx
                }
                if (newY <= 0 || newY + insect.height >= gameContainer.height) {
                    dy = -dy
                    newY = insect.y + dy
                }

                insect.x = newX
                insect.y = newY

                insect.postDelayed(this, stepTime)
            }
        }
        insect.post(moveRunnable)
    }

    private fun removeInsect(insect : ImageView){
        insect.clearAnimation()
        insect.setOnClickListener(null)
        activeInsects.remove(insect)
    }

    private fun getRandomInsectType() : InsectType{
        return insectTypes.random()
    }

    private fun updateUI(){
        scoreView.text = "Очки: $score"
        livesView.text = "Жизни: $lives"
    }

    private fun handleMiss(){
        if(!isGameActive) return

        lives--
        if(score > 0){
            score -= 2
            score = score.coerceAtLeast(0)
        }
        updateUI()

        gameContainer.animate()
            .scaleX(0.95f)
            .scaleY(0.95f)
            .setDuration(100L)
            .withEndAction {
                gameContainer.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(100L)
                    .start()
            }
            .start()
        if(lives <= 0){
            gameOver()
        }
    }

    private fun gameOver(){
        isGameActive = false
        gameTimer?.cancel()
        insectSpawnTimer?.cancel()

        activeInsects.forEach { insect ->
            insect.clearAnimation()
            insect.setOnClickListener(null)
            insect.animate().cancel()
            gameContainer.removeView(insect)
        }
        activeInsects.clear()

        endGame("Жизни закончились!")
    }

    private fun endGame(message: String){
        timerView.visibility = TextView.GONE
        gameOverText.text = message
        gameOverText.visibility = TextView.VISIBLE

        activeInsects.forEach { insect ->
            insect.clearAnimation()
            insect.setOnClickListener(null)
        }
        activeInsects.clear()

        if (playerId != 0L && settingsId != 0L) {
            viewModel.saveGameResult(playerId, settingsId, score)
        }

        gameContainer.postDelayed({
            gameContainer.removeAllViews()
            showGameResultDialog()
        }, 1000)
    }

    private fun showGameResultDialog(){
        val dialog = androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Игра окончена!")
            .setMessage("""
                Ваш результат: $score очков
                Уровень сложности: ${player.difficulty}
                Скорость игры: ${settings.gameSpeed}
                Игрок: ${player.fullName}
            """.trimIndent())
            .setPositiveButton("OK"){dialog, _ ->
                dialog.dismiss()
                finish()
            }
            .setCancelable(false)
            .create()
        dialog.show()
    }

    override fun onDestroy(){
        super.onDestroy()
        gameTimer?.cancel()
        insectSpawnTimer?.cancel()
        isGameActive = false
        bonusTimer?.cancel()
        sensorManager.unregisterListener(tiltListener)
        screamSound?.release()
    }

    private fun startBonusTimer() {
        val intervalMs = (settings.bonusInterval * 1000L).coerceAtLeast(5000L)
        bonusTimer = object : CountDownTimer(settings.roundDuration * 1000L, intervalMs) {
            override fun onTick(millisUntilFinished: Long) {
                showBonus()
            }

            override fun onFinish() {}
        }.start()
    }

    private fun showBonus() {
        val bonus = ImageView(this).apply {
            setImageResource(R.drawable.ic_bonus)
            layoutParams = ViewGroup.LayoutParams(160, 160)
            x = Random.nextInt(50, gameContainer.width - 200).toFloat()
            y = Random.nextInt(50, gameContainer.height - 200).toFloat()
            alpha = 0f

            animate().alpha(1f).setDuration(300).start()

            setOnClickListener {
                activateTiltControl()
                gameContainer.removeView(this)
            }
        }
        gameContainer.addView(bonus)

        startBonusMovement(bonus)

        bonus.postDelayed({
            if (bonus.parent != null) {
                gameContainer.removeView(bonus)
            }
        }, 5000)
    }

    private fun startBonusMovement(bonus: ImageView) {
        var dx = if (Random.nextBoolean()) 5f else -5f
        var dy = if (Random.nextBoolean()) 5f else -5f
        val stepTime = 16L

        val moveRunnable = object : Runnable {
            override fun run() {
                if (bonus.parent == null) return

                var newX = bonus.x + dx
                var newY = bonus.y + dy

                if (newX <= 0 || newX + bonus.width >= gameContainer.width) {
                    dx = -dx
                    newX = bonus.x + dx
                }
                if (newY <= 0 || newY + bonus.height >= gameContainer.height) {
                    dy = -dy
                    newY = bonus.y + dy
                }

                bonus.x = newX
                bonus.y = newY

                bonus.postDelayed(this, stepTime)
            }
        }
        bonus.post(moveRunnable)
    }

    private fun activateTiltControl() {
        if (isTiltControlActive) return
        isTiltControlActive = true

        screamSound?.start()

        val sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        sensorManager.registerListener(tiltListener, sensor, SensorManager.SENSOR_DELAY_GAME)

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
                for (insect in activeInsects) {
                    insect.x = (insect.x + tiltX).coerceIn(0f, gameContainer.width - insect.width.toFloat())
                    insect.y = (insect.y + tiltY).coerceIn(0f, gameContainer.height - insect.height.toFloat())
                }
            }
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    }
}