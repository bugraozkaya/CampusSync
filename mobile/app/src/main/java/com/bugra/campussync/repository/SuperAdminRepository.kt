package com.bugra.campussync.repository

import com.bugra.campussync.network.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SuperAdminRepository @Inject constructor(
    private val apiService: ApiService
) {
    suspend fun getInstitutions(): List<Map<String, Any>> {
        return apiService.getInstitutions()
    }

    suspend fun createInstitution(data: Map<String, String>): Map<String, Any> {
        return apiService.createInstitution(data)
    }

    suspend fun createAdmin(data: Map<String, String>): Map<String, String> {
        return apiService.createAdmin(data)
    }
}
