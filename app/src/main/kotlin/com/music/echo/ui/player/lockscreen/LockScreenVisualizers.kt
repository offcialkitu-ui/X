package iad1tya.echo.music.ui.player.lockscreen

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.draw.alpha
import iad1tya.echo.music.extensions.*
import iad1tya.echo.music.constants.LockScreenVisualizerType
import iad1tya.echo.music.utils.SongMood
import kotlin.math.*
import kotlin.random.Random

/**
 * Master visualizer dispatcher – renders the selected visualizer type.
 */
@Composable
fun LockScreenVisualizer(
    visualizerType: LockScreenVisualizerType,
    dominantColor: Color,
    isPlaying: Boolean,
    mood: SongMood,
    intensity: Float = 1f,
    speed: Float = 1f,
    modifier: Modifier = Modifier,
) {
    val effectiveType = if (visualizerType == LockScreenVisualizerType.MOOD_AUTO) {
        when (mood) {
            SongMood.PHONK -> LockScreenVisualizerType.AURORA_NEBULA
            SongMood.LOFI -> LockScreenVisualizerType.AURORA_NEBULA
            SongMood.POP -> LockScreenVisualizerType.CIRCULAR_WAVE
            SongMood.DEFAULT -> LockScreenVisualizerType.AURORA_NEBULA
        }
    } else {
        visualizerType
    }

    if (!isPlaying && effectiveType !in listOf(
            LockScreenVisualizerType.EDGE_LIGHTING,
            LockScreenVisualizerType.AURORA_NEBULA,
        )) return

    Box(modifier = modifier) {
        when (effectiveType) {
            LockScreenVisualizerType.CIRCULAR_WAVE -> CircularWaveVisualizer(dominantColor, isPlaying, intensity, speed)
            LockScreenVisualizerType.AURORA_NEBULA -> AuroraNebulaVisualizer(dominantColor, isPlaying, intensity, speed)
            LockScreenVisualizerType.EDGE_LIGHTING -> EdgeLightingVisualizer(dominantColor, isPlaying, intensity, speed)
            LockScreenVisualizerType.FIRE_PARTICLES -> FireParticlesVisualizer(dominantColor, isPlaying, intensity, speed)
            LockScreenVisualizerType.RAIN_DROP -> RainDropVisualizer(dominantColor, isPlaying, intensity, speed)
            LockScreenVisualizerType.TYPOGRAPHIC -> {} // Handled separately by title text
            LockScreenVisualizerType.WARP_SPEED -> WarpSpeedVisualizer(dominantColor, isPlaying, intensity, speed)
            LockScreenVisualizerType.MATRIX_RAIN -> MatrixRainVisualizer(dominantColor, isPlaying, intensity, speed)
            LockScreenVisualizerType.MOOD_AUTO -> {} // Already resolved above
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 1. CIRCULAR WAVE – Concentric ripple rings
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun CircularWaveVisualizer(
    baseColor: Color,
    isPlaying: Boolean,
    intensity: Float = 1f,
    speed: Float = 1f,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "circular_wave")
    val waveCount = 5
    val waves = (0 until waveCount).map { i ->
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(((2000 + i * 600) / speed).toInt(), easing = LinearEasing),
            ),
            label = "wave$i"
        )
    }

    Canvas(modifier = Modifier.fillMaxSize().alpha(0.25f * intensity)) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val maxRadius = size.minDimension * 0.55f

        waves.forEachIndexed { i, wave ->
            val progress = (wave.value + i * 0.2f) % 1f
            val radius = maxRadius * (0.35f + progress * 0.65f)
            val alpha = (1f - progress).pow(2) * 0.3f
            val hue = (baseColor.toArgb().let { Color(it).hue } + i * 12f) % 360f
            val color = Color.hsl(hue, 0.7f, 0.6f, alpha)

            drawCircle(
                color = color,
                radius = radius,
                center = center,
                style = Stroke(width = (1.0f - i * 0.1f).coerceAtLeast(0.3f) * intensity),
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 2. AURORA NEBULA – Flowing aurora borealis
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun AuroraNebulaVisualizer(
    baseColor: Color,
    isPlaying: Boolean,
    intensity: Float = 1f,
    speed: Float = 1f,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "aurora")
    val flowPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween((12000 / speed).toInt(), easing = LinearEasing),
        ),
        label = "flow"
    )
    val breath by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween((6000 / speed).toInt(), easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "breath"
    )

    Canvas(modifier = Modifier.fillMaxSize().alpha(0.45f * intensity)) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val radius = size.maxDimension * 0.8f

        // Multiple flowing aurora bands
        val colors = listOf(
            baseColor,
            Color.hsl((baseColor.toArgb().let { Color(it).hue } + 40f) % 360f, 0.6f, 0.5f, breath * 0.7f),
            Color.hsl((baseColor.toArgb().let { Color(it).hue } + 80f) % 360f, 0.5f, 0.4f, breath * 0.5f),
            Color.hsl((baseColor.toArgb().let { Color(it).hue } - 40f + 360f) % 360f, 0.7f, 0.6f, breath * 0.4f),
        )

        colors.forEachIndexed { i, color ->
            val angleOffset = (flowPhase + i * 90f) * (PI.toFloat() / 180f)
            val shiftX = cos(angleOffset.toDouble()).toFloat() * size.width * 0.1f
            val shiftY = sin((angleOffset * 0.7f).toDouble()).toFloat() * size.height * 0.08f

            drawCircle(
                brush = Brush.radialGradient(
                    listOf(color, Color.Transparent),
                    center = Offset(center.x + shiftX, center.y + shiftY),
                    radius = radius * (0.7f + i * 0.1f),
                ),
                radius = radius,
            )
        }

        // Secondary nebula
        val secondaryPhase = (flowPhase * 0.6f + 120f) * (PI.toFloat() / 180f)
        drawCircle(
            brush = Brush.radialGradient(
                listOf(
                    Color.hsl((baseColor.toArgb().let { Color(it).hue } + 180f) % 360f, 0.5f, 0.6f, breath * 0.3f),
                    Color.Transparent,
                ),
                center = Offset(
                    center.x + cos(secondaryPhase.toDouble()).toFloat() * size.width * 0.15f,
                    center.y + sin((secondaryPhase * 0.5f).toDouble()).toFloat() * size.height * 0.12f,
                ),
                radius = radius * 0.9f,
            ),
            radius = radius,
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 3. EDGE LIGHTING – Screen border glow
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun EdgeLightingVisualizer(
    baseColor: Color,
    isPlaying: Boolean,
    intensity: Float = 1f,
    speed: Float = 1f,
) {
    // Feature removed
}

// ─────────────────────────────────────────────────────────────────────────────
// 4. FIRE PARTICLES – Rising glowing embers
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun FireParticlesVisualizer(
    baseColor: Color,
    isPlaying: Boolean,
    intensity: Float = 1f,
    speed: Float = 1f,
) {
    val particleCount = (20 * intensity).toInt().coerceAtLeast(8)
    val particles = remember { particleCount }

    val infiniteTransition = rememberInfiniteTransition(label = "fire")
    val particleAnimations = (0 until particles).map { i ->
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(((3000 + i * 400) / speed).toInt(), easing = LinearEasing),
                initialStartOffset = StartOffset((i * 300).toInt()),
            ),
            label = "p$i"
        )
    }

    Canvas(modifier = Modifier.fillMaxSize().alpha(0.5f * intensity)) {
        val cx = size.width / 2f
        val cy = size.height

        particleAnimations.forEachIndexed { i, anim ->
            val progress = anim.value
            val xOffset = sin((progress * PI * 2f + i * 1.2f).toFloat()) * size.width * 0.3f
            val y = cy - progress * size.height * 1.1f
            val x = cx + xOffset
            val radius = (2f + (1f - progress) * 4f) * intensity
            val alpha = (1f - progress) * 0.8f
            val hue = baseColor.toArgb().let { Color(it).hue }
            val color = Color.hsl(
                hue + (1f - progress) * 30f,
                1f,
                (0.3f + progress * 0.3f).coerceAtMost(0.9f),
                alpha,
            )

            drawCircle(color = color, radius = radius, center = Offset(x, y))
            // Glow
            drawCircle(
                color = Color.hsl(hue, 1f, 0.6f, alpha * 0.2f),
                radius = radius * 3f,
                center = Offset(x, y),
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 5. RAIN DROPS – Calming rain effect
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun RainDropVisualizer(
    baseColor: Color,
    isPlaying: Boolean,
    intensity: Float = 1f,
    speed: Float = 1f,
) {
    val dropCount = (30 * intensity).toInt().coerceAtLeast(10)
    val drops = remember { dropCount }

    val infiniteTransition = rememberInfiniteTransition(label = "rain")
    val rainAnimations = (0 until drops).map { i ->
        infiniteTransition.animateFloat(
            initialValue = -0.1f,
            targetValue = 1.1f,
            animationSpec = infiniteRepeatable(
                animation = tween(((2000 + i * 300) / speed).toInt(), easing = LinearEasing),
                initialStartOffset = StartOffset((i * 200).toInt()),
            ),
            label = "r$i"
        )
    }

    Canvas(modifier = Modifier.fillMaxSize().alpha(0.3f * intensity)) {
        rainAnimations.forEachIndexed { i, anim ->
            val progress = anim.value
            val x = (i.toFloat() / drops * size.width + progress * 30f) % size.width
            val y = progress * size.height
            val length = 20.dp.toPx()
            val alpha = (0.3f - (progress - 0.5f).absoluteValue * 0.4f).coerceIn(0.1f, 0.5f)
            val color = Color.hsl(220f, 0.2f, 0.7f, alpha)

            drawLine(
                color = color,
                start = Offset(x, y),
                end = Offset(x - 4f * intensity, y + length),
                strokeWidth = 1.5f,
            )

            // Impact splash at bottom
            if (progress > 0.95f) {
                val splashAlpha = (progress - 0.95f) / 0.05f * 0.3f
                drawCircle(
                    color = Color.hsl(220f, 0.2f, 0.7f, splashAlpha),
                    radius = 8f * (1f - (progress - 0.95f) / 0.05f),
                    center = Offset(x, y + length),
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 6. WARP SPEED – Star-field tunnel effect
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun WarpSpeedVisualizer(
    baseColor: Color,
    isPlaying: Boolean,
    intensity: Float = 1f,
    speed: Float = 1f,
) {
    val starCount = (60 * intensity).toInt().coerceAtLeast(20)
    val starSeeds = remember {
        List(starCount) {
            Triple(
                Random.nextFloat(),
                Random.nextFloat(),
                Random.nextFloat() * 0.5f + 0.1f,
            )
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "warp")
    val warpPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((3000 / speed).toInt(), easing = LinearEasing),
        ),
        label = "warp"
    )

    Canvas(modifier = Modifier.fillMaxSize().alpha(0.5f * intensity)) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val maxDist = size.maxDimension * 0.7f

        starSeeds.forEachIndexed { i, (xSeed, ySeed, depth) ->
            val progress = (warpPhase + depth) % 1f
            // Stars move from center outward
            val dist = progress * maxDist
            val angle = (xSeed * 360f + progress * 200f) * (PI.toFloat() / 180f)

            val x = center.x + cos(angle.toDouble()).toFloat() * dist
            val y = center.y + sin(angle.toDouble()).toFloat() * dist

            if (dist > 10f && x in 0f..size.width && y in 0f..size.height) {
                val starSize = (1f + (1f - depth) * 4f) * intensity
                val alpha = (1f - progress) * 0.7f
                val hue = (baseColor.toArgb().let { Color(it).hue } + i * 10f) % 360f

                drawCircle(
                    color = Color.hsl(hue, 0.8f, 0.7f, alpha),
                    radius = starSize,
                    center = Offset(x, y),
                )
                // Star streak
                val streakLength = 20f * (1f - depth) * intensity
                drawLine(
                    color = Color.hsl(hue, 0.8f, 0.7f, alpha * 0.3f),
                    start = Offset(x, y),
                    end = Offset(
                        x - cos(angle.toDouble()).toFloat() * streakLength,
                        y - sin(angle.toDouble()).toFloat() * streakLength,
                    ),
                    strokeWidth = starSize * 0.5f,
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 7. MATRIX RAIN – Digital rain code effect
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun MatrixRainVisualizer(
    baseColor: Color,
    isPlaying: Boolean,
    intensity: Float = 1f,
    speed: Float = 1f,
) {
    val columnCount = (15 * intensity).toInt().coerceAtLeast(5)
    val glyphs = remember { "アイウエオカキクケコサシスセソタチツテトナニヌネノハヒフヘホマミムメモヤユヨラリルレロワヲン0123456789" }
    val columnSeeds = remember {
        List(columnCount) {
            Triple(
                Random.nextFloat(),
                Random.nextFloat() * glyphs.length,
                Random.nextFloat() * 0.5f + 0.2f,
            )
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "matrix")
    val matrixPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((4000 / speed).toInt(), easing = LinearEasing),
        ),
        label = "matrix"
    )

    Canvas(modifier = Modifier.fillMaxSize().alpha(0.3f * intensity)) {
        val fontSize = 14.dp.toPx()
        val charHeight = fontSize * 1.2f

        columnSeeds.forEachIndexed { i, (xPos, startChar, speedOffset) ->
            val x = xPos * size.width
            val offset = (matrixPhase * size.height * 2f * speedOffset + startChar * charHeight) % (size.height + charHeight)
            val columnAlpha = 0.3f + (1f - xPos) * 0.4f

            // Draw a vertical trail
            for (j in 0 until 10) {
                val y = offset - j * charHeight
                if (y in -charHeight..size.height + charHeight) {
                    val charIndex = ((startChar.toInt() + j) % glyphs.length).coerceAtLeast(0)
                    val char = glyphs[charIndex]
                    val alpha = (1f - j / 10f) * columnAlpha
                    // Map to drawScope text drawing would go here
                    // For Canvas, we draw simplified rectangles as glyph placeholders
                    val glowColor = if (j == 0) Color.White.copy(alpha = alpha * 0.9f)
                    else Color.hsl(
                        (baseColor.toArgb().let { Color(it).hue }),
                        0.8f,
                        0.3f + j * 0.04f,
                        alpha * 0.5f,
                    )
                    drawRect(
                        color = glowColor,
                        topLeft = Offset(x, y),
                        size = androidx.compose.ui.geometry.Size(fontSize * 0.6f, fontSize * 0.3f),
                    )
                }
            }
        }
    }
}