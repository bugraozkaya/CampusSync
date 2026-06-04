package com.bugra.campussync.repository

import com.bugra.campussync.network.ApiService
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserManagementRepository @Inject constructor(
    private val apiService: ApiService
) {
    suspend fun getUsers(search: String? = null) = apiService.getUsers(search = search)
}
