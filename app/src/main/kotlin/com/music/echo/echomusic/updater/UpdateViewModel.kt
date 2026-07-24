package iad1tya.echo.music.echomusic.updater

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.*
import iad1tya.echo.music.BuildConfig
import iad1tya.echo.music.echomusic.updater.downloadmanager.UpdateDownloadWorker
import iad1tya.echo.music.utils.get
import iad1tya.echo.music.utils.dataStore
import iad1tya.echo.music.constants.DownloadOnlyOnWifiKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.io.File
import javax.inject.Inject
import dagger.hilt.android.lifecycle.HiltViewModel
import androidx.core.net.toUri

sealed class UpdateUiState {
    object Idle : UpdateUiState()
    object Checking : UpdateUiState()
    data class Available(val updateInfo: UpdateInfo) : UpdateUiState()
    data class UpToDate(val currentVersion: String) : UpdateUiState()
    data class Error(val message: String) : UpdateUiState()
}

@HiltViewModel
class UpdateViewModel @Inject constructor(
    private val repository: UpdateRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<UpdateUiState>(UpdateUiState.Idle)
    val uiState: StateFlow<UpdateUiState> = _uiState.asStateFlow()

    private val _downloadState = MutableStateFlow<DownloadState>(DownloadState.Idle)
    val downloadState: StateFlow<DownloadState> = _downloadState.asStateFlow()

    fun checkForUpdates(context: Context? = null, silent: Boolean = false) {
        viewModelScope.launch {
            if (!silent) _uiState.value = UpdateUiState.Checking
            repository.fetchUpdateInfo(force = true).onSuccess { info ->
                if (repository.isUpdateAvailable(info)) {
                    _uiState.value = UpdateUiState.Available(info)
                    context?.let { ctx ->
                        checkDownloadedApk(ctx, info.versionName)
                        observeDownloadProgress(ctx, info.versionName)
                    }
                } else {
                    _uiState.value = UpdateUiState.UpToDate(BuildConfig.VERSION_NAME)
                }
            }.onFailure { error ->
                if (!silent) _uiState.value = UpdateUiState.Error(error.message ?: "Network error. Check connection.")
            }
        }
    }

    fun checkDownloadedApk(context: Context, versionName: String) {
        val downloadedFile = File(getDownloadedApksDir(context), "MelodyX_$versionName.apk")
        if (downloadedFile.exists() && downloadedFile.length() > 0) {
            _downloadState.value = DownloadState.Completed(downloadedFile.absolutePath)
        }
    }

    fun observeDownloadProgress(context: Context, versionName: String) {
        viewModelScope.launch {
            val downloadedFile = File(getDownloadedApksDir(context), "MelodyX_$versionName.apk")
            if (downloadedFile.exists() && downloadedFile.length() > 0) {
                _downloadState.value = DownloadState.Completed(downloadedFile.absolutePath)
            }

            WorkManager.getInstance(context)
                .getWorkInfosForUniqueWorkFlow("update_download")
                .collect { workInfoList ->
                    val workInfo = workInfoList.firstOrNull() ?: return@collect
                    when (workInfo.state) {
                        WorkInfo.State.RUNNING -> {
                            val progress = workInfo.progress.getFloat("progress", 0f)
                            val speed = workInfo.progress.getString("speed") ?: ""
                            val eta = workInfo.progress.getString("eta") ?: ""
                            val downloaded = workInfo.progress.getLong("downloaded", 0L)
                            val total = workInfo.progress.getLong("total", 0L)
                            _downloadState.value = DownloadState.Downloading(progress, speed, eta, downloaded, total)
                        }
                        WorkInfo.State.SUCCEEDED -> {
                            val filePath = workInfo.outputData.getString("file_path") ?: downloadedFile.absolutePath
                            _downloadState.value = DownloadState.Completed(filePath)
                        }
                        WorkInfo.State.FAILED -> {
                            _downloadState.value = DownloadState.Failed("Download failed")
                        }
                        WorkInfo.State.CANCELLED -> {
                            _downloadState.value = DownloadState.Idle
                        }
                        else -> {}
                    }
                }
        }
    }

    fun startDownload(context: Context, info: UpdateInfo) {
        // Manual trigger bypasses Wi-Fi only constraint for a better user experience
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val downloadRequest = OneTimeWorkRequestBuilder<UpdateDownloadWorker>()
            .setInputData(workDataOf(
                "apk_url" to info.apkUrl,
                "version" to info.versionName,
                "file_size" to info.apkSize
            ))
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 10, java.util.concurrent.TimeUnit.SECONDS)
            .addTag("update_download")
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            "update_download",
            ExistingWorkPolicy.REPLACE,
            downloadRequest
        )

        observeDownloadProgress(context, info.versionName)
    }

    fun installApk(context: Context, filePath: String) {
        val file = File(filePath)
        if (!file.exists()) {
            android.util.Log.e("MelodyX", "APK file not found: $filePath")
            return
        }

        try {
            val authority = "${iad1tya.echo.music.BuildConfig.APPLICATION_ID}.FileProvider"
            val uri = FileProvider.getUriForFile(context, authority, file)
            
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            
            context.startActivity(intent)
            android.util.Log.d("MelodyX", "Installer launched with authority: $authority")
        } catch (e: Exception) {
            android.util.Log.e("MelodyX", "Installation failed", e)
            
            // Fallback for some devices/OS versions
            try {
                val intent = Intent(Intent.ACTION_INSTALL_PACKAGE).apply {
                    val authority = "${iad1tya.echo.music.BuildConfig.APPLICATION_ID}.FileProvider"
                    data = FileProvider.getUriForFile(context, authority, file)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } catch (e2: Exception) {
                android.util.Log.e("MelodyX", "Fallback installation also failed", e2)
            }
        }
    }
}

