package com.example.gamapulse.network

import com.example.gamapulse.model.CalendarResponse
import com.example.gamapulse.model.LoginRequest
import com.example.gamapulse.model.LoginResponse
import com.example.gamapulse.model.MoodNotesRequest
import com.example.gamapulse.model.MoodNotesResponse
import com.example.gamapulse.model.NotificationActionRequest
import com.example.gamapulse.model.NotificationResponse
import com.example.gamapulse.model.ProfileResponse
import com.example.gamapulse.model.RegisterRequest
import com.example.gamapulse.model.RegisterResponse
import com.example.gamapulse.model.ReportResponse
import com.example.gamapulse.model.StoreMoodRequest
import com.example.gamapulse.model.StoreProgressRequest
import com.example.gamapulse.model.UpdateProfileRequest
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

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

    @POST("api/mahasiswa/progress/store")
    suspend fun storeProgress(
        @Header("Authorization") token: String,
        @Body request: StoreProgressRequest
    ): Response<ResponseBody>

    @GET("api/mahasiswa/notifikasi")
    suspend fun getNotifications(
        @Header("Authorization") token: String
    ): Response<NotificationResponse>

    @PUT("api/mahasiswa/notifikasi/{id}")
    suspend fun respondToNotification(
        @Header("Authorization") token: String,
        @Path("id") notificationId: Int,
        @Body request: NotificationActionRequest
    ): Response<ResponseBody>

    @GET("api/mahasiswa/report")
    suspend fun getReport(
        @Header("Authorization") token: String,
        @Query("month") month: Int,
        @Query("year") year: Int
    ): Response<ReportResponse>

    @GET("api/mahasiswa/edit-mood-notes")
    suspend fun getMoodNotes(
        @Header("Authorization") token: String,
        @Query("day") day: Int,
        @Query("month") month: Int,
        @Query("year") year: Int
    ): Response<MoodNotesResponse>

    @GET("api/mahasiswa/calendar")
    suspend fun getCalendarMoods(
        @Header("Authorization") token: String,
        @Query("month") month: Int,
        @Query("year") year: Int
    ): Response<CalendarResponse>

    @POST("api/logout")
    suspend fun logout(@Header("Authorization") token: String): Response<ResponseBody>
}
