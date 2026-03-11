package com.bugra.campussync.network

import retrofit2.http.Body
import retrofit2.http.POST

interface ApiService {
    // Django'da oluşturduğumuz token alma linki
    @POST("api/token/")
    suspend fun login(@Body request: LoginRequest): LoginResponse
}