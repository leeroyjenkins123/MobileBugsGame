package com.example.mobilebugsgame

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import android.widget.TextView
import androidx.fragment.app.Fragment

class SettingsFragment : Fragment() {

    var settings = Settings(1,10,5,60)

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_settings, container, false)

        val sbSpeed = view.findViewById<SeekBar>(R.id.sbSpeed)
        val tvSpeed = view.findViewById<TextView>(R.id.tvSpeed)
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
}
