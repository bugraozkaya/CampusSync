package com.bugra.campussync.network

import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.http.*

interface ApiService {

    // --- Auth ---
    @POST("api/v1/token/")
    suspend fun login(@Body request: LoginRequest): LoginResponse

    // --- Schedules ---
    @GET("api/v1/schedules/")
    suspend fun getSchedules(): PagedResponse<ScheduleItem>

    @GET("api/v1/schedules/")
    suspend fun getScheduleDetails(): PagedResponse<Map<String, Any>>

    @POST("api/v1/schedules/")
    suspend fun createSchedule(@Body data: Map<String, String>): Map<String, Any>

    @DELETE("api/v1/schedules/{id}/")
    suspend fun deleteSchedule(@Path("id") id: Int): Unit

    @GET("api/v1/schedules/admin-summary/")
    suspend fun getAdminSummary(): Map<String, Any>

    @POST("api/v1/schedules/generate-auto/")
    suspend fun generateAutoSchedule(): Map<String, Any>

    @GET("api/v1/schedules/available_for_slot/")
    suspend fun getAvailableClassrooms(
        @Query("day") day: String,
        @Query("start_time") startTime: String,
        @Query("end_time") endTime: String,
        @Query("session_type") sessionType: String,
        @Query("course_id") courseId: String
    ): List<ClassroomItem>

    // --- Courses ---
    @GET("api/v1/courses/")
    suspend fun getCourses(): PagedResponse<CourseItem>

    @POST("api/v1/courses/")
    suspend fun createCourse(@Body data: Map<String, String>): Map<String, Any>

    @PUT("api/v1/courses/{id}/")
    suspend fun updateCourse(@Path("id") id: Int, @Body data: Map<String, String>): Map<String, Any>

    @DELETE("api/v1/courses/{id}/")
    suspend fun deleteCourse(@Path("id") id: Int): Unit

    @Multipart
    @POST("api/v1/courses/bulk-import-excel/")
    suspend fun bulkImport(@Part file: MultipartBody.Part): List<Map<String, String>>

    // --- Classrooms ---
    @GET("api/v1/classrooms/")
    suspend fun getClassrooms(): PagedResponse<ClassroomItem>

    @POST("api/v1/classrooms/")
    suspend fun createClassroom(@Body data: Map<String, Any>): Map<String, Any>

    @PUT("api/v1/classrooms/{id}/")
    suspend fun updateClassroom(@Path("id") id: Int, @Body data: Map<String, Any>): Map<String, Any>

    @DELETE("api/v1/classrooms/{id}/")
    suspend fun deleteClassroom(@Path("id") id: Int): Unit

    @Multipart
    @POST("api/v1/classrooms/bulk-import-excel/")
    suspend fun bulkImportClassrooms(@Part file: MultipartBody.Part): List<Map<String, String>>

    // --- Users ---
    @GET("api/v1/users/")
    suspend fun getUsers(@Query("search") search: String? = null): PagedResponse<Map<String, Any>>

    @POST("api/v1/users/create-admin/")
    suspend fun createAdmin(@Body data: Map<String, String>): Map<String, String>

    @POST("api/v1/users/change-password/")
    suspend fun changePassword(@Body data: Map<String, String>): Map<String, String>

    @PATCH("api/v1/users/update-profile/")
    suspend fun updateProfile(@Body data: Map<String, String>): Map<String, String>

    // --- Institutions ---
    @GET("api/v1/institutions/")
    suspend fun getInstitutions(): List<Map<String, Any>>

    @POST("api/v1/institutions/")
    suspend fun createInstitution(@Body data: Map<String, String>): Map<String, Any>

    // --- Departments ---
    @GET("api/v1/departments/")
    suspend fun getDepartments(): List<Map<String, Any>>

    @POST("api/v1/departments/")
    suspend fun createDepartment(@Body data: Map<String, String>): Map<String, Any>

    // --- Unavailability ---
    @GET("api/v1/unavailability/")
    suspend fun getUnavailability(): List<Map<String, String>>

    @POST("api/v1/unavailability/sync/")
    suspend fun syncUnavailability(@Body slots: List<Map<String, String>>): Map<String, String>

    // --- Enrollments ---
    @GET("api/v1/enrollments/")
    suspend fun getMyEnrollments(): List<EnrollmentItem>

    @POST("api/v1/enrollments/")
    suspend fun enrollCourse(@Body body: Map<String, Int>): EnrollmentItem

    @DELETE("api/v1/enrollments/{id}/")
    suspend fun unenrollCourse(@Path("id") id: Int): retrofit2.Response<Unit>

    @GET("api/v1/enrollments/my_schedule/")
    suspend fun getStudentSchedule(): List<ScheduleItem>

    // --- Announcements ---
    @GET("api/v1/announcements/")
    suspend fun getAnnouncements(): PagedResponse<AnnouncementItem>

    @POST("api/v1/announcements/")
    suspend fun createAnnouncement(@Body body: Map<String, String>): AnnouncementItem

    @POST("api/v1/announcements/{id}/mark_read/")
    suspend fun markAnnouncementRead(@Path("id") id: Int): retrofit2.Response<Unit>

    @POST("api/v1/announcements/mark_all_read/")
    suspend fun markAllRead(): retrofit2.Response<Unit>

    @GET("api/v1/announcements/unread_count/")
    suspend fun getUnreadCount(): Map<String, Int>

    // --- FCM ---
    @POST("api/v1/fcm/register/")
    suspend fun registerFcmToken(@Body body: Map<String, String>): retrofit2.Response<Unit>

    // --- PDF Export ---
    @Streaming
    @GET("api/v1/schedules/export_pdf/")
    suspend fun exportSchedulePdf(
        @Query("type") type: String = "institution"
    ): ResponseBody

    // --- Attendance ---
    @POST("api/v1/attendance/create_session/")
    suspend fun createAttendanceSession(@Body body: Map<String, Any>): AttendanceSessionItem

    @POST("api/v1/attendance/check_in/")
    suspend fun checkInAttendance(@Body body: Map<String, String>): Map<String, String>

    @GET("api/v1/attendance/session_records/")
    suspend fun getSessionRecords(@Query("session_id") sessionId: Int): List<AttendanceRecordItem>

    @GET("api/v1/attendance/my_sessions/")
    suspend fun getMySessions(): List<AttendanceSessionItem>

    @GET("api/v1/attendance/my_attendance/")
    suspend fun getMyAttendance(): List<AttendanceRecordItem>

    // --- Chat ---
    @GET("api/v1/chat/inbox/")
    suspend fun getChatInbox(): List<ChatConversation>

    @GET("api/v1/chat/messages/")
    suspend fun getChatMessages(@Query("partner_id") partnerId: Int): List<ChatMessage>

    @POST("api/v1/chat/send/")
    suspend fun sendChatMessage(@Body body: Map<String, Any>): ChatMessage

    @GET("api/v1/chat/contacts/")
    suspend fun getChatContacts(): List<ChatContact>

    @GET("api/v1/chat/unread_count/")
    suspend fun getChatUnreadCount(): Map<String, Int>

    // --- Materials ---
    @GET("api/v1/materials/")
    suspend fun getMaterials(@Query("course_id") courseId: Int? = null): PagedResponse<CourseMaterialItem>

    @Multipart
    @POST("api/v1/materials/")
    suspend fun uploadMaterial(
        @Part("course") course: RequestBody,
        @Part("title") title: RequestBody,
        @Part("description") description: RequestBody,
        @Part("material_type") materialType: RequestBody,
        @Part file: MultipartBody.Part
    ): CourseMaterialItem

    @DELETE("api/v1/materials/{id}/")
    suspend fun deleteMaterial(@Path("id") id: Int): retrofit2.Response<Unit>

    // --- Grades ---
    @GET("api/v1/grades/my_grades/")
    suspend fun getMyGrades(): PagedResponse<GradeItem>

    @GET("api/v1/grades/course_grades/")
    suspend fun getCourseGrades(@Query("course_id") courseId: Int): PagedResponse<GradeItem>

    @POST("api/v1/grades/")
    suspend fun createGrade(@Body body: Map<String, Any>): GradeItem

    @PUT("api/v1/grades/{id}/")
    suspend fun updateGrade(@Path("id") id: Int, @Body body: Map<String, Any>): GradeItem

    @DELETE("api/v1/grades/{id}/")
    suspend fun deleteGrade(@Path("id") id: Int): retrofit2.Response<Unit>
}
