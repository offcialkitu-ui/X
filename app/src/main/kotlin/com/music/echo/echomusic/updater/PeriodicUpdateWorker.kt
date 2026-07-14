package iad1tya.echo.music.echomusic.updater

import android.content.Context
import android.os.SystemClock
import androidx.work.*
import iad1tya.echo.music.echomusic.updater.downloadmanager.DownloadNotificationManager
import timber.log.Timber
import java.util.concurrent.TimeUnit

/**
 * Background worker that periodically checks for new app versions.
 * Uses [SystemClock.elapsedRealtime] for clock-skew-safe rate limiting.
 */
class PeriodicUpdateWorker(private val context: Context, workerParams: WorkerParameters) :
    CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        if (!getAutoUpdateCheckSetting(context)) return Result.success()

        // ── Clock-skew safe rate limiting ────────────────────────────────
        // Double-check using monotonic time to handle edge cases where
        // WorkManager fires earlier than expected due to clock changes.
        val nowElapsed = SystemClock.elapsedRealtime()
        val lastElapsed = getLastCheckedElapsed(context)
        if (lastElapsed > 0 && (nowElapsed - lastElapsed) < 2 * 60 * 60 * 1000L) {
            Timber.d("PeriodicUpdateWorker: Skipping (cooldown active)")
            return Result.success()
        }
        saveLastCheckedElapsed(context, nowElapsed)

        val repository = UpdateRepository()
        val betaEnabled = getBetaUpdatesSetting(context)
        
        return try {
            repository.fetchUpdateInfo(force = false).onSuccess { info ->
                if (repository.isUpdateAvailable(info, betaEnabled)) {
                    saveUpdateAvailableState(context, true)
                    if (getUpdateNotificationsSetting(context)) {
                        DownloadNotificationManager.showUpdateNotification(context, info.versionName)
                    }
                } else {
                    saveUpdateAvailableState(context, false)
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
