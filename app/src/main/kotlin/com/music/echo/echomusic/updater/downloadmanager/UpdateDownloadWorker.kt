package iad1tya.echo.music.echomusic.updater.downloadmanager

import android.content.Context
import android.os.Environment
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import iad1tya.echo.music.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.work.ForegroundInfo
import java.io.IOException
import java.util.zip.ZipInputStream
import kotlin.math.roundToInt

class UpdateDownloadWorker(private val context: Context, workerParams: WorkerParameters) :
    CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val apkUrl = inputData.getString("apk_url") ?: return@withContext Result.failure()
        val version = inputData.getString("version") ?: "unknown"
        val fileSizeStr = inputData.getString("file_size") ?: ""

        try {
            val startingNotification = DownloadNotificationManager.getDownloadStartingNotification(version, fileSizeStr)
            val foregroundInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ForegroundInfo(
                    DownloadNotificationManager.NOTIFICATION_ID, 
                    startingNotification, 
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
                )
            } else {
                ForegroundInfo(DownloadNotificationManager.NOTIFICATION_ID, startingNotification)
            }
            setForeground(foregroundInfo)
        } catch (e: Exception) {
            android.util.Log.e("UpdateDownloadWorker", "Failed to set foreground info", e)
        }

        try {
            var currentUrl = apkUrl
            var connection: HttpURLConnection
            var responseCode: Int
            var redirects = 0
            val maxRedirects = 5

            do {
                val url = URL(currentUrl)
                connection = url.openConnection() as HttpURLConnection
                connection.instanceFollowRedirects = true
                connection.requestMethod = "GET"
                connection.connectTimeout = 15000
                connection.readTimeout = 15000
                connection.connect()

                responseCode = connection.responseCode
                if (responseCode == HttpURLConnection.HTTP_MOVED_PERM || responseCode == HttpURLConnection.HTTP_MOVED_TEMP || responseCode == HttpURLConnection.HTTP_SEE_OTHER) {
                    currentUrl = connection.getHeaderField("Location")
                    connection.disconnect()
                    redirects++
                } else {
                    break
                }
            } while (redirects < maxRedirects)

            if (responseCode != HttpURLConnection.HTTP_OK) {
                android.util.Log.e("UpdateDownload", "Server returned code: $responseCode")
                return@withContext Result.failure()
            }

            val fileLength = connection.contentLength.toLong()
            val inputStream = connection.inputStream

            val downloadDir = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "echo_updates")
            if (!downloadDir.exists()) downloadDir.mkdirs()

            val isZip = apkUrl.contains("nightly.link") || apkUrl.endsWith(".zip")
            val tempFile = File(downloadDir, if (isZip) "update_temp.zip.part" else "update_temp.apk.part")
            val outputStream = FileOutputStream(tempFile)

            val buffer = ByteArray(64 * 1024) // 64KB buffer for speed
            var bytesRead: Int
            var totalBytesRead: Long = 0
            
            // For Speed/ETA calculation
            var startTime = System.currentTimeMillis()
            var lastUpdateMillis = startTime
            var bytesInLastWindow: Long = 0
            
            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                if (isStopped) {
                    outputStream.close()
                    inputStream.close()
                    if (tempFile.exists()) tempFile.delete()
                    return@withContext Result.failure()
                }

                outputStream.write(buffer, 0, bytesRead)
                totalBytesRead += bytesRead
                bytesInLastWindow += bytesRead

                val currentMillis = System.currentTimeMillis()
                val elapsedSinceUpdate = currentMillis - lastUpdateMillis

                // Update metrics every 1 second
                if (elapsedSinceUpdate >= 1000) {
                    val progress = if (fileLength > 0) totalBytesRead.toFloat() / fileLength else 0f
                    val speedBytesPerSec = (bytesInLastWindow.toDouble() / (elapsedSinceUpdate / 1000.0)).toLong()
                    val speedMbps = String.format("%.1f MB/s", speedBytesPerSec / (1024.0 * 1024.0))
                    
                    val etaSeconds = if (speedBytesPerSec > 0 && fileLength > 0) {
                        ((fileLength - totalBytesRead) / speedBytesPerSec).toInt()
                    } else -1
                    
                    val etaStr = when {
                        etaSeconds < 0 -> "--:--"
                        etaSeconds < 60 -> "${etaSeconds}s"
                        else -> "${etaSeconds / 60}m ${etaSeconds % 60}s"
                    }

                    setProgress(workDataOf(
                        "progress" to progress,
                        "speed" to speedMbps,
                        "eta" to etaStr,
                        "downloaded" to totalBytesRead,
                        "total" to fileLength
                    ))

                    // Update Notification
                    val notification = DownloadNotificationManager.getDownloadProgressNotification((progress * 100).roundToInt(), version)
                    val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
                    nm.notify(DownloadNotificationManager.NOTIFICATION_ID, notification)

                    lastUpdateMillis = currentMillis
                    bytesInLastWindow = 0
                }
            }

            outputStream.flush()
            outputStream.close()
            inputStream.close()
            connection.disconnect()

            val finalFile = File(downloadDir, "MelodyX_$version.apk")
            if (finalFile.exists()) finalFile.delete()

            if (isZip) {
                ZipInputStream(tempFile.inputStream()).use { zis ->
                    var entry = zis.nextEntry
                    while (entry != null) {
                        if (!entry.isDirectory && entry.name.endsWith(".apk")) {
                            FileOutputStream(finalFile).use { fos -> zis.copyTo(fos) }
                            break
                        }
                        entry = zis.nextEntry
                    }
                }
                if (tempFile.exists()) tempFile.delete()
            } else {
                if (!tempFile.renameTo(finalFile)) {
                    // Fallback if rename fails
                    tempFile.copyTo(finalFile, overwrite = true)
                    tempFile.delete()
                }
            }

            DownloadNotificationManager.showDownloadComplete(version, finalFile.absolutePath)
            Result.success(workDataOf("file_path" to finalFile.absolutePath))

        } catch (e: Exception) {
            Result.failure()
        }
    }
}
