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
import kotlin.random.Random

class GameActivity : AppCompatActivity(){
    private lateinit var player: Player
    private lateinit var settings: Settings

    private var score = 0
    private var lives = 5
    private lateinit var scoreView: TextView
    private lateinit var timerView: TextView
    private lateinit var livesView: TextView
    private lateinit var gameContainer: ViewGroup
    private lateinit var gameOverText: TextView

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate(savedInstanceState: Bundle?){
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game)

        player = intent.getParcelableExtra("player", Player::class.java)!!
        settings = intent.getParcelableExtra("settings", Settings::class.java)!!

        scoreView = findViewById(R.id.textScore)
        timerView = findViewById(R.id.textTimer)
        livesView = findViewById(R.id.textLives)
        gameContainer = findViewById(R.id.gameContainer)
        gameOverText = findViewById(R.id.textGameOver)
    }

}