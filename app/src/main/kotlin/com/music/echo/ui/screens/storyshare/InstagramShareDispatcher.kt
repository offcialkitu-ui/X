package iad1tya.echo.music.ui.screens.storyshare

import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore

object InstagramShareDispatcher {

    private const val IG_PACKAGE = "com.instagram.android"
    private const val STORY_ACTION = "com.instagram.share.ADD_TO_STORY"

    // From Meta Developer Console. REQUIRED — modern IG silently drops the intent without it.
    private const val META_APP_ID = "YOUR_META_APP_ID"

    // Gate behind remote config. content_url only works for Meta-approved music partners;
    // do NOT promise swipe-up in UX copy until approval exists.
    var contentUrlEnabled: Boolean = false

    /**
     * Fallback chain: IG Story (sticker) → IG Feed → system share sheet → save to gallery.
     * Every rung still delivers value; there is no dead-end error state.
     */
    fun share(
        activity: Activity,
        cardUri: Uri,
        topHex: String,
        bottomHex: String,
        shareUrl: String,
    ): ShareDestination {
        if (tryStory(activity, cardUri, topHex, bottomHex, shareUrl)) return ShareDestination.INSTAGRAM_STORY
        if (tryFeed(activity, cardUri)) return ShareDestination.INSTAGRAM_FEED
        if (tryShareSheet(activity, cardUri)) return ShareDestination.SHARE_SHEET
        if (saveToGallery(activity, cardUri)) return ShareDestination.SAVED_TO_GALLERY
        return ShareDestination.FAILED
    }

    private fun tryStory(
        activity: Activity, uri: Uri, topHex: String, bottomHex: String, shareUrl: String,
    ): Boolean {
        val intent = Intent(STORY_ACTION).apply {
            putExtra("source_application", META_APP_ID)
            type = "image/png"
            putExtra("interactive_asset_uri", uri)      // card = movable/resizable sticker
            putExtra("top_background_color", topHex)    // sampled from card's top 10%
            putExtra("bottom_background_color", bottomHex)
            if (contentUrlEnabled && shareUrl.isNotBlank()) putExtra("content_url", shareUrl)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        // Explicit grant: the sticker travels via EXTRA, not intent data,
        // so IG can't get the permission implicitly.
        activity.grantUriPermission(IG_PACKAGE, uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)

        return if (activity.packageManager.resolveActivity(intent, 0) != null) {
            activity.startActivity(intent); true
        } else false
    }

    private fun tryFeed(activity: Activity, uri: Uri): Boolean {
        val intent = Intent(Intent.ACTION_SEND).apply {
            setPackage(IG_PACKAGE)
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        return if (activity.packageManager.resolveActivity(intent, 0) != null) {
            activity.startActivity(intent); true
        } else false
    }

    private fun tryShareSheet(activity: Activity, uri: Uri): Boolean = runCatching {
        activity.startActivity(Intent.createChooser(
            Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }, "Share your sound"))
    }.isSuccess

    private fun saveToGallery(activity: Activity, uri: Uri): Boolean = runCatching {
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "melodyx_story_${System.currentTimeMillis()}.png")
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/MelodyX")
        }
        val dest = activity.contentResolver.insert(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: return false
        activity.contentResolver.openOutputStream(dest)!!.use { out ->
            activity.contentResolver.openInputStream(uri)!!.use { it.copyTo(out) }
        }
    }.isSuccess
}
