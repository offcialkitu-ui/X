package iad1tya.echo.music.echomusic.updater

import android.content.Context
import android.os.Environment
import android.util.Log
import iad1tya.echo.music.BuildConfig
import iad1tya.echo.music.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.URL
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.regex.Pattern

data class ChangelogSection(val title: String, val items: List<String>)

fun getDownloadedApksDir(context: Context): File {
    return File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "echo_updates")
}

fun getDownloadedApkCount(context: Context): Int {
    val dir = getDownloadedApksDir(context)
    if (!dir.exists() || !dir.isDirectory) return 0
    return dir.listFiles { file ->
        file.isFile && file.name.endsWith(".apk", ignoreCase = true)
    }?.size ?: 0
}

fun clearDownloadedApks(context: Context): Boolean {
    val dir = getDownloadedApksDir(context)
    if (!dir.exists() || !dir.isDirectory) return true
    var allDeleted = true
    dir.listFiles { file ->
        file.isFile && file.name.endsWith(".apk", ignoreCase = true)
    }?.forEach { file ->
        if (!file.delete()) {
            allDeleted = false
        }
    }
    return allDeleted
}

fun autoClearOldApks(context: Context) {
    val dir = getDownloadedApksDir(context)
    if (!dir.exists() || !dir.isDirectory) return
    val oneDayAgo = System.currentTimeMillis() - (24 * 60 * 60 * 1000)
    dir.listFiles { file ->
        file.isFile && file.name.endsWith(".apk", ignoreCase = true) && file.lastModified() < oneDayAgo
    }?.forEach { it.delete() }
}

const val PREFS_NAME = "settings"
const val KEY_AUTO_UPDATE_CHECK = "auto_update_check"
const val KEY_LAST_CHECKED_TIME = "last_checked_time"
const val KEY_BETA_UPDATES = "beta_updates"
const val KEY_UPDATE_AVAILABLE = "update_available"
const val KEY_UPDATE_NOTIFICATIONS = "update_notifications"

fun getUpdateAvailableState(context: Context): Boolean {
    val sharedPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    return sharedPrefs.getBoolean(KEY_UPDATE_AVAILABLE, false)
}

fun saveUpdateAvailableState(context: Context, available: Boolean) {
    val sharedPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    sharedPrefs.edit().putBoolean(KEY_UPDATE_AVAILABLE, available).apply()
}

fun getAutoUpdateCheckSetting(context: Context): Boolean {
    val sharedPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    return sharedPrefs.getBoolean(KEY_AUTO_UPDATE_CHECK, false)
}

fun saveAutoUpdateCheckSetting(context: Context, enabled: Boolean) {
    val sharedPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    sharedPrefs.edit().putBoolean(KEY_AUTO_UPDATE_CHECK, enabled).apply()
}

fun getUpdateNotificationsSetting(context: Context): Boolean {
    val sharedPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    return sharedPrefs.getBoolean(KEY_UPDATE_NOTIFICATIONS, true)
}

fun saveUpdateNotificationsSetting(context: Context, enabled: Boolean) {
    val sharedPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    sharedPrefs.edit().putBoolean(KEY_UPDATE_NOTIFICATIONS, enabled).apply()
}

fun saveLastCheckedTime(context: Context, timestamp: String) {
    val sharedPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    sharedPrefs.edit().putString(KEY_LAST_CHECKED_TIME, timestamp).apply()
}

fun getLastCheckedTime(context: Context): String {
    val sharedPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    return sharedPrefs.getString(KEY_LAST_CHECKED_TIME, "") ?: ""
}

fun getBetaUpdatesSetting(context: Context): Boolean {
    val sharedPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    return sharedPrefs.getBoolean(KEY_BETA_UPDATES, false)
}

fun saveBetaUpdatesSetting(context: Context, enabled: Boolean) {
    val sharedPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    sharedPrefs.edit().putBoolean(KEY_BETA_UPDATES, enabled).apply()
}

fun isNewerVersion(latestVersion: String, currentVersion: String): Boolean {
    val latestVersionClean = latestVersion.removePrefix("b").removePrefix("v")
    val currentVersionClean = currentVersion.removePrefix("b").removePrefix("v")

    val latestParts = latestVersionClean.split(".").map { it.toIntOrNull() ?: 0 }
    val currentParts = currentVersionClean.split(".").map { it.toIntOrNull() ?: 0 }
    
    for (i in 0 until maxOf(latestParts.size, currentParts.size)) {
        val latest = latestParts.getOrElse(i) { 0 }
        val current = currentParts.getOrElse(i) { 0 }
        when {
            latest > current -> return true
            latest < current -> return false
        }
    }
    
    if (latestVersionClean == currentVersionClean) {
        val latestIsBeta = latestVersion.startsWith("b")
        val currentIsBeta = currentVersion.startsWith("b")
        if (currentIsBeta && !latestIsBeta) return true
    }
    return false
}

fun formatGitHubDate(githubDate: String): String = try {
    val githubFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'")
    val displayFormatter = DateTimeFormatter.ofPattern("d MMMM yyyy, h:mm a")
    val dateTime = LocalDateTime.parse(githubDate, githubFormatter)
    dateTime.format(displayFormatter)
} catch (e: Exception) {
    githubDate
}

suspend fun checkForUpdate(
    context: Context,
    onSuccess: (tag: String, isAvailable: Boolean, changelog: List<ChangelogSection>, size: String, date: String, description: String?, imageUrl: String?, apkUrl: String?) -> Unit,
    onError: () -> Unit,
) {
    withContext(Dispatchers.IO) {
        try {
            val url = URL("https://api.github.com/repos/kituontop69-cell/MelodyX/releases/latest")
            val json = url.openStream().bufferedReader().use { it.readText() }
            val targetRelease = JSONObject(json)
            
            val currentVersion = BuildConfig.VERSION_NAME
            val targetTagName = targetRelease.getString("tag_name")
            val shouldShow = isNewerVersion(targetTagName, currentVersion)

            if (shouldShow) {
                val changelogList = mutableListOf<ChangelogSection>()
                var description: String? = targetRelease.optString("body")
                var imageUrl: String? = null
                
                val publishedAt = targetRelease.getString("published_at")
                val formattedReleaseDate = formatGitHubDate(publishedAt)
                val assets = targetRelease.getJSONArray("assets")

                var apkSizeInMB = ""
                var apkDownloadUrl = ""
                for (j in 0 until assets.length()) {
                    val asset = assets.getJSONObject(j)
                    val assetName = asset.getString("name")
                    if (assetName.endsWith(".apk", ignoreCase = true) && !assetName.lowercase().contains("debug")) {
                        val apkSizeInBytes = asset.getLong("size")
                        apkSizeInMB = String.format("%.1f", apkSizeInBytes / (1024.0 * 1024.0))
                        apkDownloadUrl = asset.getString("browser_download_url")
                        break
                    }
                }

                withContext(Dispatchers.Main) {
                    onSuccess(targetTagName, true, changelogList, apkSizeInMB, formattedReleaseDate, description, imageUrl, apkDownloadUrl)
                }
            } else {
                withContext(Dispatchers.Main) {
                    onSuccess(currentVersion, false, emptyList(), "", "", null, null, null)
                }
            }
        } catch (e: Exception) {
            Log.e("UpdateCheck", "Error: ${e.message}")
            withContext(Dispatchers.Main) { onError() }
        }
    }
}

fun String.extractUrls(): List<Pair<IntRange, String>> {
    val urlPattern = Pattern.compile("(?:^|[\\s])((https?://|www\\.|pic\\.)[\\w-]+(\\.[\\w-]+)+([/?].*)?)")
    val matcher = urlPattern.matcher(this)
    val urlList = mutableListOf<Pair<IntRange, String>>()
    while (matcher.find()) {
        val url = matcher.group(1)?.trim() ?: continue
        val range = IntRange(matcher.start(1), matcher.end(1) - 1)
        val fullUrl = if (url.startsWith("http")) url else "https://$url"
        urlList.add(range to fullUrl)
    }
    return urlList
}
