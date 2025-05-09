package com.example.gamapulse.model

import com.google.gson.annotations.SerializedName

data class NotificationResponseRequest(
    @SerializedName("status") val status: String
)