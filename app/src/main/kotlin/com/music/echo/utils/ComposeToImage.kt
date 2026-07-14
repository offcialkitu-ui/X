

package iad1tya.echo.music.utils

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.FileProvider
import androidx.core.graphics.createBitmap
import androidx.core.graphics.drawable.toBitmap
import androidx.palette.graphics.Palette
import coil3.ImageLoader
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.toBitmap
import iad1tya.echo.music.R
import iad1tya.echo.music.ui.component.LyricsBackgroundStyle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.math.roundToInt

object ComposeToImage {

    suspend fun createLyricsImage(
        context: Context,
        coverArtUrl: String?,
        songTitle: String,
        artistName: String,
        lyrics: String,
        width: Int,
        height: Int,
        backgroundColor: Int? = null,
        backgroundStyle: LyricsBackgroundStyle = LyricsBackgroundStyle.SOLID,
        textColor: Int? = null,
        secondaryTextColor: Int? = null,
        lyricsAlignment: Layout.Alignment = Layout.Alignment.ALIGN_CENTER
    ): Bitmap = withContext(Dispatchers.Default) {
        
        
        val imageWidth = 2160
        val imageHeight = 2160
        
        val bitmap = createBitmap(imageWidth, imageHeight)
        val canvas = Canvas(bitmap)

        val defaultBackgroundColor = 0xFF121212.toInt()
        val defaultTextColor = 0xFFFFFFFF.toInt()
        val defaultSecondaryTextColor = 0xB3FFFFFF.toInt()

        val bgColor = backgroundColor ?: defaultBackgroundColor
        val mainTextColor = textColor ?: defaultTextColor
        val secondaryTxtColor = secondaryTextColor ?: defaultSecondaryTextColor

        
        var coverArtBitmap: Bitmap? = null
        if (coverArtUrl != null) {
            try {
                val imageLoader = ImageLoader(context)
                val request = ImageRequest.Builder(context)
                    .data(coverArtUrl)
                    .size(1024) 
                    .allowHardware(false)
                    .build()
                val result = imageLoader.execute(request)
                coverArtBitmap = result.image?.toBitmap()
            } catch (_: Exception) {}
        }

        
        val backgroundRect = RectF(0f, 0f, imageWidth.toFloat(), imageHeight.toFloat())
        val backgroundPaint = Paint().apply {
            isAntiAlias = true
        }

        when (backgroundStyle) {
            LyricsBackgroundStyle.SOLID -> {
                backgroundPaint.color = bgColor
                canvas.drawRect(backgroundRect, backgroundPaint)
            }
            LyricsBackgroundStyle.BLUR -> {
                
                backgroundPaint.color = 0xFF000000.toInt()
                canvas.drawRect(backgroundRect, backgroundPaint)

                if (coverArtBitmap != null) {
                    try {
                        
                        val scaledBitmap = Bitmap.createScaledBitmap(coverArtBitmap, imageWidth / 10, imageHeight / 10, true)
                        val blurredBitmap = fastBlur(scaledBitmap, 1f, 20) 
                        
                        if (blurredBitmap != null) {
                            val blurRect = RectF(0f, 0f, imageWidth.toFloat(), imageHeight.toFloat())
                            canvas.drawBitmap(blurredBitmap, null, blurRect, null)
                            
                            
                            val overlayPaint = Paint().apply {
                                color = 0x4D000000.toInt() 
                            }
                            canvas.drawRect(blurRect, overlayPaint)
                        }
                    } catch (e: Exception) {
                        
                        backgroundPaint.color = bgColor
                        canvas.drawRect(backgroundRect, backgroundPaint)
                    }
                } else {
                    backgroundPaint.color = bgColor
                    canvas.drawRect(backgroundRect, backgroundPaint)
                }
            }
            LyricsBackgroundStyle.GRADIENT -> {
                if (coverArtBitmap != null) {
                    val palette = Palette.from(coverArtBitmap).generate()
                    val vibrant = palette.getVibrantColor(bgColor)
                    val darkVibrant = palette.getDarkVibrantColor(bgColor)
                    
                    val gradient = LinearGradient(
                        0f, 0f, imageWidth.toFloat(), imageHeight.toFloat(),
                        intArrayOf(vibrant, darkVibrant),
                        null,
                        Shader.TileMode.CLAMP
                    )
                    backgroundPaint.shader = gradient
                    canvas.drawRect(backgroundRect, backgroundPaint)
                } else {
                    backgroundPaint.color = bgColor
                    canvas.drawRect(backgroundRect, backgroundPaint)
                }
            }
        }
        
        
        
        val scale = imageWidth / 340f
        
        val cornerRadius = 20f * scale

        
        val borderPaint = Paint().apply {
            color = mainTextColor
            alpha = (255 * 0.09).toInt()
            style = Paint.Style.STROKE
            strokeWidth = 1f * scale
            isAntiAlias = true
        }
        canvas.drawRoundRect(backgroundRect, cornerRadius, cornerRadius, borderPaint)

        val padding = 28f * scale
        
        
        val coverArtSize = 64f * scale
        val headerBottomPadding = 12f * scale
        
        val coverCornerRadius = 3f * scale
        coverArtBitmap?.let {
            val rect = RectF(padding, padding, padding + coverArtSize, padding + coverArtSize)
            val path = Path().apply {
                addRoundRect(rect, coverCornerRadius, coverCornerRadius, Path.Direction.CW)
            }
            
            
            val coverBorderPaint = Paint().apply {
                color = mainTextColor
                alpha = (255 * 0.16).toInt()
                style = Paint.Style.STROKE
                strokeWidth = 1f * scale
                isAntiAlias = true
            }

            canvas.save()
            canvas.clipPath(path)
            canvas.drawBitmap(it, null, rect, null)
            canvas.restore()
            canvas.drawRoundRect(rect, coverCornerRadius, coverCornerRadius, coverBorderPaint)
        }

        val textStartX = padding + coverArtSize + (16f * scale)
        val textMaxWidth = imageWidth - textStartX - padding
        
        val titlePaint = TextPaint().apply {
            color = mainTextColor
            textSize = 20f * scale
            typeface = Typeface.DEFAULT_BOLD
            isAntiAlias = true
        }
        
        val artistPaint = TextPaint().apply {
            color = secondaryTxtColor
            textSize = 16f * scale
            typeface = Typeface.DEFAULT
            isAntiAlias = true
        }

        val titleLayout = StaticLayout.Builder.obtain(songTitle, 0, songTitle.length, titlePaint, textMaxWidth.toInt())
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setMaxLines(1)
            .setEllipsize(android.text.TextUtils.TruncateAt.END)
            .build()
            
        val artistLayout = StaticLayout.Builder.obtain(artistName, 0, artistName.length, artistPaint, textMaxWidth.toInt())
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setMaxLines(1)
            .setEllipsize(android.text.TextUtils.TruncateAt.END)
            .build()

        
        val headerTextHeight = titleLayout.height + artistLayout.height + (2f * scale) 
        val headerCenterY = padding + coverArtSize / 2f
        val titleY = headerCenterY - headerTextHeight / 2f
        
        canvas.save()
        canvas.translate(textStartX, titleY)
        titleLayout.draw(canvas)
        canvas.translate(0f, titleLayout.height.toFloat() + (2f * scale))
        artistLayout.draw(canvas)
        canvas.restore()

        
        val logoBoxSize = 22f * scale
        val logoIconSize = 16f * scale
        val footerY = imageHeight - padding - logoBoxSize
        
        
        val logoBgPaint = Paint().apply {
            color = secondaryTxtColor
            isAntiAlias = true
        }
        val logoBoxRect = RectF(padding, footerY, padding + logoBoxSize, footerY + logoBoxSize)
        
        canvas.drawOval(logoBoxRect, logoBgPaint)
        
        
        val rawLogo = context.getDrawable(R.mipmap.ic_launcher)?.toBitmap()
        rawLogo?.let {
            val logoPaint = Paint().apply {
                isAntiAlias = true
            }
            
            
            val logoOffset = (logoBoxSize - logoIconSize) / 2f
            val logoRect = RectF(
                padding + logoOffset, 
                footerY + logoOffset, 
                padding + logoBoxSize - logoOffset, 
                footerY + logoBoxSize - logoOffset
            )
            canvas.drawBitmap(it, null, logoRect, logoPaint)
        }
        
        
        val appName = context.getString(R.string.app_name)
        val appNamePaint = TextPaint().apply {
            color = secondaryTxtColor
            textSize = 14f * scale
            typeface = Typeface.DEFAULT_BOLD
            isAntiAlias = true
        }
        
        val appNameX = padding + logoBoxSize + (8f * scale)
        
        val appNameY = footerY + logoBoxSize/2f - (appNamePaint.descent() + appNamePaint.ascent()) / 2f
        canvas.drawText(appName, appNameX, appNameY, appNamePaint)

        
        
        val lyricsTop = padding + coverArtSize + headerBottomPadding
        val lyricsBottom = footerY - (12f * scale) 
        val lyricsHeight = lyricsBottom - lyricsTop
        val lyricsWidth = imageWidth - (padding * 2)

        val lyricsPaint = TextPaint().apply {
            color = mainTextColor
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
            letterSpacing = 0.005f
        }

        
        
        var lyricsTextSize = 50f * scale 
        val minLyricsSize = 13f * scale 
        var lyricsLayout: StaticLayout

        while (lyricsTextSize > minLyricsSize) {
            lyricsPaint.textSize = lyricsTextSize
            lyricsLayout = StaticLayout.Builder.obtain(lyrics, 0, lyrics.length, lyricsPaint, lyricsWidth.toInt())
                .setAlignment(lyricsAlignment)
                .setLineSpacing(0f, 1.2f)
                .setIncludePad(false)
                .build()
            
            if (lyricsLayout.height <= lyricsHeight) {
                break
            }
            
            lyricsTextSize -= 1f * scale 
        }
        
        
        lyricsPaint.textSize = lyricsTextSize
        lyricsLayout = StaticLayout.Builder.obtain(lyrics, 0, lyrics.length, lyricsPaint, lyricsWidth.toInt())
            .setAlignment(lyricsAlignment)
            .setLineSpacing(0f, 1.2f)
            .setIncludePad(false)
            .build()

        
        val lyricsContentHeight = lyricsLayout.height
        val lyricsY = if (lyricsContentHeight < lyricsHeight) {
             lyricsTop + (lyricsHeight - lyricsContentHeight) / 2f
        } else {
            lyricsTop
        }

        canvas.save()
        canvas.translate(padding, lyricsY)
        lyricsLayout.draw(canvas)
        canvas.restore()

        return@withContext bitmap
    }

    
    
    
    
    
    
    
    
    suspend fun createSongShareImage(
        context: Context,
        coverArtUrl: String?,
        songTitle: String,
        artistName: String,
        backgroundColor: Int? = null,
        textColor: Int? = null
    ): Bitmap = withContext(Dispatchers.Default) {
        val imageWidth = 1080
        val imageHeight = 1920 // Portrait 9:16 for stories
        
        val bitmap = createBitmap(imageWidth, imageHeight)
        val canvas = Canvas(bitmap)

        val bgColor = backgroundColor ?: 0xFF121212.toInt()
        val mainTextColor = textColor ?: 0xFFFFFFFF.toInt()
        val secondaryTxtColor = (mainTextColor and 0x00FFFFFF) or 0xB3000000.toInt().inv().and(0xFF000000.toInt()).inv().and(0xB3000000.toInt()) // ~70% alpha
        
        // Correct way to get ~70% alpha white if main is white
        val secondaryTxtColorFixed = if (mainTextColor == 0xFFFFFFFF.toInt()) 0xB3FFFFFF.toInt() else mainTextColor

        // Draw background
        canvas.drawColor(bgColor)

        // Load cover art
        var coverArtBitmap: Bitmap? = null
        if (coverArtUrl != null) {
            try {
                val imageLoader = ImageLoader(context)
                val request = ImageRequest.Builder(context)
                    .data(coverArtUrl)
                    .size(1080)
                    .allowHardware(false)
                    .build()
                val result = imageLoader.execute(request)
                coverArtBitmap = result.image?.toBitmap()
            } catch (_: Exception) {}
        }

        // Draw Blurred Background if cover art exists
        if (coverArtBitmap != null) {
            val scaledBitmap = Bitmap.createScaledBitmap(coverArtBitmap, 108, 192, true)
            val blurredBitmap = fastBlur(scaledBitmap, 1f, 20)
            if (blurredBitmap != null) {
                canvas.drawBitmap(blurredBitmap, null, RectF(0f, 0f, imageWidth.toFloat(), imageHeight.toFloat()), null)
                // Dark overlay
                canvas.drawColor(0x66000000.toInt(), PorterDuff.Mode.SRC_OVER)
            }
        }

        val padding = 80f
        val cardWidth = imageWidth - (padding * 2)
        val cardHeight = cardWidth + 240f // Square art + space for text
        val cardTop = (imageHeight - cardHeight) / 2f
        val cardRect = RectF(padding, cardTop, imageWidth - padding, cardTop + cardHeight)

        // Draw Card Shadow/Glow
        val shadowPaint = Paint().apply {
            color = 0x44000000.toInt()
            isAntiAlias = true
        }
        canvas.drawRoundRect(RectF(padding + 10, cardTop + 10, imageWidth - padding + 10, cardTop + cardHeight + 10), 40f, 40f, shadowPaint)

        // Draw Card Background
        val cardPaint = Paint().apply {
            color = 0xFF1E1E1E.toInt()
            isAntiAlias = true
        }
        canvas.drawRoundRect(cardRect, 40f, 40f, cardPaint)

        // Draw Cover Art in Card
        coverArtBitmap?.let {
            val artPadding = 40f
            val artSize = cardWidth - (artPadding * 2)
            val artRect = RectF(padding + artPadding, cardTop + artPadding, imageWidth - padding - artPadding, cardTop + artPadding + artSize)
            val path = Path().apply {
                addRoundRect(artRect, 24f, 24f, Path.Direction.CW)
            }
            canvas.save()
            canvas.clipPath(path)
            canvas.drawBitmap(it, null, artRect, Paint(Paint.FILTER_BITMAP_FLAG))
            canvas.restore()
        }

        // Draw Text
        val textPadding = 60f
        val textTop = cardTop + (cardWidth - 20f) + 40f
        
        val titlePaint = TextPaint().apply {
            color = Color.White.toArgb()
            textSize = 64f
            typeface = Typeface.DEFAULT_BOLD
            isAntiAlias = true
        }
        
        val artistPaint = TextPaint().apply {
            color = 0xB3FFFFFF.toInt()
            textSize = 48f
            typeface = Typeface.DEFAULT
            isAntiAlias = true
        }

        val titleLayout = StaticLayout.Builder.obtain(songTitle, 0, songTitle.length, titlePaint, (cardWidth - textPadding * 2).toInt())
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setMaxLines(1)
            .setEllipsize(android.text.TextUtils.TruncateAt.END)
            .build()
            
        val artistLayout = StaticLayout.Builder.obtain(artistName, 0, artistName.length, artistPaint, (cardWidth - textPadding * 2).toInt())
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setMaxLines(1)
            .setEllipsize(android.text.TextUtils.TruncateAt.END)
            .build()

        canvas.save()
        canvas.translate(padding + textPadding, textTop)
        titleLayout.draw(canvas)
        canvas.translate(0f, titleLayout.height.toFloat() + 10f)
        artistLayout.draw(canvas)
        canvas.restore()

        // Brand Footer
        val brandPaint = TextPaint().apply {
            color = 0x80FFFFFF.toInt()
            textSize = 36f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
            letterSpacing = 0.1f
        }
        val brandText = context.getString(R.string.app_name).uppercase()
        val brandWidth = brandPaint.measureText(brandText)
        canvas.drawText(brandText, (imageWidth - brandWidth) / 2f, imageHeight - 100f, brandPaint)

        return@withContext bitmap
    }

    private fun fastBlur(sentBitmap: Bitmap, scale: Float, radius: Int): Bitmap? {
        val width = (sentBitmap.width * scale).roundToInt()
        val height = (sentBitmap.height * scale).roundToInt()
        
        if (width <= 0 || height <= 0) return null
        
        val bitmap = Bitmap.createScaledBitmap(sentBitmap, width, height, false)
        val w = bitmap.width
        val h = bitmap.height
        val pix = IntArray(w * h)
        bitmap.getPixels(pix, 0, w, 0, 0, w, h)
        val wm = w - 1
        val hm = h - 1
        val wh = w * h
        val div = radius + radius + 1
        val r = IntArray(wh)
        val g = IntArray(wh)
        val b = IntArray(wh)
        var rsum: Int
        var gsum: Int
        var bsum: Int
        var x: Int
        var y: Int
        var i: Int
        var p: Int
        var yp: Int
        var yi: Int
        var yw: Int
        val vmin = IntArray(Math.max(w, h))
        var divsum = div + 1 shr 1
        divsum *= divsum
        val dv = IntArray(256 * divsum)
        i = 0
        while (i < 256 * divsum) {
            dv[i] = i / divsum
            i++
        }
        yw = 0
        yi = 0
        val stack = Array(div) { IntArray(3) }
        var stackpointer: Int
        var stackstart: Int
        var sir: IntArray
        var rbs: Int
        var r1 = radius + 1
        var routsum: Int
        var goutsum: Int
        var boutsum: Int
        var rinsum: Int
        var ginsum: Int
        var binsum: Int
        y = 0
        while (y < h) {
            bsum = 0
            gsum = 0
            rsum = 0
            boutsum = 0
            goutsum = 0
            routsum = 0
            binsum = 0
            ginsum = 0
            rinsum = 0
            i = -radius
            while (i <= radius) {
                p = pix[yi + Math.min(wm, Math.max(i, 0))]
                sir = stack[i + radius]
                sir[0] = p and 0xff0000 shr 16
                sir[1] = p and 0x00ff00 shr 8
                sir[2] = p and 0x0000ff
                rbs = r1 - Math.abs(i)
                rsum += sir[0] * rbs
                gsum += sir[1] * rbs
                bsum += sir[2] * rbs
                if (i > 0) {
                    rinsum += sir[0]
                    ginsum += sir[1]
                    binsum += sir[2]
                } else {
                    routsum += sir[0]
                    goutsum += sir[1]
                    boutsum += sir[2]
                }
                i++
            }
            stackpointer = radius
            x = 0
            while (x < w) {
                r[yi] = dv[rsum]
                g[yi] = dv[gsum]
                b[yi] = dv[bsum]
                rsum -= routsum
                gsum -= goutsum
                bsum -= boutsum
                stackstart = stackpointer - radius + div
                sir = stack[stackstart % div]
                routsum -= sir[0]
                goutsum -= sir[1]
                boutsum -= sir[2]
                if (y == 0) {
                    vmin[x] = Math.min(x + radius + 1, wm)
                }
                p = pix[yw + vmin[x]]
                sir[0] = p and 0xff0000 shr 16
                sir[1] = p and 0x00ff00 shr 8
                sir[2] = p and 0x0000ff
                rinsum += sir[0]
                ginsum += sir[1]
                binsum += sir[2]
                rsum += rinsum
                gsum += ginsum
                bsum += binsum
                stackpointer = (stackpointer + 1) % div
                sir = stack[stackpointer % div]
                routsum += sir[0]
                goutsum += sir[1]
                boutsum += sir[2]
                rinsum -= sir[0]
                ginsum -= sir[1]
                binsum -= sir[2]
                yi++
                x++
            }
            yw += w
            y++
        }
        x = 0
        while (x < w) {
            bsum = 0
            gsum = 0
            rsum = 0
            boutsum = 0
            goutsum = 0
            routsum = 0
            binsum = 0
            ginsum = 0
            rinsum = 0
            yp = -radius * w
            i = -radius
            while (i <= radius) {
                yi = Math.max(0, yp) + x
                sir = stack[i + radius]
                sir[0] = r[yi]
                sir[1] = g[yi]
                sir[2] = b[yi]
                rbs = r1 - Math.abs(i)
                rsum += sir[0] * rbs
                gsum += sir[1] * rbs
                bsum += sir[2] * rbs
                if (i > 0) {
                    rinsum += sir[0]
                    ginsum += sir[1]
                    binsum += sir[2]
                } else {
                    routsum += sir[0]
                    goutsum += sir[1]
                    boutsum += sir[2]
                }
                if (i < hm) {
                    yp += w
                }
                i++
            }
            yi = x
            stackpointer = radius
            y = 0
            while (y < h) {
                pix[yi] = -0x1000000 or (dv[rsum] shl 16) or (dv[gsum] shl 8) or dv[bsum]
                rsum -= routsum
                gsum -= goutsum
                bsum -= boutsum
                stackstart = stackpointer - radius + div
                sir = stack[stackstart % div]
                routsum -= sir[0]
                goutsum -= sir[1]
                boutsum -= sir[2]
                if (x == 0) {
                    vmin[y] = Math.min(y + r1, hm) * w
                }
                p = x + vmin[y]
                sir[0] = r[p]
                sir[1] = g[p]
                sir[2] = b[p]
                rinsum += sir[0]
                ginsum += sir[1]
                binsum += sir[2]
                rsum += rinsum
                gsum += ginsum
                bsum += binsum
                stackpointer = (stackpointer + 1) % div
                sir = stack[stackpointer % div]
                routsum += sir[0]
                goutsum += sir[1]
                boutsum += sir[2]
                rinsum -= sir[0]
                ginsum -= sir[1]
                binsum -= sir[2]
                yi += w
                y++
            }
            x++
        }
        bitmap.setPixels(pix, 0, w, 0, 0, w, h)
        return bitmap
    }

    fun saveBitmapAsFile(context: Context, bitmap: Bitmap, fileName: String): Uri {
        val shareDir = File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "share")
        shareDir.mkdirs()
        val imageFile = File(shareDir, "$fileName.png")
        FileOutputStream(imageFile).use { outputStream ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
        }
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.FileProvider",
            imageFile
        )
    }

    suspend fun getBitmapFromUrl(context: Context, url: String, size: Int = 1024): Bitmap? = withContext(Dispatchers.IO) {
        try {
            val loader = ImageLoader(context)
            val request = ImageRequest.Builder(context)
                .data(url)
                .size(size)
                .allowHardware(false)
                .build()
            val result = loader.execute(request)
            result.image?.toBitmap()
        } catch (e: Exception) {
            null
        }
    }
}