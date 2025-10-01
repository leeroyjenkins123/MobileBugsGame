package com.example.mobilebugsgame

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import android.widget.Button
import android.widget.CalendarView
import android.widget.EditText
import android.widget.ImageView
import android.widget.RadioGroup
import android.widget.SeekBar
import android.widget.Spinner
import android.widget.TextView
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date

class RegistrationFragment : Fragment(){
    private var selectedDate: Long = Calendar.getInstance().timeInMillis
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_registration,container,false)

        val etFullName = view.findViewById<EditText>(R.id.etFullName)
        val rgGender = view.findViewById<RadioGroup>(R.id.rgGender)
        val spinnerCourse = view.findViewById<Spinner>(R.id.spinnerCourse)
        val tvGameDifficulty = view.findViewById<TextView>(R.id.tvGameDifficulty)
        val sbGameDifficulty = view.findViewById<SeekBar>(R.id.sbGameDifficulty)
        val cvCalendar = view.findViewById<CalendarView>(R.id.cvCalendar)
        val tvZodiacSign = view.findViewById<TextView>(R.id.tvZodiacSign)
        val ivZodiacSign = view.findViewById<ImageView>(R.id.ivZodiacSign)
        val btnRegister = view.findViewById<Button>(R.id.btnRegister)
        val tvResult = view.findViewById<TextView>(R.id.tvResult)

        selectedDate = cvCalendar.date
        updateZodiacDisplay(selectedDate, ivZodiacSign,tvZodiacSign)

        spinnerCourse.adapter = setSpinnerCourse()

        sbGameDifficulty.setOnSeekBarChangeListener(object: SeekBar.OnSeekBarChangeListener{
            override fun onProgressChanged(
                seekBar: SeekBar?,
                progress: Int,
                fromUser: Boolean
            ) {
                tvGameDifficulty.text = "Уровень сложности: $progress"
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        cvCalendar.setOnDateChangeListener { _,year,month, dayOfMonth ->
            val cal = Calendar.getInstance()
            cal.set(year,month,dayOfMonth)
            selectedDate = cal.timeInMillis
            updateZodiacDisplay(selectedDate, ivZodiacSign,tvZodiacSign)
        }
        selectedDate = cvCalendar.date

        btnRegister.setOnClickListener {
            val fullName = etFullName.text.toString()
            val gender = when(rgGender.checkedRadioButtonId){
                R.id.rbMale -> "Мужчина"
                R.id.rbFemale -> "Женщина"
                else -> "Не выбран"
            }
            val course = spinnerCourse.selectedItem.toString()
            val difficulty = sbGameDifficulty.progress
            val zodiacSign = getZodiacSign(selectedDate)

            val settingsFragment = parentFragmentManager.fragments.find { it is SettingsFragment } as? SettingsFragment
            val settings = settingsFragment?.settings ?: Settings(1,10,5,60)

            val player = createPlayer(fullName,gender,course,difficulty,selectedDate,zodiacSign.first)
//            val info = formatPlayerInfo(player, settings)
//            tvResult.text = info
//            tvResult.visibility = TextView.VISIBLE
            val intent = Intent(requireContext(), GameActivity::class.java).apply {
                putExtra("player", player)
                putExtra("settings", settings)
            }
            startActivity(intent)
        }
        return view
    }
    private fun getZodiacSign(dateMillis: Long): Pair<String, String>{
        val calendar = Calendar.getInstance().apply { timeInMillis = dateMillis }
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        val month = calendar.get(Calendar.MONTH) + 1
        return when (month * 100 + day) {
            in 120..218 -> Pair("Водолей","Aquarius")
            in 219..320 -> Pair("Рыбы","Pisces")
            in 321..419 -> Pair("Овен","Aries")
            in 420..520 -> Pair("Телец","Taurus")
            in 521..620 -> Pair("Близнецы","Gemini")
            in 621..722 -> Pair("Рак","Cancer")
            in 723..822 -> Pair("Лев","Leo")
            in 823..922 -> Pair("Дева","Virgo")
            in 923..1022 -> Pair("Весы","Libra")
            in 1023..1121 -> Pair("Скорпион","Scorpio")
            in 1122..1221 -> Pair("Стрелец","Sagittatius")
            in 1222..1231, in 101..119 -> Pair("Козерог","Capricorn")
            else -> Pair("Неизвестно", "Unknown")
        }
    }

    private fun getZodiacDrawable(zodiac: String): Int{
        return when (zodiac.lowercase()) {
            "aries" -> R.drawable.ic_zodiac_aries
            "taurus" -> R.drawable.ic_zodiac_taurus
            "gemini" -> R.drawable.ic_zodiac_gemini
            "cancer" -> R.drawable.ic_zodiac_cancer
            "leo" -> R.drawable.ic_zodiac_leo
            "virgo" -> R.drawable.ic_zodiac_virgo
            "libra" -> R.drawable.ic_zodiac_libra
            "scorpio" -> R.drawable.ic_zodiac_scorpio
            "sagittarius" -> R.drawable.ic_zodiac_sagittarius
            "capricorn" -> R.drawable.ic_zodiac_capricorn
            "aquarius" -> R.drawable.ic_zodiac_aquarius
            "pisces" -> R.drawable.ic_zodiac_pisces
            else -> R.drawable.ic_launcher_foreground // создайте эту иконку тоже
        }
    }

    private fun updateZodiacDisplay(dateMillis: Long, imageView: ImageView, textView: TextView){
        val zodiac = getZodiacSign(dateMillis)
        textView.text = zodiac.first

        val resourceId = getZodiacDrawable(zodiac.second)
        imageView.setImageResource(resourceId)
    }

    private fun createPlayer(
        fullName: String,
        gender: String,
        course: String,
        difficulty: Int,
        birthDate: Long,
        zodiacSign: String
    ):Player{
        return Player(fullName,gender,course,difficulty,birthDate,zodiacSign)
    }

    private fun setSpinnerCourse(): ArrayAdapter<String>{
        val courses = arrayOf("Бакалавриат. 1 курс", "Бакалавриат. 2 курс", "Бакалавриат. 3 курс", "Бакалавриат. 4 курс", "Магистратура. 1 курс","Магистратура. 2 курс")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item,courses)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        return adapter
    }

    private fun formatPlayerInfo(player: Player, settings: Settings): String{
        val calendar = Calendar.getInstance().apply { timeInMillis = player.birthDate }
        val birthDateStr = "${calendar.get(Calendar.DAY_OF_MONTH)}.${calendar.get(Calendar.MONTH) + 1}.${calendar.get(Calendar.YEAR)}"
        return """
            Регистрация успешна:
            
            ФИО: ${player.fullName}
            Пол: ${player.gender}
            Курс: ${player.course}
            Уровень сложности: ${player.difficulty}/10
            Дата рождения: $birthDateStr
            Знак зодиака: ${player.zodiacSign}
            
            --- Настройки игры ---
            Скорость: ${settings.gameSpeed}
            Макс. тараканов: ${settings.maxInsects}
            Интервал бонусов: ${settings.bonusInterval} сек
            Длительность раунда: ${settings.roundDuration} сек
        """.trimIndent()
    }
}