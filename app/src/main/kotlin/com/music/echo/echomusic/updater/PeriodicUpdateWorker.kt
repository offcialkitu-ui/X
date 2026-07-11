package iad1tya.echo.music.echomusic.updater

import android.content.Context
import androidx.work.*
import iad1tya.echo.music.echomusic.updater.downloadmanager.DownloadNotificationManager
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import dagger.hilt.android.EntryPointAccessors

class PeriodicUpdateWorker(private val context: Context, workerParams: WorkerParameters) :
    CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        if (!getAutoUpdateCheckSetting(context)) return Result.success()

        val repository = UpdateRepository() // Using simple instantiation for worker or inject via Hilt if configured
        
        return try {
            repository.fetchUpdateInfo().onSuccess { info ->
                if (repository.isUpdateAvailable(info)) {
                    saveUpdateAvailableState(context, true)
                    if (getUpdateNotificationsSetting(context)) {
                        DownloadNotificationManager.showUpdateNotification(context, info.versionName)
                    }
                }
            }
            Result.success()
        } catch (e: Exception) {
            Timber.e(e, "Periodic update check failed")
            Result.retry()
        }
    }

    companion object {
        private const val WORK_NAME = "periodic_update_check"

        fun setup(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = PeriodicWorkRequestBuilder<PeriodicUpdateWorker>(24, TimeUnit.HOURS)
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }
}
