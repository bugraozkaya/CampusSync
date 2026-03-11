package com.bugra.campussync.network

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST

interface ApiService {
    // 1. Token alma (Giriş) isteği
    @POST("api/token/")
    suspend fun login(@Body request: LoginRequest): LoginResponse

    // 2. YENİ: Ders programını getirme isteği
    // (Django'daki endpoint'inin /api/schedules/ olduğunu varsayıyorum, farklıysa burayı düzeltiriz)
    @GET("api/schedules/")
    suspend fun getSchedules(
        @Header("Authorization") token: String // Token'ı header'a (başlığa) ekliyoruz
    ): List<ScheduleItem>
}