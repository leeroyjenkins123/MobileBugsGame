package com.example.mobilebugsgame
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Player (
    val fullName: String,
    val gender: String,
    val course: String,
    val difficulty: Int,
    val birthDate: Long,
    val zodiacSign: String
) : Parcelable