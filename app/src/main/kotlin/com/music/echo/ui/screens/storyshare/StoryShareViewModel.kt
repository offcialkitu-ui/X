package iad1tya.echo.music.ui.screens.storyshare

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import iad1tya.echo.music.utils.ComposeToImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface StoryShareUiState {
    data object Loading : StoryShareUiState
    data class Preview(
        val data: SongShareData,
        val palette: CardPalette,
        val blurredArt: Bitmap?,
        val selected: StoryTemplate = StoryTemplate.LIQUID_GLASS,
        val exporting: Boolean = false,
    ) : StoryShareUiState
}

class StoryShareViewModel : ViewModel() {

    private val _state = MutableStateFlow<StoryShareUiState>(StoryShareUiState.Loading)
    val state = _state.asStateFlow()

    /** Precompute palette + blur ONCE on open → template switching is instant (<100ms). */
    fun load(context: Context, songId: String, title: String, artist: String, thumbnailUrl: String, isExplicit: Boolean, shareUrl: String) {
        viewModelScope.launch {
            val bitmap = ComposeToImage.getBitmapFromUrl(context, thumbnailUrl)
            val data = SongShareData(songId, title, artist, isExplicit, bitmap, shareUrl)
            
            val palette = PaletteEngine.extract(data.songId, data.albumArt)
            val blurred = data.albumArt?.let {
                withContext(Dispatchers.Default) { BlurEngine.megaBlur(it) }
            }
            _state.value = StoryShareUiState.Preview(data, palette, blurred)
        }
    }

    fun selectTemplate(t: StoryTemplate) {
        (_state.value as? StoryShareUiState.Preview)?.let { _state.value = it.copy(selected = t) }
    }

    fun export(activity: Activity, onDone: (ShareDestination) -> Unit) {
        val s = _state.value as? StoryShareUiState.Preview ?: return
        if (s.exporting) return
        _state.value = s.copy(exporting = true)
        viewModelScope.launch {
            val uri = CardRenderer.renderToUri(activity) {
                StoryCard(s.data, s.palette, s.selected, s.blurredArt)
            }
            // bg colors sampled from the card's dominant top/bottom tones
            val dest = InstagramShareDispatcher.share(
                activity, uri,
                topHex = PaletteEngine.hexOf(s.palette.darkMuted),
                bottomHex = PaletteEngine.hexOf(s.palette.posterBg),
                shareUrl = s.data.shareUrl,
            )
            _state.value = s.copy(exporting = false)
            onDone(dest)
        }
    }
}
