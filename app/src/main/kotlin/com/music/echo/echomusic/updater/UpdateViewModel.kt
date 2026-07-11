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

    fun checkForUpdates() {
        viewModelScope.launch {
            _uiState.value = UpdateUiState.Checking
            repository.fetchUpdateInfo().onSuccess { info ->
                if (repository.isUpdateAvailable(info)) {
                    _uiState.value = UpdateUiState.Available(info)
                } else {
                    _uiState.value = UpdateUiState.UpToDate(BuildConfig.VERSION_NAME)
                }
            }.onFailure { error ->
                _uiState.value = UpdateUiState.Error(error.message ?: "Network error. Check connection.")
            }
        }
    }

    fun startDownload(context: Context, info: UpdateInfo) {
        val downloadOnlyOnWifi = runBlocking { context.dataStore.get(DownloadOnlyOnWifiKey, false) }
        
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(if (downloadOnlyOnWifi) NetworkType.UNMETERED else NetworkType.CONNECTED)
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
    }

    fun installApk(context: Context, filePath: String) {
        val file = File(filePath)
        if (!file.exists()) return

        if (!context.packageManager.canRequestPackageInstalls()) {
            val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                data = "package:${context.packageName}".toUri()
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            return
        }

        val uri = FileProvider.getUriForFile(context, "${context.packageName}.FileProvider", file)
        val installIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(installIntent)
    }
}
