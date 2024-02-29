package com.canopas.yourspace.data.repository

import com.canopas.yourspace.data.models.messages.ApiThread
import com.canopas.yourspace.data.models.messages.ApiThreadMessage
import com.canopas.yourspace.data.models.messages.ThreadInfo
import com.canopas.yourspace.data.models.user.UserInfo
import com.canopas.yourspace.data.service.auth.AuthService
import com.canopas.yourspace.data.service.messages.ApiMessagesService
import com.canopas.yourspace.data.service.user.ApiUserService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import javax.inject.Inject

class MessagesRepository @Inject constructor(
    private val apiMessagesService: ApiMessagesService,
    private val userService: ApiUserService,
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

    suspend fun getThreads(spaceId: String): Flow<List<ApiThread>> {
        val userId = authService.currentUser?.id ?: return  emptyFlow()
        return apiMessagesService.getThreads(spaceId,userId)
    }

    suspend fun getThreadsWithMessages(spaceId: String): Flow<List<ThreadInfo>> {
        val userId = authService.currentUser?.id ?: return  emptyFlow()
        return apiMessagesService.getThreadsWithLatestMessage(spaceId,userId)
    }

    suspend fun getThread(threadId: String) =
        apiMessagesService.getThread(threadId).map {
            val thread = it ?: return@map null
            val members =
                thread.member_ids.map { memberId -> userService.getUser(memberId) }.filterNotNull()
                    .map { UserInfo(it) }
            ThreadInfo(it, members)
        }

}