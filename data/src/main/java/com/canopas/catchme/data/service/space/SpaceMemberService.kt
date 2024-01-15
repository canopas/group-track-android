package com.canopas.catchme.data.service.space

import com.canopas.catchme.data.models.auth.ApiUser
import com.canopas.catchme.data.models.space.ApiSpaceMember
import com.canopas.catchme.data.models.space.SPACE_MEMBER_ROLE_MEMBER
import com.canopas.catchme.data.utils.FirestoreConst
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SpaceMemberService @Inject constructor(private val db: FirebaseFirestore) {
    private val spaceMemberRef = db.collection(FirestoreConst.FIRESTORE_COLLECTION_SPACE_MEMBERS)

    suspend fun getSpaceMembers(spaceId: String): List<ApiUser> {
        return emptyList()
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
}
