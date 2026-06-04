package com.bugra.campussync.navigation

import kotlinx.serialization.Serializable

@Serializable
sealed interface Screen {
    @Serializable data object Onboarding : Screen
    @Serializable data object Auth : Screen
    @Serializable data object ChangePassword : Screen
    @Serializable data object Home : Screen
    @Serializable data object Calendar : Screen
    @Serializable data object Classrooms : Screen
    @Serializable data object Data : Screen
    @Serializable data object Availability : Screen
    @Serializable data object Users : Screen
    @Serializable data object SuperAdmin : Screen
    @Serializable data object StudentHome : Screen
    @Serializable data object Announcements : Screen
    @Serializable data object Attendance : Screen
    @Serializable data object ChatInbox : Screen
    @Serializable data object Materials : Screen
    @Serializable data object Grades : Screen
    @Serializable data object Settings : Screen
    @Serializable data object CourseContent : Screen
    
    @Serializable
    data class Chat(val partnerId: Int, val partnerName: String) : Screen

    @Serializable
    data class CourseDetail(val courseId: Int, val courseName: String, val courseCode: String) : Screen
}
