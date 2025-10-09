package com.example.mobilebugsgame

import android.content.Intent
import android.os.Build
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
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date

class RegistrationFragment : Fragment(){
    private var selectedDate: Long = Calendar.getInstance().timeInMillis
    private lateinit var viewModel: GameViewModel

    private var currentPlayerId: Long = 0
    private var currentSettingsId: Long = 0
    private var isEditingExistingPlayer = false

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_registration,container,false)

        val database = AppDatabase.getInstance(requireContext())
        val repository = GameRepository(
            database.playerDao(),
            database.gameSettingsDao(),
            database.gameResultsDao()
        )
        viewModel = ViewModelProvider(this, GameViewModelFactory(repository))[GameViewModel::class.java]

        val etFullName = view.findViewById<EditText>(R.id.etFullName)
        val rgGender = view.findViewById<RadioGroup>(R.id.rgGender)
        val spinnerCourse = view.findViewById<Spinner>(R.id.spinnerCourse)
        val tvGameDifficulty = view.findViewById<TextView>(R.id.tvGameDifficulty)
        val sbGameDifficulty = view.findViewById<SeekBar>(R.id.sbGameDifficulty)
        val cvCalendar = view.findViewById<CalendarView>(R.id.cvCalendar)
        val tvZodiacSign = view.findViewById<TextView>(R.id.tvZodiacSign)
        val ivZodiacSign = view.findViewById<ImageView>(R.id.ivZodiacSign)
        val btnRegister = view.findViewById<Button>(R.id.btnRegister)
        val btnSelectPlayer = view.findViewById<Button>(R.id.btnSelectPlayer)
        val btnNewPlayer = view.findViewById<Button>(R.id.btnNewPlayer)
        val tvResult = view.findViewById<TextView>(R.id.tvResult)

        selectedDate = cvCalendar.date
        updateZodiacDisplay(selectedDate, ivZodiacSign,tvZodiacSign)

        spinnerCourse.adapter = setSpinnerCourse()

        sbGameDifficulty.min = 1
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

        btnSelectPlayer.setOnClickListener {
            showPlayerSelectionDialog { player, settings ->
                loadPlayerData(
                    player,
                    settings,
                    etFullName,
                    rgGender,
                    spinnerCourse,
                    sbGameDifficulty,
                    tvGameDifficulty,
                    cvCalendar,
                    ivZodiacSign,
                    tvZodiacSign,
                    btnRegister,
                    btnNewPlayer,
                    tvResult
                )
            }
        }

        btnNewPlayer.setOnClickListener {
            resetForm(
                etFullName,
                rgGender,
                spinnerCourse,
                sbGameDifficulty,
                tvGameDifficulty,
                cvCalendar,
                ivZodiacSign,
                tvZodiacSign,
                btnRegister,
                btnNewPlayer,
                btnSelectPlayer,
                tvResult
            )
        }

        btnRegister.setOnClickListener {
            if (isEditingExistingPlayer) {
                updateExistingPlayer(
                    etFullName,
                    rgGender,
                    spinnerCourse,
                    sbGameDifficulty,
                    selectedDate,
                    btnRegister,
                    tvResult
                )
            } else {
                registerNewPlayer(
                    etFullName,
                    rgGender,
                    spinnerCourse,
                    sbGameDifficulty,
                    selectedDate,
                    btnRegister,
                    btnNewPlayer,
                    tvResult
                )
            }
        }

        // Инициализация UI в режиме регистрации
        updateUIForRegistrationMode(btnRegister, btnNewPlayer, btnSelectPlayer, tvResult)

        return view
    }

    private fun showPlayerSelectionDialog(onPlayerSelected: (PlayerEntity, GameSettingsEntity?) -> Unit) {
        val dialog = PlayerSelectionDialog(onPlayerSelected)
        dialog.show(parentFragmentManager, "PlayerSelectionDialog")
    }

    private fun loadPlayerData(
        player: PlayerEntity,
        settings: GameSettingsEntity?,
        etFullName: EditText,
        rgGender: RadioGroup,
        spinnerCourse: Spinner,
        sbGameDifficulty: SeekBar,
        tvGameDifficulty: TextView,
        cvCalendar: CalendarView,
        ivZodiacSign: ImageView,
        tvZodiacSign: TextView,
        btnRegister: Button,
        btnNewPlayer: Button,
        tvResult: TextView
    ) {
        etFullName.setText(player.fullName)

        when (player.gender) {
            "Мужчина" -> rgGender.check(R.id.rbMale)
            "Женщина" -> rgGender.check(R.id.rbFemale)
            else -> rgGender.clearCheck()
        }

        val courses = resources.getStringArray(R.array.courses_array)
        val courseIndex = courses.indexOf(player.course)
        if (courseIndex != -1) {
            spinnerCourse.setSelection(courseIndex)
        }

        sbGameDifficulty.progress = player.difficulty
        tvGameDifficulty.text = "Уровень сложности: ${player.difficulty}"

        cvCalendar.date = player.birthDate
        selectedDate = player.birthDate
        updateZodiacDisplay(selectedDate, ivZodiacSign, tvZodiacSign)

        currentPlayerId = player.id
        currentSettingsId = settings?.id ?: 0
        isEditingExistingPlayer = true

        btnRegister.text = "Обновить и играть"
        btnNewPlayer.visibility = View.VISIBLE
        tvResult.text = "Редактирование: ${player.fullName}\nНажмите 'Новый игрок' для регистрации нового"
        tvResult.visibility = View.VISIBLE

        if (settings != null) {
            updateSettingsFragment(settings)
        }
    }

    private fun updateSettingsFragment(settings: GameSettingsEntity) {
        val settingsFragment = parentFragmentManager.fragments.find { it is SettingsFragment } as? SettingsFragment
        settingsFragment?.settings = Settings(
            gameSpeed = settings.gameSpeed,
            maxInsects = settings.maxInsects,
            bonusInterval = settings.bonusInterval,
            roundDuration = settings.roundDuration
        )
        settingsFragment?.updateUI()
    }

    private fun resetForm(
        etFullName: EditText,
        rgGender: RadioGroup,
        spinnerCourse: Spinner,
        sbGameDifficulty: SeekBar,
        tvGameDifficulty: TextView,
        cvCalendar: CalendarView,
        ivZodiacSign: ImageView,
        tvZodiacSign: TextView,
        btnRegister: Button,
        btnNewPlayer: Button,
        btnSelectPlayer: Button,
        tvResult: TextView
    ) {
        etFullName.text.clear()
        rgGender.clearCheck()
        spinnerCourse.setSelection(0)
        sbGameDifficulty.progress = 1
        tvGameDifficulty.text = "Уровень сложности: 1"

        val currentDate = Calendar.getInstance().timeInMillis
        cvCalendar.date = currentDate
        selectedDate = currentDate
        updateZodiacDisplay(selectedDate, ivZodiacSign, tvZodiacSign)

        currentPlayerId = 0
        currentSettingsId = 0
        isEditingExistingPlayer = false

        updateUIForRegistrationMode(btnRegister, btnNewPlayer, btnSelectPlayer, tvResult)
        resetSettingsToDefault()
    }

    private fun updateUIForRegistrationMode(
        btnRegister: Button,
        btnNewPlayer: Button,
        btnSelectPlayer: Button,
        tvResult: TextView
    ) {
        btnRegister.text = "Зарегистрировать и играть"
        btnNewPlayer.visibility = View.GONE
        btnSelectPlayer.visibility = View.VISIBLE
        tvResult.text = "Заполните данные для регистрации нового игрока"
        tvResult.visibility = View.VISIBLE
    }

    private fun resetSettingsToDefault() {
        val defaultSettings = Settings(1, 10, 5, 60)
        val settingsFragment = parentFragmentManager.fragments.find { it is SettingsFragment } as? SettingsFragment
        settingsFragment?.settings = defaultSettings
        settingsFragment?.updateUI()
    }

    private fun registerNewPlayer(
        etFullName: EditText,
        rgGender: RadioGroup,
        spinnerCourse: Spinner,
        sbGameDifficulty: SeekBar,
        birthDate: Long,
        btnRegister: Button,
        btnNewPlayer: Button,
        tvResult: TextView
    ) {
        val fullName = etFullName.text.toString().trim()
        if (fullName.isEmpty()) {
            tvResult.text = "Введите ФИО"
            tvResult.visibility = View.VISIBLE
            return
        }

        lifecycleScope.launch {
            viewModel.getPlayerByName(fullName).collectLatest { existingPlayer ->
                if (existingPlayer != null) {
                    tvResult.text = "Игрок с именем '$fullName' уже существует"
                    tvResult.visibility = View.VISIBLE
                    return@collectLatest
                }

                btnNewPlayer.visibility = View.VISIBLE
                val gender = when (rgGender.checkedRadioButtonId) {
                    R.id.rbMale -> "Мужчина"
                    R.id.rbFemale -> "Женщина"
                    else -> "Не выбран"
                }
                val course = spinnerCourse.selectedItem.toString()
                val difficulty = sbGameDifficulty.progress
                val zodiacSign = getZodiacSign(birthDate)

                val settingsFragment = parentFragmentManager.fragments.find { it is SettingsFragment } as? SettingsFragment
                val baseSettings = settingsFragment?.settings ?: Settings(1, 10, 5, 60)

                val playerEntity = PlayerEntity(
                    fullName = fullName,
                    gender = gender,
                    course = course,
                    difficulty = difficulty,
                    birthDate = birthDate,
                    zodiacSign = zodiacSign.first
                )

                val gameSettingsEntity = GameSettingsEntity(
                    playerId = 0,
                    gameSpeed = baseSettings.gameSpeed,
                    maxInsects = baseSettings.maxInsects,
                    bonusInterval = baseSettings.bonusInterval,
                    roundDuration = baseSettings.roundDuration
                )

                viewModel.registerPlayerAndSettings(
                    player = playerEntity,
                    settings = gameSettingsEntity,
                    onSuccess = { playerId, settingsId ->
                        startGameActivity(playerId, settingsId, fullName, gender, course, difficulty, zodiacSign.first, baseSettings)
                    },
                    onError = { exception ->
                        tvResult.text = "Ошибка регистрации: ${exception.message}"
                        tvResult.visibility = View.VISIBLE
                    }
                )
            }
        }
    }

    private fun updateExistingPlayer(
        etFullName: EditText,
        rgGender: RadioGroup,
        spinnerCourse: Spinner,
        sbGameDifficulty: SeekBar,
        birthDate: Long,
        btnRegister: Button,
        tvResult: TextView
    ) {
        val fullName = etFullName.text.toString().trim()
        if (fullName.isEmpty()) {
            tvResult.text = "Введите ФИО"
            tvResult.visibility = View.VISIBLE
            return
        }

        val gender = when (rgGender.checkedRadioButtonId) {
            R.id.rbMale -> "Мужчина"
            R.id.rbFemale -> "Женщина"
            else -> "Не выбран"
        }
        val course = spinnerCourse.selectedItem.toString()
        val difficulty = sbGameDifficulty.progress
        val zodiacSign = getZodiacSign(birthDate)

        val settingsFragment = parentFragmentManager.fragments.find { it is SettingsFragment } as? SettingsFragment
        val baseSettings = settingsFragment?.settings ?: Settings(1, 10, 5, 60)

        val updatedPlayer = PlayerEntity(
            id = currentPlayerId,
            fullName = fullName,
            gender = gender,
            course = course,
            difficulty = difficulty,
            birthDate = birthDate,
            zodiacSign = zodiacSign.first
        )

        viewModel.updatePlayer(updatedPlayer)

        if (currentSettingsId != 0L) {
            val updatedSettings = GameSettingsEntity(
                id = currentSettingsId,
                playerId = currentPlayerId,
                gameSpeed = baseSettings.gameSpeed,
                maxInsects = baseSettings.maxInsects,
                bonusInterval = baseSettings.bonusInterval,
                roundDuration = baseSettings.roundDuration
            )
            viewModel.updateSettings(updatedSettings)
        }

        startGameActivity(currentPlayerId, currentSettingsId, fullName, gender, course, difficulty, zodiacSign.first, baseSettings)
    }

    private fun startGameActivity(
        playerId: Long,
        settingsId: Long,
        fullName: String,
        gender: String,
        course: String,
        difficulty: Int,
        zodiacSign: String,
        baseSettings: Settings
    ) {
        val player = Player(fullName, gender, course, difficulty, selectedDate, zodiacSign)
        val intent = Intent(requireContext(), GameActivity::class.java).apply {
            putExtra("player", player)
            putExtra("settings", baseSettings)
            putExtra("playerId", playerId)
            putExtra("settingsId", settingsId)
        }
        startActivity(intent)
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
            in 1122..1221 -> Pair("Стрелец","Sagittarius")
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