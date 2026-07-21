package iad1tya.echo.music.extensions

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import android.graphics.Color as AndroidColor

val Color.hue: Float
    get() {
        val hsv = FloatArray(3)
        AndroidColor.colorToHSV(this.toArgb(), hsv)
        return hsv[0]
    }

val Color.saturation: Float
    get() {
        val hsv = FloatArray(3)
        AndroidColor.colorToHSV(this.toArgb(), hsv)
        return hsv[1]
    }

val Color.value: Float
    get() {
        val hsv = FloatArray(3)
        AndroidColor.colorToHSV(this.toArgb(), hsv)
        return hsv[2]
    }

fun Color.Companion.hsl(
    hue: Float,
    saturation: Float,
    lightness: Float,
    alpha: Float = 1f
): Color {
    val hsv = floatArrayOf(hue, saturation, lightness)
    val argb = AndroidColor.HSVToColor((alpha * 255).toInt(), hsv)
    return Color(argb)
}
