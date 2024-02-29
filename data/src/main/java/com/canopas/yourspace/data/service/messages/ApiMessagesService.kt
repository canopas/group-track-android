package com.canopas.yourspace.data.service.messages

import com.canopas.yourspace.data.models.messages.ApiThread
import com.canopas.yourspace.data.models.messages.ApiThreadMessage
import com.canopas.yourspace.data.models.messages.ThreadInfo
import com.canopas.yourspace.data.service.user.ApiUserService
import com.canopas.yourspace.data.utils.Config
import com.canopas.yourspace.data.utils.Config.FIRESTORE_COLLECTION_SPACE_THREADS
import com.canopas.yourspace.data.utils.snapshotFlow
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
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

    private fun threadMessagesRef(threadId: String) =
        threadRef.document(threadId).collection(Config.FIRESTORE_COLLECTION_THREAD_MESSAGES)

    suspend fun createThread(spaceId: String, adminId: String): String {
        val docRef = threadRef.document()
        val threadId = docRef.id
        val thread = ApiThread(
            id = threadId,
            space_id = spaceId,
            admin_id = adminId,
            member_ids = listOf(adminId),
            created_at = System.currentTimeMillis()
        )
        docRef.set(thread).await()
        return threadId
    }

    suspend fun joinThread(threadId: String, userIds: List<String>) {
        threadRef.document(threadId)
            .update("member_ids", FieldValue.arrayUnion(*userIds.toTypedArray()))
            .await()
    }

    fun getThread(threadId: String) =
        threadRef.document(threadId).snapshotFlow(ApiThread::class.java)


    fun getThreads(spaceId: String, userId: String) =
        threadRef.whereEqualTo("space_id", spaceId)
            .whereArrayContains("member_ids", userId)
            .snapshotFlow(ApiThread::class.java)

    fun getMessages(threadId: String) =
        threadMessagesRef(threadId).snapshotFlow(ApiThreadMessage::class.java)

    suspend fun sendMessage(threadId: String, senderId: String, message: String) {
        val docRef = threadMessagesRef(threadId).document()
        val threadMessage = ApiThreadMessage(
            id = docRef.id,
            thread_id = threadId,
            sender_id = senderId,
            message = message,
            read_by = listOf(senderId),
            created_at = System.currentTimeMillis()
        )
        docRef.set(threadMessage).await()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    suspend fun getThreadsWithLatestMessage(spaceId: String, userId: String): Flow<List<ThreadInfo>> {
        return getThreads(spaceId, userId).flatMapLatest { threads ->
            if (threads.isEmpty()) return@flatMapLatest flowOf(emptyList())
            val flows = threads.map { thread ->
                threadMessagesRef(thread.id).orderBy("created_at", Query.Direction.DESCENDING)
                    .limit(15)
                    .snapshotFlow(ApiThreadMessage::class.java).map { latestMessages ->
                        ThreadInfo(thread, messages = latestMessages)
                    }
            }
            combine(flows) { it.toList() }
        }
    }
}
