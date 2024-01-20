package com.canopas.catchme.data.service.space

import com.canopas.catchme.data.models.auth.ApiUser
import com.canopas.catchme.data.models.space.ApiSpace
import com.canopas.catchme.data.models.space.ApiSpaceMember
import com.canopas.catchme.data.models.space.SPACE_MEMBER_ROLE_ADMIN
import com.canopas.catchme.data.models.space.SPACE_MEMBER_ROLE_MEMBER
import com.canopas.catchme.data.service.user.UserService
import com.canopas.catchme.data.utils.FirestoreConst
import com.canopas.catchme.data.utils.FirestoreConst.FIRESTORE_COLLECTION_SPACES
import com.canopas.catchme.data.utils.snapshotFlow
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SpaceService @Inject constructor(
    db: FirebaseFirestore,
    private val invitationService: SpaceInvitationService,
    private val userService: UserService
) {
    private val spaceRef = db.collection(FIRESTORE_COLLECTION_SPACES)
    private val spaceMemberRef = db.collection(FirestoreConst.FIRESTORE_COLLECTION_SPACE_MEMBERS)
    private val userRef = db.collection(FirestoreConst.FIRESTORE_COLLECTION_USERS)

    suspend fun getSpaces()= flow<List<ApiSpace>> {
        val userId = userService.currentUser?.id ?: ""
        getSpaces(userId).collectLatest { space ->

        }
    }

    suspend fun createSpace(spaceName: String): String {
        val docRef = spaceRef.document()
        val spaceId = docRef.id
        val userId = userService.currentUser?.id ?: ""
        val space = ApiSpace(id = spaceId, name = spaceName, admin_id = userId)
        docRef.set(space).await()
        val generatedCode = invitationService.createInvitation(spaceId)
        joinSpace(spaceId, SPACE_MEMBER_ROLE_ADMIN)
        return generatedCode
    }

    suspend fun joinSpace(spaceId: String, role: Int = SPACE_MEMBER_ROLE_MEMBER) {
        val userId = userService.currentUser?.id ?: ""
        addMember(spaceId, userId, role)
    }

    suspend fun getSpace(spaceId: String): ApiSpace? {
        return spaceRef.document(spaceId).get().await().toObject(ApiSpace::class.java)
    }

    suspend fun getSpaces(userId: String) = flow<List<ApiSpaceMember>> {
        spaceMemberRef.whereEqualTo("user_id", userId).snapshotFlow(ApiSpaceMember::class.java)
            .collectLatest {
                if (it.isNotEmpty()) {
                    emit(it)
                }
            }
    }

    suspend fun getSpaceMembers(spaceId: String) = flow<List<ApiUser>> {
        spaceMemberRef.whereEqualTo("space_id", spaceId).snapshotFlow(ApiSpaceMember::class.java)
            .collectLatest {
                if (it.isNotEmpty()) {
                    val userIds = it.map { member -> member.user_id }
                    val users = userRef.whereIn("user_id", userIds).get().await()
                    emit(users.toObjects(ApiUser::class.java))
                }
            }
    }

    suspend fun addMember(spaceId: String, userId: String, role: Int = SPACE_MEMBER_ROLE_MEMBER) {
        val docRef = spaceMemberRef.document()
        val member = ApiSpaceMember(
            id = docRef.id,
            space_id = spaceId,
            user_id = userId,
            role = role,
            location_enabled = true
        )

        docRef.set(member).await()
    }

    suspend fun isMember(spaceId: String, userId: String): Boolean {
        val query = spaceMemberRef.whereEqualTo("space_id", spaceId)
            .whereEqualTo("user_id", userId)
        val result = query.get().await()
        return result.documents.isNotEmpty()
    }
}
