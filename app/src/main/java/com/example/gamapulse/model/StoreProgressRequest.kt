package com.example.gamapulse.model

import com.google.gson.annotations.SerializedName

data class StoreProgressRequest(
    @SerializedName("expected_target") val expectedTarget: Int,
    @SerializedName("actual_target") val actualTarget: Int,
    @SerializedName("is_achieved") val isAchieved: Boolean
)