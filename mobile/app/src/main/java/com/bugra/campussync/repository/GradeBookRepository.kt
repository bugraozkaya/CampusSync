package com.bugra.campussync.repository

import com.bugra.campussync.network.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GradeBookRepository @Inject constructor(
    private val apiService: ApiService
) {
    suspend fun getCourses(): PagedResponse<CourseItem> {
        return apiService.getCourses()
    }

    suspend fun getMyGrades(): PagedResponse<GradeItem> {
        return apiService.getMyGrades()
    }

    suspend fun getCourseGrades(courseId: Int): PagedResponse<GradeItem> {
        return apiService.getCourseGrades(courseId)
    }

    suspend fun createGrade(body: Map<String, Any>): GradeItem {
        return apiService.createGrade(body)
    }

    suspend fun deleteGrade(id: Int): retrofit2.Response<Unit> {
        return apiService.deleteGrade(id)
    }

    suspend fun getUsers(search: String? = null): PagedResponse<Map<String, Any>> {
        return apiService.getUsers(search)
    }
}
