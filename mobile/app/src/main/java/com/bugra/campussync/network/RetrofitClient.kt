package com.bugra.campussync.network

import android.content.Context
import com.bugra.campussync.utils.TokenManager
import com.google.gson.GsonBuilder
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {
    const val BASE_URL = "http://10.0.2.2:8000/"

    @Volatile
    var authToken: String? = null

    private var _apiService: ApiService? = null
    val apiService: ApiService
        get() = _apiService ?: error("RetrofitClient.init(context) çağrılmadı")

    fun init(context: Context) {
        val tokenManager = TokenManager(context.applicationContext)
        tokenManager.getToken()?.let { authToken = it }

        val authenticator = TokenAuthenticator(tokenManager, BASE_URL)

        val authInterceptor = Interceptor { chain ->
            val builder = chain.request().newBuilder()
            authToken?.let { builder.header("Authorization", "Bearer $it") }
            builder.header("Accept", "application/json; charset=utf-8")
            builder.header("Accept-Charset", "utf-8")
            chain.proceed(builder.build())
        }

        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val client = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(authInterceptor)
            .addInterceptor(logging)
            .authenticator(authenticator)
            .build()

        val gson = GsonBuilder()
            .disableHtmlEscaping()
            .create()

        _apiService = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
            .create(ApiService::class.java)
    }
}
