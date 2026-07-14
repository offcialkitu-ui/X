package iad1tya.echo.music.echomusic.updater

import android.content.Context
import android.os.Environment
import android.os.SystemClock
import android.util.Log
import iad1tya.echo.music.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
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
const val KEY_LAST_CHECKED_ELAPSED = "last_checked_elapsed"
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
    return sharedPrefs.getBoolean(KEY_AUTO_UPDATE_CHECK, true) // Default: true so check runs by default!
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

/**
 * Store the *elapsed realtime* (monotonic clock) of the last background check.
 * Using monotonic time means the system clock cannot be skewed to suppress
 * or repeatedly force update checks.
 */
fun saveLastCheckedElapsed(context: Context, elapsedMs: Long) {
    val sharedPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    sharedPrefs.edit().putLong(KEY_LAST_CHECKED_ELAPSED, elapsedMs).apply()
}

fun getLastCheckedElapsed(context: Context): Long {
    val sharedPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    return sharedPrefs.getLong(KEY_LAST_CHECKED_ELAPSED, 0L)
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

/**
 * Minimum time between automatic background update checks (6 hours) measured
 * in monotonic elapsed-realtime to be immune to system-clock changes.
 */
private const val BACKGROUND_CHECK_COOLDOWN_MS = 6 * 60 * 60 * 1000L

/**
 * Check for app updates using the consolidated [UpdateRepository].
 * This version:
 * - Uses monotonic elapsed-realtime for clock-skew safety
 * - Consolidates to the single `update.json` source of truth
 * - Respects beta-update preference
 * - Enforces a minimum cooldown between background checks
 */
suspend fun checkForUpdate(
    context: Context,
    onSuccess: (tag: String, isAvailable: Boolean, changelog: List<ChangelogSection>, size: String, date: String, description: String?, imageUrl: String?, apkUrl: String?) -> Unit,
    onError: () -> Unit,
) {
    withContext(Dispatchers.IO) {
        // ── Clock-skew safe rate limiting ────────────────────────────────
        val nowElapsed = android.os.SystemClock.elapsedRealtime()
        val lastElapsed = getLastCheckedElapsed(context)
        if (lastElapsed > 0 && (nowElapsed - lastElapsed) < BACKGROUND_CHECK_COOLDOWN_MS) {
            Log.d("UpdateCheck", "Skipping check (cooldown): ${(nowElapsed - lastElapsed) / 1000}s ago")
            return@withContext
        }

        try {
            val repository = UpdateRepository()
            val result = repository.fetchUpdateInfo(force = false)

            if (result.isSuccess) {
                // ── Only save cooldown timestamp on successful fetch ──────
                saveLastCheckedElapsed(context, nowElapsed)
                val info = result.getOrThrow()
                val betaEnabled = getBetaUpdatesSetting(context)
                val isAvailable = repository.isUpdateAvailable(info, betaEnabled)
                val currentVersion = BuildConfig.VERSION_NAME
                val targetTagName = "v${info.versionName}"

                if (isAvailable) {
                    val changelogList = mutableListOf<ChangelogSection>()
                    // description/imageUrl aren't in the Retrofit model; pass null
                    // (changelog content is loaded via UpdateScreen's own ViewModel pipeline)
                    withContext(Dispatchers.Main) {
                        onSuccess(targetTagName, true, changelogList, info.apkSize, info.releaseDate, null, null, info.apkUrl)
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        onSuccess(currentVersion, false, emptyList(), "", "", null, null, null)
                    }
                }
            } else {
                val error = result.exceptionOrNull()
                Log.e("UpdateCheck", "Repository fetch failed: ${error?.message}")
                withContext(Dispatchers.Main) { onError() }
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
