package iad1tya.echo.music.echomusic.updater.downloadmanager

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.graphics.toColorInt
import iad1tya.echo.music.R

object DownloadNotificationManager {
    private lateinit var notificationManager: NotificationManager
    private lateinit var appContext: Context

    const val CHANNEL_ID = "download_progress_channel"
    const val UPDATE_CHANNEL_ID = "app_updates_channel"
    private const val CHANNEL_NAME = "Download Progress" 
    const val NOTIFICATION_ID = 5678
    const val UPDATE_NOTIFICATION_ID = 5679

    fun initialize(context: Context) {
        appContext = context
        notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val progressChannel = NotificationChannel(
                CHANNEL_ID,
                "Download Progress",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                setShowBadge(false)
            }
            
            val updateChannel = NotificationChannel(
                UPDATE_CHANNEL_ID,
                "App Updates",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for new app versions"
                enableLights(true)
                vibrationPattern = longArrayOf(0, 500, 250, 500)
            }
            
            notificationManager.createNotificationChannel(progressChannel)
            notificationManager.createNotificationChannel(updateChannel)
        }
    }

    fun showUpdateNotification(context: Context, version: String) {
        val intent = Intent(context, iad1tya.echo.music.MainActivity::class.java).apply {
            putExtra("navigate_to", "update")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent, 
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(context, UPDATE_CHANNEL_ID)
            .setSmallIcon(R.drawable.update)
            .setContentTitle("New Update Available")
            .setContentText("MELODY X v$version is ready for download.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .addAction(R.drawable.update, "Update Now", pendingIntent)
            .build()

        notificationManager.notify(UPDATE_NOTIFICATION_ID, notification)
    }

    fun getDownloadStartingNotification(version: String, fileSize: String): Notification {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) {
            buildDownloadStartingModern(version, fileSize)
        } else {
            buildDownloadStartingLegacy(version, fileSize)
        }
    }

    fun getDownloadProgressNotification(progress: Int, version: String): Notification {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) {
            buildDownloadProgressModern(progress, version)
        } else {
            buildDownloadProgressLegacy(progress, version)
        }
    }

    fun showDownloadComplete(version: String, filePath: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) {
            showDownloadCompleteModern(version, filePath)
        } else {
            showDownloadCompleteLegacy(version, filePath)
        }
    }

    fun showDownloadFailed(version: String, errorMessage: String) {
        val notification = NotificationCompat.Builder(appContext, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_warning)
            .setContentTitle(appContext.getString(R.string.update_failed))
            .setContentText(appContext.getString(R.string.failed_to_download_version, version))
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(appContext.getString(R.string.failed_to_download_version_error, version, errorMessage))
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    fun cancelNotification() {
        notificationManager.cancel(NOTIFICATION_ID)
    }

    @RequiresApi(Build.VERSION_CODES.BAKLAVA)
    private fun buildDownloadStartingModern(version: String, fileSize: String): Notification {
        val progressStyle = Notification.ProgressStyle()
            .also {
                for (i in 0 until 4) {
                    it.addProgressSegment(
                        Notification.ProgressStyle.Segment(25)
                            .setColor(if (i % 2 == 0) "#4285F4".toColorInt() else "#8E24AA".toColorInt())
                    )
                }
            }
            .setProgress(0)

        val builder = Notification.Builder(appContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground) 
            .setContentTitle(appContext.getString(R.string.downloading_update))
            .setContentText(appContext.getString(R.string.version_file_size, version, fileSize))
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setStyle(progressStyle)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .setCategory(Notification.CATEGORY_PROGRESS)
            .setWhen(System.currentTimeMillis())

        setRequestPromotedOngoingSafely(builder, true)
        setShortCriticalTextSafely(builder, appContext.getString(R.string.starting))

        return builder.build()
    }

    @RequiresApi(Build.VERSION_CODES.BAKLAVA)
    private fun buildDownloadProgressModern(progress: Int, version: String): Notification {
        val progressStyle = Notification.ProgressStyle()
            .also {
                for (i in 0 until 4) {
                    it.addProgressSegment(
                        Notification.ProgressStyle.Segment(25)
                            .setColor(if (i % 2 == 0) "#4285F4".toColorInt() else "#8E24AA".toColorInt())
                    )
                }
            }
            .setProgress(progress)

        val builder = Notification.Builder(appContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground) 
            .setContentTitle(appContext.getString(R.string.downloading_update))
            .setContentText(appContext.getString(R.string.version_progress, version, progress))
            .setOngoing(progress < 100)
            .setOnlyAlertOnce(true)
            .setStyle(progressStyle)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .setCategory(Notification.CATEGORY_PROGRESS)
            .setWhen(System.currentTimeMillis())

        setRequestPromotedOngoingSafely(builder, progress < 100)
        setShortCriticalTextSafely(builder, "$progress%")

        return builder.build()
    }

    @RequiresApi(Build.VERSION_CODES.BAKLAVA)
    private fun showDownloadCompleteModern(version: String, filePath: String) {
        val file = java.io.File(filePath)
        val uri = androidx.core.content.FileProvider.getUriForFile(
            appContext,
            "${appContext.packageName}.FileProvider",
            file
        )
        
        val installIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        val pendingIntent = PendingIntent.getActivity(
            appContext, 0, installIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val progressStyle = Notification.ProgressStyle()
            .also {
                for (i in 0 until 4) {
                    it.addProgressSegment(
                        Notification.ProgressStyle.Segment(25)
                            .setColor(if (i % 2 == 0) "#4285F4".toColorInt() else "#8E24AA".toColorInt())
                    )
                }
            }
            .setProgress(100)

        val builder = Notification.Builder(appContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.updated) 
            .setContentTitle(appContext.getString(R.string.update_ready))
            .setContentText(appContext.getString(R.string.tap_to_install_version, version))
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setStyle(progressStyle)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .setCategory(Notification.CATEGORY_STATUS)
            .setWhen(System.currentTimeMillis())

        setRequestPromotedOngoingSafely(builder, false)
        setShortCriticalTextSafely(builder, appContext.getString(R.string.done))

        notificationManager.notify(NOTIFICATION_ID, builder.build())
    }

    private fun showDownloadCompleteLegacy(version: String, filePath: String) {
        val file = java.io.File(filePath)
        val uri = androidx.core.content.FileProvider.getUriForFile(
            appContext,
            "${appContext.packageName}.FileProvider",
            file
        )

        val installIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        val pendingIntent = PendingIntent.getActivity(
            appContext, 0, installIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(appContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.updated) 
            .setContentTitle(appContext.getString(R.string.update_ready))
            .setContentText(appContext.getString(R.string.tap_to_install_version, version))
            .setProgress(0, 0, false)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun setShortCriticalTextSafely(builder: Notification.Builder, text: String) {
        try {
            val method = Notification.Builder::class.java.getMethod("setShortCriticalText", CharSequence::class.java)
            method.invoke(builder, text)
        } catch (e: Exception) {
            builder.getExtras().putCharSequence("android.shortCriticalText", text)
        }
    }

    private fun setRequestPromotedOngoingSafely(builder: Notification.Builder, promoted: Boolean) {
        builder.getExtras().putBoolean("android.requestPromotedOngoing", promoted)
        try {
            val methodNames = arrayOf("setRequestPromotedOngoing", "setPromotedOngoing", "setOngoingActivity")
            for (name in methodNames) {
                try {
                    val method = Notification.Builder::class.java.getMethod(name, Boolean::class.javaPrimitiveType)
                    method.invoke(builder, promoted)
                    break
                } catch (e: Exception) {}
            }
        } catch (e: Exception) {}
    }

    private fun buildDownloadStartingLegacy(version: String, fileSize: String): Notification {
        return NotificationCompat.Builder(appContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground) 
            .setContentTitle(appContext.getString(R.string.downloading_update))
            .setContentText(appContext.getString(R.string.version_file_size, version, fileSize))
            .setProgress(100, 0, false)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .build()
    }

    private fun buildDownloadProgressLegacy(progress: Int, version: String): Notification {
        return NotificationCompat.Builder(appContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher) 
            .setContentTitle(appContext.getString(R.string.downloading_update))
            .setContentText(appContext.getString(R.string.version_progress, version, progress))
            .setProgress(100, progress, false)
            .setOngoing(progress < 100)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .build()
    }
}
