package com.example.gamapulse.model

data class ReportResponse(
    val success: Boolean,
    val chartData: ChartData,
    val averageMood: Map<String, Double>
)
