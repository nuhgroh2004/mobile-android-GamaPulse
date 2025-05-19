package com.example.gamapulse.model

data class ChartData(
    val labels: List<Int>,
    val mood: Map<String, MoodInfo>,
    val progress: Map<String, ProgressInfo>,
    val averageMood: Map<String, Double>
)
