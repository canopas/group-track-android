package com.canopas.catchme.data.utils

import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

fun <T> Query.snapshotFlow(dataType: Class<T>): Flow<List<T>> = callbackFlow {
    val listenerRegistration = addSnapshotListener { value, error ->
        if (error != null) {
            close()
            return@addSnapshotListener
        }
        if (value != null) {
            trySend(value.toObjects(dataType))
        } else {
            trySend(emptyList())
        }
    }
    awaitClose {
        listenerRegistration.remove()
    }
}
