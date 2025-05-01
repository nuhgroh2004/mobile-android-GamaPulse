package com.example.gamapulse.model

data class RegisterRequest(
    val name: String,
    val email: String,
    val prodi: String,
    val tanggal_lahir: String,
    val phone_number: String,
    val nim: String,
    val password: String
)