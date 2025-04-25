package com.example.gamapulse.network

import com.example.gamapulse.model.LoginRequest
import com.example.gamapulse.model.LoginResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface ApiService {
    @POST("api/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>
}