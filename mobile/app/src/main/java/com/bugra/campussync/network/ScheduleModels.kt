package com.bugra.campussync.network

import com.google.gson.annotations.SerializedName

data class PagedResponse<T>(
    val count: Int = 0,
    val next: String? = null,
    val previous: String? = null,
    val results: List<T> = emptyList()
)

data class ScheduleItem(
    val id: Int,
    @SerializedName("course") val courseId: Int,
    val course_code: String? = null,
    val course_name: String,
    @SerializedName("day_of_week") val day: String,
    val start_time: String,
    val end_time: String,
    @SerializedName("classroom") val classroomId: Int? = null,
    val classroom_name: String? = null,
    @SerializedName("lecturer") val lecturerId: Int? = null,
    @SerializedName("lecturer_id_val") val lecturerIdVal: Int? = null,
    val lecturer_name: String? = null,
    val lecturer_username: String? = null,
    val session_type: String? = "LECTURE"
)

data class CourseItem(
    val id: Int,
    val course_code: String,
    val course_name: String,
    val department: Int,
    val department_name: String? = null,
    val has_lab: Boolean = false,
    val weekly_hours: Int = 2,
    val lab_hours: Int = 0
)

data class ClassroomItem(
    val id: Int,
    val room_code: String,
    val capacity: Int,
    val classroom_type: String = "LECTURE",
    val classroom_type_display: String? = null
)

data class ImportedCourse(
    val department: String,
    val code: String,
    val name: String,
    val lecturer: String
)

data class EnrollmentItem(
    val id: Int,
    val student: Int,
    val student_username: String,
    val course: Int,
    val course_code: String,
    val course_name: String,
    val department_name: String,
    val enrolled_at: String
)

data class AnnouncementItem(
    val id: Int,
    val title: String,
    val body: String,
    val audience: String,
    val created_by_name: String,
    val created_at: String,
    val is_active: Boolean,
    val unread_count: Int = 0,
    val is_read: Boolean = false
)

data class AttendanceSessionItem(
    val id: Int,
    val schedule: Int,
    val course_code: String,
    val course_name: String,
    val token: String,
    val token_str: String,
    val created_at: String,
    val expires_at: String,
    val is_active: Boolean,
    val is_expired: Boolean,
    val session_date: String,
    val record_count: Int
)

data class AttendanceRecordItem(
    val id: Int,
    val session: Int,
    val student: Int,
    val student_username: String,
    val student_first_name: String,
    val student_last_name: String,
    val checked_in_at: String
)

data class ChatContact(
    val id: Int,
    val username: String,
    val name: String,
    val role: String
)

data class ChatConversation(
    val partner_id: Int,
    val partner_username: String,
    val partner_name: String,
    val partner_role: String,
    val last_message: String,
    val last_message_at: String,
    val unread_count: Int,
    val is_mine: Boolean
)

data class ChatMessage(
    val id: Int,
    val sender: Int,
    val sender_name: String,
    val sender_username: String = "",
    val receiver: Int,
    val receiver_name: String,
    val content: String,
    val created_at: String,
    val is_read: Boolean
)

data class CourseMaterialItem(
    val id: Int,
    val course: Int,
    val course_code: String,
    val course_name: String,
    val uploaded_by: Int,
    val uploaded_by_name: String,
    val title: String,
    val description: String,
    val file: String,
    val file_url: String?,
    val material_type: String,
    val material_type_display: String,
    val created_at: String
)

data class GradeItem(
    val id: Int,
    val student: Int,
    val student_username: String,
    val student_name: String,
    val course: Int,
    val course_code: String,
    val course_name: String,
    val grade_type: String,
    val grade_type_display: String,
    val score: Double,
    val max_score: Double,
    val percentage: Double,
    val graded_by: Int?,
    val notes: String,
    val created_at: String
)
