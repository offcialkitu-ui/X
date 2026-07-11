package iad1tya.echo.music.echomusic.updater

import android.content.Context
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
            .baseUrl("https://raw.githubusercontent.com/kituontop69-cell/MelodyX/main/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(UpdateApiService::class.java)
    }

    suspend fun fetchUpdateInfo(): Result<UpdateInfo> = withContext(Dispatchers.IO) {
        try {
            val info = api.getUpdateInfo()
            Result.success(info)
        } catch (e: Exception) {
            Timber.e(e, "Failed to fetch update info")
            Result.failure(e)
        }
    }

    fun isUpdateAvailable(updateInfo: UpdateInfo): Boolean {
        return updateInfo.versionCode > BuildConfig.VERSION_CODE
    }
}
