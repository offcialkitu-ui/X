package iad1tya.echo.music.constants

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey

// ─── Lock Screen Visualizer Config ─────────────────────────────────────────
val LockScreenVisualizerTypeKey = stringPreferencesKey("lockscreen_visualizer_type")
val LockScreenVisualizerColorKey = intPreferencesKey("lockscreen_visualizer_color")
val LockScreenVisualizerIntensityKey = floatPreferencesKey("lockscreen_visualizer_intensity")
val LockScreenVisualizerSpeedKey = floatPreferencesKey("lockscreen_visualizer_speed")

enum class LockScreenVisualizerType(val label: String, val description: String) {
    MOOD_AUTO("Auto (Mood)", "Matches the song's mood — Phonk/LoFi/Pop/Default"),
    CIRCULAR_WAVE("Circular Wave", "Concentric ripple rings expanding from the artwork"),
    AURORA_NEBULA("Aurora Nebula", "Flowing aurora borealis colors across the screen"),
    EDGE_LIGHTING("Edge Lighting", "Screen-edge glow that responds to the music"),
    FIRE_PARTICLES("Fire Particles", "Rising glowing particles like embers"),
    RAIN_DROP("Rain Drops", "Calming rain effect that reacts to audio"),
    TYPOGRAPHIC("Typographic Art", "Animated song title typography"),
    WARP_SPEED("Warp Speed", "Star-field tunnel effect"),
    MATRIX_RAIN("Matrix Rain", "Digital rain code effect in theme colors"),
}

// ─── Lock Screen Theme Config ──────────────────────────────────────────────
val LockScreenThemeModeKey = stringPreferencesKey("lockscreen_theme_mode")
val LockScreenCustomPrimaryColorKey = intPreferencesKey("lockscreen_custom_primary_color")
val LockScreenCustomSecondaryColorKey = intPreferencesKey("lockscreen_custom_secondary_color")
val LockScreenGradientStartKey = intPreferencesKey("lockscreen_gradient_start")
val LockScreenGradientEndKey = intPreferencesKey("lockscreen_gradient_end")
val LockScreenBlurIntensityKey = floatPreferencesKey("lockscreen_blur_intensity")
val LockScreenArtworkOpacityKey = floatPreferencesKey("lockscreen_artwork_opacity")

enum class LockScreenThemeMode(val label: String) {
    DYNAMIC("Dynamic (Album Art)"),
    MATERIAL_YOU("Material You"),
    GRADIENT("Custom Gradient"),
    SOLID_COLOR("Solid Color"),
    PURE_BLACK("Pure Black"),
    GLASSMORPHISM("Glassmorphism"),
    SOLAR_DYNAMIC("Solar Dynamic"),
}

// ─── Lock Screen Lyrics Config ─────────────────────────────────────────────
val LockScreenShowLyricsKey = booleanPreferencesKey("lockscreen_show_lyrics")
val LockScreenLyricsPositionKey = stringPreferencesKey("lockscreen_lyrics_position")
val LockScreenLyricsFontSizeKey = floatPreferencesKey("lockscreen_lyrics_font_size")
val LockScreenLyricsKaraokeKey = booleanPreferencesKey("lockscreen_lyrics_karaoke")

enum class LockScreenLyricsPosition(val label: String) {
    BOTTOM("Bottom (below controls)"),
    OVERLAY("Overlay (on top of artwork)"),
    FULLSCREEN("Fullscreen (cover entire lock screen)"),
}

// ─── Lock Screen Layout Config ─────────────────────────────────────────────
val LockScreenShowClockKey = booleanPreferencesKey("lockscreen_show_clock")
val LockScreenShowVolumeSliderKey = booleanPreferencesKey("lockscreen_show_volume_slider")
val LockScreenShowWaveformKey = booleanPreferencesKey("lockscreen_show_waveform")
val LockScreenShowSwipeHintKey = booleanPreferencesKey("lockscreen_show_swipe_hint")
val LockScreenQuickControlsKey = stringPreferencesKey("lockscreen_quick_controls")

enum class LockScreenQuickControl(val id: String, val icon: Int, val label: String) {
    LIKE("like", 0, "Like"),
    SHUFFLE("shuffle", 0, "Shuffle"),
    REPEAT("repeat", 0, "Repeat"),
    CAST("cast", 0, "Cast"),
    SLEEP_TIMER("sleep_timer", 0, "Sleep Timer"),
    SHARE("share", 0, "Share"),
    EQUALIZER("equalizer", 0, "Equalizer"),
    LYRICS("lyrics", 0, "Lyrics"),
}