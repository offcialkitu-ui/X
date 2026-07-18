package iad1tya.echo.music.ui.screens.storyshare

import android.graphics.Bitmap

/** Everything the story engine needs. Pure data holder — no app internals. */
data class SongShareData(
    val songId: String,
    val title: String,
    val artist: String,
    val explicit: Boolean = false,
    val albumArt: Bitmap?,            // null → branded fallback card
    val shareUrl: String = "",        // content_url (Meta partner-approved accounts only)
)

enum class StoryTemplate(val label: String) {
    LIQUID_GLASS("Liquid Glass"),
    MOOD_PULSE("Mood Pulse"),
    VINYL_CLASSIC("Vinyl Classic"),
}

/** Where the share actually ended up — feed this to analytics. */
enum class ShareDestination { INSTAGRAM_STORY, INSTAGRAM_FEED, SHARE_SHEET, SAVED_TO_GALLERY, FAILED }
