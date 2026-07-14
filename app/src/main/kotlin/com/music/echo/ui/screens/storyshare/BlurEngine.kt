package iad1tya.echo.music.ui.screens.storyshare

import android.graphics.Bitmap
import androidx.core.graphics.scale

/**
 * We bake blurs into bitmaps BEFORE composition instead of using RenderEffect,
 * because RenderEffect only renders on a hardware canvas — and our export path
 * draws into a software Bitmap canvas. Downscale→upscale with bilinear filtering
 * approximates a massive gaussian blur at near-zero cost.
 */
object BlurEngine {
    /** intensity 1..64 — higher = blurrier. 48 ≈ the spec's "150dp" mega-blur. */
    fun megaBlur(src: Bitmap, intensity: Int = 48): Bitmap {
        val w = (src.width / intensity).coerceAtLeast(8)
        val h = (src.height / intensity).coerceAtLeast(8)
        val tiny = src.scale(w, h)                       // destroys high frequencies
        val out = tiny.scale(src.width, src.height)      // bilinear up = smooth blur
        // tiny.recycle() // Removed to avoid potential issues if scale returns same bitmap
        return out
    }
}
