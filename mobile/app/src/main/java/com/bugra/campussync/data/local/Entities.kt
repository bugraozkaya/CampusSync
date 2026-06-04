package com.bugra.campussync.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "announcements")
data class AnnouncementEntity(
    @PrimaryKey val id: Int,
    val title: String,
    val content: String,
    val created_at: String,
    val is_read: Boolean,
    val type: String
)

@Entity(tableName = "schedules")
data class ScheduleEntity(
    @PrimaryKey val id: Int,
    val course_id: Int,
    val course_name: String,
    val course_code: String?,
    val day: String,
    val start_time: String,
    val end_time: String,
    val classroom_name: String?
)
