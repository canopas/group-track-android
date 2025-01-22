package com.canopas.yourspace.data.service.messages

import com.canopas.yourspace.data.models.messages.ApiThread
import com.canopas.yourspace.data.models.messages.ApiThreadMessage
import com.canopas.yourspace.data.models.messages.ThreadInfo
import com.canopas.yourspace.data.utils.Config
import com.canopas.yourspace.data.utils.Config.FIRESTORE_COLLECTION_SPACE_THREADS
import com.canopas.yourspace.data.utils.snapshotFlow
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.toObject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.Date
import javax.inject.Inject

class ApiMessagesService @Inject constructor(
    private val db: FirebaseFirestore
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

    fun getLatestMessages(threadId: String, limit: Int) = threadMessagesRef(threadId)
        .orderBy("created_at", Query.Direction.DESCENDING)
        .limit(limit.toLong()).snapshotFlow(ApiThreadMessage::class.java)

    suspend fun getMessages(
        threadId: String,
        from: Date?,
        limit: Int = 20
    ): List<ApiThreadMessage> {
        if (from == null) {
            return threadMessagesRef(threadId)
                .orderBy("created_at", Query.Direction.DESCENDING)
                .limit(limit.toLong())
                .get().await().documents.mapNotNull { it.toObject<ApiThreadMessage>() }
        }
        return threadMessagesRef(threadId)
            .orderBy("created_at", Query.Direction.DESCENDING)
            .whereLessThan("created_at", from)
            .limit(limit.toLong())
            .get().await().documents.mapNotNull { it.toObject<ApiThreadMessage>() }
    }

    suspend fun sendMessage(threadId: String, senderId: String, message: String) {
        val docRef = threadMessagesRef(threadId).document()

        val threadMessage = ApiThreadMessage(
            id = docRef.id,
            thread_id = threadId,
            sender_id = senderId,
            message = message
        )
        docRef.set(threadMessage).await()
    }

    fun generateMessage(threadId: String, senderId: String, message: String): ApiThreadMessage {
        val docRef = threadMessagesRef(threadId).document()

        return ApiThreadMessage(
            id = docRef.id,
            thread_id = threadId,
            sender_id = senderId,
            message = message
        )
    }

    suspend fun sendMessage(message: ApiThreadMessage) {
        val docRef = threadMessagesRef(message.thread_id).document(message.id)
        docRef.set(message).await()
    }

    suspend fun markMessagesAsSeen(threadId: String, userId: String) {
        threadRef.document(threadId).update("seen_by_ids", FieldValue.arrayUnion(userId)).await()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    suspend fun getThreadsWithLatestMessage(
        spaceId: String,
        userId: String
    ): Flow<List<ThreadInfo>> {
        return getThreads(spaceId, userId).flatMapLatest { threads ->
            if (threads.isEmpty()) return@flatMapLatest flowOf(emptyList())
            val flows = threads.map { thread ->
                threadMessagesRef(thread.id).orderBy("created_at", Query.Direction.DESCENDING)
                    .limit(1)
                    .snapshotFlow(ApiThreadMessage::class.java).map { latestMessages ->
                        ThreadInfo(thread, messages = latestMessages)
                    }
            }
            combine(flows) { it.toList() }
        }
    }

    suspend fun deleteThread(thread: ApiThread, userId: String) {
        if (thread.admin_id == userId) {
            threadRef.document(thread.id).delete().await()
        } else {
            val archivedMap =
                thread.archived_for.toMutableMap()
                    .apply { this[userId] = System.currentTimeMillis() }
            threadRef.document(thread.id)
                .update("archived_for", archivedMap)
                .await()
        }
    }
}
