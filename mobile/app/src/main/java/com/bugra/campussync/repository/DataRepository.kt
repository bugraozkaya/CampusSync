package com.bugra.campussync.repository

import com.bugra.campussync.network.*
import okhttp3.MultipartBody
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DataRepository @Inject constructor(
    private val apiService: ApiService
) {
    suspend fun getUsers(search: String? = null): PagedResponse<Map<String, Any>> {
        return apiService.getUsers(search)
    }

    suspend fun bulkImport(file: MultipartBody.Part): List<Map<String, String>> {
        return apiService.bulkImport(file)
    }

    suspend fun createAdmin(data: Map<String, String>): Map<String, String> {
        return apiService.createAdmin(data)
    }

    suspend fun createCourse(data: Map<String, String>): Map<String, Any> {
        return apiService.createCourse(data)
    }

    suspend fun getCourses(): PagedResponse<CourseItem> {
        return apiService.getCourses()
    }

    suspend fun getLecturerCourses(lecturerId: Int): List<Map<String, Any>> {
        return apiService.getLecturerCourses(lecturerId)
    }

    suspend fun assignCourse(lecturerId: Int, courseId: Int): Map<String, Any> {
        return apiService.assignCourseToLecturer(lecturerId, mapOf("course_id" to courseId))
    }

    suspend fun removeCourse(lecturerId: Int, courseId: Int) {
        apiService.removeCourseFromLecturer(lecturerId, courseId)
    }

    suspend fun getLecturerSchedules(lecturerId: Int): PagedResponse<Map<String, Any>> {
        return apiService.getLecturerSchedules(lecturerId)
    }

    suspend fun getLecturerUnavailability(userId: Int): List<Map<String, String>> {
        return apiService.getLecturerUnavailability(userId)
    }
}
