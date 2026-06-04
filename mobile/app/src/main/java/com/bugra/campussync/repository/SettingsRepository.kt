package com.bugra.campussync.repository

import com.bugra.campussync.network.ApiService
import com.bugra.campussync.network.NetworkResult
import com.bugra.campussync.utils.safeApiCall
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepository @Inject constructor(
    private val apiService: ApiService
) {
    suspend fun updateProfile(firstName: String, lastName: String, title: String, department: String): NetworkResult<Map<String, String>> {
        val data = mutableMapOf(
            "first_name" to firstName,
            "last_name" to lastName,
            "title" to title,
            "department" to department,
            "department_name" to department
        )
        return safeApiCall { apiService.updateProfile(data) }
    }
}
