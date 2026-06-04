package com.bugra.campussync.repository

import com.bugra.campussync.network.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CalendarRepository @Inject constructor(
    private val apiService: ApiService
) {
    suspend fun getScheduleDetails(): PagedResponse<Map<String, Any>> {
        return apiService.getScheduleDetails()
    }

    suspend fun getUsers(search: String? = null): PagedResponse<Map<String, Any>> {
        return apiService.getUsers(search)
    }

    suspend fun getClassrooms(): PagedResponse<ClassroomItem> {
        return apiService.getClassrooms()
    }

    suspend fun getCourses(): PagedResponse<CourseItem> {
        return apiService.getCourses()
    }

    suspend fun getAvailableClassrooms(
        day: String, startTime: String, endTime: String,
        sessionType: String, courseId: String
    ): List<ClassroomItem> {
        return apiService.getAvailableClassrooms(day, startTime, endTime, sessionType, courseId)
    }

    suspend fun createSchedule(data: Map<String, String>): Map<String, Any> {
        return apiService.createSchedule(data)
    }

    suspend fun deleteSchedule(id: Int) {
        apiService.deleteSchedule(id)
    }

    suspend fun generateAutoSchedule(): Map<String, Any> {
        return apiService.generateAutoSchedule()
    }
}
