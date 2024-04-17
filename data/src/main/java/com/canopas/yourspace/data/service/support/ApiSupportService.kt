package com.canopas.yourspace.data.service.support

import android.net.Uri
import androidx.core.net.toUri
import com.canopas.yourspace.data.service.auth.AuthService
import com.canopas.yourspace.data.utils.Device
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import java.io.File
import javax.inject.Inject

class ApiSupportService @Inject constructor(
    private val storage: FirebaseStorage,
    private val authService: AuthService,
    private val device: Device,
    private val functions: FirebaseFunctions
) {

    suspend fun uploadImage(file: File): Uri? {
        val user = authService.currentUser ?: return null
        val storageRef = storage.reference
        val fileName = "IMG_${System.currentTimeMillis()}.jpg"
        val imageRef = storageRef.child("contact_support/${user.id}/$fileName")
        val uploadTask = imageRef.putFile(file.toUri()).await()
        uploadTask.task.await()
        return imageRef.downloadUrl.await()
    }

    suspend fun submitSupportRequest(title: String, description: String, attachments: List<Uri>) {
        val data = mapOf(
            "title" to title,
            "description" to description,
            "device_name" to device.deviceName(),
            "app_version" to device.getAppVersionCode().toString(),
            "device_os" to device.getDeviceOsVersion(),
            "user_id" to authService.currentUser?.id,
            "attachments" to attachments.map { it.toString() }
        )

        functions.getHttpsCallable("sendSupportRequest").call(data).await()
    }
}
