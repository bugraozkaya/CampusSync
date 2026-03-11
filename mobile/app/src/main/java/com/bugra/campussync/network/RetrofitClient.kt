package com.bugra.campussync.network

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {
    // Emülatörden bilgisayarın localhost'una (Django'ya) erişmek için özel IP: 10.0.2.2
    private const val BASE_URL = "http://10.0.2.2:8000/"

    val apiService: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create()) // Gelen JSON'u Kotlin sınıflarına çevirir
            .build()
            .create(ApiService::class.java)
    }
}