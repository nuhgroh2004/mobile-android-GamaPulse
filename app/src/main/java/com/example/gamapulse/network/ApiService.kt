package com.example.gamapulse.network

import com.example.gamapulse.model.LoginRequest
import com.example.gamapulse.model.LoginResponse
import com.example.gamapulse.model.ProfileResponse
import com.example.gamapulse.model.RegisterRequest
import com.example.gamapulse.model.RegisterResponse
import com.example.gamapulse.model.StoreMoodRequest
import com.example.gamapulse.model.UpdateProfileRequest
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.PUT

interface ApiService {
    @POST("api/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    @POST("api/register/mahasiswa")
    suspend fun register(@Body request: RegisterRequest): Response<RegisterResponse>

    @GET("api/mahasiswa/profil")
    suspend fun getProfile(@Header("Authorization") token: String): Response<ProfileResponse>

    @PUT("api/mahasiswa/update-profil")
    suspend fun updateProfile(
        @Header("Authorization") token: String,
        @Body request: UpdateProfileRequest
    ): Response<ResponseBody>

    @POST("api/mahasiswa/store-mood")
    suspend fun storeMood(
        @Header("Authorization") token: String,
        @Body request: StoreMoodRequest
    ): Response<ResponseBody>

    @POST("api/logout")
    suspend fun logout(@Header("Authorization") token: String): Response<ResponseBody>
}