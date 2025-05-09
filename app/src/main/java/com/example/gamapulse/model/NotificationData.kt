package com.example.gamapulse.model


data class NotificationData(
    val notification_id: Int,
    val dosen_id: Int,
    val mahasiswa_id: Int,
    val progress_id: Int?,
    val mood_id: Int?,
    val request_status: String,
    val read_status: String,
    val created_at: String,
    val updated_at: String,
    val accepted_at: String?,
    val email: String,
    val name: String
)
