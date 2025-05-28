package com.example.gamapulse.model

data class MoodNotesRequest(
    val day: Int,
    val month: Int,
    val year: Int,
    val mood: MoodData?
)

