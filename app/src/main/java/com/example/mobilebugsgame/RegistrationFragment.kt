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
import java.util.Calendar

class RegistrationFragment : Fragment(){
    private var selectedDate: Long = Calendar.getInstance().timeInMillis
    private lateinit var viewModel: GameViewModel
    private lateinit var sharedSettingsViewModel: SharedSettingsViewModel

    private var currentPlayerId: Long = 0
    private var currentSettingsId: Long = 0
    private var isEditingExistingPlayer = false

    private var savedButtonText: String = "Зарегистрировать и играть"
    private var savedNewPlayerVisibility: Int = View.GONE
    private var savedSelectPlayerVisibility: Int = View.VISIBLE
    private var savedResultText: String = "Заполните данные для регистрации нового игрока"
    private var savedResultVisibility: Int = View.VISIBLE

    companion object {
        private const val KEY_BUTTON_TEXT = "button_text"
        private const val KEY_NEW_PLAYER_VISIBILITY = "new_player_visibility"
        private const val KEY_SELECT_PLAYER_VISIBILITY = "select_player_visibility"
        private const val KEY_RESULT_TEXT = "result_text"
        private const val KEY_RESULT_VISIBILITY = "result_visibility"
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_registration,container,false)

        savedInstanceState?.let {
            savedButtonText = it.getString(KEY_BUTTON_TEXT) ?: "Зарегистрировать и играть"
            savedNewPlayerVisibility = it.getInt(KEY_NEW_PLAYER_VISIBILITY, View.GONE)
            savedSelectPlayerVisibility = it.getInt(KEY_SELECT_PLAYER_VISIBILITY, View.VISIBLE)
            savedResultText = it.getString(KEY_RESULT_TEXT) ?: "Заполните данные для регистрации нового игрока"
            savedResultVisibility = it.getInt(KEY_RESULT_VISIBILITY, View.VISIBLE)
        }

        val database = AppDatabase.getInstance(requireContext())
        val repository = GameRepository(
            database.playerDao(),
            database.gameSettingsDao(),
            database.gameResultsDao()
        )
        viewModel = ViewModelProvider(this, GameViewModelFactory(repository))[GameViewModel::class.java]

        sharedSettingsViewModel = ViewModelProvider(requireActivity())[SharedSettingsViewModel::class.java]

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

        cvCalendar.date = selectedDate
        updateZodiacDisplay(selectedDate, ivZodiacSign,tvZodiacSign)

        spinnerCourse.adapter = setSpinnerCourse()

        sbGameDifficulty.min = 1
        sbGameDifficulty.progress = 1
        tvGameDifficulty.text = "Уровень сложности: ${sbGameDifficulty.progress}"

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

        lifecycleScope.launch {
            sharedSettingsViewModel.settings.collectLatest { newSettings ->
                if (isEditingExistingPlayer && currentPlayerId != 0L && currentSettingsId != 0L) {
                    saveSettingsToDatabase(newSettings)
                }
            }
        }

        btnRegister.text = savedButtonText
        btnNewPlayer.visibility = savedNewPlayerVisibility
        btnSelectPlayer.visibility = savedSelectPlayerVisibility
        tvResult.text = savedResultText
        tvResult.visibility = savedResultVisibility

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
                    btnSelectPlayer,
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

        return view
    }

    private fun saveSettingsToDatabase(settings: Settings) {
        lifecycleScope.launch {
            val updatedSettings = GameSettingsEntity(
                id = currentSettingsId,
                playerId = currentPlayerId,
                gameSpeed = settings.gameSpeed,
                maxInsects = settings.maxInsects,
                bonusInterval = settings.bonusInterval,
                roundDuration = settings.roundDuration
            )
            viewModel.updateSettings(updatedSettings)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(KEY_BUTTON_TEXT, savedButtonText)
        outState.putInt(KEY_NEW_PLAYER_VISIBILITY, savedNewPlayerVisibility)
        outState.putInt(KEY_SELECT_PLAYER_VISIBILITY, savedSelectPlayerVisibility)
        outState.putString(KEY_RESULT_TEXT, savedResultText)
        outState.putInt(KEY_RESULT_VISIBILITY, savedResultVisibility)
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
        btnSelectPlayer: Button,
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

        // Обновляем состояние всех кнопок и сохраняем его
        btnRegister.text = "Обновить и играть"
        btnNewPlayer.visibility = View.VISIBLE
        btnSelectPlayer.visibility = View.VISIBLE // или View.GONE в зависимости от логики
        tvResult.text = "Редактирование: ${player.fullName}\nНажмите 'Новый игрок' для регистрации нового"
        tvResult.visibility = View.VISIBLE

        // Сохраняем состояние для восстановления
        savedButtonText = "Обновить и играть"
        savedNewPlayerVisibility = View.VISIBLE
        savedSelectPlayerVisibility = View.VISIBLE
        savedResultText = "Редактирование: ${player.fullName}\nНажмите 'Новый игрок' для регистрации нового"
        savedResultVisibility = View.VISIBLE

        lifecycleScope.launch {
            val currentSettings = if (settings != null) {
                settings
            } else {
                val defaultSettings = GameSettingsEntity(
                    playerId = player.id,
                    gameSpeed = 1,
                    maxInsects = 10,
                    bonusInterval = 5,
                    roundDuration = 60
                )
                val newSettingsId = viewModel.insertSettings(defaultSettings)
                defaultSettings.copy(id = newSettingsId)
            }

            currentSettingsId = currentSettings.id

            val settingsObj = Settings(
                gameSpeed = currentSettings.gameSpeed,
                maxInsects = currentSettings.maxInsects,
                bonusInterval = currentSettings.bonusInterval,
                roundDuration = currentSettings.roundDuration
            )
            sharedSettingsViewModel.updateSettings(settingsObj)
            sharedSettingsViewModel.enableAutoSave()
        }
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

        sharedSettingsViewModel.disableAutoSave()

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

        // Сохраняем состояние для восстановления
        savedButtonText = "Зарегистрировать и играть"
        savedNewPlayerVisibility = View.GONE
        savedSelectPlayerVisibility = View.VISIBLE
        savedResultText = "Заполните данные для регистрации нового игрока"
        savedResultVisibility = View.VISIBLE
    }

    private fun resetSettingsToDefault() {
        val defaultSettings = Settings(1, 10, 5, 60)
        sharedSettingsViewModel.updateSettings(defaultSettings)
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

                val baseSettings = sharedSettingsViewModel.settings.value

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

        val baseSettings = sharedSettingsViewModel.settings.value

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

        saveSettingsForCurrentPlayer()
        startGameActivity(currentPlayerId, currentSettingsId, fullName, gender, course, difficulty, zodiacSign.first, baseSettings)
    }

    private fun saveSettingsForCurrentPlayer(): Boolean {
        val currentSettings = sharedSettingsViewModel.settings.value

        return if (currentSettingsId != 0L && currentPlayerId != 0L) {
            lifecycleScope.launch {
                val updatedSettings = GameSettingsEntity(
                    id = currentSettingsId,
                    playerId = currentPlayerId,
                    gameSpeed = currentSettings.gameSpeed,
                    maxInsects = currentSettings.maxInsects,
                    bonusInterval = currentSettings.bonusInterval,
                    roundDuration = currentSettings.roundDuration
                )
                viewModel.updateSettings(updatedSettings)
            }
            true
        } else {
            false
        }
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
            else -> R.drawable.ic_launcher_foreground
        }
    }

    private fun updateZodiacDisplay(dateMillis: Long, imageView: ImageView, textView: TextView){
        val zodiac = getZodiacSign(dateMillis)
        textView.text = zodiac.first

        val resourceId = getZodiacDrawable(zodiac.second)
        imageView.setImageResource(resourceId)
    }

    private fun setSpinnerCourse(): ArrayAdapter<String>{
        val courses = arrayOf("Бакалавриат. 1 курс", "Бакалавриат. 2 курс", "Бакалавриат. 3 курс", "Бакалавриат. 4 курс", "Магистратура. 1 курс","Магистратура. 2 курс")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item,courses)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        return adapter
    }

}