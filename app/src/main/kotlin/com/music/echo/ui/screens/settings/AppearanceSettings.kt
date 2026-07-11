

package iad1tya.echo.music.ui.screens.settings

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.compose.foundation.border
import androidx.core.content.edit
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import iad1tya.echo.music.LocalPlayerAwareWindowInsets
import iad1tya.echo.music.R
import iad1tya.echo.music.constants.*
import iad1tya.echo.music.ui.component.*
import iad1tya.echo.music.ui.theme.DefaultThemeColor
import iad1tya.echo.music.ui.theme.PlayerSliderColors
import iad1tya.echo.music.ui.utils.backToMain
import iad1tya.echo.music.utils.*
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import androidx.compose.animation.core.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

@Composable
private fun PartyModeTitle() {
    val infiniteTransition = rememberInfiniteTransition(label = "party_glow")
    val colors = listOf(
        Color(0xFF8B5CF6), // Electric Purple
        Color(0xFF3B82F6), // Neon Blue
        Color(0xFF06B6D4), // Cyan
        Color(0xFFD946EF), // Magenta
        Color(0xFF8B5CF6)
    )
    
    val offset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(10000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "offset"
    )

    val brush = Brush.linearGradient(
        colors = colors,
        start = Offset(offset, offset),
        end = Offset(offset + 400f, offset + 400f),
        tileMode = TileMode.Mirror
    )

    Text(
        text = "PARTY MODE",
        style = MaterialTheme.typography.titleMedium.copy(
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 1.5.sp,
            brush = brush
        )
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppearanceSettings(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
    activity: Activity,
    snackbarHostState: SnackbarHostState,
    highlightKey: String? = null
) {
    val scrollState = rememberScrollState()
    val context = activity as Context
    val coroutineScope = rememberCoroutineScope()

    val (dynamicTheme, onDynamicThemeChange) = rememberPreference(DynamicThemeKey, true)
    val (enableHighRefreshRate, onEnableHighRefreshRateChange) = rememberPreference(EnableHighRefreshRateKey, true)
    val (selectedThemeColorInt) = rememberPreference(SelectedThemeColorKey, DefaultThemeColor.toArgb())
    val isUsingCustomColor = selectedThemeColorInt != DefaultThemeColor.toArgb()

    val (purpleTheme, onPurpleThemeChange) = rememberPreference(PurpleThemeKey, false)
    val (partyMode, onPartyModeChange) = rememberPreference(PartyModeKey, false)
    val (glassmorphismMode, onGlassmorphismModeChange) = rememberPreference(GlassmorphismModeKey, false)
    val (solarDynamicMode, onSolarDynamicModeChange) = rememberPreference(SolarDynamicModeKey, false)
    val (batteryProMode, onBatteryProModeChange) = rememberPreference(BatteryProModeKey, false)
    val (lockScreenOverlay, onLockScreenOverlayChange) = rememberPreference(LockScreenOverlayKey, true)
    val (dynamicCapsule, onDynamicCapsuleChange) = rememberPreference(DynamicCapsuleKey, true)

    val (useNewPlayerDesign, onUseNewPlayerDesignChange) = rememberPreference(UseNewPlayerDesignKey, true)
    val (showCodecOnPlayer, onShowCodecOnPlayerChange) = rememberPreference(ShowCodecOnPlayerKey, false)
    val (hidePlayerSlider, onHidePlayerSliderChange) = rememberPreference(HidePlayerSliderKey, false)
    val (hidePlayerThumbnail, onHidePlayerThumbnailChange) = rememberPreference(HidePlayerThumbnailKey, false)
    val (cropAlbumArt, onCropAlbumArtChange) = rememberPreference(CropAlbumArtKey, false)
    val (playerBackground, onPlayerBackgroundChange) = rememberEnumPreference(PlayerBackgroundStyleKey, PlayerBackgroundStyle.GRADIENT)
    val (miniPlayerBackground, onMiniPlayerBackgroundChange) = rememberEnumPreference(MiniPlayerBackgroundStyleKey, PlayerBackgroundStyle.DEFAULT)

    val (defaultOpenTab, onDefaultOpenTabChange) = rememberEnumPreference(DefaultOpenTabKey, NavigationTab.HOME)
    val (playerButtonsStyle, onPlayerButtonsStyleChange) = rememberEnumPreference(PlayerButtonsStyleKey, PlayerButtonsStyle.DEFAULT)
    val (lyricsPosition, onLyricsPositionChange) = rememberEnumPreference(LyricsTextPositionKey, LyricsPosition.LEFT)
    val (lyricsClick, onLyricsClickChange) = rememberPreference(LyricsClickKey, true)
    val (lyricsScroll, onLyricsScrollChange) = rememberPreference(LyricsScrollKey, true)
    val (lyricsAnimationStyle, onLyricsAnimationStyleChange) = rememberEnumPreference(LyricsAnimationStyleKey, LyricsAnimationStyle.echomusic_1)
    val (lyricsTextSize, onLyricsTextSizeChange) = rememberPreference(LyricsTextSizeKey, 24f)
    val (lyricsLineSpacing, onLyricsLineSpacingChange) = rememberPreference(LyricsLineSpacingKey, 1.3f)
    val (lyricsGlowEffect, onLyricsGlowEffectChange) = rememberPreference(LyricsGlowEffectKey, false)
    val (appleMusicLyricsBlur, onAppleMusicLyricsBlurChange) = rememberPreference(AppleMusicLyricsBlurKey, true)
    val (lyricsStandardBlur, onLyricsStandardBlurChange) = rememberPreference(LyricsStandardBlurKey, false)
    val (swipeLyrics, onSwipeLyricsChange) = rememberPreference(SwipeLyricsKey, false)
    val (enableLyricsThumbnailPlayPause, onEnableLyricsThumbnailPlayPauseChange) = rememberPreference(EnableLyricsThumbnailPlayPauseKey, false)
    val (hideStatusBarOnFullscreen, onHideStatusBarOnFullscreenChange) = rememberPreference(HideStatusBarOnFullscreenKey, false)

    val (sliderStyle, onSliderStyleChange) = rememberEnumPreference(SliderStyleKey, SliderStyle.DEFAULT)
    val (squigglySlider, onSquigglySliderChange) = rememberPreference(SquigglySliderKey, false)
    val (swipeThumbnail, onSwipeThumbnailChange) = rememberPreference(SwipeThumbnailKey, true)
    val (swipeSensitivity, onSwipeSensitivityChange) = rememberPreference(SwipeSensitivityKey, 0.73f)
    val (canvasThumbnailAnimation, onCanvasThumbnailAnimationChange) = rememberPreference(CanvasThumbnailAnimationKey, false)
    val (rotatingThumbnail, onRotatingThumbnailChange) = rememberPreference(RotatingThumbnailKey, false)
    val (gridItemSize, onGridItemSizeChange) = rememberEnumPreference(GridItemsSizeKey, GridItemSize.SMALL)

    val sharedPreferences = remember { context.getSharedPreferences("echomusic_settings", Context.MODE_PRIVATE) }
    val prefDensityScale = remember(sharedPreferences) { sharedPreferences.getFloat("density_scale_factor", 1.0f) }
    val (densityScale, setDensityScale) = rememberPreference(DensityScaleKey, prefDensityScale)
    var showRestartDialog by rememberSaveable { mutableStateOf(false) }
    var showDensityScaleDialog by rememberSaveable { mutableStateOf(false) }

    val onDensityScaleChange: (Float) -> Unit = { newScale ->
        setDensityScale(newScale)
        sharedPreferences.edit { putFloat("density_scale_factor", newScale) }
        showRestartDialog = true
    }

    val (listenTogetherInTopBar, onListenTogetherInTopBarChange) = rememberPreference(ListenTogetherInTopBarKey, true)
    val (swipeToSong, onSwipeToSongChange) = rememberPreference(SwipeToSongKey, false)
    val (swipeToRemoveSong, onSwipeToRemoveSongChange) = rememberPreference(SwipeToRemoveSongKey, false)

    val (showLikedPlaylist, onShowLikedPlaylistChange) = rememberPreference(ShowLikedPlaylistKey, true)
    val (showDownloadedPlaylist, onShowDownloadedPlaylistChange) = rememberPreference(ShowDownloadedPlaylistKey, true)
    val (showExportedPlaylist, onShowExportedPlaylistChange) = rememberPreference(ShowExportedPlaylistKey, true)
    val (showTopPlaylist, onShowTopPlaylistChange) = rememberPreference(ShowTopPlaylistKey, true)
    val (showCachedPlaylist, onShowCachedPlaylistChange) = rememberPreference(ShowCachedPlaylistKey, true)
    val (showCommentButton, onShowCommentButtonChange) = rememberPreference(ShowCommentButtonKey, false)

    val availableBackgroundStyles = PlayerBackgroundStyle.entries.filter {
        it != PlayerBackgroundStyle.BLUR || Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    }
    val availableMiniPlayerBackgroundStyles = availableBackgroundStyles.filter { 
        it != PlayerBackgroundStyle.APPLE_MUSIC && it != PlayerBackgroundStyle.GRADIENT
    }
    val (defaultChip, onDefaultChipChange) = rememberEnumPreference(key = ChipSortTypeKey, defaultValue = LibraryFilter.LIBRARY)

    var showSliderOptionDialog by rememberSaveable { mutableStateOf(false) }
    var showPlayerBackgroundDialog by rememberSaveable { mutableStateOf(false) }
    var showMiniPlayerBackgroundDialog by rememberSaveable { mutableStateOf(false) }
    var showPlayerButtonsStyleDialog by rememberSaveable { mutableStateOf(false) }
    var showLyricsPositionDialog by rememberSaveable { mutableStateOf(false) }
    var showLyricsAnimationStyleDialog by rememberSaveable { mutableStateOf(false) }
    var showLyricsTextSizeDialog by rememberSaveable { mutableStateOf(false) }
    var showLyricsLineSpacingDialog by rememberSaveable { mutableStateOf(false) }
    var showSensitivityDialog by rememberSaveable { mutableStateOf(false) }
    var showThumbnailCornerRadiusDialog by rememberSaveable { mutableStateOf(false) }
    var showDefaultOpenTabDialog by rememberSaveable { mutableStateOf(false) }
    var showDefaultChipDialog by rememberSaveable { mutableStateOf(false) }
    var showGridSizeDialog by rememberSaveable { mutableStateOf(false) }

    if (showLyricsPositionDialog) {
        EnumDialog(
            onDismiss = { showLyricsPositionDialog = false },
            onSelect = { onLyricsPositionChange(it); showLyricsPositionDialog = false },
            title = stringResource(R.string.lyrics_text_position),
            current = lyricsPosition,
            values = LyricsPosition.values().toList(),
            valueText = {
                when (it) {
                    LyricsPosition.LEFT -> stringResource(R.string.left)
                    LyricsPosition.CENTER -> stringResource(R.string.center)
                    LyricsPosition.RIGHT -> stringResource(R.string.right)
                }
            }
        )
    }

    if (showLyricsAnimationStyleDialog) {
        EnumDialog(
            onDismiss = { showLyricsAnimationStyleDialog = false },
            onSelect = { onLyricsAnimationStyleChange(it); showLyricsAnimationStyleDialog = false },
            title = stringResource(R.string.lyrics_animation_style),
            current = lyricsAnimationStyle,
            values = LyricsAnimationStyle.values().toList(),
            valueText = {
                when (it) {
                    LyricsAnimationStyle.NONE -> stringResource(R.string.none)
                    LyricsAnimationStyle.FADE -> stringResource(R.string.fade)
                    LyricsAnimationStyle.GLOW -> stringResource(R.string.glow)
                    LyricsAnimationStyle.SLIDE -> stringResource(R.string.slide)
                    LyricsAnimationStyle.KARAOKE -> stringResource(R.string.karaoke)
                    LyricsAnimationStyle.APPLE -> stringResource(R.string.apple_music_style)
                    LyricsAnimationStyle.APPLE_V2 -> stringResource(R.string.apple_music_style_letter)
                    LyricsAnimationStyle.echomusic_1 -> stringResource(R.string.echomusic_1)
                    LyricsAnimationStyle.LYRICS_V2 -> stringResource(R.string.lyrics_v2_fluid)
                    LyricsAnimationStyle.METRO_LYRICS -> stringResource(R.string.lyrics_animation_metro)
                }
            }
        )
    }

    if (showLyricsTextSizeDialog) {
        var tempTextSize by remember { mutableFloatStateOf(lyricsTextSize) }
        DefaultDialog(
            onDismiss = { showLyricsTextSizeDialog = false },
            buttons = {
                TextButton(onClick = { tempTextSize = 24f }) { Text(stringResource(R.string.reset)) }
                Spacer(modifier = Modifier.weight(1f))
                TextButton(onClick = { showLyricsTextSizeDialog = false }) { Text(stringResource(android.R.string.cancel)) }
                TextButton(onClick = { onLyricsTextSizeChange(tempTextSize); showLyricsTextSizeDialog = false }) { Text(stringResource(android.R.string.ok)) }
            }
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(16.dp)) {
                Text(text = stringResource(R.string.lyrics_text_size), style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(bottom = 16.dp))
                Text(text = "${tempTextSize.roundToInt()} sp", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(bottom = 16.dp))
                Slider(value = tempTextSize, onValueChange = { tempTextSize = it }, valueRange = 16f..36f, steps = 19, modifier = Modifier.fillMaxWidth())
            }
        }
    }

    if (showLyricsLineSpacingDialog) {
        var tempLineSpacing by remember { mutableFloatStateOf(lyricsLineSpacing) }
        DefaultDialog(
            onDismiss = { showLyricsLineSpacingDialog = false },
            buttons = {
                TextButton(onClick = { tempLineSpacing = 1.3f }) { Text(stringResource(R.string.reset)) }
                Spacer(modifier = Modifier.weight(1f))
                TextButton(onClick = { showLyricsLineSpacingDialog = false }) { Text(stringResource(android.R.string.cancel)) }
                TextButton(onClick = { onLyricsLineSpacingChange(tempLineSpacing); showLyricsLineSpacingDialog = false }) { Text(stringResource(android.R.string.ok)) }
            }
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(16.dp)) {
                Text(text = stringResource(R.string.lyrics_line_spacing), style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(bottom = 16.dp))
                Text(text = "%.1f x".format(tempLineSpacing), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(bottom = 16.dp))
                Slider(value = tempLineSpacing, onValueChange = { tempLineSpacing = it }, valueRange = 1.0f..4.0f, steps = 59, modifier = Modifier.fillMaxWidth())
            }
        }
    }

    if (showPlayerButtonsStyleDialog) {
        EnumDialog(
            onDismiss = { showPlayerButtonsStyleDialog = false },
            onSelect = { onPlayerButtonsStyleChange(it); showPlayerButtonsStyleDialog = false },
            title = stringResource(R.string.player_buttons_style),
            current = playerButtonsStyle,
            values = PlayerButtonsStyle.values().toList(),
            valueText = {
                when (it) {
                    PlayerButtonsStyle.DEFAULT -> stringResource(R.string.default_style)
                    PlayerButtonsStyle.PRIMARY -> stringResource(R.string.primary_color_style)
                    PlayerButtonsStyle.TERTIARY -> stringResource(R.string.tertiary_color_style)
                }
            }
        )
    }

    if (showPlayerBackgroundDialog) {
        EnumDialog(
            onDismiss = { showPlayerBackgroundDialog = false },
            onSelect = { onPlayerBackgroundChange(it); showPlayerBackgroundDialog = false },
            title = stringResource(R.string.player_background_style),
            current = playerBackground,
            values = availableBackgroundStyles,
            valueText = {
                when (it) {
                    PlayerBackgroundStyle.DEFAULT -> stringResource(R.string.follow_theme)
                    PlayerBackgroundStyle.GRADIENT -> stringResource(R.string.gradient)
                    PlayerBackgroundStyle.BLUR -> stringResource(R.string.player_background_blur)
                    PlayerBackgroundStyle.GLOW_ANIMATED -> stringResource(R.string.glow_animated)
                    PlayerBackgroundStyle.APPLE_MUSIC -> stringResource(R.string.apple_music)
                    PlayerBackgroundStyle.LIVE_MESH -> stringResource(R.string.live_mesh)
                }
            }
        )
    }

    if (showMiniPlayerBackgroundDialog) {
        EnumDialog(
            onDismiss = { showMiniPlayerBackgroundDialog = false },
            onSelect = { onMiniPlayerBackgroundChange(it); showMiniPlayerBackgroundDialog = false },
            title = stringResource(R.string.miniplayer_background_style),
            current = miniPlayerBackground,
            values = availableMiniPlayerBackgroundStyles,
            valueText = {
                when (it) {
                    PlayerBackgroundStyle.DEFAULT -> stringResource(R.string.follow_theme)
                    PlayerBackgroundStyle.GRADIENT -> stringResource(R.string.gradient)
                    PlayerBackgroundStyle.BLUR -> stringResource(R.string.player_background_blur)
                    PlayerBackgroundStyle.GLOW_ANIMATED -> stringResource(R.string.glow_animated)
                    PlayerBackgroundStyle.LIVE_MESH -> stringResource(R.string.live_mesh)
                    else -> "Unknown"
                }
            }
        )
    }

    if (showDefaultOpenTabDialog) {
        EnumDialog(
            onDismiss = { showDefaultOpenTabDialog = false },
            onSelect = { onDefaultOpenTabChange(it); showDefaultOpenTabDialog = false },
            title = stringResource(R.string.default_open_tab),
            current = defaultOpenTab,
            values = NavigationTab.values().toList(),
            valueText = {
                when (it) {
                    NavigationTab.HOME -> stringResource(R.string.home)
                    NavigationTab.SEARCH -> stringResource(R.string.search)
                    NavigationTab.LIBRARY -> stringResource(R.string.filter_library)
                }
            }
        )
    }

    if (showDefaultChipDialog) {
        EnumDialog(
            onDismiss = { showDefaultChipDialog = false },
            onSelect = { onDefaultChipChange(it); showDefaultChipDialog = false },
            title = stringResource(R.string.default_lib_chips),
            current = defaultChip,
            values = LibraryFilter.values().toList(),
            valueText = {
                when (it) {
                    LibraryFilter.SONGS -> stringResource(R.string.songs)
                    LibraryFilter.ARTISTS -> stringResource(R.string.artists)
                    LibraryFilter.ALBUMS -> stringResource(R.string.albums)
                    LibraryFilter.PLAYLISTS -> stringResource(R.string.playlists)
                    LibraryFilter.LIBRARY -> stringResource(R.string.filter_library)
                    LibraryFilter.LOCAL -> stringResource(R.string.filter_local)
                }
            }
        )
    }

    if (showGridSizeDialog) {
        EnumDialog(
            onDismiss = { showGridSizeDialog = false },
            onSelect = { onGridItemSizeChange(it); showGridSizeDialog = false },
            title = stringResource(R.string.grid_cell_size),
            current = gridItemSize,
            values = GridItemSize.values().toList(),
            valueText = {
                when (it) {
                    GridItemSize.BIG -> stringResource(R.string.big)
                    GridItemSize.SMALL -> stringResource(R.string.small)
                }
            }
        )
    }

    if (showRestartDialog) {
        DefaultDialog(
            onDismiss = { showRestartDialog = false },
            buttons = {
                TextButton(onClick = { showRestartDialog = false }) { Text(stringResource(android.R.string.cancel)) }
                TextButton(onClick = {
                    showRestartDialog = false
                    val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)?.apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    }
                    context.startActivity(intent)
                    Runtime.getRuntime().exit(0)
                }) { Text(stringResource(R.string.restart)) }
            }
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(text = stringResource(R.string.restart_required), style = MaterialTheme.typography.titleLarge)
                Text(text = stringResource(R.string.density_restart_message), style = MaterialTheme.typography.bodyMedium)
            }
        }
    }

    if (showDensityScaleDialog) {
        DefaultDialog(
            onDismiss = { showDensityScaleDialog = false },
            buttons = { TextButton(onClick = { showDensityScaleDialog = false }) { Text(stringResource(android.R.string.cancel)) } }
        ) {
            Column {
                DensityScale.entries.forEach { scale ->
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { onDensityScaleChange(scale.value); showDensityScaleDialog = false }.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = scale.label, style = MaterialTheme.typography.bodyLarge, color = if (densityScale == scale.value) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
        }
    }

    if (showSliderOptionDialog) {
        DefaultDialog(
            buttons = { TextButton(onClick = { showSliderOptionDialog = false }) { Text(stringResource(android.R.string.cancel)) } },
            onDismiss = { showSliderOptionDialog = false }
        ) {
            val sliderPreviewColors = PlayerSliderColors.getSliderColors(MaterialTheme.colorScheme.primary, PlayerBackgroundStyle.DEFAULT, isSystemInDarkTheme())
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.aspectRatio(1f).weight(1f).clip(RoundedCornerShape(16.dp)).border(1.dp, if (sliderStyle == SliderStyle.DEFAULT && !squigglySlider) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp)).clickable { onSliderStyleChange(SliderStyle.DEFAULT); onSquigglySliderChange(false); showSliderOptionDialog = false }.padding(12.dp)) {
                        Slider(value = 0.35f, onValueChange = { }, colors = sliderPreviewColors, enabled = false, modifier = Modifier.weight(1f))
                        Text(text = stringResource(R.string.default_), style = MaterialTheme.typography.labelSmall)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.aspectRatio(1f).weight(1f).clip(RoundedCornerShape(16.dp)).border(1.dp, if (sliderStyle == SliderStyle.WAVY && !squigglySlider) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp)).clickable { onSliderStyleChange(SliderStyle.WAVY); onSquigglySliderChange(false); showSliderOptionDialog = false }.padding(12.dp)) {
                        WavySlider(value = 0.5f, onValueChange = { }, colors = sliderPreviewColors, modifier = Modifier.weight(1f), isPlaying = true, enabled = false)
                        Text(text = stringResource(R.string.wavy), style = MaterialTheme.typography.labelSmall)
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.aspectRatio(1f).weight(1f).clip(RoundedCornerShape(16.dp)).border(1.dp, if (sliderStyle == SliderStyle.SLIM) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp)).clickable { onSliderStyleChange(SliderStyle.SLIM); onSquigglySliderChange(false); showSliderOptionDialog = false }.padding(12.dp)) {
                        Slider(value = 0.65f, onValueChange = { }, thumb = { Spacer(modifier = Modifier.size(0.dp)) }, track = { sliderState -> PlayerSliderTrack(sliderState = sliderState, colors = sliderPreviewColors) }, colors = sliderPreviewColors, enabled = false, modifier = Modifier.weight(1f))
                        Text(text = stringResource(R.string.slim), style = MaterialTheme.typography.labelSmall)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.aspectRatio(1f).weight(1f).clip(RoundedCornerShape(16.dp)).border(1.dp, if (sliderStyle == SliderStyle.WAVY && squigglySlider) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp)).clickable { onSliderStyleChange(SliderStyle.WAVY); onSquigglySliderChange(true); showSliderOptionDialog = false }.padding(12.dp)) {
                        SquigglySlider(value = 0.5f, onValueChange = { }, modifier = Modifier.weight(1f), enabled = false, colors = sliderPreviewColors, isPlaying = true)
                        Text(text = stringResource(R.string.squiggly), style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }

    if (showSensitivityDialog) {
        var tempSensitivity by remember { mutableFloatStateOf(swipeSensitivity) }
        DefaultDialog(
            onDismiss = { showSensitivityDialog = false },
            buttons = {
                TextButton(onClick = { tempSensitivity = 0.73f }) { Text(stringResource(R.string.reset)) }
                Spacer(modifier = Modifier.weight(1f))
                TextButton(onClick = { showSensitivityDialog = false }) { Text(stringResource(android.R.string.cancel)) }
                TextButton(onClick = { onSwipeSensitivityChange(tempSensitivity); showSensitivityDialog = false }) { Text(stringResource(android.R.string.ok)) }
            }
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(16.dp)) {
                Text(text = stringResource(R.string.swipe_sensitivity), style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(bottom = 16.dp))
                Text(text = stringResource(R.string.sensitivity_percentage, (tempSensitivity * 100).roundToInt()), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(bottom = 16.dp))
                Slider(value = tempSensitivity, onValueChange = { tempSensitivity = it }, valueRange = 0f..1f, modifier = Modifier.fillMaxWidth())
            }
        }
    }

    if (showThumbnailCornerRadiusDialog) {
        val (thumbnailCornerRadius, onThumbnailCornerRadiusChange) = rememberPreference(ThumbnailCornerRadiusKey, 3f)
        ThumbnailCornerRadiusModal(initialRadius = thumbnailCornerRadius, onDismiss = { showThumbnailCornerRadiusDialog = false }, onRadiusSelected = { onThumbnailCornerRadiusChange(it); showThumbnailCornerRadiusDialog = false })
    }

    Column(
        Modifier
            .windowInsetsPadding(LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Horizontal))
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp),
    ) {
        Spacer(Modifier.windowInsetsPadding(LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Top)))
        
        Material3SettingsGroup(scrollState = scrollState, 
            title = stringResource(R.string.theme),
            items = buildList {
                add(
                    Material3SettingsItem(
                        isHighlighted = (highlightKey == "Party Mode"),
                        icon = painterResource(R.drawable.palette),
                        title = { PartyModeTitle() },
                        description = { Text("Enable dark mode to enjoy party mode") },
                        trailingContent = {
                            Switch(
                                checked = partyMode,
                                onCheckedChange = { 
                                    onPartyModeChange(it)
                                    if (it) {
                                        onGlassmorphismModeChange(false)
                                        onSolarDynamicModeChange(false)
                                        onBatteryProModeChange(false)
                                        onPurpleThemeChange(false)
                                        iad1tya.echo.music.utils.IconManager.setIcon(context, "MainActivityParty")
                                        coroutineScope.launch {
                                            snackbarHostState.showSnackbar("Party mode enabled!")
                                        }
                                    } else {
                                        iad1tya.echo.music.utils.IconManager.setIcon(context, "MainActivityAlias")
                                    }
                                }
                            )
                        },
                        onClick = { 
                            val newVal = !partyMode
                            onPartyModeChange(newVal)
                            if (newVal) {
                                onGlassmorphismModeChange(false)
                                onSolarDynamicModeChange(false)
                                onBatteryProModeChange(false)
                                onPurpleThemeChange(false)
                                iad1tya.echo.music.utils.IconManager.setIcon(context, "MainActivityParty")
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("Party mode enabled!")
                                }
                            } else {
                                iad1tya.echo.music.utils.IconManager.setIcon(context, "MainActivityAlias")
                            }
                        }
                    )
                )
                add(
                    Material3SettingsItem(
                        isHighlighted = (highlightKey == "Glassmorphism"),
                        icon = painterResource(R.drawable.palette),
                        title = { Text("Glassmorphism Edition") },
                        description = { Text("Semi-transparent frosted glass UI") },
                        trailingContent = {
                            Switch(
                                checked = glassmorphismMode,
                                onCheckedChange = { 
                                    onGlassmorphismModeChange(it)
                                    if (it) {
                                        onPartyModeChange(false)
                                        onSolarDynamicModeChange(false)
                                        onBatteryProModeChange(false)
                                        onPurpleThemeChange(false)
                                        iad1tya.echo.music.utils.IconManager.setIcon(context, "MainActivityAlias")
                                    }
                                }
                            )
                        },
                        onClick = { 
                            val newVal = !glassmorphismMode
                            onGlassmorphismModeChange(newVal)
                            if (newVal) {
                                onPartyModeChange(false)
                                onSolarDynamicModeChange(false)
                                onBatteryProModeChange(false)
                                onPurpleThemeChange(false)
                                iad1tya.echo.music.utils.IconManager.setIcon(context, "MainActivityAlias")
                            }
                        }
                    )
                )
                add(
                    Material3SettingsItem(
                        isHighlighted = (highlightKey == "Solar Dynamic"),
                        icon = painterResource(R.drawable.palette),
                        title = { Text("Solar Dynamic Mode") },
                        description = { Text("Theme follows the position of the sun") },
                        trailingContent = {
                            Switch(
                                checked = solarDynamicMode,
                                onCheckedChange = { 
                                    onSolarDynamicModeChange(it)
                                    if (it) {
                                        onPartyModeChange(false)
                                        onGlassmorphismModeChange(false)
                                        onBatteryProModeChange(false)
                                        onPurpleThemeChange(false)
                                        iad1tya.echo.music.utils.IconManager.setIcon(context, "MainActivityAlias")
                                    }
                                }
                            )
                        },
                        onClick = { 
                            val newVal = !solarDynamicMode
                            onSolarDynamicModeChange(newVal)
                            if (newVal) {
                                onPartyModeChange(false)
                                onGlassmorphismModeChange(false)
                                onBatteryProModeChange(false)
                                onPurpleThemeChange(false)
                                iad1tya.echo.music.utils.IconManager.setIcon(context, "MainActivityAlias")
                            }
                        }
                    )
                )
                add(
                    Material3SettingsItem(
                        isHighlighted = (highlightKey == "Battery Pro"),
                        icon = painterResource(R.drawable.speed),
                        title = { Text("Battery Pro Mode") },
                        description = { Text("Maximum AMOLED optimization for battery") },
                        trailingContent = {
                            Switch(
                                checked = batteryProMode,
                                onCheckedChange = { 
                                    onBatteryProModeChange(it)
                                    if (it) {
                                        onPartyModeChange(false)
                                        onGlassmorphismModeChange(false)
                                        onSolarDynamicModeChange(false)
                                        onPurpleThemeChange(false)
                                        iad1tya.echo.music.utils.IconManager.setIcon(context, "MainActivityAlias")
                                    }
                                }
                            )
                        },
                        onClick = { 
                            val newVal = !batteryProMode
                            onBatteryProModeChange(newVal)
                            if (newVal) {
                                onPartyModeChange(false)
                                onGlassmorphismModeChange(false)
                                onSolarDynamicModeChange(false)
                                onPurpleThemeChange(false)
                                iad1tya.echo.music.utils.IconManager.setIcon(context, "MainActivityAlias")
                            }
                        }
                    )
                )
                add(
                    Material3SettingsItem(
                        isHighlighted = (highlightKey == "Purple Theme"),
                        icon = painterResource(R.drawable.palette),
                        title = { Text("Purple Theme") },
                        description = { Text("Apply a premium neon purple AMOLED theme") },
                        trailingContent = {
                            Switch(
                                checked = purpleTheme,
                                onCheckedChange = {
                                    onPurpleThemeChange(it)
                                    if (it) {
                                        onPartyModeChange(false)
                                        onGlassmorphismModeChange(false)
                                        onSolarDynamicModeChange(false)
                                        onBatteryProModeChange(false)
                                        iad1tya.echo.music.utils.IconManager.setIcon(context, "MainActivityPurple")
                                    } else {
                                        iad1tya.echo.music.utils.IconManager.setIcon(context, "MainActivityAlias")
                                    }
                                }
                            )
                        },
                        onClick = { 
                            val newVal = !purpleTheme
                            onPurpleThemeChange(newVal)
                            if (newVal) {
                                onPartyModeChange(false)
                                onGlassmorphismModeChange(false)
                                onSolarDynamicModeChange(false)
                                onBatteryProModeChange(false)
                                iad1tya.echo.music.utils.IconManager.setIcon(context, "MainActivityPurple")
                            } else {
                                iad1tya.echo.music.utils.IconManager.setIcon(context, "MainActivityAlias")
                            }
                        }
                    )
                )

                add(
                    Material3SettingsItem(
                        isHighlighted = (highlightKey == stringResource(R.string.theme)),
                        icon = painterResource(R.drawable.palette),
                        title = { Text(stringResource(R.string.theme)) },
                        description = { Text(stringResource(R.string.theme_desc)) },
                        onClick = { navController.navigate("settings/appearance/theme") }
                    )
                )
                add(
                    Material3SettingsItem(
                        isHighlighted = (highlightKey == stringResource(R.string.enable_high_refresh_rate)),
                        icon = painterResource(R.drawable.speed),
                        title = { Text(stringResource(R.string.enable_high_refresh_rate)) },
                        description = { Text(stringResource(R.string.enable_high_refresh_rate_desc)) },
                        trailingContent = {
                            Switch(
                                checked = enableHighRefreshRate,
                                onCheckedChange = onEnableHighRefreshRateChange
                            )
                        },
                        onClick = { onEnableHighRefreshRateChange(!enableHighRefreshRate) }
                    )
                )
                if (!isUsingCustomColor) {
                    add(
                        Material3SettingsItem(
                            isHighlighted = (highlightKey == stringResource(R.string.enable_dynamic_theme)),
                            icon = painterResource(R.drawable.palette),
                            title = { Text(stringResource(R.string.enable_dynamic_theme)) },
                            trailingContent = {
                                Switch(
                                    checked = dynamicTheme,
                                    onCheckedChange = onDynamicThemeChange
                                )
                            },
                            onClick = { onDynamicThemeChange(!dynamicTheme) }
                        )
                    )
                }
            }
        )

        Spacer(modifier = Modifier.height(27.dp))

        Material3SettingsGroup(scrollState = scrollState, 
            title = stringResource(id = R.string.mini_player),
            items = buildList {
                add(
                    Material3SettingsItem(
                        isHighlighted = (highlightKey == stringResource(R.string.miniplayer_background_style)),
                        icon = painterResource(R.drawable.palette),
                        title = { Text(stringResource(R.string.miniplayer_background_style)) },
                        description = {
                            Text(
                                when (miniPlayerBackground) {
                                    PlayerBackgroundStyle.DEFAULT -> stringResource(R.string.follow_theme)
                                    PlayerBackgroundStyle.GRADIENT -> stringResource(R.string.gradient)
                                    PlayerBackgroundStyle.BLUR -> stringResource(R.string.player_background_blur)
                                    PlayerBackgroundStyle.GLOW_ANIMATED -> stringResource(R.string.glow_animated)
                                    PlayerBackgroundStyle.LIVE_MESH -> stringResource(R.string.live_mesh)
                                    else -> stringResource(R.string.follow_theme)
                                }
                            )
                        },
                        onClick = { showMiniPlayerBackgroundDialog = true }
                    )
                )
                add(
                    Material3SettingsItem(
                        isHighlighted = (highlightKey == stringResource(R.string.swipe_sensitivity)),
                        icon = painterResource(R.drawable.tune),
                        title = { Text(stringResource(R.string.swipe_sensitivity)) },
                        description = { Text("${(swipeSensitivity * 100).roundToInt()}%") },
                        onClick = { showSensitivityDialog = true }
                    )
                )
            }
        )

        Spacer(modifier = Modifier.height(27.dp))

        Material3SettingsGroup(scrollState = scrollState, 
            title = stringResource(R.string.player),
            items = listOfNotNull(
                Material3SettingsItem(
                    isHighlighted = (highlightKey == "Apple Music Inspired"),
                    icon = painterResource(R.drawable.palette),
                    title = { Text("Apple Music Inspired") },
                    trailingContent = {
                        Switch(
                            checked = !useNewPlayerDesign,
                            onCheckedChange = { isChecked ->
                                onUseNewPlayerDesignChange(!isChecked)
                                if (isChecked) onPlayerBackgroundChange(PlayerBackgroundStyle.APPLE_MUSIC)
                            }
                        )
                    },
                    onClick = { 
                        onUseNewPlayerDesignChange(useNewPlayerDesign) 
                        if (useNewPlayerDesign) onPlayerBackgroundChange(PlayerBackgroundStyle.APPLE_MUSIC)
                    }
                ),
                Material3SettingsItem(
                    isHighlighted = (highlightKey == stringResource(R.string.player_background_style)),
                    icon = painterResource(R.drawable.gradient),
                    title = { Text(stringResource(R.string.player_background_style)) },
                    description = {
                        Text(
                            when (playerBackground) {
                                PlayerBackgroundStyle.DEFAULT -> stringResource(R.string.follow_theme)
                                PlayerBackgroundStyle.GRADIENT -> stringResource(R.string.gradient)
                                PlayerBackgroundStyle.BLUR -> stringResource(R.string.player_background_blur)
                                PlayerBackgroundStyle.GLOW_ANIMATED -> stringResource(R.string.glow_animated)
                                PlayerBackgroundStyle.APPLE_MUSIC -> stringResource(R.string.apple_music)
                                PlayerBackgroundStyle.LIVE_MESH -> stringResource(R.string.live_mesh)
                            }
                        )
                    },
                    onClick = { showPlayerBackgroundDialog = true }
                ),
                Material3SettingsItem(
                    isHighlighted = (highlightKey == stringResource(R.string.player_buttons_style)),
                    icon = painterResource(R.drawable.palette),
                    title = { Text(stringResource(R.string.player_buttons_style)) },
                    description = {
                        Text(
                            when (playerButtonsStyle) {
                                PlayerButtonsStyle.DEFAULT -> stringResource(R.string.default_style)
                                PlayerButtonsStyle.PRIMARY -> stringResource(R.string.primary_color_style)
                                PlayerButtonsStyle.TERTIARY -> stringResource(R.string.tertiary_color_style)
                            }
                        )
                    },
                    onClick = { showPlayerButtonsStyleDialog = true }
                ),
                Material3SettingsItem(
                    isHighlighted = (highlightKey == stringResource(R.string.player_slider_style)),
                    icon = painterResource(R.drawable.sliders),
                    title = { Text(stringResource(R.string.player_slider_style)) },
                    description = {
                        Text(
                            when (sliderStyle) {
                                SliderStyle.DEFAULT -> stringResource(R.string.default_)
                                SliderStyle.WAVY -> if (squigglySlider) stringResource(R.string.squiggly) else stringResource(R.string.wavy)
                                SliderStyle.SLIM -> stringResource(R.string.slim)
                            }
                        )
                    },
                    onClick = { showSliderOptionDialog = true }
                ),
                Material3SettingsItem(
                    isHighlighted = (highlightKey == stringResource(R.string.enable_swipe_thumbnail)),
                    icon = painterResource(R.drawable.swipe),
                    title = { Text(stringResource(R.string.enable_swipe_thumbnail)) },
                    trailingContent = {
                        Switch(checked = swipeThumbnail, onCheckedChange = onSwipeThumbnailChange)
                    },
                    onClick = { onSwipeThumbnailChange(!swipeThumbnail) }
                ),
                Material3SettingsItem(
                    isHighlighted = (highlightKey == stringResource(R.string.echomusic_canvas)),
                    icon = painterResource(R.drawable.palette),
                    title = { Text(stringResource(R.string.echomusic_canvas)) },
                    description = { Text(stringResource(R.string.echomusic_canvas_desc)) },
                    trailingContent = {
                        Switch(checked = canvasThumbnailAnimation, onCheckedChange = onCanvasThumbnailAnimationChange)
                    },
                    onClick = { onCanvasThumbnailAnimationChange(!canvasThumbnailAnimation) }
                ),
                Material3SettingsItem(
                    isHighlighted = (highlightKey == stringResource(R.string.rotating_thumbnail)),
                    icon = painterResource(R.drawable.image),
                    title = { Text(stringResource(R.string.rotating_thumbnail)) },
                    description = { Text(stringResource(R.string.rotating_thumbnail_desc)) },
                    trailingContent = {
                        Switch(checked = rotatingThumbnail, onCheckedChange = onRotatingThumbnailChange)
                    },
                    onClick = { onRotatingThumbnailChange(!rotatingThumbnail) }
                ),
                Material3SettingsItem(
                    isHighlighted = (highlightKey == stringResource(R.string.show_comment_button)),
                    icon = painterResource(R.drawable.chat_msg),
                    title = { Text(stringResource(R.string.show_comment_button)) },
                    description = { Text(stringResource(R.string.show_comment_button_description)) },
                    trailingContent = {
                        Switch(checked = showCommentButton, onCheckedChange = onShowCommentButtonChange)
                    },
                    onClick = { onShowCommentButtonChange(!showCommentButton) }
                ),
                Material3SettingsItem(
                    isHighlighted = (highlightKey == "Show codec on player"),
                    icon = painterResource(R.drawable.info),
                    title = { Text("Show codec on player") },
                    description = { Text("Display audio codec information below the timeline") },
                    trailingContent = {
                        Switch(checked = showCodecOnPlayer, onCheckedChange = onShowCodecOnPlayerChange)
                    },
                    onClick = { onShowCodecOnPlayerChange(!showCodecOnPlayer) }
                )
            )
        )

        Spacer(modifier = Modifier.height(27.dp))

        Material3SettingsGroup(scrollState = scrollState, 
            title = stringResource(R.string.lyrics),
            items = buildList {
                add(Material3SettingsItem(
                    isHighlighted = (highlightKey == stringResource(R.string.lyrics_text_position)),
                    icon = painterResource(R.drawable.lyrics),
                    title = { Text(stringResource(R.string.lyrics_text_position)) },
                    description = {
                        Text(
                            when (lyricsPosition) {
                                LyricsPosition.LEFT -> stringResource(R.string.left)
                                LyricsPosition.CENTER -> stringResource(R.string.center)
                                LyricsPosition.RIGHT -> stringResource(R.string.right)
                            }
                        )
                    },
                    onClick = { showLyricsPositionDialog = true }
                ))
                add(Material3SettingsItem(
                    isHighlighted = (highlightKey == stringResource(R.string.lyrics_animation_style)),
                    icon = painterResource(R.drawable.lyrics),
                    title = { Text(stringResource(R.string.lyrics_animation_style)) },
                    description = {
                        Text(
                            when (lyricsAnimationStyle) {
                                LyricsAnimationStyle.NONE -> stringResource(R.string.none)
                                LyricsAnimationStyle.FADE -> stringResource(R.string.fade)
                                LyricsAnimationStyle.GLOW -> stringResource(R.string.glow)
                                LyricsAnimationStyle.SLIDE -> stringResource(R.string.slide)
                                LyricsAnimationStyle.KARAOKE -> stringResource(R.string.karaoke)
                                LyricsAnimationStyle.APPLE -> stringResource(R.string.apple_music_style)
                                LyricsAnimationStyle.APPLE_V2 -> stringResource(R.string.apple_music_style_letter)
                                LyricsAnimationStyle.echomusic_1 -> stringResource(R.string.echomusic_1)
                                LyricsAnimationStyle.LYRICS_V2 -> stringResource(R.string.lyrics_v2_fluid)
                                LyricsAnimationStyle.METRO_LYRICS -> stringResource(R.string.lyrics_animation_metro)
                            }
                        )
                    },
                    onClick = { showLyricsAnimationStyleDialog = true }
                ))
                add(Material3SettingsItem(
                    isHighlighted = (highlightKey == stringResource(R.string.lyrics_glow_effect)),
                    icon = painterResource(R.drawable.lyrics),
                    title = { Text(stringResource(R.string.lyrics_glow_effect)) },
                    description = { Text(stringResource(R.string.lyrics_glow_effect_desc)) },
                    trailingContent = {
                        Switch(checked = lyricsGlowEffect, onCheckedChange = onLyricsGlowEffectChange)
                    },
                    onClick = { onLyricsGlowEffectChange(!lyricsGlowEffect) }
                ))
                add(Material3SettingsItem(
                    isHighlighted = (highlightKey == stringResource(R.string.apple_music_lyrics_blur)),
                    icon = painterResource(R.drawable.lyrics),
                    title = { Text(stringResource(R.string.apple_music_lyrics_blur)) },
                    description = { Text(stringResource(R.string.apple_music_lyrics_blur_desc)) },
                    trailingContent = {
                        Switch(checked = appleMusicLyricsBlur, onCheckedChange = onAppleMusicLyricsBlurChange)
                    },
                    onClick = { onAppleMusicLyricsBlurChange(!appleMusicLyricsBlur) }
                ))
                add(Material3SettingsItem(
                    isHighlighted = (highlightKey == stringResource(R.string.standard_lyrics_blur)),
                    icon = painterResource(R.drawable.lyrics),
                    title = { Text(stringResource(R.string.standard_lyrics_blur)) },
                    description = { Text(stringResource(R.string.apple_music_lyrics_blur_desc)) },
                    trailingContent = {
                        Switch(checked = lyricsStandardBlur, onCheckedChange = onLyricsStandardBlurChange)
                    },
                    onClick = { onLyricsStandardBlurChange(!lyricsStandardBlur) }
                ))
                add(Material3SettingsItem(
                    isHighlighted = (highlightKey == stringResource(R.string.lyrics_text_size)),
                    icon = painterResource(R.drawable.lyrics),
                    title = { Text(stringResource(R.string.lyrics_text_size)) },
                    description = { Text("${lyricsTextSize.roundToInt()} sp") },
                    onClick = { showLyricsTextSizeDialog = true }
                ))
                add(Material3SettingsItem(
                    isHighlighted = (highlightKey == stringResource(R.string.lyrics_line_spacing)),
                    icon = painterResource(R.drawable.lyrics),
                    title = { Text(stringResource(R.string.lyrics_line_spacing)) },
                    description = { Text("${String.format("%.1f", lyricsLineSpacing)}x") },
                    onClick = { showLyricsLineSpacingDialog = true }
                ))
                add(Material3SettingsItem(
                    isHighlighted = (highlightKey == stringResource(R.string.lyrics_click_change)),
                    icon = painterResource(R.drawable.lyrics),
                    title = { Text(stringResource(R.string.lyrics_click_change)) },
                    trailingContent = {
                        Switch(checked = lyricsClick, onCheckedChange = onLyricsClickChange)
                    },
                    onClick = { onLyricsClickChange(!lyricsClick) }
                ))
                add(Material3SettingsItem(
                    isHighlighted = (highlightKey == stringResource(R.string.lyrics_auto_scroll)),
                    icon = painterResource(R.drawable.lyrics),
                    title = { Text(stringResource(R.string.lyrics_auto_scroll)) },
                    trailingContent = {
                        Switch(checked = lyricsScroll, onCheckedChange = onLyricsScrollChange)
                    },
                    onClick = { onLyricsScrollChange(!lyricsScroll) }
                ))
                add(Material3SettingsItem(
                    isHighlighted = (highlightKey == stringResource(R.string.lyrics_swipe_to_change_song)),
                    icon = painterResource(R.drawable.swipe),
                    title = { Text(stringResource(R.string.lyrics_swipe_to_change_song)) },
                    description = { Text(stringResource(R.string.lyrics_swipe_to_change_song_desc)) },
                    trailingContent = {
                        Switch(checked = swipeLyrics, onCheckedChange = onSwipeLyricsChange)
                    },
                    onClick = { onSwipeLyricsChange(!swipeLyrics) }
                ))
                add(Material3SettingsItem(
                    isHighlighted = (highlightKey == stringResource(R.string.lyrics_thumbnail_play_pause)),
                    icon = painterResource(R.drawable.play),
                    title = { Text(stringResource(R.string.lyrics_thumbnail_play_pause)) },
                    description = { Text(stringResource(R.string.lyrics_thumbnail_play_pause_desc)) },
                    trailingContent = {
                        Switch(checked = enableLyricsThumbnailPlayPause, onCheckedChange = onEnableLyricsThumbnailPlayPauseChange)
                    },
                    onClick = { onEnableLyricsThumbnailPlayPauseChange(!enableLyricsThumbnailPlayPause) }
                ))
                add(Material3SettingsItem(
                    isHighlighted = (highlightKey == stringResource(R.string.hide_status_bar_on_fullscreen)),
                    icon = painterResource(R.drawable.fullscreen),
                    title = { Text(stringResource(R.string.hide_status_bar_on_fullscreen)) },
                    description = { Text(stringResource(R.string.hide_status_bar_on_fullscreen_desc)) },
                    trailingContent = {
                        Switch(checked = hideStatusBarOnFullscreen, onCheckedChange = onHideStatusBarOnFullscreenChange)
                    },
                    onClick = { onHideStatusBarOnFullscreenChange(!hideStatusBarOnFullscreen) }
                ))
            }
        )

        Spacer(modifier = Modifier.height(27.dp))

        Material3SettingsGroup(scrollState = scrollState, 
            title = stringResource(R.string.misc),
            items = buildList {
                add(Material3SettingsItem(
                    isHighlighted = (highlightKey == stringResource(R.string.default_open_tab)),
                    icon = painterResource(R.drawable.nav_bar),
                    title = { Text(stringResource(R.string.default_open_tab)) },
                    description = {
                        Text(
                            when (defaultOpenTab) {
                                NavigationTab.HOME -> stringResource(R.string.home)
                                NavigationTab.SEARCH -> stringResource(R.string.search)
                                NavigationTab.LIBRARY -> stringResource(R.string.filter_library)
                            }
                        )
                    },
                    onClick = { showDefaultOpenTabDialog = true }
                ))
                add(Material3SettingsItem(
                    isHighlighted = (highlightKey == stringResource(R.string.default_lib_chips)),
                    icon = painterResource(R.drawable.tab),
                    title = { Text(stringResource(R.string.default_lib_chips)) },
                    description = {
                        Text(
                            when (defaultChip) {
                                LibraryFilter.SONGS -> stringResource(R.string.songs)
                                LibraryFilter.ARTISTS -> stringResource(R.string.artists)
                                LibraryFilter.ALBUMS -> stringResource(R.string.albums)
                                LibraryFilter.PLAYLISTS -> stringResource(R.string.playlists)
                                LibraryFilter.LIBRARY -> stringResource(R.string.filter_library)
                                LibraryFilter.LOCAL -> stringResource(R.string.filter_local)
                            }
                        )
                    },
                    onClick = { showDefaultChipDialog = true }
                ))
                add(Material3SettingsItem(
                    isHighlighted = (highlightKey == stringResource(R.string.swipe_song_to_add)),
                    icon = painterResource(R.drawable.swipe),
                    title = { Text(stringResource(R.string.swipe_song_to_add)) },
                    trailingContent = {
                        Switch(checked = swipeToSong, onCheckedChange = onSwipeToSongChange)
                    },
                    onClick = { onSwipeToSongChange(!swipeToSong) }
                ))
                add(Material3SettingsItem(
                    isHighlighted = (highlightKey == stringResource(R.string.swipe_song_to_remove)),
                    icon = painterResource(R.drawable.swipe),
                    title = { Text(stringResource(R.string.swipe_song_to_remove)) },
                    trailingContent = {
                        Switch(checked = swipeToRemoveSong, onCheckedChange = onSwipeToRemoveSongChange)
                    },
                    onClick = { onSwipeToRemoveSongChange(!swipeToRemoveSong) }
                ))
                add(Material3SettingsItem(
                    isHighlighted = (highlightKey == stringResource(R.string.listen_together_in_top_bar)),
                    icon = painterResource(R.drawable.group_outlined),
                    title = { Text(stringResource(R.string.listen_together_in_top_bar)) },
                    description = { Text(stringResource(R.string.listen_together_in_top_bar_desc)) },
                    trailingContent = {
                        Switch(checked = listenTogetherInTopBar, onCheckedChange = onListenTogetherInTopBarChange)
                    },
                    onClick = { onListenTogetherInTopBarChange(!listenTogetherInTopBar) }
                ))
                add(Material3SettingsItem(
                    isHighlighted = (highlightKey == stringResource(R.string.grid_cell_size)),
                    icon = painterResource(R.drawable.grid_view),
                    title = { Text(stringResource(R.string.grid_cell_size)) },
                    description = {
                        Text(
                            when (gridItemSize) {
                                GridItemSize.BIG -> stringResource(R.string.big)
                                GridItemSize.SMALL -> stringResource(R.string.small)
                            }
                        )
                    },
                    onClick = { showGridSizeDialog = true }
                ))
                add(Material3SettingsItem(
                    isHighlighted = (highlightKey == "Interactive Lock Screen"),
                    icon = painterResource(R.drawable.lock),
                    title = { Text("Interactive Lock Screen Widget") },
                    description = { Text("Custom lock screen overlay (Android 12+)") },
                    trailingContent = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (!android.provider.Settings.canDrawOverlays(context)) {
                                TextButton(onClick = {
                                    val intent = Intent(
                                        android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                        android.net.Uri.parse("package:${context.packageName}")
                                    )
                                    context.startActivity(intent)
                                }) {
                                    Text("Grant Permission", color = MaterialTheme.colorScheme.error)
                                }
                            }
                            TextButton(onClick = {
                                context.startActivity(Intent(context, iad1tya.echo.music.ui.player.LockScreenActivity::class.java).apply {
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                })
                            }) {
                                Text("Test")
                            }
                            Switch(checked = lockScreenOverlay, onCheckedChange = onLockScreenOverlayChange)
                        }
                    },
                    onClick = { onLockScreenOverlayChange(!lockScreenOverlay) }
                ))
                add(Material3SettingsItem(
                    isHighlighted = (highlightKey == "Dynamic Capsule"),
                    icon = painterResource(R.drawable.palette),
                    title = { Text("Dynamic Mini-Capsule") },
                    description = { Text("Nothing-style status bar pill when app is in background") },
                    trailingContent = {
                        Switch(checked = dynamicCapsule, onCheckedChange = onDynamicCapsuleChange)
                    },
                    onClick = { onDynamicCapsuleChange(!dynamicCapsule) }
                ))
                add(Material3SettingsItem(
                    isHighlighted = (highlightKey == stringResource(R.string.display_density)),
                    icon = painterResource(R.drawable.grid_view),
                    title = { Text(stringResource(R.string.display_density)) },
                    description = { Text(DensityScale.fromValue(densityScale).label) },
                    onClick = { showDensityScaleDialog = true }
                ))
            }
        )

        Spacer(modifier = Modifier.height(27.dp))

        Material3SettingsGroup(scrollState = scrollState, 
            title = stringResource(R.string.auto_playlists),
            items = buildList {
                add(Material3SettingsItem(
                    isHighlighted = (highlightKey == stringResource(R.string.show_liked_playlist)),
                    icon = painterResource(R.drawable.favorite),
                    title = { Text(stringResource(R.string.show_liked_playlist)) },
                    trailingContent = {
                        Switch(checked = showLikedPlaylist, onCheckedChange = onShowLikedPlaylistChange)
                    },
                    onClick = { onShowLikedPlaylistChange(!showLikedPlaylist) }
                ))
                add(Material3SettingsItem(
                    isHighlighted = (highlightKey == stringResource(R.string.show_downloaded_playlist)),
                    icon = painterResource(R.drawable.offline),
                    title = { Text(stringResource(R.string.show_downloaded_playlist)) },
                    trailingContent = {
                        Switch(checked = showDownloadedPlaylist, onCheckedChange = onShowDownloadedPlaylistChange)
                    },
                    onClick = { onShowDownloadedPlaylistChange(!showDownloadedPlaylist) }
                ))
                add(Material3SettingsItem(
                    isHighlighted = (highlightKey == stringResource(R.string.action_exported)),
                    icon = painterResource(R.drawable.download),
                    title = { Text(stringResource(R.string.action_exported)) },
                    trailingContent = {
                        Switch(checked = showExportedPlaylist, onCheckedChange = onShowExportedPlaylistChange)
                    },
                    onClick = { onShowExportedPlaylistChange(!showExportedPlaylist) }
                ))
                add(Material3SettingsItem(
                    isHighlighted = (highlightKey == stringResource(R.string.show_top_playlist)),
                    icon = painterResource(R.drawable.trending_up),
                    title = { Text(stringResource(R.string.show_top_playlist)) },
                    trailingContent = {
                        Switch(checked = showTopPlaylist, onCheckedChange = onShowTopPlaylistChange)
                    },
                    onClick = { onShowTopPlaylistChange(!showTopPlaylist) }
                ))
                add(Material3SettingsItem(
                    isHighlighted = (highlightKey == stringResource(R.string.show_cached_playlist)),
                    icon = painterResource(R.drawable.cached),
                    title = { Text(stringResource(R.string.show_cached_playlist)) },
                    trailingContent = {
                        Switch(checked = showCachedPlaylist, onCheckedChange = onShowCachedPlaylistChange)
                    },
                    onClick = { onShowCachedPlaylistChange(!showCachedPlaylist) }
                ))
            }
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        Spacer(Modifier.windowInsetsPadding(LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Bottom)))
    }

    TopAppBar(
        title = { Text(stringResource(R.string.appearance)) },
        navigationIcon = {
            IconButton(
                onClick = navController::navigateUp,
                onLongClick = navController::backToMain,
            ) {
                Icon(painterResource(R.drawable.arrow_back), contentDescription = null)
            }
        }
    )
}

enum class DarkMode { ON, OFF, AUTO }
enum class NavigationTab { HOME, SEARCH, LIBRARY }
enum class LyricsPosition { LEFT, CENTER, RIGHT }
