package iad1tya.echo.music.ui.screens.storyshare

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.Uri
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.android.awaitFrame
import kotlinx.coroutines.withContext
import java.io.File

object CardRenderer {
    // 1080×1920 export designed on a 360dp grid → density 3.0 makes dp math exact.
    const val EXPORT_W = 1080
    const val EXPORT_H = 1920
    private val EXPORT_DENSITY = Density(3f)
    private const val CACHE_DIR = "story_share"
    private const val MAX_AGE_MS = 24 * 60 * 60 * 1000L

    /**
     * Renders [content] off-screen at exactly 1080×1920 regardless of device density,
     * and returns a FileProvider URI to the PNG.
     *
     * Must be called from a RESUMED ComponentActivity (the invisible host view
     * inherits the activity's ViewTree lifecycle owners — required by ComposeView).
     * NEVER screenshots visible UI: pixel-identical output on every device.
     */
    suspend fun renderToUri(activity: Activity, content: @Composable () -> Unit): Uri {
        val bitmap = renderToBitmap(activity, content)
        return withContext(Dispatchers.IO) {          // encode OFF the main thread
            gcOldFiles(activity)
            val dir = File(activity.cacheDir, CACHE_DIR).apply { mkdirs() }
            val file = File(dir, "melodyx_story_${System.currentTimeMillis()}.png")
            file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            bitmap.recycle()
            FileProvider.getUriForFile(activity, "${activity.packageName}.FileProvider", file)
        }
    }

    private suspend fun renderToBitmap(activity: Activity, content: @Composable () -> Unit): Bitmap {
        val composeView = ComposeView(activity).apply {
            setContent {
                CompositionLocalProvider(LocalDensity provides EXPORT_DENSITY) { content() }
            }
        }
        val host = FrameLayout(activity).apply {
            visibility = View.INVISIBLE     // laid out & composed, never painted to screen
            addView(composeView, EXPORT_W, EXPORT_H)
        }
        val root = activity.window.decorView as ViewGroup
        root.addView(host, EXPORT_W, EXPORT_H)
        try {
            awaitFrame(); awaitFrame()      // let composition + layout settle
            return runCatching { draw(composeView) }.getOrElse { oom ->
                if (oom !is OutOfMemoryError) throw oom
                draw(composeView, scale = 0.75f)   // low-RAM retry: 810×1440
            }
        } finally {
            root.removeView(host)
        }
    }

    private fun draw(view: View, scale: Float = 1f): Bitmap {
        val bmp = Bitmap.createBitmap(
            (EXPORT_W * scale).toInt(), (EXPORT_H * scale).toInt(), Bitmap.Config.ARGB_8888
        )
        Canvas(bmp).apply { if (scale != 1f) scale(scale, scale) }.let { view.draw(it) }
        return bmp
    }

    private fun gcOldFiles(activity: Activity) {
        val cutoff = System.currentTimeMillis() - MAX_AGE_MS
        File(activity.cacheDir, CACHE_DIR).listFiles()
            ?.filter { it.lastModified() < cutoff }?.forEach { it.delete() }
    }
}
