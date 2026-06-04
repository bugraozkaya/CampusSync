package com.bugra.campussync.repository

import com.bugra.campussync.network.*
import com.bugra.campussync.utils.safeApiCall
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatRepository @Inject constructor(
    private val apiService: ApiService
) {
    suspend fun getChatInbox(): NetworkResult<List<ChatConversation>> {
        return safeApiCall { apiService.getChatInbox() }
    }

    suspend fun getChatMessages(partnerId: Int): NetworkResult<List<ChatMessage>> {
        return safeApiCall { apiService.getChatMessages(partnerId) }
    }

    suspend fun sendChatMessage(receiverId: Int, content: String): NetworkResult<ChatMessage> {
        return safeApiCall { apiService.sendChatMessage(SendMessageRequest(receiver_id = receiverId, content = content)) }
    }

    suspend fun sendChatMessageWithFile(
        receiverId: Int, content: String,
        file: okhttp3.MultipartBody.Part
    ): NetworkResult<ChatMessage> {
        val rId = receiverId.toString().toRequestBody("text/plain".toMediaTypeOrNull())
        val cont = content.toRequestBody("text/plain".toMediaTypeOrNull())
        return safeApiCall { apiService.sendChatMessageWithFile(rId, cont, file) }
    }

    suspend fun getChatContacts(): NetworkResult<List<ChatContact>> {
        return safeApiCall { apiService.getChatContacts() }
    }

    suspend fun getChatUnreadCount(): NetworkResult<Map<String, Int>> {
        return safeApiCall { apiService.getChatUnreadCount() }
    }
}
