package com.example.mobilebugsgame

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment

class SettingsFragment : Fragment() {

    var settings = Settings(1,10,5,60)

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_settings, container, false)

        val sbSpeed = view.findViewById<SeekBar>(R.id.sbSpeed)
        val tvSpeed = view.findViewById<TextView>(R.id.tvSpeed)
        sbSpeed.min = 1
        sbSpeed.progress = settings.gameSpeed
        tvSpeed.text = "Скорость игры: ${settings.gameSpeed}"
        sbSpeed.setOnSeekBarChangeListener(object: SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                settings = settings.copy(gameSpeed = progress)
                tvSpeed.text = "Скорость игры: ${settings.gameSpeed}"
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // Максимальное количество тараканов
        val sbMaxCockroaches = view.findViewById<SeekBar>(R.id.sbMaxCockroaches)
        val tvMaxCockroaches = view.findViewById<TextView>(R.id.tvMaxCockroaches)
        sbMaxCockroaches.min = 1
        sbMaxCockroaches.progress = settings.maxInsects
        tvMaxCockroaches.text = "Максимальное количество тараканов: ${settings.maxInsects}"
        sbMaxCockroaches.setOnSeekBarChangeListener(object: SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                settings = settings.copy(maxInsects = progress)
                tvMaxCockroaches.text = "Максимальное количество тараканов: ${settings.maxInsects}"
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // Интервал появления бонусов
        val sbBonusInterval = view.findViewById<SeekBar>(R.id.sbBonusInterval)
        val tvBonusInterval = view.findViewById<TextView>(R.id.tvBonusInterval)
        sbBonusInterval.min = 1
        sbBonusInterval.progress = settings.bonusInterval
        tvBonusInterval.text = "Интервал появления бонусов: ${settings.bonusInterval} сек"
        sbBonusInterval.setOnSeekBarChangeListener(object: SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                settings = settings.copy(bonusInterval = progress)
                tvBonusInterval.text = "Интервал появления бонусов: ${settings.bonusInterval} сек"
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // Длительность раунда
        val sbRoundDuration = view.findViewById<SeekBar>(R.id.sbRoundDuration)
        val tvRoundDuration = view.findViewById<TextView>(R.id.tvRoundDuration)
        sbRoundDuration.min = 1
        sbRoundDuration.progress = settings.roundDuration
        tvRoundDuration.text = "Длительность раунда: ${settings.roundDuration} сек"
        sbRoundDuration.setOnSeekBarChangeListener(object: SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                settings = settings.copy(roundDuration = progress)
                tvRoundDuration.text = "Длительность раунда: ${settings.roundDuration} сек"
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        return view
    }

    // Добавьте этот метод в класс SettingsFragment:
    fun updateUI() {
        view?.findViewById<TextView>(R.id.tvSpeed)?.text = "Скорость игры: ${settings.gameSpeed}"
        view?.findViewById<TextView>(R.id.tvMaxCockroaches)?.text = "Максимальное количество тараканов: ${settings.maxInsects}"
        view?.findViewById<TextView>(R.id.tvBonusInterval)?.text = "Интервал появления бонусов: ${settings.bonusInterval} сек"
        view?.findViewById<TextView>(R.id.tvRoundDuration)?.text = "Длительность раунда: ${settings.roundDuration} сек"

        view?.findViewById<SeekBar>(R.id.sbSpeed)?.progress = settings.gameSpeed
        view?.findViewById<SeekBar>(R.id.sbMaxCockroaches)?.progress = settings.maxInsects
        view?.findViewById<SeekBar>(R.id.sbBonusInterval)?.progress = settings.bonusInterval
        view?.findViewById<SeekBar>(R.id.sbRoundDuration)?.progress = settings.roundDuration
    }
}