package com.canopas.yourspace.data.utils

import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.EventListener
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

fun <T> Query.snapshotFlow(dataType: Class<T>): Flow<List<T>> = callbackFlow {
    val listenerRegistration = addSnapshotListener { value, error ->
        if (error != null) {
            trySend(emptyList())
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

fun <T> DocumentReference.snapshotFlow(dataType: Class<T>): Flow<T?> = callbackFlow {
    val listener = object : EventListener<DocumentSnapshot> {
        override fun onEvent(snapshot: DocumentSnapshot?, exception: FirebaseFirestoreException?) {
            if (exception != null) {
                cancel()
                return
            }

            if (snapshot != null && snapshot.exists()) {
                val data = snapshot.toObject(dataType)
                trySend(data)
            } else {
                trySend(null)
            }
        }
    }

    val registration = addSnapshotListener(listener)
    awaitClose { registration.remove() }
}
