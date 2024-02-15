package com.canopas.yourspace.data.service.messages

import com.canopas.yourspace.data.models.messages.ApiThread
import com.canopas.yourspace.data.models.messages.ApiThreadMember
import com.canopas.yourspace.data.utils.Config
import com.canopas.yourspace.data.utils.Config.FIRESTORE_COLLECTION_SPACE_THREADS
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class MessagesService @Inject constructor(
    private val db: FirebaseFirestore
) {

    private val threadRef = db.collection(FIRESTORE_COLLECTION_SPACE_THREADS)

    private fun threadMemberRef(threadId: String) =
        threadRef.document(threadId).collection(Config.FIRESTORE_COLLECTION_THREAD_MEMBERS)

    private fun threadMessagesRef(threadId: String) =
        threadRef.document(threadId).collection(Config.FIRESTORE_COLLECTION_THREAD_MESSAGES)


    suspend fun createThread(spaceId: String, adminId: String) {
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




}