package com.bugra.campussync.repository

import com.bugra.campussync.data.local.AnnouncementDao
import com.bugra.campussync.data.local.AnnouncementEntity
import com.bugra.campussync.network.AnnouncementItem
import com.bugra.campussync.network.ApiService
import com.bugra.campussync.network.CourseItem
import com.bugra.campussync.network.PagedResponse
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AnnouncementsRepository @Inject constructor(
    private val apiService: ApiService,
    private val announcementDao: AnnouncementDao
) {
    val localAnnouncements: Flow<List<AnnouncementItem>> = announcementDao.getAllAnnouncements().map { entities ->
        entities.map { it.toDomain() }
    }

    suspend fun getAnnouncements(): PagedResponse<AnnouncementItem> {
        val response = apiService.getAnnouncements()
        // Cache the results
        announcementDao.deleteAll()
        announcementDao.insertAnnouncements(response.results.map { it.toEntity() })
        return response
    }

    suspend fun markAnnouncementRead(id: Int): retrofit2.Response<Unit> {
        return apiService.markAnnouncementRead(id)
    }

    suspend fun markAllRead(): retrofit2.Response<Unit> {
        return apiService.markAllRead()
    }

    suspend fun getCourses(): List<CourseItem> = apiService.getCourses().results

    suspend fun createAnnouncement(title: String, body: String, audience: String, courseId: Int? = null): AnnouncementItem {
        val body2 = mutableMapOf<String, Any>("title" to title, "body" to body, "audience" to audience)
        if (courseId != null) body2["course"] = courseId
        return apiService.createAnnouncement(body2)
    }

    private fun AnnouncementItem.toEntity() = AnnouncementEntity(
        id = id,
        title = title,
        content = body,
        created_at = created_at,
        is_read = is_read,
        type = audience
    )

    private fun AnnouncementEntity.toDomain() = AnnouncementItem(
        id = id,
        title = title,
        body = content,
        created_at = created_at,
        is_read = is_read,
        audience = type,
        created_by_name = "", // Not stored in local for now
        is_active = true
    )
}
