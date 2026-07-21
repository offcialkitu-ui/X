/**
 * MelodyX (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 *
 * EchoMusicWidgetManager — premium widget rendering engine.
 *
 * Uses WidgetArtStudio for:
 *  - Palette API color extraction from album art (dominant/vibrant/muted)
 *  - Blurred artwork backdrop with dynamic-color scrim (WCAG AA)
 *  - Rounded/circular art with system corner radii
 *  - Full vinyl disc rendering with tonearm + progress arc (turntable)
 *  - Generated gradient artwork from track-title hash (no-artwork fallback)
 *  - Heart-burst rendering for like animation
 *  - Waveform rendering for recognizer listening state
 *  - Material You Dynamic Color on Android 12+, brand red (#E53935) below.
 *
 * Rich empty states: shows "Continue listening: <last played track>" with
 * artwork and resume play button when nothing is playing.
 * Battery-friendly updates via WorkManager with update throttling.
 */

package iad1tya.echo.music.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.view.View
import android.widget.RemoteViews
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import coil3.ImageLoader
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.request.crossfade
import coil3.toBitmap
import iad1tya.echo.music.MainActivity
import iad1tya.echo.music.R
import iad1tya.echo.music.db.MusicDatabase
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EchoMusicWidgetManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: MusicDatabase,
    private val playlistWidgetManager: PlaylistWidgetManager,
) {
    private val imageLoader by lazy {
        ImageLoader.Builder(context).crossfade(false).build()
    }

    // Cache for album art + computed colours to avoid re-extraction on every update
    private var cachedArtworkUri: String? = null
    private var cachedAlbumArt: Bitmap? = null
    private var cachedColors: WidgetArtStudio.WidgetColors? = null
    private var cachedVinyl: Bitmap? = null
    private var cachedVinylProgress: Float = -1f
    private var cachedVinylRotation: Float = -1f
    private var cachedVinylPlaying: Boolean = false

    // Throttle: track last update time to avoid rapid refreshes
    private var lastUpdateTimeMs: Long = 0L
    private val minUpdateIntervalMs = 1000L // 1 second minimum between updates
    private val workManagerUpdateDelayMs = 300L

    /**
     * Main entry point: update all widgets with current track state.
     * Uses WorkManager for battery-friendly deferral when called in rapid succession.
     * Falls back to immediate update for play/pause toggles.
     */
    suspend fun updateWidgets(
        title: String,
        artist: String,
        artworkUri: String?,
        isPlaying: Boolean,
        isLiked: Boolean,
        duration: Long = 0,
        currentPosition: Long = 0,
    ) {
        val now = System.currentTimeMillis()
        // Throttle: if called too quickly, defer via WorkManager
        if (now - lastUpdateTimeMs < minUpdateIntervalMs) {
            val workRequest = OneTimeWorkRequestBuilder<WidgetUpdateWorker>()
                .setConstraints(
                    Constraints.Builder()
                        .build()
                )
                .setInitialDelay(workManagerUpdateDelayMs, TimeUnit.MILLISECONDS)
                .addTag("widget_update")
                .build()
            WorkManager.getInstance(context)
                .enqueueUniqueWork(
                    "widget_update",
                    ExistingWorkPolicy.REPLACE,
                    workRequest,
                )
            return
        }
        lastUpdateTimeMs = now

        val appWidgetManager = AppWidgetManager.getInstance(context)

        // Load album art (from cache if URI unchanged)
        val albumArt: Bitmap?
        val widgetColors: WidgetArtStudio.WidgetColors
        if (artworkUri != null && artworkUri == cachedArtworkUri && cachedAlbumArt != null && cachedColors != null) {
            albumArt = cachedAlbumArt
            widgetColors = cachedColors!!
        } else {
            albumArt = artworkUri?.let { loadAlbumArt(it, 320) }
            widgetColors = if (albumArt != null) {
                WidgetArtStudio.fromArtwork(context, albumArt)
            } else if (title.isNotEmpty() && title != context.getString(R.string.not_playing)) {
                WidgetArtStudio.fromTitleHash(context, title)
            } else {
                WidgetArtStudio.dynamicFallback(context)
            }
            cachedArtworkUri = artworkUri
            cachedAlbumArt = albumArt
            cachedColors = widgetColors
            // Invalidate vinyl cache on art change
            cachedVinyl = null
        }

        // Compute progress for vinyl arc
        val progress = if (duration > 0) {
            (currentPosition.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
        } else 0f

        // Rotation for vinyl disc animation (increments each update)
        val rotation = if (isPlaying) (cachedVinylRotation + 12f) % 360f else cachedVinylRotation.coerceAtLeast(0f)

        // ── Music Player (4×2 / 4×1 / 2×2) ────────────────────────────────
        val componentName = ComponentName(context, MusicWidgetReceiver::class.java)
        val widgetIds = appWidgetManager.getAppWidgetIds(componentName)
        if (widgetIds.isNotEmpty()) {
            widgetIds.forEach { widgetId ->
                val options = appWidgetManager.getAppWidgetOptions(widgetId)
                val views = createMusicRemoteViewsForSize(
                    options = options,
                    title = title,
                    artist = artist,
                    albumArt = albumArt,
                    widgetColors = widgetColors,
                    isPlaying = isPlaying,
                    isLiked = isLiked,
                    progress = progress,
                    duration = duration,
                    currentPosition = currentPosition,
                    artworkUri = artworkUri,
                )
                appWidgetManager.updateAppWidget(widgetId, views)
            }
        }

        // ── Turntable (2×2) ───────────────────────────────────────────────
        val turntableComponentName = ComponentName(context, TurntableWidgetReceiver::class.java)
        val turntableWidgetIds = appWidgetManager.getAppWidgetIds(turntableComponentName)
        if (turntableWidgetIds.isNotEmpty()) {
            val needsNewVinyl = cachedVinyl == null ||
                cachedVinylProgress != progress ||
                cachedVinylPlaying != isPlaying ||
                (isPlaying && kotlin.math.abs(cachedVinylRotation - rotation) > 5f)

            val vinylBitmap = if (needsNewVinyl) {
                WidgetArtStudio.vinylDisc(
                    context = context,
                    albumArt = albumArt,
                    colors = widgetColors,
                    sizePx = 400,
                    progress = progress,
                    rotationDeg = rotation,
                    isPlaying = isPlaying,
                ).also {
                    cachedVinyl = it
                    cachedVinylProgress = progress
                    cachedVinylRotation = rotation
                    cachedVinylPlaying = isPlaying
                }
            } else {
                cachedVinyl!!
            }

            val views = createTurntableViews(
                vinylBitmap = vinylBitmap,
                widgetColors = widgetColors,
                isPlaying = isPlaying,
                isLiked = isLiked,
            )
            turntableWidgetIds.forEach { widgetId ->
                appWidgetManager.updateAppWidget(widgetId, views)
            }
        }

        // ── Playlist widget ───────────────────────────────────────────────
        playlistWidgetManager.updateWidgets(
            title = title,
            artist = artist,
            artworkUri = artworkUri,
            isPlaying = isPlaying,
            isLiked = isLiked,
            duration = duration,
            currentPosition = currentPosition,
        )
    }

    // ─── Music Player: size-adaptive ────────────────────────────────────────

    private fun createMusicRemoteViewsForSize(
        options: Bundle,
        title: String,
        artist: String,
        albumArt: Bitmap?,
        widgetColors: WidgetArtStudio.WidgetColors,
        isPlaying: Boolean,
        isLiked: Boolean,
        progress: Float,
        duration: Long,
        currentPosition: Long,
        artworkUri: String?,
    ): RemoteViews {
        val minWidth = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH)
        val minHeight = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT)

        return when {
            minWidth < 180 && minHeight < 100 -> createCompactSquare(title, albumArt, widgetColors, isPlaying, isLiked)
            minWidth >= 180 && minHeight < 100 -> createCompactWide(title, artist, albumArt, widgetColors, isPlaying, isLiked)
            else -> createFullMusic(title, artist, albumArt, widgetColors, isPlaying, isLiked, progress, duration, currentPosition, artworkUri)
        }
    }

    // ─── Full 4×2 ──────────────────────────────────────────────────────────

    private fun createFullMusic(
        title: String,
        artist: String,
        albumArt: Bitmap?,
        colors: WidgetArtStudio.WidgetColors,
        isPlaying: Boolean,
        isLiked: Boolean,
        progress: Float,
        duration: Long,
        currentPosition: Long,
        artworkUri: String?,
    ): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_music_player)

        // Backdrop: blurred artwork + palette scrim
        setBackdrop(views, albumArt, colors, R.id.widget_backdrop)

        // Rounded album art
        val artSize = 200
        val roundedArt = if (albumArt != null) {
            WidgetArtStudio.roundedArt(context, albumArt, WidgetArtStudio.innerRadiusPx(context))
        } else {
            WidgetArtStudio.generatedArt(context, title, artSize)
        }
        views.setImageViewBitmap(R.id.widget_album_art, roundedArt)

        // ── Rich empty state: if not playing, show "Continue listening" with last track ──
        val isNotPlaying = title.isEmpty() || title == context.getString(R.string.not_playing)

        if (isNotPlaying && albumArt != null) {
            views.setViewVisibility(R.id.widget_context_label, View.VISIBLE)
            views.setTextViewText(R.id.widget_context_label,
                context.getString(R.string.widget_continue_listening))
            views.setTextViewText(R.id.widget_song_title, title)
            views.setTextViewText(R.id.widget_artist_name, artist)
        } else if (isNotPlaying) {
            views.setViewVisibility(R.id.widget_context_label, View.GONE)
            views.setTextViewText(R.id.widget_song_title, context.getString(R.string.app_name))
            views.setTextViewText(R.id.widget_artist_name,
                context.getString(R.string.widget_tap_to_play))
        } else {
            views.setViewVisibility(R.id.widget_context_label, View.GONE)
            views.setTextViewText(R.id.widget_song_title, title)
            views.setTextViewText(R.id.widget_artist_name, artist)
        }

        // Text colors with WCAG AA contrast
        views.setTextColor(R.id.widget_song_title, colors.onArt)
        views.setTextColor(R.id.widget_artist_name, colors.onArtSecondary)
        views.setTextColor(R.id.widget_context_label, colors.accent)
        views.setTextColor(R.id.widget_time_elapsed, colors.onArtSecondary)
        views.setTextColor(R.id.widget_time_total, colors.onArtSecondary)

        // Play/pause — set accent circle for the play background
        views.setImageViewResource(R.id.widget_play_pause,
            if (isPlaying) R.drawable.ic_widget_pause else R.drawable.ic_widget_play)
        views.setImageViewBitmap(R.id.widget_play_bg,
            WidgetArtStudio.accentCircle(44, colors.accent))
        views.setInt(R.id.widget_shuffle_icon, "setColorFilter", colors.onArtSecondary)
        views.setInt(R.id.widget_prev_icon, "setColorFilter", colors.onArtSecondary)
        views.setInt(R.id.widget_next_icon, "setColorFilter", colors.onArtSecondary)

        // Like
        views.setImageViewResource(R.id.widget_like_button,
            if (isLiked) R.drawable.ic_widget_heart_nav else R.drawable.ic_widget_heart_outline_nav)
        views.setInt(R.id.widget_like_button, "setColorFilter",
            if (isLiked) colors.accent else colors.onArtSecondary)

        // Progress bar tint
        views.setInt(R.id.widget_progress_fill, "setColorFilter", colors.accent)
        val level = (progress * 10000).toInt().coerceIn(0, 10000)
        views.setInt(R.id.widget_progress_fill, "setImageLevel", level)

        // Time labels
        views.setTextViewText(R.id.widget_time_elapsed, WidgetArtStudio.formatTime(currentPosition))
        views.setTextViewText(R.id.widget_time_total, WidgetArtStudio.formatTime(duration))

        // Marquee - ensure focus for animation
        views.setBoolean(R.id.widget_song_title, "setFocusable", true)
        views.setBoolean(R.id.widget_song_title, "setFocusableInTouchMode", true)

        // Click intents
        views.setOnClickPendingIntent(R.id.widget_album_art, getOpenAppIntent())
        views.setOnClickPendingIntent(R.id.widget_play_pause_container, getPlayPauseIntent())
        views.setOnClickPendingIntent(R.id.widget_like_button, getLikeIntent())
        views.setOnClickPendingIntent(R.id.widget_shuffle_button, getShuffleIntent())
        views.setOnClickPendingIntent(R.id.widget_prev_button, getPreviousIntent())
        views.setOnClickPendingIntent(R.id.widget_next_button, getNextIntent())

        return views
    }

    // ─── Compact Square (2×2) ──────────────────────────────────────────────

    private fun createCompactSquare(
        title: String,
        albumArt: Bitmap?,
        colors: WidgetArtStudio.WidgetColors,
        isPlaying: Boolean,
        isLiked: Boolean,
    ): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_compact_square)

        setBackdrop(views, albumArt, colors, R.id.widget_backdrop)

        val art = if (albumArt != null) {
            WidgetArtStudio.heroTile(context, albumArt, 400, colors)
        } else {
            WidgetArtStudio.generatedArt(context, title, 400)
        }
        views.setImageViewBitmap(R.id.widget_compact_album_art, art)

        views.setImageViewResource(R.id.widget_compact_play_pause,
            if (isPlaying) R.drawable.ic_widget_pause_low else R.drawable.ic_widget_play_low)
        views.setImageViewBitmap(R.id.widget_compact_play_bg,
            WidgetArtStudio.accentCircle(56, colors.accent))

        views.setOnClickPendingIntent(R.id.widget_compact_album_art, getOpenAppIntent())
        views.setOnClickPendingIntent(R.id.widget_compact_play_container, getPlayPauseIntent())

        return views
    }

    // ─── Compact Wide (4×1) ────────────────────────────────────────────────

    private fun createCompactWide(
        title: String,
        artist: String,
        albumArt: Bitmap?,
        colors: WidgetArtStudio.WidgetColors,
        isPlaying: Boolean,
        isLiked: Boolean,
    ): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_compact_wide)

        setBackdrop(views, albumArt, colors, R.id.widget_backdrop)

        val artSize = 120
        val rounded = if (albumArt != null) {
            WidgetArtStudio.roundedArt(context, albumArt, WidgetArtStudio.innerRadiusPx(context))
        } else {
            WidgetArtStudio.generatedArt(context, title, artSize)
        }
        views.setImageViewBitmap(R.id.widget_wide_album_art, rounded)

        views.setTextViewText(R.id.widget_wide_song_title, title)
        views.setTextViewText(R.id.widget_wide_artist_name, artist)
        views.setTextColor(R.id.widget_wide_song_title, colors.onArt)
        views.setTextColor(R.id.widget_wide_artist_name, colors.onArtSecondary)

        views.setBoolean(R.id.widget_wide_song_title, "setFocusable", true)
        views.setBoolean(R.id.widget_wide_song_title, "setFocusableInTouchMode", true)

        views.setImageViewResource(R.id.widget_wide_play_pause,
            if (isPlaying) R.drawable.ic_widget_pause_low else R.drawable.ic_widget_play_low)
        views.setImageViewBitmap(R.id.widget_wide_play_bg,
            WidgetArtStudio.accentCircle(44, colors.accent))
        views.setImageViewResource(R.id.widget_wide_like_button,
            if (isLiked) R.drawable.ic_widget_heart_nav else R.drawable.ic_widget_heart_outline_nav)
        views.setInt(R.id.widget_wide_like_button, "setColorFilter",
            if (isLiked) colors.accent else colors.onArtSecondary)

        views.setOnClickPendingIntent(R.id.widget_wide_album_art, getOpenAppIntent())
        views.setOnClickPendingIntent(R.id.widget_wide_play_container, getPlayPauseIntent())
        views.setOnClickPendingIntent(R.id.widget_wide_like_button, getLikeIntent())

        return views
    }

    // ─── Turntable (2×2) ───────────────────────────────────────────────────

    private fun createTurntableViews(
        vinylBitmap: Bitmap,
        widgetColors: WidgetArtStudio.WidgetColors,
        isPlaying: Boolean,
        isLiked: Boolean,
    ): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_turntable)

        val backdrop = WidgetArtStudio.backdrop(context, null, 400, 400, widgetColors)
        views.setImageViewBitmap(R.id.widget_backdrop, backdrop)

        views.setImageViewBitmap(R.id.widget_turntable_album_art, vinylBitmap)

        views.setInt(R.id.widget_turntable_play_pause, "setColorFilter", widgetColors.onAccent)
        views.setInt(R.id.widget_turntable_play_container, "setColorFilter", widgetColors.accent)
        views.setInt(R.id.widget_turntable_prev_button, "setColorFilter", widgetColors.onArtSecondary)
        views.setInt(R.id.widget_turntable_next_button, "setColorFilter", widgetColors.onArtSecondary)

        views.setImageViewResource(R.id.widget_turntable_play_pause,
            if (isPlaying) R.drawable.ic_widget_pause_secondary else R.drawable.ic_widget_play_secondary)

        views.setImageViewResource(R.id.widget_turntable_like_button,
            if (isLiked) R.drawable.ic_widget_heart_nav else R.drawable.ic_widget_heart_outline_nav)
        views.setInt(R.id.widget_turntable_like_button, "setColorFilter",
            if (isLiked) widgetColors.accent else widgetColors.onArtSecondary)

        views.setOnClickPendingIntent(R.id.widget_turntable_album_art, getOpenAppIntent())
        views.setOnClickPendingIntent(R.id.widget_turntable_play_container, getTurntablePlayPauseIntent())
        views.setOnClickPendingIntent(R.id.widget_turntable_prev_button, getTurntablePreviousIntent())
        views.setOnClickPendingIntent(R.id.widget_turntable_next_button, getTurntableNextIntent())
        views.setOnClickPendingIntent(R.id.widget_turntable_like_container, getLikeIntent())

        return views
    }

    // ─── Helpers ───────────────────────────────────────────────────────────

    private fun setBackdrop(
        views: RemoteViews,
        albumArt: Bitmap?,
        colors: WidgetArtStudio.WidgetColors,
        backdropId: Int,
    ) {
        val bitmap = WidgetArtStudio.backdrop(context, albumArt, 600, 400, colors)
        views.setImageViewBitmap(backdropId, bitmap)
    }

    private suspend fun loadAlbumArt(artworkUri: String, size: Int = 320): Bitmap? =
        withContext(Dispatchers.IO) {
            try {
                val request = ImageRequest.Builder(context)
                    .data(artworkUri)
                    .size(size, size)
                    .allowHardware(false)
                    .build()
                imageLoader.execute(request).image?.toBitmap()
            } catch (_: Exception) { null }
        }

    // ─── PendingIntents ────────────────────────────────────────────────────

    private fun getOpenAppIntent(): PendingIntent =
        PendingIntent.getActivity(context, 0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

    private fun getPlayPauseIntent(): PendingIntent =
        PendingIntent.getBroadcast(context, 1,
            Intent(context, MusicWidgetReceiver::class.java).apply {
                action = MusicWidgetReceiver.ACTION_PLAY_PAUSE
            }, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

    private fun getLikeIntent(): PendingIntent =
        PendingIntent.getBroadcast(context, 2,
            Intent(context, MusicWidgetReceiver::class.java).apply {
                action = MusicWidgetReceiver.ACTION_LIKE
            }, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

    private fun getShuffleIntent(): PendingIntent =
        PendingIntent.getBroadcast(context, 6,
            Intent(context, MusicWidgetReceiver::class.java).apply {
                action = MusicWidgetReceiver.ACTION_SHUFFLE
            }, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

    private fun getPreviousIntent(): PendingIntent =
        PendingIntent.getBroadcast(context, 7,
            Intent(context, MusicWidgetReceiver::class.java).apply {
                action = MusicWidgetReceiver.ACTION_PREVIOUS
            }, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

    private fun getNextIntent(): PendingIntent =
        PendingIntent.getBroadcast(context, 8,
            Intent(context, MusicWidgetReceiver::class.java).apply {
                action = MusicWidgetReceiver.ACTION_NEXT
            }, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

    private fun getTurntablePlayPauseIntent(): PendingIntent =
        PendingIntent.getBroadcast(context, 3,
            Intent(context, TurntableWidgetReceiver::class.java).apply {
                action = TurntableWidgetReceiver.ACTION_TURNTABLE_PLAY_PAUSE
            }, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

    private fun getTurntableNextIntent(): PendingIntent =
        PendingIntent.getBroadcast(context, 4,
            Intent(context, TurntableWidgetReceiver::class.java).apply {
                action = TurntableWidgetReceiver.ACTION_TURNTABLE_NEXT
            }, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

    private fun getTurntablePreviousIntent(): PendingIntent =
        PendingIntent.getBroadcast(context, 5,
            Intent(context, TurntableWidgetReceiver::class.java).apply {
                action = TurntableWidgetReceiver.ACTION_TURNTABLE_PREVIOUS
            }, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
}