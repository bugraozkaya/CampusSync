package com.bugra.campussync.network

import okhttp3.MultipartBody
import retrofit2.http.*

interface ApiService {
    @POST("api/token/")
    suspend fun login(@Body request: LoginRequest): LoginResponse

    @GET("api/schedules/")
    suspend fun getSchedules(
        @Header("Authorization") token: String
    ): List<ScheduleItem>

    @Multipart
    @POST("api/courses/bulk-import-excel/")
    suspend fun bulkImportExcel(
        @Header("Authorization") token: String,
        @Part file: MultipartBody.Part
    ): List<Map<String, String>>

    @POST("api/unavailability/sync/")
    suspend fun syncUnavailability(
        @Header("Authorization") token: String,
        @Body slots: List<Map<String, String>>
    ): Map<String, String>

    @GET("api/unavailability/")
    suspend fun getUnavailability(
        @Header("Authorization") token: String
    ): List<Map<String, String>>

    @POST("api/schedules/generate-auto/")
    suspend fun generateAutoSchedule(
        @Header("Authorization") token: String
    ): Map<String, Any>

    @GET("api/institutions/")
    suspend fun getInstitutions(
        @Header("Authorization") token: String
    ): List<Map<String, Any>>

    @POST("api/institutions/")
    suspend fun createInstitution(
        @Header("Authorization") token: String,
        @Body data: Map<String, String>
    ): Map<String, Any>

    @POST("api/users/create-admin/")
    suspend fun createAdmin(
        @Header("Authorization") token: String,
        @Body data: Map<String, String>
    ): Map<String, String>

    // --- YENİ: Kullanıcı Listesini Getir (Hocalar vb.) ---
    @GET("api/users/")
    suspend fun getUsers(
        @Header("Authorization") token: String
    ): List<Map<String, Any>>
}
