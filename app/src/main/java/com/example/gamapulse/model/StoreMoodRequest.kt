package com.example.gamapulse.model

data class StoreMoodRequest(
    val selectedEmotion: String,
    val selectedIntensity: String,
    val notes: String = ""
)