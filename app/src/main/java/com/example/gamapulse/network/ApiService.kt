package com.example.gamapulse.network

import com.example.gamapulse.model.LoginRequest
import com.example.gamapulse.model.LoginResponse
import com.example.gamapulse.model.ProfileResponse
import com.example.gamapulse.model.RegisterRequest
import com.example.gamapulse.model.RegisterResponse
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST

interface ApiService {
    @POST("api/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    @POST("api/register/mahasiswa")
    suspend fun register(@Body request: RegisterRequest): Response<RegisterResponse>

    @GET("api/mahasiswa/profil")
    suspend fun getProfile(@Header("Authorization") token: String): Response<ProfileResponse>

}