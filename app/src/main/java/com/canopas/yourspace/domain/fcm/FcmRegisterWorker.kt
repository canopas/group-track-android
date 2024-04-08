package com.canopas.yourspace.domain.fcm

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.canopas.yourspace.data.service.auth.AuthService
import com.canopas.yourspace.data.service.user.ApiUserService
import com.canopas.yourspace.data.storage.UserPreferences
import com.google.firebase.messaging.FirebaseMessaging
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import timber.log.Timber
import java.util.concurrent.CountDownLatch
import javax.inject.Inject

private const val FCM_SERVICE = "FCMRegisterService"

@HiltWorker
class FcmRegisterWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    @Inject
    lateinit var userPreferences: UserPreferences

    @Inject
    lateinit var authService: AuthService

    @Inject
    lateinit var apiUserService: ApiUserService

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.Main + job)

    override suspend fun doWork(): Result {

        if (userPreferences.currentUser != null && !userPreferences.isFCMRegistered) {
            val deviceToken = getFCMToken()
            try {
                val job = scope.async {
                    val user = userPreferences.currentUser!!
                    apiUserService.registerFcmToken(user.id, deviceToken)
                    authService.saveUser(user.copy(fcm_token = deviceToken))
                    userPreferences.isFCMRegistered = true
                }
                job.await()
            } catch (e: Exception) {
                Timber.e(e, "Failed to register FCM token")
            }
        }

        return Result.success(inputData)
    }

    private fun getFCMToken(): String {
        val countDownLatch = CountDownLatch(1)
        var token = ""
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                task.result?.let {
                    token = it
                }
            } else {
                Timber.e(task.exception, "FCM token fetch failed")
            }
            countDownLatch.countDown()
        }
        countDownLatch.await()
        return token
    }


    companion object {
        fun startService(context: Context) {
            val data = Data.Builder().putString(FCM_SERVICE, FCM_SERVICE).build()
            val request = OneTimeWorkRequestBuilder<FcmRegisterWorker>()
                .setInputData(data)
                .build()
            val workManager = WorkManager.getInstance(context)
            workManager.enqueue(request)
        }
    }

}