package com.canopas.catchme.data.service.user

import com.canopas.catchme.data.models.user.ApiUser
import com.canopas.catchme.data.utils.FirestoreConst.FIRESTORE_COLLECTION_USERS
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ApiUserService @Inject constructor(
    db: FirebaseFirestore

) {
    private val userRef = db.collection(FIRESTORE_COLLECTION_USERS)

    suspend fun getUser(userId: String): ApiUser? {
        return userRef.document(userId).get().await().toObject(ApiUser::class.java)
    }

}
