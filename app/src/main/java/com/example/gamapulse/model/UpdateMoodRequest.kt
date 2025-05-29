package com.example.gamapulse.model

data class UpdateMoodRequest(
    val mood_level: Int,
    val mood_note: String?,
    val mood_intensity: String
)
