package iad1tya.echo.music.ui.player.lockscreen

import android.content.Context
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.ColorUtils
import androidx.compose.ui.unit.dp
import iad1tya.echo.music.extensions.*
import iad1tya.echo.music.constants.*
import iad1tya.echo.music.utils.dataStore
import iad1tya.echo.music.utils.get

/**
 * LockScreenThemeEngine – Manages all lock screen theme state
 *
 * Supports:
 * - Dynamic (album art based)
 * - Material You (system accent)
 * - Custom Gradient
 * - Solid Color
 * - Pure Black
 * - Glassmorphism
 * - Solar Dynamic (changes based on time of day)
 */
data class LockScreenThemeConfig(
    val themeMode: LockScreenThemeMode = LockScreenThemeMode.DYNAMIC,
    val primaryColor: Color = Color(0xFF8B5CF6),
    val secondaryColor: Color = Color(0xFF3B82F6),
    val gradientStart: Color = Color(0xFF8B5CF6),
    val gradientEnd: Color = Color(0xFF3B82F6),
    val blurIntensity: Float = 100f,
    val artworkOpacity: Float = 0.45f,
)

class LockScreenThemeEngine(private val context: Context) {

    /**
     * Load saved theme config from DataStore
     */
    fun loadConfig(): LockScreenThemeConfig {
        val ds = context.dataStore
        return LockScreenThemeConfig(
            themeMode = ds.get(LockScreenThemeModeKey)?.let { name ->
                try { LockScreenThemeMode.valueOf(name) } catch (_: Exception) { LockScreenThemeMode.DYNAMIC }
            } ?: LockScreenThemeMode.DYNAMIC,
            primaryColor = Color(ds.get(LockScreenCustomPrimaryColorKey, 0xFF8B5CF6.toInt())),
            secondaryColor = Color(ds.get(LockScreenCustomSecondaryColorKey, 0xFF3B82F6.toInt())),
            gradientStart = Color(ds.get(LockScreenGradientStartKey, 0xFF8B5CF6.toInt())),
            gradientEnd = Color(ds.get(LockScreenGradientEndKey, 0xFF3B82F6.toInt())),
            blurIntensity = ds.get(LockScreenBlurIntensityKey, 100f),
            artworkOpacity = ds.get(LockScreenArtworkOpacityKey, 0.45f),
        )
    }

    /**
     * Resolve the effective colors for the lock screen based on current config
     * and the dominant color extracted from album art.
     */
    fun resolveColors(
        config: LockScreenThemeConfig,
        albumArtDominantColor: Color,
        isNightMode: Boolean,
    ): ResolvedThemeColors {
        return when (config.themeMode) {
            LockScreenThemeMode.DYNAMIC -> resolveDynamic(albumArtDominantColor, config)
            LockScreenThemeMode.MATERIAL_YOU -> resolveMaterialYou(isNightMode)
            LockScreenThemeMode.GRADIENT -> resolveGradient(config)
            LockScreenThemeMode.SOLID_COLOR -> resolveSolidColor(config)
            LockScreenThemeMode.PURE_BLACK -> resolvePureBlack()
            LockScreenThemeMode.GLASSMORPHISM -> resolveGlassmorphism(albumArtDominantColor, config)
            LockScreenThemeMode.SOLAR_DYNAMIC -> resolveSolarDynamic()
        }
    }

    private fun resolveDynamic(dominant: Color, config: LockScreenThemeConfig): ResolvedThemeColors {
        val hue = dominant.toArgb().let { Color(it).hue }
        return ResolvedThemeColors(
            backgroundColor = Color.Black,
            surfaceColor = dominant.copy(alpha = 0.15f),
            accentColor = dominant,
            secondaryAccent = Color.hsl((hue + 40f) % 360f, 0.7f, 0.6f),
            textPrimary = Color.White,
            textSecondary = Color.White.copy(alpha = 0.7f),
            artworkOpacity = config.artworkOpacity,
            blurRadius = config.blurIntensity.dp,
            isDark = true,
        )
    }

    private fun resolveMaterialYou(isNight: Boolean): ResolvedThemeColors {
        // Fall back to purple-blue since Material You dynamic colors
        // are handled at the system level via the theme
        return ResolvedThemeColors(
            backgroundColor = if (isNight) Color(0xFF1C1B1F) else Color(0xFFFFFBFE),
            surfaceColor = if (isNight) Color(0xFF2B2930) else Color(0xFFF3EDF7),
            accentColor = if (isNight) Color(0xFFD0BCFF) else Color(0xFF6750A4),
            secondaryAccent = if (isNight) Color(0xFFCCC2DC) else Color(0xFF625B71),
            textPrimary = if (isNight) Color.White else Color(0xFF1C1B1F),
            textSecondary = if (isNight) Color.White.copy(alpha = 0.7f) else Color(0xFF1C1B1F).copy(alpha = 0.7f),
            artworkOpacity = 0.6f,
            blurRadius = 80.dp,
            isDark = isNight,
        )
    }

    private fun resolveGradient(config: LockScreenThemeConfig): ResolvedThemeColors {
        return ResolvedThemeColors(
            backgroundColor = Color.Black,
            surfaceColor = config.gradientStart.copy(alpha = 0.2f),
            accentColor = config.gradientStart,
            secondaryAccent = config.gradientEnd,
            textPrimary = Color.White,
            textSecondary = Color.White.copy(alpha = 0.7f),
            gradientStart = config.gradientStart,
            gradientEnd = config.gradientEnd,
            artworkOpacity = 0.3f,
            blurRadius = 60.dp,
            isDark = true,
        )
    }

    private fun resolveSolidColor(config: LockScreenThemeConfig): ResolvedThemeColors {
        val bgColor = config.primaryColor
        val isDark = ColorUtils.calculateLuminance(bgColor.toArgb()) < 0.5
        return ResolvedThemeColors(
            backgroundColor = bgColor,
            surfaceColor = bgColor.copy(alpha = 0.3f),
            accentColor = if (isDark) bgColor.copy(alpha = 0.5f) else bgColor,
            secondaryAccent = config.secondaryColor,
            textPrimary = if (isDark) Color.White else Color.Black,
            textSecondary = if (isDark) Color.White.copy(alpha = 0.7f) else Color.Black.copy(alpha = 0.7f),
            artworkOpacity = 0.2f,
            blurRadius = 40.dp,
            isDark = isDark,
        )
    }

    private fun resolvePureBlack(): ResolvedThemeColors {
        return ResolvedThemeColors(
            backgroundColor = Color.Black,
            surfaceColor = Color(0xFF121212),
            accentColor = Color(0xFFBB86FC),
            secondaryAccent = Color(0xFF03DAC6),
            textPrimary = Color.White,
            textSecondary = Color.White.copy(alpha = 0.6f),
            artworkOpacity = 0.15f,
            blurRadius = 30.dp,
            isDark = true,
        )
    }

    private fun resolveGlassmorphism(dominant: Color, config: LockScreenThemeConfig): ResolvedThemeColors {
        val glassBg = dominant.copy(alpha = 0.08f)
        return ResolvedThemeColors(
            backgroundColor = Color.Black.copy(alpha = 0.85f),
            surfaceColor = glassBg,
            accentColor = dominant,
            secondaryAccent = dominant.copy(alpha = 0.6f),
            textPrimary = Color.White.copy(alpha = 0.95f),
            textSecondary = Color.White.copy(alpha = 0.6f),
            glassBlur = 40f,
            glassOpacity = 0.12f,
            artworkOpacity = config.artworkOpacity,
            blurRadius = config.blurIntensity.dp,
            isDark = true,
        )
    }

    private fun resolveSolarDynamic(): ResolvedThemeColors {
        val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        val isNight = hour < 6 || hour >= 18
        val warmFactor = when {
            hour in 6..8 -> 0.8f   // Sunrise
            hour in 17..19 -> 0.6f  // Sunset
            hour in 9..16 -> 0.2f   // Day
            else -> 1.0f            // Night
        }

        val accentColor = Color.hsl(
            hue = if (isNight) 260f else if (warmFactor > 0.5f) 30f else 220f,
            saturation = 0.7f,
            lightness = 0.5f + warmFactor * 0.2f,
        )

        return ResolvedThemeColors(
            backgroundColor = if (isNight) Color(0xFF0D0D1A) else Color(0xFFFFF8F0),
            surfaceColor = accentColor.copy(alpha = if (isNight) 0.15f else 0.08f),
            accentColor = accentColor,
            secondaryAccent = Color.hsl((accentColor.hue + 30f) % 360f, 0.6f, 0.5f),
            textPrimary = if (isNight) Color.White else Color(0xFF1C1B1F),
            textSecondary = if (isNight) Color.White.copy(alpha = 0.7f) else Color(0xFF1C1B1F).copy(alpha = 0.7f),
            artworkOpacity = if (isNight) 0.5f else 0.3f,
            blurRadius = if (isNight) 100.dp else 60.dp,
            isDark = isNight,
        )
    }
}

data class ResolvedThemeColors(
    val backgroundColor: Color,
    val surfaceColor: Color,
    val accentColor: Color,
    val secondaryAccent: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val gradientStart: Color = Color.Transparent,
    val gradientEnd: Color = Color.Transparent,
    val glassBlur: Float = 0f,
    val glassOpacity: Float = 0f,
    val artworkOpacity: Float = 0.45f,
    val blurRadius: androidx.compose.ui.unit.Dp = 100.dp,
    val isDark: Boolean = true,
) {
    val materialColorScheme by lazy {
        if (isDark) {
            darkColorScheme(
                primary = accentColor,
                secondary = secondaryAccent,
                background = backgroundColor,
                surface = surfaceColor,
                onPrimary = textPrimary,
                onSecondary = textSecondary,
                onBackground = textPrimary,
                onSurface = textPrimary,
            )
        } else {
            lightColorScheme(
                primary = accentColor,
                secondary = secondaryAccent,
                background = backgroundColor,
                surface = surfaceColor,
                onPrimary = textPrimary,
                onSecondary = textSecondary,
                onBackground = textPrimary,
                onSurface = textPrimary,
            )
        }
    }
}

// Extension to create a Dp value from Float
private val Float.dp: androidx.compose.ui.unit.Dp
    get() = androidx.compose.ui.unit.Dp(this)