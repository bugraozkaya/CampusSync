package com.bugra.campussync.repository

import com.bugra.campussync.network.ApiService
import com.bugra.campussync.network.NetworkResult
import com.bugra.campussync.utils.safeApiCall
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChangePasswordRepository @Inject constructor(
    private val apiService: ApiService
) {
    suspend fun changePassword(currentPassword: String, newPassword: String): NetworkResult<Map<String, String>> {
        return safeApiCall {
            apiService.changePassword(
                mapOf("current_password" to currentPassword, "new_password" to newPassword)
            )
        }
    }
}
