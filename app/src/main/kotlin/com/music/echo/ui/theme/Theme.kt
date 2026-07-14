
package iad1tya.echo.music.ui.theme

import android.graphics.Bitmap
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.SaverScope
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.palette.graphics.Palette
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamiccolor.ColorSpec
import com.materialkolor.rememberDynamicColorScheme
import com.materialkolor.score.Score
import java.util.Calendar

val DefaultThemeColor = Color(0xFFED5564)

val LocalPurpleTheme = staticCompositionLocalOf { false }
val LocalGlassmorphismMode = staticCompositionLocalOf { false }

@Composable
fun echomusicTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    pureBlack: Boolean = false,
    purpleTheme: Boolean = false,
    glassmorphismMode: Boolean = false,
    solarDynamicMode: Boolean = false,
    batteryProMode: Boolean = false,
    themeColor: Color = DefaultThemeColor,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    
    val solarColor = remember(solarDynamicMode) {
        if (!solarDynamicMode) null
        else {
            val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
            when (hour) {
                in 6..10 -> Color(0xFFFFD700) // Sunrise Gold
                in 11..15 -> Color(0xFF00BFFF) // Noon Blue
                in 16..19 -> Color(0xFFFF4500) // Sunset Orange
                else -> Color(0xFF191970) // Night Midnight Blue
            }
        }
    }

    val effectiveThemeColor = when {
        batteryProMode -> Color.White
        solarDynamicMode -> solarColor ?: themeColor
        purpleTheme -> Color(0xFF8B5CF6)
        else -> themeColor
    }

    val isActuallyDark = darkTheme || purpleTheme || glassmorphismMode || batteryProMode || (solarDynamicMode && (solarColor == Color(0xFF191970)))

    val useSystemDynamicColor = (!purpleTheme && !glassmorphismMode && !batteryProMode && !solarDynamicMode && themeColor == DefaultThemeColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)

    
    val baseColorScheme = if (useSystemDynamicColor) {
        
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else {
        
        rememberDynamicColorScheme(
            seedColor = effectiveThemeColor, 
            isDark = isActuallyDark,
            specVersion = ColorSpec.SpecVersion.SPEC_2025,
            style = if (purpleTheme || glassmorphismMode) PaletteStyle.Vibrant else PaletteStyle.TonalSpot 
        )
    }

    
    val colorScheme = remember(baseColorScheme, pureBlack, isActuallyDark, purpleTheme, glassmorphismMode, batteryProMode) {
        when {
            batteryProMode -> baseColorScheme.copy(
                primary = Color.White,
                surface = Color.Black,
                background = Color.Black,
                surfaceContainer = Color.Black,
                surfaceContainerHigh = Color.Black,
                surfaceContainerLow = Color.Black,
                outline = Color.DarkGray
            )
            glassmorphismMode -> baseColorScheme.copy(
                surface = Color.Black.copy(alpha = 0.4f),
                background = Color.Black,
                surfaceContainer = Color.White.copy(alpha = 0.05f),
                surfaceContainerHigh = Color.White.copy(alpha = 0.1f),
                surfaceContainerLow = Color.White.copy(alpha = 0.02f),
                outline = Color.White.copy(alpha = 0.1f)
            )
            purpleTheme -> {
                baseColorScheme.copy(
                    primary = Color(0xFF8B5CF6),
                    onPrimary = Color.White,
                    primaryContainer = Color(0xFF8B5CF6).copy(alpha = 0.2f),
                    onPrimaryContainer = Color(0xFF8B5CF6),
                    secondary = Color(0xFF8B5CF6).copy(alpha = 0.8f),
                    onSecondary = Color.White,
                    secondaryContainer = Color(0xFF8B5CF6).copy(alpha = 0.15f),
                    onSecondaryContainer = Color(0xFF8B5CF6),
                    surface = Color(0xFF0A0A0A),
                    onSurface = Color.White,
                    background = Color(0xFF0A0A0A),
                    onBackground = Color.White,
                    surfaceContainer = Color(0xFF0F0F0F),
                    surfaceContainerHigh = Color(0xFF161616),
                    surfaceContainerLow = Color(0xFF080808),
                    surfaceContainerLowest = Color(0xFF050505),
                    surfaceContainerHighest = Color(0xFF1A1A1A),
                    outline = Color(0xFF8B5CF6).copy(alpha = 0.4f),
                    outlineVariant = Color(0xFF8B5CF6).copy(alpha = 0.2f),
                )
            }
            isActuallyDark && pureBlack -> {
                baseColorScheme.pureBlack(true)
            }
            else -> baseColorScheme
        }
    }

    CompositionLocalProvider(
        LocalPurpleTheme provides purpleTheme,
        LocalGlassmorphismMode provides glassmorphismMode
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = AppTypography, 
            content = content
        )
    }
}

fun Bitmap.extractThemeColor(): Color {
    val colorsToPopulation = Palette.from(this)
        .maximumColorCount(8)
        .generate()
        .swatches
        .associate { it.rgb to it.population }
    val rankedColors = Score.score(colorsToPopulation)
    return Color(rankedColors.first())
}

fun Bitmap.extractGradientColors(): List<Color> {
    val extractedColors = Palette.from(this)
        .maximumColorCount(64)
        .generate()
        .swatches
        .associate { it.rgb to it.population }

    val orderedColors = Score.score(extractedColors, 2, 0xff4285f4.toInt(), true)
        .sortedByDescending { Color(it).luminance() }

    return if (orderedColors.size >= 2)
        listOf(Color(orderedColors[0]), Color(orderedColors[1]))
    else
        listOf(Color(0xFF595959), Color(0xFF0D0D0D))
}

fun ColorScheme.pureBlack(apply: Boolean) =
    if (apply) copy(
        surface = Color.Black,
        background = Color.Black
    ) else this

val ColorSaver = object : Saver<Color, Int> {
    override fun restore(value: Int): Color = Color(value)
    override fun SaverScope.save(value: Color): Int = value.toArgb()
}
