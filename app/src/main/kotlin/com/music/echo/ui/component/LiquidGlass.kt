
package iad1tya.echo.music.ui.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.unit.dp
import iad1tya.echo.music.constants.*
import iad1tya.echo.music.utils.rememberPreference

@Composable
fun LiquidGlassBackground(
    dominantColor: Color,
    modifier: Modifier = Modifier
) {
    val (enabled) = rememberPreference(LiquidGlassEnabledKey, defaultValue = false)
    if (!enabled) return

    val (vibrancy) = rememberPreference(LiquidGlassVibrancyKey, defaultValue = 1.0f)
    val (blurRadius) = rememberPreference(LiquidGlassBlurRadiusKey, defaultValue = 25f)
    val (surfaceOpacity) = rememberPreference(LiquidGlassSurfaceOpacityKey, defaultValue = 0.1f)
    val (chromaticAberration) = rememberPreference(LiquidGlassChromaticAberrationKey, defaultValue = true)
    val (depthEffect) = rememberPreference(LiquidGlassDepthEffectKey, defaultValue = true)
    val (surfaceTint) = rememberPreference(LiquidGlassSurfaceTintKey, defaultValue = 0)
    
    val tintColor = if (surfaceTint != 0) Color(surfaceTint) else dominantColor

    Box(modifier = modifier.fillMaxSize()) {
        // 1. Base Blurred Layer
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            tintColor.copy(alpha = (surfaceOpacity * vibrancy).coerceIn(0f, 1f)),
                            Color.Black.copy(alpha = 0.6f)
                        )
                    )
                )
                .blur(blurRadius.dp)
        )

        // 2. Refraction & Chromatic Aberration Simulation
        if (chromaticAberration || depthEffect) {
            Canvas(modifier = Modifier.fillMaxSize().alpha(0.15f)) {
                if (depthEffect) {
                    drawRect(
                        brush = Brush.radialGradient(
                            colors = listOf(Color.White.copy(alpha = 0.1f), Color.Transparent),
                            center = Offset(size.width * 0.2f, size.height * 0.2f),
                            radius = maxOf(size.width, size.height)
                        )
                    )
                }
                
                // For a real chromatic aberration we'd need more complex shader logic
                // But we can tint the edges
                drawRect(
                    brush = Brush.verticalGradient(
                        0.0f to Color.Red.copy(alpha = 0.05f),
                        0.5f to Color.Transparent,
                        1.0f to Color.Blue.copy(alpha = 0.05f)
                    ),
                    blendMode = BlendMode.Screen
                )
            }
        }
        
        // 3. Noise/Grain texture for "Glass" feel
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White.copy(alpha = 0.02f))
        )
    }
}

/**
 * Adaptive text color helper for Glass surfaces
 */
@Composable
fun getAdaptiveGlassTextColor(
    isDark: Boolean,
    dominantColor: Color
): Color {
    val (enabled) = rememberPreference(LiquidGlassEnabledKey, defaultValue = false)
    if (!enabled) return if (isDark) Color.White else Color.Black
    
    val (customColor) = rememberPreference(LiquidGlassTextColorKey, defaultValue = -1)
    if (customColor != -1) return Color(customColor)

    // Calculate contrast and return best legible color
    return if (dominantColor.luminance() > 0.5f) Color.Black else Color.White
}

@Composable
fun Modifier.liquidGlassSurface(
    enabled: Boolean = true,
    opacity: Float = 0.1f,
    blur: Float = 20f
): Modifier {
    if (!enabled) return this
    
    return this
        .background(Color.White.copy(alpha = opacity), RoundedCornerShape(16.dp))
        .blur(blur.dp)
}
