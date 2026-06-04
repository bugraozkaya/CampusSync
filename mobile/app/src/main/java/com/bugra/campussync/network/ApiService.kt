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
    suspend fun getScheduleDetails(): PagedResponse<@JvmSuppressWildcards Map<String, Any>>

    @POST("api/v1/schedules/")
    suspend fun createSchedule(@Body data: @JvmSuppressWildcards Map<String, String>): @JvmSuppressWildcards Map<String, Any>

    @DELETE("api/v1/schedules/{id}/")
    suspend fun deleteSchedule(@Path("id") id: Int): Unit

    @GET("api/v1/schedules/admin-summary/")
    suspend fun getAdminSummary(): @JvmSuppressWildcards Map<String, Any>

    @POST("api/v1/schedules/generate-auto/")
    suspend fun generateAutoSchedule(): @JvmSuppressWildcards Map<String, Any>

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
    suspend fun createCourse(@Body data: @JvmSuppressWildcards Map<String, String>): @JvmSuppressWildcards Map<String, Any>

    @PUT("api/v1/courses/{id}/")
    suspend fun updateCourse(@Path("id") id: Int, @Body data: @JvmSuppressWildcards Map<String, String>): @JvmSuppressWildcards Map<String, Any>

    @DELETE("api/v1/courses/{id}/")
    suspend fun deleteCourse(@Path("id") id: Int): Unit

    @Multipart
    @POST("api/v1/courses/bulk-import-excel/")
    suspend fun bulkImport(@Part file: MultipartBody.Part): List<@JvmSuppressWildcards Map<String, String>>

    // --- Classrooms ---
    @GET("api/v1/classrooms/")
    suspend fun getClassrooms(): PagedResponse<ClassroomItem>

    @POST("api/v1/classrooms/")
    suspend fun createClassroom(@Body data: @JvmSuppressWildcards Map<String, String>): @JvmSuppressWildcards Map<String, Any>

    @PATCH("api/v1/classrooms/{id}/")
    suspend fun updateClassroom(@Path("id") id: Int, @Body data: @JvmSuppressWildcards Map<String, String>): @JvmSuppressWildcards Map<String, Any>

    @DELETE("api/v1/classrooms/{id}/")
    suspend fun deleteClassroom(@Path("id") id: Int): retrofit2.Response<Unit>

    @Multipart
    @POST("api/v1/classrooms/bulk-import-excel/")
    suspend fun bulkImportClassrooms(@Part file: MultipartBody.Part): List<@JvmSuppressWildcards Map<String, String>>

    // --- Users ---
    @GET("api/v1/users/")
    suspend fun getUsers(@Query("search") search: String? = null): PagedResponse<@JvmSuppressWildcards Map<String, Any>>

    @POST("api/v1/users/create-admin/")
    suspend fun createAdmin(@Body data: @JvmSuppressWildcards Map<String, String>): @JvmSuppressWildcards Map<String, String>

    @POST("api/v1/users/change-password/")
    suspend fun changePassword(@Body data: @JvmSuppressWildcards Map<String, String>): @JvmSuppressWildcards Map<String, String>

    @POST("api/v1/forgot-password/")
    suspend fun forgotPassword(@Body data: @JvmSuppressWildcards Map<String, String>): @JvmSuppressWildcards Map<String, String>

    @PATCH("api/v1/users/update-profile/")
    suspend fun updateProfile(@Body data: @JvmSuppressWildcards Map<String, String>): @JvmSuppressWildcards Map<String, String>

    @GET("api/v1/users/{id}/courses/")
    suspend fun getLecturerCourses(@Path("id") id: Int): List<@JvmSuppressWildcards Map<String, Any>>

    @POST("api/v1/users/{id}/courses/")
    suspend fun assignCourseToLecturer(@Path("id") id: Int, @Body body: @JvmSuppressWildcards Map<String, Int>): @JvmSuppressWildcards Map<String, Any>

    @DELETE("api/v1/users/{id}/courses/{courseId}/")
    suspend fun removeCourseFromLecturer(@Path("id") id: Int, @Path("courseId") courseId: Int): retrofit2.Response<Unit>

    // --- Institutions ---
    @GET("api/v1/institutions/")
    suspend fun getInstitutions(): List<@JvmSuppressWildcards Map<String, Any>>

    @POST("api/v1/institutions/")
    suspend fun createInstitution(@Body data: @JvmSuppressWildcards Map<String, String>): @JvmSuppressWildcards Map<String, Any>

    // --- Departments ---
    @GET("api/v1/departments/")
    suspend fun getDepartments(): List<@JvmSuppressWildcards Map<String, Any>>

    @POST("api/v1/departments/")
    suspend fun createDepartment(@Body data: @JvmSuppressWildcards Map<String, String>): @JvmSuppressWildcards Map<String, Any>

    // --- Unavailability ---
    @GET("api/v1/unavailability/")
    suspend fun getUnavailability(): List<@JvmSuppressWildcards Map<String, String>>

    @GET("api/v1/unavailability/")
    suspend fun getLecturerUnavailability(@Query("user_id") userId: Int): List<@JvmSuppressWildcards Map<String, String>>

    @GET("api/v1/schedules/")
    suspend fun getLecturerSchedules(@Query("lecturer_id") lecturerId: Int): PagedResponse<@JvmSuppressWildcards Map<String, Any>>

    @POST("api/v1/unavailability/sync/")
    suspend fun syncUnavailability(@Body slots: List<@JvmSuppressWildcards Map<String, String>>): okhttp3.ResponseBody

    // --- Enrollments ---
    @GET("api/v1/enrollments/")
    suspend fun getMyEnrollments(): List<EnrollmentItem>

    @POST("api/v1/enrollments/")
    suspend fun enrollCourse(@Body body: @JvmSuppressWildcards Map<String, Int>): EnrollmentItem

    @DELETE("api/v1/enrollments/{id}/")
    suspend fun unenrollCourse(@Path("id") id: Int): retrofit2.Response<Unit>

    @GET("api/v1/enrollments/my_schedule/")
    suspend fun getStudentSchedule(): List<ScheduleItem>

    // --- Announcements ---
    @GET("api/v1/announcements/")
    suspend fun getAnnouncements(): PagedResponse<AnnouncementItem>

    @POST("api/v1/announcements/")
    suspend fun createAnnouncement(@Body body: @JvmSuppressWildcards Map<String, Any>): AnnouncementItem

    @POST("api/v1/announcements/{id}/mark_read/")
    suspend fun markAnnouncementRead(@Path("id") id: Int): retrofit2.Response<Unit>

    @POST("api/v1/announcements/mark_all_read/")
    suspend fun markAllRead(): retrofit2.Response<Unit>

    @GET("api/v1/announcements/unread_count/")
    suspend fun getUnreadCount(): @JvmSuppressWildcards Map<String, Int>

    // --- FCM ---
    @POST("api/v1/fcm/register/")
    suspend fun registerFcmToken(@Body body: @JvmSuppressWildcards Map<String, String>): retrofit2.Response<Unit>

    // --- PDF Export ---
    @Streaming
    @GET("api/v1/schedules/export_pdf/")
    suspend fun exportSchedulePdf(
        @Query("type") type: String = "institution"
    ): ResponseBody

    // --- Attendance ---
    @POST("api/v1/attendance/create_session/")
    suspend fun createAttendanceSession(@Body body: @JvmSuppressWildcards Map<String, Any>): AttendanceSessionItem

    @POST("api/v1/attendance/check_in/")
    suspend fun checkInAttendance(@Body body: @JvmSuppressWildcards Map<String, String>): @JvmSuppressWildcards Map<String, String>

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
    suspend fun sendChatMessage(@Body body: SendMessageRequest): ChatMessage

    @Multipart
    @POST("api/v1/chat/send/")
    suspend fun sendChatMessageWithFile(
        @Part("receiver_id") receiverId: RequestBody,
        @Part("content") content: RequestBody,
        @Part file: MultipartBody.Part
    ): ChatMessage

    @GET("api/v1/chat/contacts/")
    suspend fun getChatContacts(): List<ChatContact>

    @GET("api/v1/chat/unread_count/")
    suspend fun getChatUnreadCount(): @JvmSuppressWildcards Map<String, Int>

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

    // --- Notes ---
    @GET("api/v1/notes/")
    suspend fun getNotes(@Query("course_id") courseId: Int): List<CourseNoteItem>

    @POST("api/v1/notes/")
    suspend fun createNote(@Body body: @JvmSuppressWildcards Map<String, Any>): CourseNoteItem

    @DELETE("api/v1/notes/{id}/")
    suspend fun deleteNote(@Path("id") id: Int): retrofit2.Response<Unit>

    // --- Grades ---
    @GET("api/v1/grades/my_grades/")
    suspend fun getMyGrades(): PagedResponse<GradeItem>

    @GET("api/v1/grades/course_grades/")
    suspend fun getCourseGrades(@Query("course_id") courseId: Int): PagedResponse<GradeItem>

    @POST("api/v1/grades/")
    suspend fun createGrade(@Body body: @JvmSuppressWildcards Map<String, Any>): GradeItem

    @PUT("api/v1/grades/{id}/")
    suspend fun updateGrade(@Path("id") id: Int, @Body body: @JvmSuppressWildcards Map<String, Any>): GradeItem

    @DELETE("api/v1/grades/{id}/")
    suspend fun deleteGrade(@Path("id") id: Int): retrofit2.Response<Unit>
}
