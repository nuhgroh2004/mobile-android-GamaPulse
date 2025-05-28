package com.example.gamapulse.model

data class MoodNotesResponse(
    val day: Int,
    val month: Int,
    val year: Int,
    val mood: MoodData?
)
