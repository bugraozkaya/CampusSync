package com.bugra.campussync.repository

import com.bugra.campussync.network.*
import okhttp3.MultipartBody
import okhttp3.RequestBody
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CourseMaterialsRepository @Inject constructor(
    private val apiService: ApiService
) {
    suspend fun getMaterials(courseId: Int? = null): PagedResponse<CourseMaterialItem> {
        return apiService.getMaterials(courseId)
    }

    suspend fun getCourses(): PagedResponse<CourseItem> {
        return apiService.getCourses()
    }

    suspend fun uploadMaterial(
        course: RequestBody,
        title: RequestBody,
        description: RequestBody,
        materialType: RequestBody,
        file: MultipartBody.Part
    ): CourseMaterialItem {
        return apiService.uploadMaterial(course, title, description, materialType, file)
    }

    suspend fun deleteMaterial(id: Int): retrofit2.Response<Unit> {
        return apiService.deleteMaterial(id)
    }
}
