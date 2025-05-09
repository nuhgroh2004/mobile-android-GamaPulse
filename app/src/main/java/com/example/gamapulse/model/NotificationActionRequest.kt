package com.example.gamapulse.model

import com.google.gson.annotations.SerializedName

data class NotificationActionRequest(
    @SerializedName("action") val action: String  // "approve" or "reject"
)