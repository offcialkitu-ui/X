package iad1tya.echo.music.playback

import android.app.Service
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import coil3.compose.AsyncImage
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.allowHardware
import coil3.toBitmap
import dagger.hilt.android.AndroidEntryPoint
import iad1tya.echo.music.MainActivity
import iad1tya.echo.music.R
import iad1tya.echo.music.constants.EnableHighRefreshRateKey
import iad1tya.echo.music.db.MusicDatabase
import iad1tya.echo.music.ui.theme.echomusicTheme
import iad1tya.echo.music.ui.theme.extractThemeColor
import iad1tya.echo.music.utils.dataStore
import iad1tya.echo.music.utils.get
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sin

@OptIn(ExperimentalFoundationApi::class)
@AndroidEntryPoint
class DynamicCapsuleService : Service(), LifecycleOwner, SavedStateRegistryOwner {

    private val lifecycleRegistry = LifecycleRegistry(this)
    override val lifecycle: Lifecycle get() = lifecycleRegistry

    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry

    @Inject
    lateinit var database: MusicDatabase

    private lateinit var windowManager: WindowManager
    private var composeView: ComposeView? = null

    private var playerConnection by mutableStateOf<PlayerConnection?>(null)

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            if (service is MusicService.MusicBinder) {
                playerConnection = PlayerConnection(this@DynamicCapsuleService, service, database, lifecycleScope)
            }
        }
        override fun onServiceDisconnected(name: ComponentName?) {
            playerConnection = null
        }
    }

    override fun onCreate() {
        super.onCreate()
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager

        bindService(
            Intent(this, MusicService::class.java),
            serviceConnection,
            BIND_AUTO_CREATE
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        showCapsule()
        return START_STICKY
    }

    private fun showCapsule() {
        if (composeView != null) return

        val statusBarHeight = try {
            val resourceId = resources.getIdentifier("status_bar_height", "dimen", "android")
            if (resourceId > 0) resources.getDimensionPixelSize(resourceId) else 0
        } catch (e: Exception) { 0 }

        val enableHighRefreshRate = dataStore[EnableHighRefreshRateKey, true]

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            y = if (statusBarHeight > 0) statusBarHeight + 8 else 48

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                if (enableHighRefreshRate) {
                    val modes = windowManager.defaultDisplay.supportedModes
                    val highRefreshTargets = listOf(144f, 120f, 90f)
                    val bestMode = highRefreshTargets.firstNotNullOfOrNull { targetHz ->
                        modes.firstOrNull { kotlin.math.abs(it.refreshRate - targetHz) < 1f }
                    } ?: modes.maxByOrNull { it.refreshRate }
                    preferredDisplayModeId = bestMode?.modeId ?: 0
                }
            } else {
                preferredRefreshRate = if (enableHighRefreshRate) 144f else 60f
            }
        }

        composeView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@DynamicCapsuleService)
            setViewTreeSavedStateRegistryOwner(this@DynamicCapsuleService)
            setContent {
                CapsuleUI()
            }
        }

        try {
            windowManager.addView(composeView, params)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @Composable
    private fun CapsuleUI() {
        val connection = playerConnection
        val metadata by connection?.mediaMetadata?.collectAsState() ?: remember { mutableStateOf(null) }
        val isPlaying by connection?.isPlaying?.collectAsState() ?: remember { mutableStateOf(false) }

        val context = LocalContext.current
        val haptic = LocalHapticFeedback.current

        var expanded by remember { mutableStateOf(false) }
        var accent by remember { mutableStateOf(Color.White) }
        val scope = rememberCoroutineScope()
        val swipeOffset = remember { Animatable(0f) }

        // Entry: clean slide-down + fade (Dynamic Island style)
        val entryAlpha = remember { Animatable(0f) }
        val entrySlideY = remember { Animatable(-20f) }
        LaunchedEffect(Unit) {
            launch {
                entrySlideY.animateTo(0f, spring(dampingRatio = 0.9f, stiffness = 200f))
            }
            delay(80)
            entryAlpha.animateTo(1f, tween(300))
        }

        // Accent color from album art (muted)
        LaunchedEffect(metadata?.thumbnailUrl) {
            metadata?.thumbnailUrl?.let { url ->
                runCatching {
                    val result = context.imageLoader.execute(
                        ImageRequest.Builder(context).data(url).allowHardware(false).build()
                    )
                    if (result is SuccessResult) {
                        val raw = result.image.toBitmap().extractThemeColor()
                        accent = Color(
                            red = raw.red * 0.6f + 0.15f,
                            green = raw.green * 0.6f + 0.15f,
                            blue = raw.blue * 0.6f + 0.15f,
                            alpha = 1f
                        )
                    }
                }
            }
        }

        // Progress
        var progress by remember { mutableFloatStateOf(0f) }
        LaunchedEffect(connection) {
            while (connection != null) {
                val pos = connection.player.currentPosition
                val dur = connection.player.duration.takeIf { it > 0 } ?: 1L
                progress = (pos.toFloat() / dur.toFloat()).coerceIn(0f, 1f)
                delay(1000)
            }
        }

        // Auto-collapse
        LaunchedEffect(expanded) {
            if (expanded) {
                delay(6000)
                expanded = false
            }
        }

        // Dimensions — Dynamic Island proportions
        val pillHeight = 38.dp
        val expandedHeight = 72.dp
        val collapsedWidth = 164.dp
        val expandedWidth = 300.dp

        val morphSpring = spring<Dp>(dampingRatio = 0.85f, stiffness = 250f)
        val width by animateDpAsState(
            targetValue = if (expanded) expandedWidth else collapsedWidth,
            animationSpec = morphSpring, label = "w"
        )
        val height by animateDpAsState(
            targetValue = if (expanded) expandedHeight else pillHeight,
            animationSpec = morphSpring, label = "h"
        )

        // Press feedback
        val interactionSource = remember { MutableInteractionSource() }
        val isPressed by interactionSource.collectIsPressedAsState()
        val pressScale by animateFloatAsState(
            targetValue = if (isPressed) 0.96f else 1f,
            animationSpec = spring(dampingRatio = 0.7f, stiffness = 400f),
            label = "press"
        )

        echomusicTheme(darkTheme = true, pureBlack = true) {
            Box(
                modifier = Modifier
                    .width(width)
                    .height(height)
                    .offset { IntOffset(swipeOffset.value.roundToInt(), entrySlideY.value.roundToInt()) }
                    .alpha(entryAlpha.value)
                    .clip(RoundedCornerShape(height / 2))
                    .background(Color.Black)
                    .draggable(
                        orientation = Orientation.Horizontal,
                        state = rememberDraggableState { delta ->
                            scope.launch {
                                swipeOffset.snapTo(
                                    (swipeOffset.value + delta * 0.7f).coerceIn(-500f, 500f)
                                )
                            }
                        },
                        onDragStarted = {},
                        onDragStopped = {
                            scope.launch {
                                if (abs(swipeOffset.value) > 150f) {
                                    swipeOffset.animateTo(
                                        targetValue = if (swipeOffset.value > 0) 800f else -800f,
                                        animationSpec = tween(250)
                                    )
                                    stopSelf()
                                } else {
                                    swipeOffset.animateTo(0f, spring())
                                }
                            }
                        }
                    )
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null
                    ) {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        if (expanded) {
                            val intent = Intent(this@DynamicCapsuleService, MainActivity::class.java).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                            }
                            startActivity(intent)
                        }
                        expanded = !expanded
                    },
                contentAlignment = Alignment.Center
            ) {
                AnimatedContent(
                    targetState = expanded,
                    transitionSpec = {
                        (fadeIn(tween(250)) + slideInVertically(tween(300)) { it / 4 })
                            .togetherWith(fadeOut(tween(200)) + slideOutVertically(tween(250)) { -it / 4 })
                            .using(SizeTransform(clip = false))
                    },
                    label = "morph"
                ) { isExpanded ->
                    if (isExpanded) {
                        ExpandedLayout(metadata, accent, isPlaying, connection) {
                            scope.launch {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                connection?.togglePlayPause()
                            }
                        }
                    } else {
                        CompactLayout(metadata, accent, progress, isPlaying)
                    }
                }
            }
        }
    }

    // ─── Compact: Album art + title + waveform ───────────────────────────
    @Composable
    private fun CompactLayout(
        metadata: iad1tya.echo.music.models.MediaMetadata?,
        accent: Color,
        progress: Float,
        isPlaying: Boolean
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Album art — small, clean circle
            Box(contentAlignment = Alignment.Center) {
                AsyncImage(
                    model = metadata?.thumbnailUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                )
                // Tiny progress arc — single clean stroke
                Canvas(modifier = Modifier.size(28.dp)) {
                    val stroke = 1.5.dp.toPx()
                    val inset = stroke / 2
                    drawArc(
                        color = accent.copy(alpha = 0.6f),
                        startAngle = -90f,
                        sweepAngle = 360f * progress,
                        useCenter = false,
                        style = Stroke(
                            width = stroke,
                            cap = StrokeCap.Round
                        ),
                        topLeft = Offset(inset, inset),
                        size = Size(size.width - stroke, size.height - stroke)
                    )
                }
            }

            Spacer(Modifier.width(8.dp))

            // Title — single line, clean
            Text(
                text = metadata?.title?.uppercase() ?: "MELODY X",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    fontSize = 11.sp,
                    letterSpacing = 0.8.sp
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f)
                    .basicMarquee(
                        iterations = Int.MAX_VALUE,
                        initialDelayMillis = 2000,
                        velocity = 30.dp
                    )
            )

            Spacer(Modifier.width(6.dp))

            // Mini waveform — only 3 bars, very subtle
            MiniWaveform(isPlaying, accent)
        }
    }

    // ─── Expanded: Art + controls ────────────────────────────────────────
    @Composable
    private fun ExpandedLayout(
        metadata: iad1tya.echo.music.models.MediaMetadata?,
        accent: Color,
        isPlaying: Boolean,
        connection: PlayerConnection?,
        onTogglePlay: () -> Unit
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Album art — larger
            AsyncImage(
                model = metadata?.thumbnailUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
            )

            Spacer(Modifier.width(10.dp))

            // Song info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = metadata?.title ?: "",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        fontSize = 13.sp,
                        letterSpacing = 0.3.sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = metadata?.artists?.joinToString { it.name } ?: "",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = Color.White.copy(alpha = 0.45f),
                        fontSize = 11.sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(Modifier.width(8.dp))

            // Controls — clean, no borders, no glass effects
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Prev
                IconButtonSmall(
                    iconRes = R.drawable.skip_previous,
                    onClick = { connection?.seekToPrevious() }
                )

                // Play/Pause — accent background
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(accent.copy(alpha = 0.25f))
                        .clickable { onTogglePlay() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painterResource(if (isPlaying) R.drawable.pause else R.drawable.play),
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Next
                IconButtonSmall(
                    iconRes = R.drawable.skip_next,
                    onClick = { connection?.seekToNext() }
                )
            }
        }
    }

    // ─── Tiny icon button — no border, no glass, just touchable ──────────
    @Composable
    private fun IconButtonSmall(iconRes: Int, onClick: () -> Unit) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(CircleShape)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painterResource(iconRes),
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.7f),
                modifier = Modifier.size(16.dp)
            )
        }
    }

    // ─── Mini waveform: 3 clean bars, no gradient ────────────────────────
    @Composable
    private fun MiniWaveform(isPlaying: Boolean, accent: Color) {
        val barCount = 3
        var phase by remember { mutableFloatStateOf(0f) }
        LaunchedEffect(isPlaying) {
            if (!isPlaying) {
                phase = 0f
                return@LaunchedEffect
            }
            while (true) {
                delay(120L)
                phase = (phase + 0.4f) % (Math.PI.toFloat() * 2f)
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.height(14.dp)
        ) {
            repeat(barCount) { i ->
                val barPhase = phase + i * 1.2f
                val barHeight = if (isPlaying) {
                    4.dp + 6.dp * abs(sin(barPhase))
                } else {
                    3.dp
                }

                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(barHeight)
                        .clip(RoundedCornerShape(1.dp))
                        .background(
                            if (isPlaying) Color.White.copy(alpha = 0.6f)
                            else Color.White.copy(alpha = 0.2f)
                        )
                )
            }
        }
    }

    override fun onDestroy() {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        composeView?.let {
            try {
                windowManager.removeView(it)
            } catch (_: Exception) {}
        }
        composeView = null
        try {
            unbindService(serviceConnection)
        } catch (_: Exception) {}
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
