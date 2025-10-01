package com.example.mobilebugsgame
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Settings (
    val gameSpeed: Int,
    val maxInsects: Int,
    val bonusInterval: Int,
    val roundDuration: Int
) : Parcelable