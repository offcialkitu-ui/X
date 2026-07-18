/**
 * MelodyX (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 *
 * WidgetUpdateWorker – Battery-friendly deferred widget update via WorkManager.
 * Called by EchoMusicWidgetManager when updates are requested faster than the
 * throttle interval (1 second). This coalesces rapid updates into a single
 * deferred pass, preventing excessive bitmap regeneration and AppWidgetManager
 * calls during seek-bar scrubbing or rapid track skips.
 */

package iad1tya.echo.music.widget

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * WorkManager worker that triggers a widget refresh from the last known track state.
 * The actual update data is read from the MusicService's current playing state
 * when the worker executes.
 */
class WidgetUpdateWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        // The MusicService observes MediaSession state changes and calls
        // EchoMusicWidgetManager.updateWidgets() itself. This worker exists
        // as a coalescing mechanism – it tells the service to re-issue its
        // latest state to the widget manager.
        val intent = android.content.Intent(
            applicationContext,
            Class.forName("iad1tya.echo.music.playback.MusicService"),
        ).apply {
            action = "iad1tya.echo.music.widget.UPDATE_WIDGET"
        }
        return try {
            applicationContext.startService(intent)
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    companion object {
        const val WORK_NAME = "widget_update_work"
        const val TAG = "widget_update"
    }
}