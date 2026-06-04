package com.bugra.campussync.repository

import com.bugra.campussync.network.ApiService
import com.bugra.campussync.network.LoginRequest
import com.bugra.campussync.network.LoginResponse
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val apiService: ApiService
) {
    suspend fun login(request: LoginRequest): LoginResponse {
        return apiService.login(request)
    }

    suspend fun forgotPassword(username: String): Map<String, String> {
        return apiService.forgotPassword(mapOf("username" to username))
    }
}
