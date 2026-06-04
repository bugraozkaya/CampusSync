package com.bugra.campussync.repository

import com.bugra.campussync.network.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AttendanceRepository @Inject constructor(
    private val apiService: ApiService
) {
    suspend fun getSchedules(): PagedResponse<ScheduleItem> {
        return apiService.getSchedules()
    }

    suspend fun getMyAttendance(): List<AttendanceRecordItem> {
        return apiService.getMyAttendance()
    }

    suspend fun checkInAttendance(token: String): Map<String, String> {
        return apiService.checkInAttendance(mapOf("token" to token))
    }

    suspend fun createAttendanceSession(scheduleId: Int, sessionDate: String): AttendanceSessionItem {
        return apiService.createAttendanceSession(mapOf("schedule_id" to scheduleId, "session_date" to sessionDate))
    }

    suspend fun getSessionRecords(sessionId: Int): List<AttendanceRecordItem> {
        return apiService.getSessionRecords(sessionId)
    }

    suspend fun getMySessions(): List<AttendanceSessionItem> {
        return apiService.getMySessions()
    }
}
