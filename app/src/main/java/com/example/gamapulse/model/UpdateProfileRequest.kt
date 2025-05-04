package com.example.gamapulse.model

data class UpdateProfileRequest(
    val name: String,
    val email: String,
    val prodi: String,
    val tanggal_lahir: String, // yyyy-MM-dd format
    val phone_number: String?,
    val nim: String,
    val password: String?
)