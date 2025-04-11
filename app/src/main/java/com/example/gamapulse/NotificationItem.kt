package com.example.gamapulse

data class NotificationItem(
    val id: Int,
    val sender: String,
    val message: String,
    val email: String,
    val status: NotificationStatus = NotificationStatus.PENDING
)
