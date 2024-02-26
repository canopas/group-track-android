package com.canopas.yourspace.data.repository

import com.canopas.yourspace.data.models.messages.ApiThreadMessage
import com.canopas.yourspace.data.service.auth.AuthService
import com.canopas.yourspace.data.service.messages.ApiMessagesService
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class MessagesRepository @Inject constructor(
    private val apiMessagesService: ApiMessagesService,
    private val authService: AuthService
) {

    suspend fun createThread(spaceId: String, adminId: String, memberIds: List<String>): String {
        val threadId = apiMessagesService.createThread(spaceId, adminId)
        apiMessagesService.joinThread(threadId, memberIds)
        return threadId
    }

    suspend fun sendMessage(
        message: String,
        senderId: String,
        threadId: String,
    ) {

        apiMessagesService.sendMessage(threadId, senderId, message)

    }

    suspend fun getMessages(threadId: String) = apiMessagesService.getMessages(threadId)


}