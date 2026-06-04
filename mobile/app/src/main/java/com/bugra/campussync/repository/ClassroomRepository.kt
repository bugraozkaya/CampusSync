package com.bugra.campussync.repository

import com.bugra.campussync.network.*
import com.bugra.campussync.utils.safeApiCall
import okhttp3.MultipartBody
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ClassroomRepository @Inject constructor(
    private val apiService: ApiService
) {
    suspend fun getClassrooms(): NetworkResult<PagedResponse<ClassroomItem>> {
        return safeApiCall { apiService.getClassrooms() }
    }

    suspend fun createClassroom(roomCode: String, capacity: Int, type: String): NetworkResult<Map<String, Any>> {
        return safeApiCall {
            apiService.createClassroom(
                mapOf("room_code" to roomCode, "capacity" to capacity.toString(), "classroom_type" to type)
            )
        }
    }

    suspend fun bulkImportClassrooms(filePart: MultipartBody.Part): NetworkResult<List<Map<String, String>>> {
        return safeApiCall { apiService.bulkImportClassrooms(filePart) }
    }

    suspend fun updateClassroom(id: Int, roomCode: String, capacity: Int, type: String): NetworkResult<Map<String, Any>> {
        return safeApiCall {
            apiService.updateClassroom(id, mapOf("room_code" to roomCode, "capacity" to capacity.toString(), "classroom_type" to type))
        }
    }

    suspend fun deleteClassroom(id: Int): NetworkResult<Unit> {
        return try {
            val response = apiService.deleteClassroom(id)
            if (response.isSuccessful) NetworkResult.Success(Unit)
            else NetworkResult.Error("Silme hatası: ${response.code()}", response.code())
        } catch (e: retrofit2.HttpException) {
            NetworkResult.Error("Silme hatası: ${e.code()}", e.code())
        } catch (e: Exception) {
            NetworkResult.Error("Sunucuya ulaşılamıyor.")
        }
    }
}
