package com.canopas.yourspace.data.service.messages

import com.canopas.yourspace.data.models.messages.ApiThread
import com.canopas.yourspace.data.models.messages.ApiThreadMember
import com.canopas.yourspace.data.models.messages.ApiThreadMessage
import com.canopas.yourspace.data.models.messages.ThreadInfo
import com.canopas.yourspace.data.service.user.ApiUserService
import com.canopas.yourspace.data.utils.Config
import com.canopas.yourspace.data.utils.Config.FIRESTORE_COLLECTION_SPACE_THREADS
import com.canopas.yourspace.data.utils.snapshotFlow
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class ApiMessagesService @Inject constructor(
    private val db: FirebaseFirestore,
    private val userService: ApiUserService
) {

    private val threadRef = db.collection(FIRESTORE_COLLECTION_SPACE_THREADS)

    private fun threadMemberRef(threadId: String) =
        threadRef.document(threadId).collection(Config.FIRESTORE_COLLECTION_THREAD_MEMBERS)

    private fun threadMessagesRef(threadId: String) =
        threadRef.document(threadId).collection(Config.FIRESTORE_COLLECTION_THREAD_MESSAGES)

    suspend fun createThread(spaceId: String, adminId: String): String {
        val docRef = threadRef.document()
        val threadId = docRef.id
        val thread = ApiThread(
            id = threadId,
            space_id = spaceId,
            admin_id = adminId,
            created_at = System.currentTimeMillis()
        )
        docRef.set(thread).await()
        joinThread(threadId, adminId)
        return threadId
    }

    suspend fun joinThread(threadId: String, userId: String) {
        val docRef = threadMemberRef(threadId).document(userId)
        val member = ApiThreadMember(
            thread_id = threadId,
            user_id = userId,
            created_at = System.currentTimeMillis()
        )
        docRef.set(member).await()
    }

     fun getThreads(spaceId: String) =
        threadRef.whereEqualTo("space_id", spaceId).snapshotFlow(ApiThread::class.java)

     fun getMessages(threadId: String) =
        threadMessagesRef(threadId).snapshotFlow(ApiThreadMessage::class.java)

    private fun getLatestMessages(threadId: String) =
        threadMessagesRef(threadId).snapshotFlow(ApiThreadMessage::class.java).map {
            it.maxByOrNull { it.created_at }
        }

     fun getMembers(threadId: String) =
        threadMemberRef(threadId).snapshotFlow(ApiThreadMember::class.java)

    suspend fun sendMessage(threadId: String, senderId: String, message: String) {
        val docRef = threadMessagesRef(threadId).document()
        val threadMessage = ApiThreadMessage(
            id = docRef.id,
            thread_id = threadId,
            sender_id = senderId,
            message = message,
            created_at = System.currentTimeMillis()
        )
        docRef.set(threadMessage).await()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    suspend fun getThreadsWithLatestMessage(spaceId: String): Flow<List<ThreadInfo>> {
        return getThreads(spaceId).flatMapLatest { threads ->
            if (threads.isEmpty()) return@flatMapLatest flowOf(emptyList())
            val flows = threads.map { thread ->
                val members = getMembers(thread.id).firstOrNull()?.map {
                    userService.getUser(it.user_id)
                }?.filterNotNull() ?: emptyList()

                getLatestMessages(thread.id).map { latestMessage ->
                    ThreadInfo(thread, members, listOfNotNull(latestMessage))
                }
            }
            combine(flows) { it.toList() }
        }
    }
}
