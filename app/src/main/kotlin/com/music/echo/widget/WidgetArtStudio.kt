/**
 * MELODY X (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 *
 * WidgetArtStudio - the shared rendering + color engine for all home-screen widgets.
 *
 * Responsibilities:
 *  - Extract dominant/vibrant colors from album art (Palette API) with WCAG AA
 *    contrast enforcement for any text/icon drawn over artwork.
 *  - Dynamic Color (Material You) fallback on Android 12+, brand red (#E53935) below.
 *  - Render premium widget bitmaps: blurred-artwork backdrops with baked-in scrims,
 *    rounded/circular artwork, spinning vinyl discs with progress arc + tonearm,
 *    and generated gradient artwork from a track-title hash when art is missing.
 *  - Heart-burst bitmap for like animation
 *  - Waveform bitmap for recognizer listening state
 *  - Corner radii follow the launcher via system_app_widget_background_radius /
 *    system_app_widget_inner_radius on Android 12+.
 */

package iad1tya.echo.music.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.os.Build
import androidx.annotation.ColorInt
import androidx.core.graphics.ColorUtils
import androidx.palette.graphics.Palette
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.cos
import kotlin.math.PI

object WidgetArtStudio {

    /** Brand accent used below Android 12 (no Material You available). */
    private const val BRAND_RED = 0xFFE53935.toInt()

    private const val MIN_CONTRAST_BODY = 4.5   // WCAG AA, normal text
    private const val MIN_CONTRAST_LARGE = 3.0  // WCAG AA, large text / icons

    /**
     * Resolved color set for one widget render pass.
     * When [fromArtwork] is true the colors were extracted from album art;
     * otherwise they come from Material You / brand fallback.
     */
    data class WidgetColors(
        @ColorInt val backdropStart: Int,
        @ColorInt val backdropEnd: Int,
        @ColorInt val onArt: Int,           // primary text over backdrop (AA vs scrimmed backdrop)
        @ColorInt val onArtSecondary: Int,  // secondary text over backdrop
        @ColorInt val accent: Int,          // vibrant control tint (play button fill, progress)
        @ColorInt val onAccent: Int,        // icon color inside accent-filled controls
        @ColorInt val controlSurface: Int,  // translucent chip/pill surface over the backdrop
        val fromArtwork: Boolean,
    )

    // ─── Color extraction ─────────────────────────────────────────────────────

    /**
     * Extract dominant + vibrant colors from [albumArt] via the Palette API.
     * Falls back to [dynamicFallback] when art is null or extraction fails.
     */
    fun fromArtwork(context: Context, albumArt: Bitmap?): WidgetColors {
        if (albumArt == null) return dynamicFallback(context)
        return try {
            val palette = Palette.from(albumArt)
                .maximumColorCount(24)
                .generate()

            val dominant = palette.getDominantColor(Color.DKGRAY)
            val vibrant = palette.getVibrantColor(
                palette.getLightVibrantColor(
                    palette.getMutedColor(dominant),
                ),
            )

            // Backdrop gradient: deepened dominant -> darker dominant. Text always
            // renders on top of a baked scrim, so we bias the gradient dark for
            // reliable contrast in both light and dark launcher themes.
            val backdropStart = darken(dominant, 0.35f)
            val backdropEnd = darken(mix(dominant, vibrant, 0.35f), 0.55f)
            val scrimmed = mix(backdropStart, backdropEnd, 0.5f)

            val onArt = ensureContrast(Color.WHITE, scrimmed, MIN_CONTRAST_BODY)
            val onArtSecondary = ColorUtils.setAlphaComponent(onArt, 0xC8)

            // Accent must pop against the backdrop AND carry readable icons.
            var accent = brighten(vibrant, 0.15f)
            if (ColorUtils.calculateContrast(accent, scrimmed) < MIN_CONTRAST_LARGE) {
                accent = ensureContrast(accent, scrimmed, MIN_CONTRAST_LARGE)
            }
            val onAccent = bestOn(accent)

            WidgetColors(
                backdropStart = backdropStart,
                backdropEnd = backdropEnd,
                onArt = onArt,
                onArtSecondary = onArtSecondary,
                accent = accent,
                onAccent = onAccent,
                controlSurface = ColorUtils.setAlphaComponent(Color.WHITE, 0x33),
                fromArtwork = true,
            )
        } catch (_: Exception) {
            dynamicFallback(context)
        }
    }

    /**
     * Material You dynamic colors on Android 12+ (tinted from wallpaper),
     * brand red below. Honors the current day/night configuration.
     */
    fun dynamicFallback(context: Context): WidgetColors {
        val night = isNightMode(context)
        val accent: Int
        val backdropStart: Int
        val backdropEnd: Int
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            accent = context.getColor(
                if (night) android.R.color.system_accent1_200 else android.R.color.system_accent1_600,
            )
            backdropStart = context.getColor(
                if (night) android.R.color.system_neutral1_800 else android.R.color.system_neutral1_50,
            )
            backdropEnd = context.getColor(
                if (night) android.R.color.system_neutral1_900 else android.R.color.system_accent1_100,
            )
        } else {
            accent = BRAND_RED
            backdropStart = if (night) 0xFF1D1B20.toInt() else 0xFFFEF7FF.toInt()
            backdropEnd = if (night) 0xFF141218.toInt() else 0xFFF3EDF7.toInt()
        }
        val scrimmed = mix(backdropStart, backdropEnd, 0.5f)
        val onArt = ensureContrast(if (night) Color.WHITE else 0xFF1C1B1F.toInt(), scrimmed, MIN_CONTRAST_BODY)
        return WidgetColors(
            backdropStart = backdropStart,
            backdropEnd = backdropEnd,
            onArt = onArt,
            onArtSecondary = ColorUtils.setAlphaComponent(onArt, 0xB4),
            accent = ensureContrast(accent, scrimmed, MIN_CONTRAST_LARGE),
            onAccent = bestOn(accent),
            controlSurface = ColorUtils.setAlphaComponent(onArt, 0x1E),
            fromArtwork = false,
        )
    }

    /**
     * Deterministic gradient palette generated from a track-title hash.
     * Used when a song is playing but has no artwork (offline / local files).
     */
    fun fromTitleHash(context: Context, title: String): WidgetColors {
        val hue = (abs(title.hashCode()) % 360).toFloat()
        val start = Color.HSVToColor(floatArrayOf(hue, 0.62f, 0.46f))
        val end = Color.HSVToColor(floatArrayOf((hue + 42f) % 360f, 0.70f, 0.28f))
        val accent = Color.HSVToColor(floatArrayOf((hue + 24f) % 360f, 0.55f, 0.92f))
        val scrimmed = mix(start, end, 0.5f)
        val onArt = ensureContrast(Color.WHITE, scrimmed, MIN_CONTRAST_BODY)
        return WidgetColors(
            backdropStart = start,
            backdropEnd = end,
            onArt = onArt,
            onArtSecondary = ColorUtils.setAlphaComponent(onArt, 0xC8),
            accent = ensureContrast(accent, scrimmed, MIN_CONTRAST_LARGE),
            onAccent = bestOn(accent),
            controlSurface = ColorUtils.setAlphaComponent(Color.WHITE, 0x33),
            fromArtwork = false,
        )
    }

    // ─── Corner radii (match the launcher on Android 12+) ─────────────────────

    fun outerRadiusPx(context: Context): Float =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.resources
                .getDimensionPixelSize(android.R.dimen.system_app_widget_background_radius)
                .toFloat()
        } else {
            28f * context.resources.displayMetrics.density
        }

    fun innerRadiusPx(context: Context): Float =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.resources
                .getDimensionPixelSize(android.R.dimen.system_app_widget_inner_radius)
                .toFloat()
        } else {
            16f * context.resources.displayMetrics.density
        }

    // ─── Bitmap rendering ─────────────────────────────────────────────────────

    /**
     * Blurred-artwork widget backdrop with a baked-in dynamic-color scrim so any
     * text drawn on top meets WCAG AA. Corners are rounded with the launcher
     * radius. Pass a null [albumArt] to get a pure gradient backdrop.
     */
    fun backdrop(
        context: Context,
        albumArt: Bitmap?,
        widthPx: Int,
        heightPx: Int,
        colors: WidgetColors,
    ): Bitmap {
        val w = widthPx.coerceIn(64, 1200)
        val h = heightPx.coerceIn(64, 800)
        val output = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val radius = outerRadiusPx(context)
        val clip = Path().apply {
            addRoundRect(RectF(0f, 0f, w.toFloat(), h.toFloat()), radius, radius, Path.Direction.CW)
        }
        canvas.clipPath(clip)

        // 1. Base gradient (always present - also the no-artwork look)
        val gradientPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f, 0f, w.toFloat(), h.toFloat(),
                colors.backdropStart, colors.backdropEnd, Shader.TileMode.CLAMP,
            )
        }
        canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), gradientPaint)

        // 2. Blurred artwork, center-cropped over the gradient
        if (albumArt != null) {
            val blurred = stackBlurDownscaled(albumArt, 40, 12)
            val src = Matrix()
            val scale = max(w.toFloat() / blurred.width, h.toFloat() / blurred.height)
            src.setScale(scale, scale)
            src.postTranslate(
                (w - blurred.width * scale) / 2f,
                (h - blurred.height * scale) / 2f,
            )
            canvas.drawBitmap(blurred, src, Paint(Paint.FILTER_BITMAP_FLAG).apply { alpha = 0xE6 })
            blurred.recycle()

            // 3. Scrim: dominant-color wash + vertical darkening so white text passes AA
            canvas.drawRect(
                0f, 0f, w.toFloat(), h.toFloat(),
                Paint().apply { color = ColorUtils.setAlphaComponent(colors.backdropStart, 0x59) },
            )
            canvas.drawRect(
                0f, 0f, w.toFloat(), h.toFloat(),
                Paint().apply {
                    shader = LinearGradient(
                        0f, 0f, 0f, h.toFloat(),
                        ColorUtils.setAlphaComponent(Color.BLACK, 0x30),
                        ColorUtils.setAlphaComponent(Color.BLACK, 0x66),
                        Shader.TileMode.CLAMP,
                    )
                },
            )
        }
        return output
    }

    /** Square, center-cropped artwork with rounded corners (>= 16dp radius). */
    fun roundedArt(context: Context, bitmap: Bitmap, radiusPx: Float = innerRadiusPx(context)): Bitmap {
        val size = min(bitmap.width, bitmap.height)
        val square = cropSquare(bitmap)
        val output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
            shader = BitmapShader(square, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
        }
        canvas.drawRoundRect(RectF(0f, 0f, size.toFloat(), size.toFloat()), radiusPx, radiusPx, paint)
        if (square !== bitmap) square.recycle()
        return output
    }

    /** Circular center-cropped artwork. */
    fun circularArt(bitmap: Bitmap): Bitmap {
        val size = min(bitmap.width, bitmap.height)
        val square = cropSquare(bitmap)
        val output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
            shader = BitmapShader(square, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
        }
        canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint)
        if (square !== bitmap) square.recycle()
        return output
    }

    /**
     * Full-bleed artwork tile for the 2x2 player: center-cropped art, launcher
     * corner radius, bottom scrim baked in for the title/controls row.
     */
    fun heroTile(context: Context, albumArt: Bitmap, sizePx: Int, colors: WidgetColors): Bitmap {
        val output = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val radius = outerRadiusPx(context)
        val clip = Path().apply {
            addRoundRect(RectF(0f, 0f, sizePx.toFloat(), sizePx.toFloat()), radius, radius, Path.Direction.CW)
        }
        canvas.clipPath(clip)

        val square = cropSquare(albumArt)
        val matrix = Matrix()
        val scale = sizePx.toFloat() / square.width
        matrix.setScale(scale, scale)
        canvas.drawBitmap(square, matrix, Paint(Paint.FILTER_BITMAP_FLAG))
        if (square !== albumArt) square.recycle()

        // Bottom scrim for the overlaid title + play control
        canvas.drawRect(
            0f, sizePx * 0.45f, sizePx.toFloat(), sizePx.toFloat(),
            Paint().apply {
                shader = LinearGradient(
                    0f, sizePx * 0.45f, 0f, sizePx.toFloat(),
                    Color.TRANSPARENT,
                    ColorUtils.setAlphaComponent(darken(colors.backdropEnd, 0.4f), 0xD9),
                    Shader.TileMode.CLAMP,
                )
            },
        )
        return output
    }

    /**
     * Turntable vinyl disc: grooved record with the album art clipped in a circle
     * as the label, a thin progress arc ring around the disc, and a tonearm
     * overlay. [rotationDeg] rotates the label with playback so the record
     * appears to spin between refreshes.
     */
    fun vinylDisc(
        context: Context,
        albumArt: Bitmap?,
        colors: WidgetColors,
        sizePx: Int,
        progress: Float,
        rotationDeg: Float,
        isPlaying: Boolean,
    ): Bitmap {
        val output = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val cx = sizePx / 2f
        val cy = sizePx / 2f
        val discRadius = sizePx * 0.42f

        // Vinyl body with a subtle radial sheen
        canvas.drawCircle(
            cx, cy, discRadius,
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = RadialGradient(
                    cx - discRadius * 0.35f, cy - discRadius * 0.35f, discRadius * 1.8f,
                    0xFF2E2A31.toInt(), 0xFF121014.toInt(), Shader.TileMode.CLAMP,
                )
            },
        )

        // Grooves
        val groovePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = sizePx * 0.004f
            color = ColorUtils.setAlphaComponent(Color.WHITE, 0x14)
        }
        var r = discRadius * 0.58f
        while (r < discRadius * 0.96f) {
            canvas.drawCircle(cx, cy, r, groovePaint)
            r += sizePx * 0.022f
        }

        // Album art label, rotated with playback position
        val labelRadius = discRadius * 0.52f
        canvas.save()
        canvas.rotate(rotationDeg, cx, cy)
        if (albumArt != null) {
            val label = circularArt(albumArt)
            val dst = RectF(cx - labelRadius, cy - labelRadius, cx + labelRadius, cy + labelRadius)
            canvas.drawBitmap(label, null, dst, Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
            label.recycle()
        } else {
            canvas.drawCircle(
                cx, cy, labelRadius,
                Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    shader = LinearGradient(
                        cx - labelRadius, cy - labelRadius, cx + labelRadius, cy + labelRadius,
                        colors.backdropStart, colors.accent, Shader.TileMode.CLAMP,
                    )
                },
            )
        }
        // Small notch on the label edge sells the rotation between frames
        canvas.drawCircle(
            cx, cy - labelRadius * 0.82f, sizePx * 0.012f,
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = ColorUtils.setAlphaComponent(Color.WHITE, 0x8C)
            },
        )
        canvas.restore()

        // Spindle
        canvas.drawCircle(cx, cy, sizePx * 0.018f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFE6E1E5.toInt() })

        // Progress arc ring around the disc
        if (progress > 0f) {
            val arcInset = sizePx * 0.035f
            val arcRect = RectF(
                cx - discRadius - arcInset, cy - discRadius - arcInset,
                cx + discRadius + arcInset, cy + discRadius + arcInset,
            )
            canvas.drawArc(
                arcRect, -90f, 360f,
                false,
                Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    style = Paint.Style.STROKE
                    strokeWidth = sizePx * 0.014f
                    color = ColorUtils.setAlphaComponent(colors.onArt, 0x2E)
                },
            )
            canvas.drawArc(
                arcRect, -90f, 360f * progress.coerceIn(0f, 1f),
                false,
                Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    style = Paint.Style.STROKE
                    strokeCap = Paint.Cap.ROUND
                    strokeWidth = sizePx * 0.014f
                    color = colors.accent
                },
            )
        }

        // Tonearm overlay (top-right pivot). Rests off the record when paused.
        drawTonearm(canvas, sizePx, discRadius, isPlaying, colors)

        return output
    }

    private fun drawTonearm(
        canvas: Canvas,
        sizePx: Int,
        discRadius: Float,
        isPlaying: Boolean,
        colors: WidgetColors,
    ) {
        val pivotX = sizePx * 0.88f
        val pivotY = sizePx * 0.12f
        val armAngle = if (isPlaying) 32f else 12f // degrees toward the record

        canvas.save()
        canvas.rotate(armAngle, pivotX, pivotY)

        val armPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeWidth = sizePx * 0.018f
            color = 0xFFB8B2BC.toInt()
        }
        // Arm shaft with an elbow
        val elbowX = pivotX - sizePx * 0.02f
        val elbowY = pivotY + sizePx * 0.30f
        canvas.drawLine(pivotX, pivotY, elbowX, elbowY, armPaint)
        canvas.drawLine(elbowX, elbowY, elbowX - sizePx * 0.07f, elbowY + sizePx * 0.13f, armPaint)
        // Headshell
        canvas.drawCircle(
            elbowX - sizePx * 0.07f, elbowY + sizePx * 0.13f, sizePx * 0.026f,
            Paint(Paint.ANTI_ALIAS_FLAG).apply { color = colors.accent },
        )
        canvas.restore()

        // Pivot base
        canvas.drawCircle(pivotX, pivotY, sizePx * 0.045f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF49454F.toInt() })
        canvas.drawCircle(pivotX, pivotY, sizePx * 0.024f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF938F99.toInt() })
    }

    /**
     * Generated artwork when a track has none: gradient from the title hash with
     * a music-note glyph, rounded to [radiusPx].
     */
    fun generatedArt(context: Context, title: String, sizePx: Int, radiusPx: Float? = null): Bitmap {
        val colors = fromTitleHash(context, title)
        val radius = radiusPx ?: innerRadiusPx(context)
        val output = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        canvas.drawRoundRect(
            RectF(0f, 0f, sizePx.toFloat(), sizePx.toFloat()), radius, radius,
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = LinearGradient(
                    0f, 0f, sizePx.toFloat(), sizePx.toFloat(),
                    colors.backdropStart, colors.backdropEnd, Shader.TileMode.CLAMP,
                )
            },
        )
        context.getDrawable(iad1tya.echo.music.R.drawable.music_note)?.mutate()?.let { icon ->
            icon.setTint(ColorUtils.setAlphaComponent(colors.onArt, 0xDC))
            val iconSize = (sizePx * 0.42f).toInt()
            val offset = (sizePx - iconSize) / 2
            icon.setBounds(offset, offset, offset + iconSize, offset + iconSize)
            icon.draw(canvas)
        }
        return output
    }

    /** Accent-filled circle used behind tintable play buttons in previews/fallbacks. */
    fun accentCircle(sizePx: Int, @ColorInt color: Int): Bitmap {
        val output = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        Canvas(output).drawCircle(
            sizePx / 2f, sizePx / 2f, sizePx / 2f,
            Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color },
        )
        return output
    }

    /**
     * Heart-burst bitmap for the like animation.
     * Draws a filled heart with a subtle glow/burst effect.
     */
    fun heartBurst(sizePx: Int, @ColorInt heartColor: Int, @ColorInt glowColor: Int): Bitmap {
        val output = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val cx = sizePx / 2f
        val cy = sizePx / 2f

        // Glow burst
        val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(
                cx, cy, sizePx * 0.6f,
                ColorUtils.setAlphaComponent(glowColor, 0x40),
                Color.TRANSPARENT,
                Shader.TileMode.CLAMP,
            )
        }
        canvas.drawCircle(cx, cy, sizePx * 0.6f, glowPaint)

        // Heart shape using two circles + triangle
        val heartPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = heartColor }
        val heartSize = sizePx * 0.22f
        val heartPath = Path().apply {
            // Start at bottom tip
            moveTo(cx, cy + heartSize * 1.1f)
            // Left curve
            cubicTo(
                cx - heartSize * 1.4f, cy + heartSize * 0.2f,
                cx - heartSize * 1.4f, cy - heartSize * 0.6f,
                cx, cy - heartSize * 0.3f,
            )
            // Right curve
            cubicTo(
                cx + heartSize * 1.4f, cy - heartSize * 0.6f,
                cx + heartSize * 1.4f, cy + heartSize * 0.2f,
                cx, cy + heartSize * 1.1f,
            )
            close()
        }
        canvas.drawPath(heartPath, heartPaint)

        return output
    }

    /**
     * Waveform bitmap for the recognizer listening state.
     * Draws animated audio waveform bars in the accent color.
     */
    fun waveformBitmap(
        sizePx: Int,
        @ColorInt color: Int,
        frame: Int = 0,
        amplitude: Float = 0.6f,
    ): Bitmap {
        val output = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val cx = sizePx / 2f
        val cy = sizePx / 2f
        val barCount = 5
        val barWidth = sizePx * 0.08f
        val spacing = sizePx * 0.12f
        val totalWidth = barCount * barWidth + (barCount - 1) * spacing
        val startX = cx - totalWidth / 2f

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            style = Paint.Style.FILL
            strokeCap = Paint.Cap.ROUND
        }

        for (i in 0 until barCount) {
            // Animate each bar with a sine wave offset
            val phase = (frame * 0.3f + i * 0.8f) % (2f * PI.toFloat())
            val barHeight = (sizePx * 0.15f + sin(phase.toDouble()).toFloat() * sizePx * 0.25f * amplitude)
                .coerceAtLeast(sizePx * 0.05f)
            val x = startX + i * (barWidth + spacing)
            val y1 = cy - barHeight / 2f
            val y2 = cy + barHeight / 2f
            canvas.drawRoundRect(
                RectF(x, y1, x + barWidth, y2),
                barWidth / 2f, barWidth / 2f,
                paint,
            )
        }
        return output
    }

    fun formatTime(ms: Long): String {
        if (ms <= 0) return "0:00"
        val totalSeconds = ms / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return if (hours > 0) {
            String.format("%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format("%d:%02d", minutes, seconds)
        }
    }

    // ─── Internals ────────────────────────────────────────────────────────────

    private fun isNightMode(context: Context): Boolean =
        (context.resources.configuration.uiMode and
            android.content.res.Configuration.UI_MODE_NIGHT_MASK) ==
            android.content.res.Configuration.UI_MODE_NIGHT_YES

    private fun cropSquare(bitmap: Bitmap): Bitmap {
        val size = min(bitmap.width, bitmap.height)
        if (bitmap.width == bitmap.height) return bitmap
        return Bitmap.createBitmap(
            bitmap,
            (bitmap.width - size) / 2,
            (bitmap.height - size) / 2,
            size, size,
        )
    }

    @ColorInt
    private fun mix(@ColorInt a: Int, @ColorInt b: Int, ratio: Float): Int =
        ColorUtils.blendARGB(a, b, ratio)

    @ColorInt
    private fun darken(@ColorInt color: Int, amount: Float): Int =
        ColorUtils.blendARGB(color, Color.BLACK, amount)

    @ColorInt
    private fun brighten(@ColorInt color: Int, amount: Float): Int =
        ColorUtils.blendARGB(color, Color.WHITE, amount)

    /** White or black, whichever contrasts more against [bg]. */
    @ColorInt
    private fun bestOn(@ColorInt bg: Int): Int =
        if (ColorUtils.calculateContrast(Color.WHITE, ColorUtils.setAlphaComponent(bg, 0xFF)) >=
            ColorUtils.calculateContrast(Color.BLACK, ColorUtils.setAlphaComponent(bg, 0xFF))
        ) Color.WHITE else Color.BLACK

    /**
     * Nudge [fg] toward white or black (whichever direction helps) until it
     * reaches [minContrast] against [bg]. Guarantees WCAG AA when possible.
     */
    @ColorInt
    private fun ensureContrast(@ColorInt fg: Int, @ColorInt bg: Int, minContrast: Double): Int {
        val opaqueBg = ColorUtils.setAlphaComponent(bg, 0xFF)
        var color = ColorUtils.setAlphaComponent(fg, 0xFF)
        if (ColorUtils.calculateContrast(color, opaqueBg) >= minContrast) return color
        val target = bestOn(opaqueBg)
        var ratio = 0f
        while (ratio < 1f) {
            ratio += 0.1f
            color = ColorUtils.blendARGB(fg, target, ratio)
            if (ColorUtils.calculateContrast(color, opaqueBg) >= minContrast) return color
        }
        return target
    }

    /**
     * Fast blur: downscale to ~[targetWidth]px, run an integer stack blur with
     * [radius], return the small bitmap (caller scales it up, which adds its own
     * softening). Costs <1ms - safe for widget refresh cadence.
     */
    private fun stackBlurDownscaled(source: Bitmap, targetWidth: Int, radius: Int): Bitmap {
        val scale = targetWidth.toFloat() / source.width
        val small = Bitmap.createScaledBitmap(
            source,
            max(8, (source.width * scale).toInt()),
            max(8, (source.height * scale).toInt()),
            true,
        )
        val blurred = stackBlur(small, radius)
        if (blurred !== small) small.recycle()
        return blurred
    }

    /** Classic stack blur (Mario Klingemann) on a mutable copy. */
    private fun stackBlur(sentBitmap: Bitmap, radius: Int): Bitmap {
        if (radius < 1) return sentBitmap
        val bitmap = sentBitmap.copy(Bitmap.Config.ARGB_8888, true)
        val w = bitmap.width
        val h = bitmap.height
        val pix = IntArray(w * h)
        bitmap.getPixels(pix, 0, w, 0, 0, w, h)

        val wm = w - 1
        val hm = h - 1
        val div = radius + radius + 1
        val r = IntArray(w * h)
        val g = IntArray(w * h)
        val b = IntArray(w * h)
        val vmin = IntArray(max(w, h))
        var divsum = (div + 1) shr 1
        divsum *= divsum
        val dv = IntArray(256 * divsum)
        for (i in 0 until 256 * divsum) dv[i] = i / divsum

        var yw = 0
        var yi = 0
        val stack = Array(div) { IntArray(3) }
        val r1 = radius + 1

        for (y in 0 until h) {
            var rinsum = 0; var ginsum = 0; var binsum = 0
            var routsum = 0; var goutsum = 0; var boutsum = 0
            var rsum = 0; var gsum = 0; var bsum = 0
            for (i in -radius..radius) {
                val p = pix[yi + min(wm, max(i, 0))]
                val sir = stack[i + radius]
                sir[0] = p and 0xFF0000 shr 16
                sir[1] = p and 0x00FF00 shr 8
                sir[2] = p and 0x0000FF
                val rbs = r1 - abs(i)
                rsum += sir[0] * rbs; gsum += sir[1] * rbs; bsum += sir[2] * rbs
                if (i > 0) { rinsum += sir[0]; ginsum += sir[1]; binsum += sir[2] }
                else { routsum += sir[0]; goutsum += sir[1]; boutsum += sir[2] }
            }
            var stackpointer = radius
            for (x in 0 until w) {
                r[yi] = dv[rsum]; g[yi] = dv[gsum]; b[yi] = dv[bsum]
                rsum -= routsum; gsum -= goutsum; bsum -= boutsum
                val stackstart = stackpointer - radius + div
                var sir = stack[stackstart % div]
                routsum -= sir[0]; goutsum -= sir[1]; boutsum -= sir[2]
                if (y == 0) vmin[x] = min(x + radius + 1, wm)
                val p = pix[yw + vmin[x]]
                sir[0] = p and 0xFF0000 shr 16
                sir[1] = p and 0x00FF00 shr 8
                sir[2] = p and 0x0000FF
                rinsum += sir[0]; ginsum += sir[1]; binsum += sir[2]
                rsum += rinsum; gsum += ginsum; bsum += binsum
                stackpointer = (stackpointer + 1) % div
                sir = stack[stackpointer % div]
                routsum += sir[0]; goutsum += sir[1]; boutsum += sir[2]
                rinsum -= sir[0]; ginsum -= sir[1]; binsum -= sir[2]
                yi++
            }
            yw += w
        }

        var x = 0
        while (x < w) {
            var rinsum = 0; var ginsum = 0; var binsum = 0
            var routsum = 0; var goutsum = 0; var boutsum = 0
            var rsum = 0; var gsum = 0; var bsum = 0
            var yp = -radius * w
            for (i in -radius..radius) {
                yi = max(0, yp) + x
                val sir = stack[i + radius]
                sir[0] = r[yi]; sir[1] = g[yi]; sir[2] = b[yi]
                val rbs = r1 - abs(i)
                rsum += r[yi] * rbs; gsum += g[yi] * rbs; bsum += b[yi] * rbs
                if (i > 0) { rinsum += sir[0]; ginsum += sir[1]; binsum += sir[2] }
                else { routsum += sir[0]; goutsum += sir[1]; boutsum += sir[2] }
                if (i < hm) yp += w
            }
            yi = x
            var stackpointer = radius
            for (y in 0 until h) {
                pix[yi] = -0x1000000 and pix[yi] or (dv[rsum] shl 16) or (dv[gsum] shl 8) or dv[bsum]
                rsum -= routsum; gsum -= goutsum; bsum -= boutsum
                val stackstart = stackpointer - radius + div
                var sir = stack[stackstart % div]
                routsum -= sir[0]; goutsum -= sir[1]; boutsum -= sir[2]
                if (x == 0) vmin[y] = min(y + r1, hm) * w
                val p = x + vmin[y]
                sir[0] = r[p]; sir[1] = g[p]; sir[2] = b[p]
                rinsum += sir[0]; ginsum += sir[1]; binsum += sir[2]
                rsum += rinsum; gsum += ginsum; bsum += binsum
                stackpointer = (stackpointer + 1) % div
                sir = stack[stackpointer]
                routsum += sir[0]; goutsum += sir[1]; boutsum += sir[2]
                rinsum -= sir[0]; ginsum -= sir[1]; binsum -= sir[2]
                yi += w
            }
            x++
        }
        bitmap.setPixels(pix, 0, w, 0, 0, w, h)
        return bitmap
    }
}