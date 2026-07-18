package iad1tya.echo.music.ui.screens.storyshare

import android.graphics.Bitmap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.ColorUtils
import androidx.core.graphics.scale
import androidx.palette.graphics.Palette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

data class CardPalette(
    val vibrant: Color,
    val muted: Color,
    val darkMuted: Color,
    val textAccent: Color,   // HSL-remapped, contrast-guarded for dark backgrounds
    val posterBg: Color,     // luminance-clamped 20–45% (Vinyl Classic)
)

object PaletteEngine {

    private val FALLBACK = CardPalette(               // brand set: #7C4DFF on #0E0E12
        vibrant = Color(0xFF7C4DFF), muted = Color(0xFF4A3A78),
        darkMuted = Color(0xFF1A1430), textAccent = Color(0xFFB39DFF),
        posterBg = Color(0xFF2A2140),
    )
    private val cache = ConcurrentHashMap<String, CardPalette>()

    suspend fun extract(songId: String, art: Bitmap?): CardPalette {
        cache[songId]?.let { return it }
        if (art == null) return FALLBACK
        return withContext(Dispatchers.Default) {
            // 112px downsample: same palette quality, ~10x faster extraction
            val small = art.scale(112, 112)
            val p = Palette.from(small).clearFilters().generate()

            CardPalette(
                vibrant = Color(p.getVibrantColor(FALLBACK.vibrant.toArgb())),
                muted = Color(p.getMutedColor(FALLBACK.muted.toArgb())),
                darkMuted = Color(p.getDarkMutedColor(FALLBACK.darkMuted.toArgb())),
                textAccent = remapForDarkBg(p.getVibrantColor(FALLBACK.vibrant.toArgb())),
                posterBg = clampLuminance(p.getMutedColor(FALLBACK.muted.toArgb()), 0.20f, 0.45f),
            ).also { cache[songId] = it }
        }
    }

    /**
     * Spec rule: keep hue, force S∈[0.55,0.85], L∈[0.65,0.80], then walk L up in
     * 0.05 steps until the color passes WCAG AA (4.5:1) against near-black.
     * Guarantees "neon that you can actually read".
     */
    private fun remapForDarkBg(argb: Int): Color {
        val hsl = FloatArray(3)
        ColorUtils.colorToHSL(argb, hsl)
        hsl[1] = hsl[1].coerceIn(0.55f, 0.85f)
        hsl[2] = hsl[2].coerceIn(0.65f, 0.80f)
        var c = ColorUtils.HSLToColor(hsl)
        while (ColorUtils.calculateContrast(c, 0xFF0A0A0A.toInt()) < 4.5 && hsl[2] < 0.95f) {
            hsl[2] += 0.05f
            c = ColorUtils.HSLToColor(hsl)
        }
        return Color(c)
    }

    private fun clampLuminance(argb: Int, min: Float, max: Float): Color {
        val hsl = FloatArray(3)
        ColorUtils.colorToHSL(argb, hsl)
        hsl[2] = hsl[2].coerceIn(min, max)
        return Color(ColorUtils.HSLToColor(hsl))
    }

    fun hexOf(color: Color): String = String.format("#%06X", 0xFFFFFF and color.toArgb())
}
