
package iad1tya.echo.music.ui.player

import android.app.KeyguardManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.animateColor
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import coil3.compose.AsyncImage
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.allowHardware
import coil3.toBitmap
import dagger.hilt.android.AndroidEntryPoint
import iad1tya.echo.music.R
import iad1tya.echo.music.constants.*
import iad1tya.echo.music.extensions.*
import iad1tya.echo.music.db.MusicDatabase
import iad1tya.echo.music.lyrics.LyricsHelper
import iad1tya.echo.music.playback.MusicService
import iad1tya.echo.music.playback.PlayerConnection
import iad1tya.echo.music.ui.theme.echomusicTheme
import iad1tya.echo.music.ui.theme.extractThemeColor
import iad1tya.echo.music.ui.player.lockscreen.LockScreenVisualizer
import iad1tya.echo.music.ui.player.lockscreen.LockScreenLyricsPanel
import iad1tya.echo.music.ui.player.lockscreen.LyricsToggleButton
import iad1tya.echo.music.utils.dataStore
import iad1tya.echo.music.utils.get
import iad1tya.echo.music.utils.rememberPreference
import iad1tya.echo.music.utils.rememberEnumPreference
import iad1tya.echo.music.utils.SongMood
import iad1tya.echo.music.utils.SongMoodDetector
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.roundToInt

@AndroidEntryPoint
class LockScreenActivity : ComponentActivity() {

    @Inject
    lateinit var database: MusicDatabase

    @Inject
    lateinit var lyricsHelper: LyricsHelper

    private var playerConnection by mutableStateOf<PlayerConnection?>(null)

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            if (service is MusicService.MusicBinder) {
                playerConnection = PlayerConnection(this@LockScreenActivity, service, database, lifecycleScope)
            }
        }
        override fun onServiceDisconnected(name: ComponentName?) {
            playerConnection = null
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        requestWindowFeature(android.view.Window.FEATURE_NO_TITLE)
        window.attributes.flags = window.attributes.flags or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        WindowCompat.setDecorFitsSystemWindows(window, false)
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        }
        
        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
            WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
            WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
        )

        WindowCompat.getInsetsController(window, window.decorView).apply {
            hide(androidx.core.view.WindowInsetsCompat.Type.statusBars())
            hide(androidx.core.view.WindowInsetsCompat.Type.navigationBars())
            systemBarsBehavior = androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.navigationBarColor = android.graphics.Color.TRANSPARENT

        val enableHighRefreshRate = dataStore[EnableHighRefreshRateKey, true]
        val layoutParams = window.attributes
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (enableHighRefreshRate) {
                val modes = window.windowManager.defaultDisplay.supportedModes
                val highRefreshTargets = listOf(144f, 120f, 90f)
                val bestMode = highRefreshTargets.firstNotNullOfOrNull { targetHz ->
                    modes.firstOrNull { kotlin.math.abs(it.refreshRate - targetHz) < 1f }
                } ?: modes.maxByOrNull { it.refreshRate }
                layoutParams.preferredDisplayModeId = bestMode?.modeId ?: 0
            }
        } else {
            layoutParams.preferredRefreshRate = if (enableHighRefreshRate) 144f else 60f
        }
        window.attributes = layoutParams

        bindService(
            Intent(this, MusicService::class.java),
            serviceConnection,
            BIND_AUTO_CREATE
        )

        setContent {
            val (enabled) = rememberPreference(LockScreenOverlayKey, defaultValue = true)
            if (enabled) {
                LockScreenUI(playerConnection)
            } else {
                finish()
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        }
    }

    override fun onStart() {
        super.onStart()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            unbindService(serviceConnection)
        } catch (e: Exception) {}
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun LockScreenUI(playerConnection: PlayerConnection?) {
        val metadata by playerConnection?.mediaMetadata?.collectAsState() ?: remember { mutableStateOf(null) }
        val isPlaying by playerConnection?.isPlaying?.collectAsState() ?: remember { mutableStateOf(false) }

        var dominantColor by remember { mutableStateOf(Color(0xFF8B5CF6)) }
        val context = LocalContext.current
        val mood = remember(metadata) { SongMoodDetector.detect(metadata) }

        var offsetY by remember { mutableStateOf(0f) }
        val draggableState = rememberDraggableState { delta ->
            if (delta < 0) offsetY += delta
        }

        LaunchedEffect(offsetY) {
            if (offsetY < -800f) finish()
        }

        LaunchedEffect(metadata?.thumbnailUrl) {
            metadata?.thumbnailUrl?.let { url ->
                val request = ImageRequest.Builder(context)
                    .data(url)
                    .allowHardware(false)
                    .build()
                val result = context.imageLoader.execute(request)
                if (result is SuccessResult) {
                    dominantColor = result.image.toBitmap().extractThemeColor()
                }
            }
        }

        val visualizerType by rememberEnumPreference<LockScreenVisualizerType>(
            key = LockScreenVisualizerTypeKey,
            defaultValue = LockScreenVisualizerType.MOOD_AUTO,
        )
        val visualizerIntensity by rememberPreference(key = LockScreenVisualizerIntensityKey, defaultValue = 1f)
        val visualizerSpeed by rememberPreference(key = LockScreenVisualizerSpeedKey, defaultValue = 1f)
        val showLyrics by rememberPreference(key = LockScreenShowLyricsKey, defaultValue = false)
        val lyricsPosition by rememberEnumPreference<LockScreenLyricsPosition>(
            key = LockScreenLyricsPositionKey,
            defaultValue = LockScreenLyricsPosition.BOTTOM,
        )
        val lyricsFontSize by rememberPreference(key = LockScreenLyricsFontSizeKey, defaultValue = 14f)
        val lyricsKaraoke by rememberPreference(key = LockScreenLyricsKaraokeKey, defaultValue = true)

        echomusicTheme(darkTheme = true, pureBlack = true, themeColor = dominantColor) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
                    .offset(y = (offsetY / 4).dp)
                    .alpha((1f + (offsetY / 1500f)).coerceIn(0f, 1f))
                    .draggable(
                        state = draggableState,
                        orientation = Orientation.Vertical,
                        onDragStopped = { if (offsetY > -800f) offsetY = 0f }
                    )
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(metadata?.thumbnailUrl)
                        .size(100, 100)
                        .allowHardware(false)
                        .build(),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .blur(20.dp)
                        .alpha(0.45f),
                    contentScale = ContentScale.Crop
                )

                LockScreenVisualizer(
                    visualizerType = visualizerType,
                    dominantColor = dominantColor,
                    isPlaying = isPlaying,
                    mood = mood,
                    intensity = visualizerIntensity,
                    speed = visualizerSpeed,
                )

                FlagshipParticles(isPlaying)
                MelodyXAmbientGlow(dominantColor)

                LockScreenLyricsPanel(
                    playerConnection = playerConnection,
                    lyricsHelper = lyricsHelper,
                    isVisible = showLyrics,
                    position = lyricsPosition,
                    accentColor = dominantColor,
                    textPrimary = Color.White,
                    textSecondary = Color.White.copy(alpha = 0.6f),
                    karaokeEnabled = lyricsKaraoke,
                    fontSize = lyricsFontSize,
                )

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(top = 80.dp).alpha(0.9f)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                        ) {
                            Text(
                                text = "MELODY X",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 6.sp,
                                    color = Color.White
                                )
                            )
                        }
                        if (mood != SongMood.DEFAULT) {
                            Text(
                                text = mood.name,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 2.sp,
                                    color = dominantColor
                                )
                            )
                        }
                    }

                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.weight(1f).fillMaxWidth()
                    ) {
                        FlagshipWaveform(dominantColor, isPlaying)
                        
                        MoodAwareRing(
                            thumbnailUrl = metadata?.thumbnailUrl,
                            mood = mood,
                            isPlaying = isPlaying,
                            dominantColor = dominantColor,
                        )
                    }

                    Column(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = metadata?.title ?: "No Song Playing",
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White,
                                fontSize = 30.sp,
                                letterSpacing = (-0.5).sp
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = metadata?.artists?.joinToString { it.name } ?: "Unknown Artist",
                            style = MaterialTheme.typography.titleMedium.copy(
                                color = Color.LightGray.copy(alpha = 0.7f),
                                fontWeight = FontWeight.Normal,
                                letterSpacing = 0.5.sp
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(44.dp))

                        FlagshipProgressBar(playerConnection, dominantColor)

                        Spacer(modifier = Modifier.height(32.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            FlagshipControlCircle(R.drawable.skip_previous, size = 64.dp) { playerConnection?.seekToPrevious() }
                            FlagshipPlayPause(isPlaying, dominantColor) { playerConnection?.togglePlayPause() }
                            FlagshipControlCircle(R.drawable.skip_next, size = 64.dp) { playerConnection?.seekToNext() }
                        }

                        Spacer(Modifier.height(20.dp))

                        val audioManager = remember(context) {
                            context.getSystemService(Context.AUDIO_SERVICE) as android.media.AudioManager
                        }
                        val maxVol = remember { audioManager.getStreamMaxVolume(android.media.AudioManager.STREAM_MUSIC) }
                        var currentVol by remember { mutableIntStateOf(audioManager.getStreamVolume(android.media.AudioManager.STREAM_MUSIC)) }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 40.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.volume_down),
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.4f),
                                modifier = Modifier.size(20.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Slider(
                                    value = currentVol.toFloat(),
                                    onValueChange = {
                                        currentVol = it.roundToInt()
                                        try {
                                            audioManager.setStreamVolume(
                                                android.media.AudioManager.STREAM_MUSIC,
                                                currentVol,
                                                0
                                            )
                                        } catch (_: Exception) {}
                                    },
                                    valueRange = 0f..maxVol.toFloat(),
                                    colors = SliderDefaults.colors(
                                        thumbColor = dominantColor,
                                        activeTrackColor = dominantColor,
                                        inactiveTrackColor = Color.White.copy(alpha = 0.15f)
                                    ),
                                    modifier = Modifier.height(24.dp)
                                )
                            }
                            Icon(
                                painter = painterResource(R.drawable.volume_up),
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.6f),
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(Modifier.height(28.dp))

                        Text(
                            text = "SWIPE UP TO UNLOCK",
                            style = MaterialTheme.typography.labelSmall.copy(
                                letterSpacing = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White.copy(alpha = 0.4f)
                            )
                        )

                        Spacer(Modifier.height(24.dp))
                    }
                }

                if (showLyrics) {
                    LyricsToggleButton(
                        isVisible = showLyrics,
                        accentColor = dominantColor,
                        onClick = { },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 80.dp, end = 24.dp),
                    )
                }
            }
        }
    }

    @Composable
    fun MelodyXAmbientGlow(baseColor: Color) {
        val infiniteTransition = rememberInfiniteTransition(label = "glow")
        val alpha by infiniteTransition.animateFloat(
            initialValue = 0.3f, targetValue = 0.6f,
            animationSpec = infiniteRepeatable(animation = tween(8000, easing = FastOutSlowInEasing), repeatMode = RepeatMode.Reverse),
            label = "alpha"
        )
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(brush = Brush.radialGradient(listOf(baseColor.copy(alpha = alpha), Color.Transparent), center = center, radius = size.maxDimension * 0.8f), blendMode = BlendMode.Screen)
        }
    }

    @Composable
    fun FlagshipParticles(isPlaying: Boolean) {
        if (!isPlaying) return
        val infiniteTransition = rememberInfiniteTransition(label = "particles")
        repeat(12) { i ->
            val startX = (50 + (i * 180)).toFloat()
            val duration = 4500 + (i * 400)
            val posY by infiniteTransition.animateFloat(initialValue = 2200f, targetValue = -200f, animationSpec = infiniteRepeatable(animation = tween(duration, easing = LinearEasing)), label = "p$i")
            Canvas(Modifier.fillMaxSize().alpha(0.12f)) { drawCircle(color = Color.White, radius = (1.2).dp.toPx(), center = Offset(startX % size.width, posY)) }
        }
    }

    @Composable
    fun FlagshipProgressBar(playerConnection: PlayerConnection?, themeColor: Color) {
        var position by remember { mutableLongStateOf(0L) }
        val duration = playerConnection?.player?.duration?.takeIf { it > 0 } ?: 1L
        LaunchedEffect(playerConnection) {
            while (true) {
                position = playerConnection?.player?.currentPosition ?: 0L
                delay(500)
            }
        }
        val progress = (position.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
            Box(modifier = Modifier.fillMaxWidth().height(3.dp).background(Color.White.copy(alpha = 0.1f), CircleShape)) {
                Box(modifier = Modifier.fillMaxWidth(progress).fillMaxHeight().shadow(elevation = 4.dp, shape = CircleShape, ambientColor = themeColor, spotColor = themeColor).background(brush = Brush.horizontalGradient(listOf(themeColor, Color.Cyan, Color.White)), shape = CircleShape))
            }
            Spacer(Modifier.height(14.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                val formatTime: (Long) -> String = { ms -> val s = ms / 1000; "%d:%02d".format(s / 60, s % 60) }
                Text(formatTime(position), style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.4f))
                Text(formatTime(duration), style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.4f))
            }
        }
    }

    @Composable
    fun FlagshipControlCircle(icon: Int, size: androidx.compose.ui.unit.Dp, onClick: () -> Unit) {
        Surface(modifier = Modifier.size(size).clip(CircleShape).clickable(interactionSource = remember { MutableInteractionSource() }, indication = ripple(color = Color.White, bounded = true)) { onClick() }, color = Color.White.copy(alpha = 0.08f), shape = CircleShape) {
            Box(contentAlignment = Alignment.Center) { Icon(painter = painterResource(icon), contentDescription = null, modifier = Modifier.size(size / 2.2f), tint = Color.White) }
        }
    }

    @Composable
    fun FlagshipPlayPause(isPlaying: Boolean, themeColor: Color, onClick: () -> Unit) {
        val beatScale by animateFloatAsState(targetValue = if (isPlaying) 1.06f else 1f, animationSpec = if (isPlaying) infiniteRepeatable(tween(1200, easing = FastOutSlowInEasing), RepeatMode.Reverse) else tween(300), label = "beat")
        Surface(modifier = Modifier.size(100.dp).graphicsLayer { scaleX = beatScale; scaleY = beatScale }.drawBehind { drawCircle(brush = Brush.radialGradient(listOf(themeColor.copy(alpha = 0.4f), Color.Transparent)), radius = size.minDimension * 0.9f) }.clip(CircleShape).clickable(interactionSource = remember { MutableInteractionSource() }, indication = ripple(color = themeColor, bounded = true)) { onClick() }, color = Color.White.copy(alpha = 0.15f), shape = CircleShape) {
            Box(contentAlignment = Alignment.Center) { Icon(painter = painterResource(if (isPlaying) R.drawable.pause else R.drawable.play), contentDescription = null, modifier = Modifier.size(46.dp), tint = Color.White) }
        }
    }

    @Composable
    fun MoodAwareRing(
        thumbnailUrl: String?,
        mood: SongMood,
        isPlaying: Boolean,
        dominantColor: Color,
        segments: Int = 32,
    ) {
        val transition = rememberInfiniteTransition(label = "moodRing")

        // 1. Rotation — speed keyed to mood; restarts seamlessly at 360°.
        val rotation by transition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(mood.rotationDurationMs, easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
            ),
            label = "rotation",
        )

        // 2. Spectrum — deterministic per-segment duration + phase offset, never in sync.
        val thicknesses = List(segments) { i ->
            val duration = 420 + (i * 137) % 480
            transition.animateFloat(
                initialValue = 1f,
                targetValue = 12f,
                animationSpec = infiniteRepeatable(
                    animation = tween(duration, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse,
                    initialStartOffset = StartOffset((i * 53) % duration),
                ),
                label = "seg$i",
            )
        }

        // 3. Beat pulse — organic spring, looped at the mood's BPM.
        val beatScale = remember { Animatable(1f) }
        LaunchedEffect(mood, isPlaying) {
            if (!isPlaying) {
                beatScale.snapTo(1f)
                return@LaunchedEffect
            }
            val beatMs = 60_000L / mood.bpm
            while (isActive) {
                val start = System.currentTimeMillis()
                beatScale.animateTo(
                    1.04f,
                    spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
                )
                beatScale.animateTo(1f, spring(stiffness = Spring.StiffnessLow))
                delay((beatMs - (System.currentTimeMillis() - start)).coerceAtLeast(0))
            }
        }

        // 4. Color grading — HSL hue-shift +4° per segment from the dominant color.
        val segmentColors = remember(dominantColor, segments) {
            val baseHue = dominantColor.hue
            val baseSaturation = dominantColor.saturation
            List(segments) { i ->
                Color.hsl(
                    hue = (baseHue + i * 4f) % 360f,
                    saturation = baseSaturation.coerceIn(0.55f, 1f),
                    lightness = 0.6f,
                )
            }
        }
        
        // Append first color so the sweep gradient wraps without a seam.
        val glowBrush = remember(segmentColors) {
            Brush.sweepGradient(segmentColors + segmentColors.first())
        }

        Box(contentAlignment = Alignment.Center) {
            Canvas(
                Modifier
                    .size(340.dp)
                    // Animated values read here only → zero recompositions per frame.
                    .graphicsLayer {
                        scaleX = beatScale.value
                        scaleY = beatScale.value
                        rotationZ = rotation
                    },
            ) {
                val maxStroke = 12.dp.toPx()
                val glowExtra = 10.dp.toPx()
                val radius = size.minDimension / 2f - (maxStroke + glowExtra) / 2f
                val arcTopLeft = Offset(center.x - radius, center.y - radius)
                val arcSize = Size(radius * 2f, radius * 2f)
                val step = 360f / segments
                val sweep = step * 0.6f // 60% bar, 40% gap

                // Layer 1 — neon bloom glow (thicker, translucent, sweep gradient).
                for (i in 0 until segments) {
                    drawArc(
                        brush = glowBrush,
                        startAngle = i * step,
                        sweepAngle = sweep,
                        useCenter = false,
                        topLeft = arcTopLeft,
                        size = arcSize,
                        alpha = 0.15f,
                        style = Stroke(width = thicknesses[i].value.dp.toPx() + glowExtra, cap = StrokeCap.Round),
                    )
                }
                // Layer 2 — sharp spectrum bars.
                for (i in 0 until segments) {
                    drawArc(
                        color = segmentColors[i],
                        startAngle = i * step,
                        sweepAngle = sweep,
                        useCenter = false,
                        topLeft = arcTopLeft,
                        size = arcSize,
                        style = Stroke(width = thicknesses[i].value.dp.toPx(), cap = StrokeCap.Round),
                    )
                }
            }

            // Album art stays stable while the ring dances around it.
            AsyncImage(
                model = thumbnailUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(280.dp)
                    .clip(RoundedCornerShape(36.dp)),
            )
        }
    }

    @Composable
    fun FlagshipWaveform(baseColor: Color, isPlaying: Boolean) {
        val infiniteTransition = rememberInfiniteTransition(label = "wave")
        val phase by infiniteTransition.animateFloat(initialValue = 0f, targetValue = 2 * Math.PI.toFloat(), animationSpec = infiniteRepeatable(animation = tween(3000, easing = LinearEasing)), label = "phase")
        Canvas(modifier = Modifier.fillMaxWidth().height(180.dp).alpha(0.25f)) {
            val path = Path()
            val centerY = size.height / 2
            val width = size.width
            path.moveTo(0f, centerY)
            for (x in 0..width.toInt() step 5) {
                val relX = x / width
                val amp = if (isPlaying) 50.dp.toPx() else 4.dp.toPx()
                val y = centerY + amp * sin(relX * 12f + phase) * sin(relX * Math.PI.toFloat())
                path.lineTo(x.toFloat(), y.toFloat())
            }
            drawPath(path = path, brush = Brush.horizontalGradient(listOf(Color.Transparent, baseColor, Color.Cyan, baseColor, Color.Transparent)), style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round))
        }
    }
}
