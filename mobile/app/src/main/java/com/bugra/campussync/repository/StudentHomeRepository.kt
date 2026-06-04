package com.bugra.campussync.repository

import com.bugra.campussync.network.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StudentHomeRepository @Inject constructor(
    private val apiService: ApiService
) {
    suspend fun getMyEnrollments(): List<EnrollmentItem> {
        return apiService.getMyEnrollments()
    }

    suspend fun getStudentSchedule(): List<ScheduleItem> {
        return apiService.getStudentSchedule()
    }

    suspend fun getCourses(): PagedResponse<CourseItem> {
        return apiService.getCourses()
    }

    suspend fun enrollCourse(courseId: Int): EnrollmentItem {
        return apiService.enrollCourse(mapOf("course" to courseId))
    }

    suspend fun unenrollCourse(id: Int): retrofit2.Response<Unit> {
        return apiService.unenrollCourse(id)
    }
}
