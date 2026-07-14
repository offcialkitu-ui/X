package iad1tya.echo.music.ui.player.lockscreen

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import iad1tya.echo.music.constants.LockScreenLyricsPosition
import iad1tya.echo.music.lyrics.LyricsEntry
import iad1tya.echo.music.lyrics.LyricsHelper
import iad1tya.echo.music.lyrics.LyricsUtils
import iad1tya.echo.music.playback.PlayerConnection
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * LockScreenLyrics – Interactive lyrics display for the lock screen.
 *
 * Features:
 * - Synced lyrics with auto-scroll
 * - Karaoke-style word highlighting
 * - Tap to seek to a specific line
 * - Configurable position (bottom, overlay, fullscreen)
 * - Toggle button to show/hide lyrics
 */
@Composable
fun LockScreenLyricsPanel(
    playerConnection: PlayerConnection?,
    lyricsHelper: LyricsHelper?,
    isVisible: Boolean,
    position: LockScreenLyricsPosition,
    accentColor: Color,
    textPrimary: Color,
    textSecondary: Color,
    karaokeEnabled: Boolean,
    fontSize: Float = 14f,
    onToggleVisibility: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    // Current playback position
    var currentPosition by remember { mutableLongStateOf(0L) }
    var lyrics by remember { mutableStateOf<List<LyricsEntry>>(emptyList()) }
    var currentLineIndex by remember { mutableIntStateOf(-1) }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    // Poll playback position
    LaunchedEffect(playerConnection) {
        while (true) {
            currentPosition = playerConnection?.player?.currentPosition ?: 0L
            delay(250)
        }
    }

    // Load lyrics when metadata changes
    LaunchedEffect(playerConnection?.mediaMetadata?.value?.title) {
        val metadata = playerConnection?.mediaMetadata?.value
        if (metadata != null && lyricsHelper != null) {
            try {
                val result = lyricsHelper.getLyrics(metadata)
                val parsed = LyricsUtils.parseLyrics(result.lyrics).filter { it.text.isNotBlank() }
                lyrics = parsed
            } catch (_: Exception) {
                lyrics = emptyList()
            }
        }
    }

    // Find current line and auto-scroll
    LaunchedEffect(currentPosition, lyrics) {
        if (lyrics.isEmpty()) return@LaunchedEffect
        val index = lyrics.indexOfLast { it.time <= currentPosition }
        if (index != currentLineIndex && index >= 0) {
            currentLineIndex = index
            // Auto-scroll to center the current line
            listState.animateScrollToItem(
                index = (index - 2).coerceAtLeast(0),
            )
        }
    }

    if (!isVisible || lyrics.isEmpty()) return

    val isFullscreen = position == LockScreenLyricsPosition.FULLSCREEN
    val isOverlay = position == LockScreenLyricsPosition.OVERLAY

    Box(
        modifier = modifier
            .then(
                if (isFullscreen) Modifier.fillMaxSize()
                else Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            )
            .then(
                if (isOverlay) Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                else Modifier
            )
            .background(
                if (isFullscreen) Color.Black.copy(alpha = 0.85f)
                else Color.Black.copy(alpha = 0.4f),
                RoundedCornerShape(if (isFullscreen) 0.dp else 16.dp),
            ),
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            itemsIndexed(lyrics) { index, entry ->
                val isCurrentLine = index == currentLineIndex
                val isPastLine = index < currentLineIndex
                val lineAlpha = when {
                    isCurrentLine -> 1f
                    isPastLine -> 0.4f
                    else -> 0.6f
                }
                val lineColor = when {
                    isCurrentLine -> accentColor
                    else -> textPrimary
                }

                Text(
                    text = entry.text,
                    color = lineColor.copy(alpha = lineAlpha),
                    fontSize = (fontSize + (if (isCurrentLine) 2f else 0f)).sp,
                    fontWeight = if (isCurrentLine) FontWeight.Bold else FontWeight.Normal,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clickable {
                            // Seek to this line's timestamp
                            playerConnection?.player?.seekTo(entry.time)
                        }
                        .animateContentSize(),
                )

                // Karaoke-style word highlight for current line
                if (isCurrentLine && karaokeEnabled && entry.words != null) {
                    val progress = if (entry.words.isNotEmpty()) {
                        val wordDuration = entry.words.last().endTime - entry.words.first().startTime
                        if (wordDuration > 0) {
                            ((currentPosition / 1000.0) - entry.words.first().startTime).toFloat() / wordDuration.toFloat()
                        } else 0f
                    } else 0f

                    if (progress in 0f..1f) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(2.dp)
                                .padding(horizontal = 60.dp)
                                .background(textSecondary.copy(alpha = 0.2f), RoundedCornerShape(1.dp)),
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(progress.coerceIn(0f, 1f))
                                    .fillMaxHeight()
                                    .background(accentColor, RoundedCornerShape(1.dp)),
                            )
                        }
                    }
                }
            }
        }

        // Gradient fade at top and bottom for fullscreen
        if (isFullscreen) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Black.copy(alpha = 0.85f), Color.Transparent),
                        )
                    )
                    .align(Alignment.TopCenter),
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f)),
                        )
                    )
                    .align(Alignment.BottomCenter),
            )
        }
    }
}

/**
 * Lyrics toggle button – floating action button to show/hide lyrics.
 */
@Composable
fun LyricsToggleButton(
    isVisible: Boolean,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val animatedColor by animateColorAsState(
        targetValue = if (isVisible) accentColor else Color.White.copy(alpha = 0.6f),
        animationSpec = tween(300),
        label = "toggleColor",
    )

    Surface(
        modifier = modifier
            .size(44.dp),
        shape = RoundedCornerShape(12.dp),
        color = Color.White.copy(alpha = 0.1f),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            Color.White.copy(alpha = 0.15f),
        ),
        onClick = onClick,
    ) {
        Box(contentAlignment = Alignment.Center) {
            // Simple "LY" text as lyrics icon placeholder
            // Replace with actual icon drawable when available
            Text(
                text = "LY",
                color = animatedColor,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
        }
    }
}