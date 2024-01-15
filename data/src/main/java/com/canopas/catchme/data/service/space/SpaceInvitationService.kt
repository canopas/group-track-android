package com.canopas.catchme.data.service.space

import com.canopas.catchme.data.models.space.ApiSpaceInvitation
import com.canopas.catchme.data.utils.FirestoreConst
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SpaceInvitationService @Inject constructor(
    private val db: FirebaseFirestore
) {
    private val spaceInvitationRef =
        db.collection(FirestoreConst.FIRESTORE_COLLECTION_SPACE_INVITATION)

    suspend fun createInvitation(spaceId: String): String {
        val invitationCode = generateInvitationCode()
        val docRef = spaceInvitationRef.document()
        val invitation =
            ApiSpaceInvitation(id = docRef.id, space_id = spaceId, code = invitationCode)

        docRef.set(invitation).await()
        return invitationCode
    }

    private fun generateInvitationCode(): String {
        val characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        return (1..6)
            .map { characters.random() }
            .joinToString("")
    }

    suspend fun getInvitation(inviteCode: String): ApiSpaceInvitation? {
        val query = spaceInvitationRef.whereEqualTo("code", inviteCode.uppercase())
        val result = query.get().await()
        val invitation = result.documents.firstOrNull()?.toObject(ApiSpaceInvitation::class.java)
        return invitation?.takeIf { !it.isExpired }
    }
}
