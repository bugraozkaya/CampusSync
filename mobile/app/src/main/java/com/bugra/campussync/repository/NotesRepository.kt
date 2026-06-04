package com.bugra.campussync.repository

import com.bugra.campussync.network.ApiService
import com.bugra.campussync.network.CourseNoteItem
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotesRepository @Inject constructor(
    private val apiService: ApiService
) {
    suspend fun getNotes(courseId: Int): List<CourseNoteItem> =
        apiService.getNotes(courseId)

    suspend fun createNote(courseId: Int, title: String, content: String): CourseNoteItem =
        apiService.createNote(mapOf("course" to courseId, "title" to title, "content" to content))

    suspend fun deleteNote(id: Int): retrofit2.Response<Unit> =
        apiService.deleteNote(id)
}
