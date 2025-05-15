package com.example.gamapulse.model

data class CalendarResponse(
    val success: Boolean,
    val month: String,
    val year: String,
    val data: Map<String, MoodData>
)