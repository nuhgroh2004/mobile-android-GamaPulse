package com.example.gamapulse.model

data class MoodData(
    val mood_id: Int,
    val mahasiswa_id: Int,
    val mood_level: Int,
    val mood_intensity: Int,
    val mood_note: String?,
    val created_at: String,
    val updated_at: String
)
