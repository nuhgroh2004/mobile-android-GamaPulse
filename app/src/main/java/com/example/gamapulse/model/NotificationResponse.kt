package com.example.gamapulse.model

data class NotificationResponse(
    val status: String,
    val unread_notifications: List<NotificationData>?,
    val history_notifications: List<NotificationData>?
)