
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
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.*
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import dagger.hilt.android.AndroidEntryPoint
import iad1tya.echo.music.MainActivity
import iad1tya.echo.music.R
import iad1tya.echo.music.db.MusicDatabase
import iad1tya.echo.music.ui.theme.echomusicTheme
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.sin

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

    private var playerConnection: PlayerConnection? = null

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            if (service is MusicService.MusicBinder) {
                playerConnection = PlayerConnection(this@DynamicCapsuleService, service, database, lifecycleScope)
                updateComposeView()
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

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            y = 12
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

    private fun updateComposeView() {
        composeView?.setContent {
            CapsuleUI()
        }
    }

    @Composable
    private fun CapsuleUI() {
        val metadata by playerConnection?.mediaMetadata?.collectAsState() ?: remember { mutableStateOf(null) }
        val isPlaying by playerConnection?.isPlaying?.collectAsState() ?: remember { mutableStateOf(false) }
        
        var expanded by remember { mutableStateOf(false) }
        val width by animateDpAsState(targetValue = if (expanded) 280.dp else 160.dp, label = "width")
        val height by animateDpAsState(targetValue = if (expanded) 60.dp else 38.dp, label = "height")

        echomusicTheme(darkTheme = true, pureBlack = true) {
            Surface(
                modifier = Modifier
                    .width(width)
                    .height(height)
                    .clip(CircleShape)
                    .clickable { 
                        if (expanded) {
                            val intent = Intent(this@DynamicCapsuleService, MainActivity::class.java).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                            }
                            startActivity(intent)
                        }
                        expanded = !expanded
                    },
                color = Color.Black.copy(alpha = 0.95f),
                tonalElevation = 0.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Small Animated RGB Waveform
                    CapsuleWaveform(isPlaying)

                    Spacer(Modifier.width(10.dp))

                    // Track Name
                    Text(
                        text = metadata?.title ?: "MelodyX",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            fontSize = 13.sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    if (expanded) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { playerConnection?.seekToPrevious() }, modifier = Modifier.size(34.dp)) {
                                Icon(painterResource(R.drawable.skip_previous), null, tint = Color.White, modifier = Modifier.size(20.dp))
                            }
                            IconButton(onClick = { playerConnection?.togglePlayPause() }, modifier = Modifier.size(34.dp)) {
                                Icon(painterResource(if (isPlaying) R.drawable.pause else R.drawable.play), null, tint = Color.White, modifier = Modifier.size(20.dp))
                            }
                            IconButton(onClick = { playerConnection?.seekToNext() }, modifier = Modifier.size(34.dp)) {
                                Icon(painterResource(R.drawable.skip_next), null, tint = Color.White, modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun CapsuleWaveform(isPlaying: Boolean) {
        val infiniteTransition = rememberInfiniteTransition(label = "capsule_wave")
        val phase by infiniteTransition.animateFloat(
            initialValue = 0f, targetValue = 2 * Math.PI.toFloat(),
            animationSpec = infiniteRepeatable(animation = tween(1200, easing = LinearEasing)),
            label = "phase"
        )

        Canvas(modifier = Modifier.size(26.dp, 18.dp)) {
            val path = Path()
            val centerY = size.height / 2
            val width = size.width
            
            path.moveTo(0f, centerY)
            for (x in 0..width.toInt()) {
                val relX = x / width
                val amp = if (isPlaying) 7.dp.toPx() else 1.5.dp.toPx()
                val y = centerY + amp * sin(relX * 10f + phase) * sin(relX * Math.PI.toFloat())
                path.lineTo(x.toFloat(), y.toFloat())
            }

            drawPath(
                path = path,
                brush = Brush.linearGradient(listOf(Color.Cyan, Color(0xFF8B5CF6), Color.Magenta)),
                style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
            )
        }
    }

    override fun onDestroy() {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        composeView?.let {
            try {
                windowManager.removeView(it)
            } catch (e: Exception) {}
        }
        composeView = null
        try {
            unbindService(serviceConnection)
        } catch (e: Exception) {}
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
