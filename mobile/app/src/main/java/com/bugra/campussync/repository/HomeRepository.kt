package com.bugra.campussync.repository

import com.bugra.campussync.data.local.ScheduleDao
import com.bugra.campussync.data.local.ScheduleEntity
import com.bugra.campussync.network.ApiService
import com.bugra.campussync.network.PagedResponse
import com.bugra.campussync.network.ScheduleItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HomeRepository @Inject constructor(
    private val apiService: ApiService,
    private val scheduleDao: ScheduleDao
) {
    val localSchedules: Flow<List<ScheduleItem>> = scheduleDao.getAllSchedules().map { entities ->
        entities.map { it.toDomain() }
    }

    suspend fun getSchedules(): PagedResponse<ScheduleItem> {
        val response = apiService.getSchedules()
        // Cache the results
        scheduleDao.deleteAll()
        scheduleDao.insertSchedules(response.results.map { it.toEntity() })
        return response
    }

    suspend fun getAdminSummary(): Map<String, Any> {
        return apiService.getAdminSummary()
    }

    suspend fun getUnreadCount(): Map<String, Int> {
        return apiService.getUnreadCount()
    }

    suspend fun exportSchedulePdf(type: String) = apiService.exportSchedulePdf(type)

    private fun ScheduleItem.toEntity() = ScheduleEntity(
        id = id,
        course_id = courseId,
        course_name = course_name,
        course_code = course_code,
        day = day,
        start_time = start_time,
        end_time = end_time,
        classroom_name = classroom_name
    )

    private fun ScheduleEntity.toDomain() = ScheduleItem(
        id = id,
        courseId = course_id,
        course_name = course_name,
        course_code = course_code,
        day = day,
        start_time = start_time,
        end_time = end_time,
        classroom_name = classroom_name,
        lecturer_name = "" // Not stored in local for now
    )
}
