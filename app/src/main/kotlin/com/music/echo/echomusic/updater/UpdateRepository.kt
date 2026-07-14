package iad1tya.echo.music.echomusic.updater

import android.content.Context
import android.os.SystemClock
import iad1tya.echo.music.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

interface UpdateApiService {
    @GET("update.json")
    suspend fun getUpdateInfo(): UpdateInfo
}

@Singleton
class UpdateRepository @Inject constructor() {
    private val api: UpdateApiService by lazy {
        Retrofit.Builder()
            .baseUrl("https://raw.githubusercontent.com/offcialkitu-ui/X/main/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(UpdateApiService::class.java)
    }

    // ── Clock-skew safe rate limiting ─────────────────────────────────────
    // Uses SystemClock.elapsedRealtime() (monotonic) instead of wall-clock,
    // so changing the device time cannot suppress or force extra checks.

    companion object {
        /** Minimum interval between fetch attempts (2 hours). */
        private const val FETCH_COOLDOWN_MS = 2 * 60 * 60 * 1000L
    }

    @Volatile
    private var lastFetchElapsedMs = 0L  // SystemClock.elapsedRealtime() at last fetch start

    @Volatile
    private var lastSuccessfulResult: Result<UpdateInfo>? = null

    /**
     * Fetch update info from the remote server, subject to a cooldown.
     * Returns the cached *successful* result if called within [FETCH_COOLDOWN_MS].
     * Failures are never cached — a subsequent call after the cooldown will
     * always attempt a fresh fetch.
     */
    suspend fun fetchUpdateInfo(force: Boolean = false): Result<UpdateInfo> = withContext(Dispatchers.IO) {
        val now = SystemClock.elapsedRealtime()

        // Only return cached result if: (a) cooldown hasn't expired AND (b) it was a success
        if (!force && lastSuccessfulResult != null && lastFetchElapsedMs != 0L &&
            (now - lastFetchElapsedMs < FETCH_COOLDOWN_MS)
        ) {
            Timber.d("UpdateRepository: Using cached result (${(now - lastFetchElapsedMs) / 1000}s ago)")
            return@withContext lastSuccessfulResult!!
        }

        lastFetchElapsedMs = now

        return@withContext try {
            val info = api.getUpdateInfo()
            val result = Result.success(info)
            lastSuccessfulResult = result
            Timber.d("UpdateRepository: Fetched update info (v${info.versionName}, code=${info.versionCode})")
            result
        } catch (e: Exception) {
            Timber.e(e, "UpdateRepository: Failed to fetch update info")
            // NEVER cache failures — transient errors should not block retries
            Result.failure(e)
        }
    }

    /**
     * Check whether a fetched update is newer than the currently installed version.
     * Also respects beta-update preferences.
     */
    fun isUpdateAvailable(updateInfo: UpdateInfo, betaEnabled: Boolean = false): Boolean {
        val remoteCode = updateInfo.versionCode
        val localCode = BuildConfig.VERSION_CODE

        if (remoteCode > localCode) return true
        if (remoteCode < localCode) return false

        // Same versionCode – check whether this is a beta upgrade
        if (betaEnabled && updateInfo.versionName.startsWith("b")) {
            return true
        }
        return false
    }

    /** Force the next fetch to hit the server regardless of cooldown. */
    fun invalidateCache() {
        lastFetchElapsedMs = 0L
        lastSuccessfulResult = null
        Timber.d("UpdateRepository: Cache invalidated")
    }
}
