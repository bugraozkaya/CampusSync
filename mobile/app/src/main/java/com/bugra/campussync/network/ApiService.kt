package com.bugra.campussync.network

import com.bugra.campussync.screens.ImportedCourse // BU SATIRI EKLEDİK
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST

interface ApiService {
    @POST("api/token/")
    suspend fun login(@Body request: LoginRequest): LoginResponse

    @GET("api/schedules/")
    suspend fun getSchedules(
        @Header("Authorization") token: String
    ): List<ScheduleItem>

    // Admin için toplu veri yükleme
    @POST("api/courses/bulk-import/")
    suspend fun bulkImport(
        @Body data: List<ImportedCourse>
    ): List<Map<String, String>>
}