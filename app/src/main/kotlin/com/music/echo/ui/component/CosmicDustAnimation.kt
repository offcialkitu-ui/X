package iad1tya.echo.music.ui.component

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import kotlin.random.Random

@Composable
fun CosmicDustAnimation(
    modifier: Modifier = Modifier,
    particleCount: Int = 100,
    speed: Int = 15000
) {
    val infiniteTransition = rememberInfiniteTransition(label = "cosmic_dust")
    
    // Global animation progress (0 to 1)
    val progress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(speed, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "progress"
    )

    // Initial particle data
    val particles = remember {
        List(particleCount) {
            ParticleData(
                initialX = Random.nextFloat() * 2 - 1f, // -1 to 1 range
                initialY = Random.nextFloat() * 2 - 1f, // -1 to 1 range
                initialZ = Random.nextFloat(),          // 0 to 1 range
                size = Random.nextFloat() * 2f + 1f      // 1 to 3px
            )
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            val centerX = width / 2f
            val centerY = height / 2f

            particles.forEach { particle ->
                // Calculate current Z by subtracting progress from initialZ
                // Result wrap around 0..1
                var currentZ = particle.initialZ - progress
                if (currentZ < 0f) currentZ += 1f
                
                // Perspective projection factor
                // factor increases as currentZ decreases (closer to 0 is closer to camera)
                val factor = 1f / (currentZ + 0.05f)
                
                // Project 3D coordinates to 2D screen coordinates
                val x2d = centerX + (particle.initialX * centerX * factor)
                val y2d = centerY + (particle.initialY * centerY * factor)
                
                // Only draw if within screen bounds (with some margin)
                if (x2d in -50f..width + 50f && y2d in -50f..height + 50f) {
                    
                    // Alpha: Fade in from distance (currentZ ~ 1) and fade out when very close (currentZ ~ 0)
                    val alpha = when {
                        currentZ > 0.8f -> (1f - currentZ) / 0.2f
                        currentZ < 0.2f -> currentZ / 0.2f
                        else -> 1f
                    }
                    
                    // Size: Increases as it gets closer
                    val currentSize = particle.size * (factor * 0.15f).coerceIn(0.5f, 5f)
                    
                    drawCircle(
                        color = Color.White.copy(alpha = alpha * 0.7f),
                        radius = currentSize,
                        center = Offset(x2d, y2d)
                    )
                }
            }
        }
    }
}

private data class ParticleData(
    val initialX: Float,
    val initialY: Float,
    val initialZ: Float,
    val size: Float
)
