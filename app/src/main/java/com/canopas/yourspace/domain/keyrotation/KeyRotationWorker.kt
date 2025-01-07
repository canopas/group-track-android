package com.canopas.yourspace.domain.keyrotation

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.canopas.yourspace.data.service.space.ApiSpaceService
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.concurrent.TimeUnit

private const val KEY_ROTATION_WORKER = "KeyRotationWorker"

@HiltWorker
class KeyRotationWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val apiSpaceService: ApiSpaceService
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        Timber.d("Starting Key Rotation Worker...")

        return@withContext try {
            val spaceIds = apiSpaceService.getUserSpacesToRotateKeys()
            spaceIds.forEach { spaceId ->
                apiSpaceService.rotateSenderKey(spaceId)
                Timber.d("Rotated keys for space: $spaceId")
            }
            Timber.d("Key Rotation Worker completed successfully.")
            Result.success()
        } catch (e: Exception) {
            Timber.e(e, "Error in Key Rotation Worker")
            Result.retry()
        }
    }

    companion object {
        fun schedule(context: Context) {
            val workRequest = PeriodicWorkRequestBuilder<KeyRotationWorker>(7, TimeUnit.DAYS)
                .setInitialDelay(7, TimeUnit.DAYS)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiresBatteryNotLow(true)
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .build()

            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(
                    KEY_ROTATION_WORKER,
                    ExistingPeriodicWorkPolicy.KEEP,
                    workRequest
                )

            Timber.d("Scheduled Key Rotation Worker")
        }
    }
}
