package com.bugra.campussync.repository

import com.bugra.campussync.network.ApiService
import okhttp3.ResponseBody
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AvailabilityRepository @Inject constructor(
    private val apiService: ApiService
) {
    suspend fun getUnavailability(): List<Map<String, String>> {
        return apiService.getUnavailability()
    }

    suspend fun syncUnavailability(slots: List<Map<String, String>>): ResponseBody {
        return apiService.syncUnavailability(slots)
    }
}
