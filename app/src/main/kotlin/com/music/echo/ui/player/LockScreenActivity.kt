
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
import androidx.compose.foundation.BorderStroke
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
import iad1tya.echo.music.constants.LockScreenOverlayKey
import iad1tya.echo.music.db.MusicDatabase
import iad1tya.echo.music.playback.MusicService
import iad1tya.echo.music.playback.PlayerConnection
import iad1tya.echo.music.ui.theme.echomusicTheme
import iad1tya.echo.music.ui.theme.extractThemeColor
import iad1tya.echo.music.utils.rememberPreference
import iad1tya.echo.music.utils.SongMood
import iad1tya.echo.music.utils.SongMoodDetector
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import kotlin.math.cos
import kotlin.math.sin

@AndroidEntryPoint
class LockScreenActivity : ComponentActivity() {

    @Inject
    lateinit var database: MusicDatabase

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
        
        // Remove title bar and make truly edge-to-edge
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

        // Force hide both status and navigation bars for absolute immersion
        WindowCompat.getInsetsController(window, window.decorView).apply {
            hide(androidx.core.view.WindowInsetsCompat.Type.statusBars())
            hide(androidx.core.view.WindowInsetsCompat.Type.navigationBars())
            systemBarsBehavior = androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.navigationBarColor = android.graphics.Color.TRANSPARENT

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
                // Background Layer: Heavy Blurred Art
                AsyncImage(
                    model = metadata?.thumbnailUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .blur(100.dp)
                        .alpha(0.45f),
                    contentScale = ContentScale.Crop
                )

                // Advanced AI Visualizers
                Box(modifier = Modifier.fillMaxSize()) {
                    when (mood) {
                        SongMood.PHONK -> PhonkVisualizer(dominantColor, isPlaying)
                        SongMood.LOFI -> LofiVisualizer(dominantColor, isPlaying)
                        SongMood.POP -> PopVisualizer(dominantColor, isPlaying)
                        else -> {
                            ScreenEdgeLighting(dominantColor)
                            MelodyXAmbientGlow(dominantColor)
                        }
                    }
                }
                
                GlassReflections()
                FlagshipParticles(isPlaying)

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp), // Removed vertical padding to eliminate potential gaps
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // Header: Completely clean, no black border
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(top = 80.dp).alpha(0.9f)
                    ) {
                        Text(
                            text = "MELODY X",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Black,
                                letterSpacing = 6.sp,
                                color = Color.White
                            )
                        )
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

                    // Main Area: Artwork & Rings
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.weight(1f).fillMaxWidth()
                    ) {
                        // Waveform behind artwork
                        FlagshipWaveform(dominantColor, isPlaying)
                        
                        // Mood-aware Dynamic Ring
                        MoodAwareRing(isPlaying, mood, dominantColor)

                        // Centered Album Artwork with Glass Border
                        Surface(
                            modifier = Modifier
                                .size(280.dp)
                                .clip(RoundedCornerShape(36.dp))
                                .border(1.5.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(36.dp)),
                            color = Color.Black.copy(alpha = 0.3f),
                            tonalElevation = 10.dp,
                            shadowElevation = 40.dp
                        ) {
                            AsyncImage(
                                model = metadata?.thumbnailUrl,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }

                    // Info, Progress, and Controls
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

                        // Glowing Progress Bar
                        FlagshipProgressBar(playerConnection, dominantColor)

                        Spacer(modifier = Modifier.height(44.dp))

                        // Frosted Glass Controls
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            FlagshipControlCircle(R.drawable.skip_previous, size = 68.dp) { playerConnection?.seekToPrevious() }
                            FlagshipPlayPause(isPlaying, dominantColor) { playerConnection?.togglePlayPause() }
                            FlagshipControlCircle(R.drawable.skip_next, size = 68.dp) { playerConnection?.seekToNext() }
                        }

                        Spacer(Modifier.height(84.dp))

                        // Bottom Swipe Hint
                        Text(
                            text = "SWIPE UP TO UNLOCK",
                            style = MaterialTheme.typography.labelSmall.copy(
                                letterSpacing = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White.copy(alpha = 0.4f)
                            )
                        )
                        
                        Spacer(Modifier.height(48.dp))
                    }
                }
            }
        }
    }

    @Composable
    fun PhonkVisualizer(themeColor: Color, isPlaying: Boolean) {
        if (!isPlaying) return
        val infiniteTransition = rememberInfiniteTransition(label = "phonk_extreme")
        val glitchIntensity by infiniteTransition.animateFloat(
            initialValue = 0f, targetValue = 1f,
            animationSpec = infiniteRepeatable(animation = tween(50, easing = LinearEasing), repeatMode = RepeatMode.Reverse),
            label = "glitch"
        )
        val flashAlpha by infiniteTransition.animateFloat(
            initialValue = 0f, targetValue = 0.2f,
            animationSpec = infiniteRepeatable(animation = tween(150, easing = FastOutLinearInEasing), repeatMode = RepeatMode.Reverse),
            label = "flash"
        )

        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(color = Color.White.copy(alpha = flashAlpha))
            val offset = (glitchIntensity * 20.dp.toPx())
            drawRect(color = Color.Cyan.copy(alpha = 0.3f), topLeft = Offset(offset, offset / 2), size = size, blendMode = BlendMode.Screen)
            drawRect(color = Color.Magenta.copy(alpha = 0.3f), topLeft = Offset(-offset, -offset / 2), size = size, blendMode = BlendMode.Screen)
            val scanlineHeight = 2.dp.toPx()
            for (y in 0..size.height.toInt() step (10.dp.toPx().toInt())) {
                drawRect(color = Color.Black.copy(alpha = 0.2f), topLeft = Offset(0f, y.toFloat()), size = size.copy(height = scanlineHeight))
            }
        }
    }

    @Composable
    fun LofiVisualizer(themeColor: Color, isPlaying: Boolean) {
        val infiniteTransition = rememberInfiniteTransition(label = "lofi_ambient")
        val fogBreath by infiniteTransition.animateFloat(
            initialValue = 0.5f, targetValue = 1f,
            animationSpec = infiniteRepeatable(animation = tween(8000, easing = LinearOutSlowInEasing), repeatMode = RepeatMode.Reverse),
            label = "fog"
        )
        val warmAlpha by infiniteTransition.animateFloat(
            initialValue = 0.1f, targetValue = 0.3f,
            animationSpec = infiniteRepeatable(animation = tween(4000, easing = LinearEasing), repeatMode = RepeatMode.Reverse),
            label = "warmth"
        )

        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(color = Color(0xFFF59E0B).copy(alpha = warmAlpha), blendMode = BlendMode.Overlay)
            drawCircle(brush = Brush.radialGradient(listOf(Color(0xFF8B5CF6).copy(alpha = 0.5f * fogBreath), Color.Transparent)), center = Offset(size.width * 0.3f, size.height * 0.2f), radius = size.maxDimension * 0.9f)
            drawCircle(brush = Brush.radialGradient(listOf(Color(0xFF3B82F6).copy(alpha = 0.4f * fogBreath), Color.Transparent)), center = Offset(size.width * 0.7f, size.height * 0.8f), radius = size.maxDimension * 0.8f)
        }
    }

    @Composable
    fun PopVisualizer(themeColor: Color, isPlaying: Boolean) {
        if (!isPlaying) return
        val infiniteTransition = rememberInfiniteTransition(label = "pop_energy")
        val ringPulse by infiniteTransition.animateFloat(
            initialValue = 0f, targetValue = 1f,
            animationSpec = infiniteRepeatable(animation = tween(1200, easing = FastOutSlowInEasing)),
            label = "pulse"
        )

        Canvas(modifier = Modifier.fillMaxSize()) {
            repeat(4) { i ->
                val delayOffset = (i * 0.25f)
                val currentProgress = (ringPulse + delayOffset) % 1f
                val scale = 0.5f + (currentProgress * 1.5f)
                val alpha = (1f - currentProgress) * 0.4f
                drawCircle(color = if (i % 2 == 0) Color.Cyan.copy(alpha = alpha) else Color.Magenta.copy(alpha = alpha), radius = (size.minDimension / 2) * scale, style = Stroke(width = (4 - i).dp.toPx()))
            }
        }
    }

    @Composable
    fun ScreenEdgeLighting(themeColor: Color) {
        val infiniteTransition = rememberInfiniteTransition(label = "edge")
        val alpha by infiniteTransition.animateFloat(
            initialValue = 0.1f, targetValue = 0.25f,
            animationSpec = infiniteRepeatable(animation = tween(4000, easing = LinearOutSlowInEasing), repeatMode = RepeatMode.Reverse),
            label = "alpha"
        )
        Canvas(modifier = Modifier.fillMaxSize()) {
            val h = 100.dp.toPx()
            drawRect(brush = Brush.verticalGradient(listOf(themeColor.copy(alpha = alpha), Color.Transparent), startY = 0f, endY = h))
            drawRect(brush = Brush.verticalGradient(listOf(Color.Transparent, Color.Cyan.copy(alpha = alpha * 0.5f)), startY = size.height - h, endY = size.height))
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
    fun GlassReflections() {
        val infiniteTransition = rememberInfiniteTransition(label = "glass")
        val offset by infiniteTransition.animateFloat(initialValue = -1000f, targetValue = 2000f, animationSpec = infiniteRepeatable(animation = tween(12000, easing = LinearEasing)), label = "offset")
        Canvas(modifier = Modifier.fillMaxSize().alpha(0.05f)) {
            drawLine(brush = Brush.linearGradient(listOf(Color.Transparent, Color.White, Color.Transparent), start = Offset(offset, 0f), end = Offset(offset + 300f, size.height)), start = Offset(offset, 0f), end = Offset(offset + 600f, size.height), strokeWidth = 100.dp.toPx())
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
                Box(modifier = Modifier.fillMaxWidth(progress).fillMaxHeight().shadow(elevation = 12.dp, shape = CircleShape, ambientColor = themeColor, spotColor = themeColor).background(brush = Brush.horizontalGradient(listOf(themeColor, Color.Cyan, Color.White)), shape = CircleShape))
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
        Surface(modifier = Modifier.size(size).clip(CircleShape).clickable(interactionSource = remember { MutableInteractionSource() }, indication = ripple(color = Color.White, bounded = true)) { onClick() }, color = Color.White.copy(alpha = 0.08f), shape = CircleShape, border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f))) {
            Box(contentAlignment = Alignment.Center) { Icon(painter = painterResource(icon), contentDescription = null, modifier = Modifier.size(size / 2.2f), tint = Color.White) }
        }
    }

    @Composable
    fun FlagshipPlayPause(isPlaying: Boolean, themeColor: Color, onClick: () -> Unit) {
        val beatScale by animateFloatAsState(targetValue = if (isPlaying) 1.06f else 1f, animationSpec = if (isPlaying) infiniteRepeatable(tween(1200, easing = FastOutSlowInEasing), RepeatMode.Reverse) else tween(300), label = "beat")
        Surface(modifier = Modifier.size(100.dp).graphicsLayer { scaleX = beatScale; scaleY = beatScale }.drawBehind { drawCircle(brush = Brush.radialGradient(listOf(themeColor.copy(alpha = 0.4f), Color.Transparent)), radius = size.minDimension * 0.9f) }.clip(CircleShape).clickable(interactionSource = remember { MutableInteractionSource() }, indication = ripple(color = themeColor, bounded = true)) { onClick() }, color = Color.White.copy(alpha = 0.15f), shape = CircleShape, border = BorderStroke(2.dp, Color.White.copy(alpha = 0.3f))) {
            Box(contentAlignment = Alignment.Center) { Icon(painter = painterResource(if (isPlaying) R.drawable.pause else R.drawable.play), contentDescription = null, modifier = Modifier.size(46.dp), tint = Color.White) }
        }
    }

    @Composable
    fun MoodAwareRing(isPlaying: Boolean, mood: SongMood, themeColor: Color) {
        val infiniteTransition = rememberInfiniteTransition(label = "mood_visuals_upgraded")
        
        val duration = when (mood) {
            SongMood.PHONK -> 1200 // Super fast
            SongMood.LOFI -> 15000 // Extremely slow
            SongMood.POP -> 3500  // Upbeat
            else -> 6000          // Elegant
        }
        
        val rotation by infiniteTransition.animateFloat(
            initialValue = 0f, targetValue = 360f,
            animationSpec = infiniteRepeatable(animation = tween(duration, easing = LinearEasing)),
            label = "rotation"
        )
        
        val colors = when (mood) {
            SongMood.PHONK -> listOf(Color.Cyan, Color.White, Color.Magenta, Color.Cyan)
            SongMood.LOFI -> listOf(Color(0xFF8B5CF6), Color(0xFFFBBF24), Color(0xFF8B5CF6))
            SongMood.POP -> listOf(Color.Cyan, Color(0xFFFF00FF), Color.Yellow, Color.Cyan)
            else -> listOf(Color(0xFF8B5CF6), Color(0xFF3B82F6), Color(0xFF06B6D4), Color(0xFF10B981), Color(0xFFFBBF24), Color(0xFFF59E0B), Color(0xFFEC4899), Color(0xFF8B5CF6))
        }

        val beatScale by animateFloatAsState(
            targetValue = if (isPlaying) {
                when (mood) {
                    SongMood.PHONK -> 1.12f // Aggressive pump
                    SongMood.POP -> 1.08f   // Energetic pulse
                    SongMood.LOFI -> 1.02f  // Gentle breath
                    else -> 1.05f
                }
            } else 1f,
            animationSpec = when (mood) {
                SongMood.PHONK -> spring(dampingRatio = 0.2f, stiffness = Spring.StiffnessHigh)
                else -> spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMedium)
            },
            label = "beat"
        )

        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(340.dp)) {
            Canvas(modifier = Modifier.fillMaxSize().graphicsLayer { 
                rotationZ = rotation
                scaleX = beatScale
                scaleY = beatScale
            }) {
                val strokeWidth = when(mood) {
                    SongMood.LOFI -> 6.dp.toPx() // Thick & soft
                    SongMood.PHONK -> 2.dp.toPx() // Sharp & thin
                    else -> 3.5.dp.toPx()
                }
                
                // Extra bloom for PHONK
                if (mood == SongMood.PHONK) {
                    drawCircle(brush = Brush.sweepGradient(colors), style = Stroke(width = strokeWidth * 6f), alpha = 0.5f)
                }

                drawCircle(brush = Brush.sweepGradient(colors), style = Stroke(width = strokeWidth * 2.5f), alpha = 0.35f)
                drawCircle(brush = Brush.sweepGradient(colors), style = Stroke(width = strokeWidth, cap = StrokeCap.Round))
            }
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
}
