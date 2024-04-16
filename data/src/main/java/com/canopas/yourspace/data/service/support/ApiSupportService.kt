package com.canopas.yourspace.data.service.support

import android.net.Uri
import androidx.core.net.toUri
import com.canopas.yourspace.data.service.auth.AuthService
import com.canopas.yourspace.data.utils.Device
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import java.io.File
import javax.inject.Inject

class ApiSupportService @Inject constructor(
    private val storage: FirebaseStorage,
    private val db: FirebaseFirestore,
    private val authService: AuthService,
    private val device: Device,
) {

    suspend fun uploadImage(file: File): Uri? {
        val user = authService.currentUser ?: return null
        val storageRef = storage.reference
        val fileName = "IMG_${System.currentTimeMillis()}.jpg"
        val imageRef = storageRef.child("contact_support/${user.id}/$fileName")
        val uploadTask = imageRef.putFile(file.toUri())

        return uploadTask.continueWithTask { task ->
            if (!task.isSuccessful) {
                task.exception?.let {
                    throw it
                }
            }
            imageRef.downloadUrl
        }.await()
    }

    suspend fun submitSupportRequest(title: String, description: String, attachments: List<Uri>) {
        val data = mapOf(
            "user_id" to authService.currentUser?.id,
            "title" to title,
            "description" to description,
            "device_name" to device.deviceName(),
            "app_version" to device.getAppVersionCode().toString(),
            "device_os" to device.getDeviceOsVersion(),
            "attachments" to attachments.map { it.toString() },
            "created_at" to FieldValue.serverTimestamp(),
        )

        db.collection("support_requests").add(data).await()
    }
}