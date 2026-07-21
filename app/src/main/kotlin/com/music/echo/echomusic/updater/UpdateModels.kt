package iad1tya.echo.music.echomusic.updater

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName

@Keep
data class UpdateInfo(
    @SerializedName("versionCode") val versionCode: Int,
    @SerializedName("versionName") val versionName: String,
    @SerializedName("releaseDate") val releaseDate: String,
    @SerializedName("apkSize") val apkSize: String,
    @SerializedName("apkUrl") val apkUrl: String,
    @SerializedName("forceUpdate") val forceUpdate: Boolean = false,
    @SerializedName("changelog") val changelog: Changelog = Changelog()
)

@Keep
data class Changelog(
    @SerializedName("features") val features: List<String> = emptyList(),
    @SerializedName("improvements") val improvements: List<String> = emptyList(),
    @SerializedName("bugFixes") val bugFixes: List<String> = emptyList()
)

sealed class DownloadState {
    object Idle : DownloadState()
    data class Downloading(
        val progress: Float,
        val speed: String,
        val remainingTime: String,
        val downloadedBytes: Long,
        val totalBytes: Long
    ) : DownloadState()
    data class Completed(val fileUri: String) : DownloadState()
    data class Failed(val error: String) : DownloadState()
}
