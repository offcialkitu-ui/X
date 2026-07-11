

package iad1tya.echo.music.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SliderColors
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import iad1tya.echo.music.constants.PlayerBackgroundStyle


import iad1tya.echo.music.ui.theme.LocalPurpleTheme

object PlayerSliderColors {

    
    @Composable
    fun getSliderColors(
        activeColor: Color,
        playerBackground: PlayerBackgroundStyle,
        useDarkTheme: Boolean
    ): SliderColors {
        val isPurple = LocalPurpleTheme.current
        val effectiveActiveColor = if (isPurple) Color(0xFF8B5CF6) else activeColor
        
        val inactiveTrackColor = when (playerBackground) {
            PlayerBackgroundStyle.DEFAULT -> {
                if (isPurple) {
                    Color(0xFF8B5CF6).copy(alpha = 0.25f)
                } else if (useDarkTheme) {
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                }
            }
            PlayerBackgroundStyle.BLUR, PlayerBackgroundStyle.GRADIENT, PlayerBackgroundStyle.GLOW_ANIMATED, PlayerBackgroundStyle.APPLE_MUSIC, PlayerBackgroundStyle.LIVE_MESH -> {
                if (isPurple) Color(0xFF8B5CF6).copy(alpha = 0.4f) else Color.White.copy(alpha = 0.4f)
            }
        }
        
        return SliderDefaults.colors(
            activeTrackColor = effectiveActiveColor,
            activeTickColor = effectiveActiveColor,
            thumbColor = effectiveActiveColor,
            inactiveTrackColor = inactiveTrackColor,
            disabledActiveTrackColor = effectiveActiveColor,
            disabledInactiveTrackColor = inactiveTrackColor,
            disabledThumbColor = effectiveActiveColor
        )
    }
}
