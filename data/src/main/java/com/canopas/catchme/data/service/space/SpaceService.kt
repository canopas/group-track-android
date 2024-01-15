package com.canopas.catchme.data.service.space

import com.canopas.catchme.data.models.space.ApiSpace
import com.canopas.catchme.data.models.space.SPACE_MEMBER_ROLE_ADMIN
import com.canopas.catchme.data.models.space.SPACE_MEMBER_ROLE_MEMBER
import com.canopas.catchme.data.service.user.UserService
import com.canopas.catchme.data.utils.FirestoreConst.FIRESTORE_COLLECTION_SPACES
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SpaceService @Inject constructor(
    db: FirebaseFirestore,
    private val invitationService: SpaceInvitationService,
    private val spaceMemberService: SpaceMemberService,
    private val userService: UserService
) {

    private val spaceRef = db.collection(FIRESTORE_COLLECTION_SPACES)

    suspend fun getSpaces(): List<ApiSpace> {
        return emptyList()
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
        spaceMemberService.addMember(spaceId, userId, role)
    }

    suspend fun getSpace(spaceId: String): ApiSpace? {
        return spaceRef.document(spaceId).get().await().toObject(ApiSpace::class.java)
    }
}
