package com.canopas.catchme.data.service.space

import com.canopas.catchme.data.models.space.ApiSpace
import com.canopas.catchme.data.models.space.ApiSpaceMember
import com.canopas.catchme.data.models.space.SPACE_MEMBER_ROLE_ADMIN
import com.canopas.catchme.data.models.space.SPACE_MEMBER_ROLE_MEMBER
import com.canopas.catchme.data.service.auth.AuthService
import com.canopas.catchme.data.utils.FirestoreConst
import com.canopas.catchme.data.utils.FirestoreConst.FIRESTORE_COLLECTION_SPACES
import com.canopas.catchme.data.utils.FirestoreConst.FIRESTORE_COLLECTION_SPACE_MEMBERS
import com.canopas.catchme.data.utils.snapshotFlow
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ApiSpaceService @Inject constructor(
    private val db: FirebaseFirestore,
    private val authService: AuthService
) {
    private val spaceRef = db.collection(FIRESTORE_COLLECTION_SPACES)
    private fun spaceMemberRef(spaceId: String) =
        spaceRef.document(spaceId).collection(FirestoreConst.FIRESTORE_COLLECTION_SPACE_MEMBERS)


    suspend fun createSpace(spaceName: String): String {
        val docRef = spaceRef.document()
        val spaceId = docRef.id
        val userId = authService.currentUser?.id ?: ""
        val space = ApiSpace(id = spaceId, name = spaceName, admin_id = userId)
        docRef.set(space).await()
        joinSpace(spaceId, SPACE_MEMBER_ROLE_ADMIN)
        return spaceId
    }

    suspend fun joinSpace(spaceId: String, role: Int = SPACE_MEMBER_ROLE_MEMBER) {
        val userId = authService.currentUser?.id ?: ""
        spaceMemberRef(spaceId)
            .document().also {
                val member = ApiSpaceMember(
                    id = it.id,
                    space_id = spaceId,
                    user_id = userId,
                    role = role,
                    location_enabled = true
                )
                it.set(member).await()
            }
    }

    suspend fun enableLocation(spaceId: String, userId: String, enable: Boolean) {
        spaceMemberRef(spaceId)
            .whereEqualTo("user_id", userId).get()
            .await().documents.firstOrNull()
            ?.reference?.update("location_enabled", enable)?.await()
    }

    suspend fun isMember(spaceId: String, userId: String): Boolean {
        val query = spaceMemberRef(spaceId)
            .whereEqualTo("user_id", userId)
        val result = query.get().await()
        return result.documents.isNotEmpty()
    }

    suspend fun getSpace(spaceId: String) =
        spaceRef.document(spaceId).get().await().toObject(ApiSpace::class.java)

    fun getSpaceMemberByUserId(userId: String) =
        db.collectionGroup(FIRESTORE_COLLECTION_SPACE_MEMBERS).whereEqualTo("user_id", userId)
            .snapshotFlow(ApiSpaceMember::class.java)

    fun getMemberBySpaceId(spaceId: String) =
        spaceMemberRef(spaceId).snapshotFlow(ApiSpaceMember::class.java)

    suspend fun deleteMembers(spaceId: String) {
        spaceMemberRef(spaceId).get().await().documents.forEach { doc ->
            doc.reference.delete().await()
        }
    }

    suspend fun deleteSpace(spaceId: String) {
        deleteMembers(spaceId)
        spaceRef.document(spaceId).delete().await()
    }

    suspend fun removeUserFromSpace(spaceId: String, userId: String) {
        spaceMemberRef(spaceId)
            .whereEqualTo("user_id", userId).get().await().documents.forEach {
                it.reference.delete().await()
            }
    }
}
