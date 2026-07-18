package iad1tya.echo.music.ui.screens.storyshare

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.random.Random

/** Single entry point — same composable powers on-screen preview AND PNG export. */
@Composable
fun StoryCard(
    data: SongShareData,
    palette: CardPalette,
    template: StoryTemplate,
    blurredArt: Bitmap?,      // pre-baked by BlurEngine (Liquid Glass only)
    modifier: Modifier = Modifier,
) = when (template) {
    StoryTemplate.LIQUID_GLASS -> LiquidGlassCard(data, palette, blurredArt, modifier)
    StoryTemplate.MOOD_PULSE -> MoodPulseCard(data, palette, modifier)
    StoryTemplate.VINYL_CLASSIC -> VinylClassicCard(data, palette, modifier)
}

// ───────────────────────── Template A: Liquid Glass ─────────────────────────

@Composable
private fun LiquidGlassCard(
    data: SongShareData, palette: CardPalette, blurredArt: Bitmap?, modifier: Modifier,
) = Box(modifier.fillMaxSize().background(palette.darkMuted)) {
    blurredArt?.let {
        Image(
            it.asImageBitmap(), null,
            Modifier.fillMaxSize(), contentScale = ContentScale.Crop, // 1.4× overscan crop
        )
    }
    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.20f))) // text-safety scrim

    // Frosted pane: 12% white fill + gradient hairline border simulating refraction
    Column(
        Modifier
            .align(Alignment.Center)
            .padding(horizontal = 36.dp)
            .clip(RoundedCornerShape(32.dp))
            .background(Color.White.copy(alpha = 0.12f))
            .border(
                1.dp,
                Brush.linearGradient(
                    listOf(Color.White.copy(alpha = 0.40f), Color.White.copy(alpha = 0.05f))
                ),
                RoundedCornerShape(32.dp),
            )
            .padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        AlbumArt(data)
        Spacer(Modifier.height(24.dp))
        SongTitle(data, Color.White, shadow = true)
        Spacer(Modifier.height(6.dp))
        Text(
            data.artist, maxLines = 1, overflow = TextOverflow.Ellipsis,
            style = TextStyle(palette.textAccent, 18.sp, FontWeight.Medium),
        )
        Spacer(Modifier.height(24.dp))
        ListenBadge(tint = Color.White.copy(alpha = 0.18f), textColor = Color.White)
    }
    MelodyXLogo(
        Modifier.align(Alignment.TopCenter).padding(top = 72.dp),
        // iridescent silver sweep
        Brush.linearGradient(listOf(Color(0xFFE8E8F0), Color(0xFFB8C4D8), Color(0xFFF0E8F4))),
    )
}

// ───────────────────────── Template B: Mood Pulse ─────────────────────────

@Composable
private fun MoodPulseCard(data: SongShareData, palette: CardPalette, modifier: Modifier) {
    val neon = palette.textAccent
    Box(
        modifier.fillMaxSize().background(
            Brush.verticalGradient(listOf(Color(0xFF0A0A0A), palette.darkMuted))
        )
    ) {
        Column(
            Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Dynamic Ring frozen at peak amplitude; seeded by songId → same song,
            // same frame, every export (deterministic = screenshot-testable)
            Box(contentAlignment = Alignment.Center) {
                MoodRing(neon, seed = data.songId.hashCode(), Modifier.size(300.dp))
                AlbumArt(data, size = 220.dp)
            }
            Spacer(Modifier.height(36.dp))
            SongTitle(data, Color.White)
            Spacer(Modifier.height(6.dp))
            // "Bloom": blurred glow layer under the crisp text
            Box {
                Text(data.artist, style = TextStyle(neon.copy(alpha = 0.4f), 19.sp, FontWeight.Medium,
                    shadow = Shadow(neon.copy(alpha = 0.6f), blurRadius = 24f)))
                Text(data.artist, style = TextStyle(neon, 19.sp, FontWeight.Medium))
            }
            Spacer(Modifier.height(28.dp))
            ListenBadge(tint = neon.copy(alpha = 0.15f), textColor = neon)
        }
        MelodyXLogo(
            Modifier.align(Alignment.TopCenter).padding(top = 72.dp),
            Brush.linearGradient(listOf(neon, neon)),
        )
    }
}

@Composable
private fun MoodRing(color: Color, seed: Int, modifier: Modifier) {
    val rnd = Random(seed)
    val rotation = rnd.nextFloat() * 360f
    Canvas(modifier.rotate(rotation)) {
        val stroke = 10.dp.toPx()
        val sweep = Brush.sweepGradient(
            listOf(color, color.copy(alpha = 0.15f), color, color.copy(alpha = 0.4f), color)
        )
        // glow: fat translucent pass under the crisp ring
        drawCircle(color.copy(alpha = 0.18f), style = Stroke(stroke * 3.2f))
        drawCircle(sweep, style = Stroke(stroke))
        // amplitude ticks
        repeat(48) { i ->
            val a = Math.toRadians(i * 7.5).toFloat()
            val amp = 8.dp.toPx() + rnd.nextFloat() * 20.dp.toPx()
            val r0 = size.minDimension / 2 + stroke
            val c = center
            drawLine(
                color.copy(alpha = 0.55f),
                Offset(c.x + kotlin.math.cos(a) * r0, c.y + kotlin.math.sin(a) * r0),
                Offset(c.x + kotlin.math.cos(a) * (r0 + amp), c.y + kotlin.math.sin(a) * (r0 + amp)),
                strokeWidth = 3.dp.toPx(),
            )
        }
    }
}

// ──────────────────────── Template C: Vinyl Classic ────────────────────────

@Composable
private fun VinylClassicCard(data: SongShareData, palette: CardPalette, modifier: Modifier) {
    val rotation = Random(data.songId.hashCode()).nextFloat() * 360f // unique per song
    Box(modifier.fillMaxSize().background(palette.posterBg)) {
        Column(
            Modifier.align(Alignment.Center).padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            VinylDisc(data, rotation, Modifier.size(280.dp))
            Spacer(Modifier.height(40.dp))
            // Gig-poster type: uppercase, stacked, tight leading
            Text(
                data.title.uppercase() + if (data.explicit) " 🅴" else "",
                textAlign = TextAlign.Center, maxLines = 3, overflow = TextOverflow.Ellipsis,
                style = TextStyle(palette.textAccent, 40.sp, FontWeight.ExtraBold,
                    letterSpacing = (-0.5).sp, lineHeight = 38.sp),
            )
            Spacer(Modifier.height(10.dp))
            Text(data.artist.uppercase(),
                style = TextStyle(Color.White.copy(alpha = 0.85f), 18.sp, FontWeight.Medium,
                    letterSpacing = 3.sp))
            Spacer(Modifier.height(32.dp))
            ListenBadge(tint = Color.Black.copy(alpha = 0.30f), textColor = Color.White)
        }
        MelodyXLogo(
            Modifier.align(Alignment.TopCenter).padding(top = 72.dp),
            Brush.linearGradient(listOf(Color.White, Color.White)),
        )
    }
}

@Composable
private fun VinylDisc(data: SongShareData, rotation: Float, modifier: Modifier) =
    Box(modifier.rotate(rotation), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            drawCircle(Color(0xFF111111))
            // 40 groove rings, alternating 4% / 8% white
            val maxR = size.minDimension / 2
            repeat(40) { i ->
                val r = maxR * (0.45f + 0.55f * i / 40f)
                drawCircle(
                    Color.White.copy(alpha = if (i % 2 == 0) 0.04f else 0.08f),
                    radius = r, style = Stroke(1.5f),
                )
            }
        }
        // album art as the 42% center label + spindle hole
        Box(Modifier.fillMaxSize(0.42f).clip(CircleShape), contentAlignment = Alignment.Center) {
            data.albumArt?.let {
                Image(it.asImageBitmap(), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            } ?: Box(Modifier.fillMaxSize().background(Color(0xFF7C4DFF)))
            Box(Modifier.size(10.dp).clip(CircleShape).background(Color(0xFF111111)))
        }
    }

// ─────────────────────────── Shared elements ───────────────────────────

@Composable
private fun AlbumArt(data: SongShareData, size: androidx.compose.ui.unit.Dp = 240.dp) =
    Box(
        Modifier.size(size).clip(RoundedCornerShape(size * 0.058f))
            // 12%-white hairline for edge definition on any background
            .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(size * 0.058f)),
    ) {
        data.albumArt?.let {
            Image(it.asImageBitmap(), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        } ?: Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(listOf(Color(0xFF7C4DFF), Color(0xFF0E0E12)))
            ),
            contentAlignment = Alignment.Center,
        ) { Text("♪", style = TextStyle(Color.White, 96.sp, FontWeight.ExtraBold)) }
    }

@Composable
private fun SongTitle(data: SongShareData, color: Color, shadow: Boolean = false) = Text(
    data.title + if (data.explicit) " 🅴" else "",
    textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis,
    style = TextStyle(
        color, 30.sp, FontWeight.ExtraBold, letterSpacing = (-0.6).sp,
        shadow = if (shadow) Shadow(Color.Black.copy(alpha = 0.35f), Offset(0f, 2f), 12f) else null,
    ),
)

@Composable
private fun ListenBadge(tint: Color, textColor: Color) = Row(
    Modifier.height(44.dp).clip(RoundedCornerShape(22.dp)).background(tint)
        .border(1.dp, textColor.copy(alpha = 0.25f), RoundedCornerShape(22.dp))
        .padding(horizontal = 20.dp),
    verticalAlignment = Alignment.CenterVertically,
) {
    Text("▶", style = TextStyle(textColor, 13.sp))
    Spacer(Modifier.width(8.dp))
    Text("Listen on Melody X", style = TextStyle(textColor, 14.sp, FontWeight.SemiBold))
}

@Composable
private fun MelodyXLogo(modifier: Modifier, brush: Brush) = Text(
    "MELODY X", modifier,
    style = TextStyle(brush = brush, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold,
        letterSpacing = 6.sp),
)
