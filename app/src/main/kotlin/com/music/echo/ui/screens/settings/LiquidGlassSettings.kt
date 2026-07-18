
package iad1tya.echo.music.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import iad1tya.echo.music.R
import iad1tya.echo.music.constants.*
import iad1tya.echo.music.ui.component.Material3SettingsGroup
import iad1tya.echo.music.ui.component.Material3SettingsItem
import iad1tya.echo.music.utils.rememberPreference
import iad1tya.echo.music.utils.rememberEnumPreference
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiquidGlassSettings(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
) {
    val scrollState = rememberScrollState()
    
    val (floatingNavBar, onFloatingNavBarChange) = rememberPreference(FloatingNavBarKey, defaultValue = true)
    val (liquidGlassEnabled, onLiquidGlassEnabledChange) = rememberPreference(LiquidGlassEnabledKey, defaultValue = false)
    
    val (vibrancy, onVibrancyChange) = rememberPreference(LiquidGlassVibrancyKey, defaultValue = 1.0f)
    val (blurRadius, onBlurRadiusChange) = rememberPreference(LiquidGlassBlurRadiusKey, defaultValue = 25f)
    val (refractionHeight, onRefractionHeightChange) = rememberPreference(LiquidGlassRefractionHeightKey, defaultValue = 0.5f)
    val (refractionAmount, onRefractionAmountChange) = rememberPreference(LiquidGlassRefractionAmountKey, defaultValue = 0.5f)
    val (chromaticAberration, onChromaticAberrationChange) = rememberPreference(LiquidGlassChromaticAberrationKey, defaultValue = true)
    val (depthEffect, onDepthEffectChange) = rememberPreference(LiquidGlassDepthEffectKey, defaultValue = true)
    
    val (surfaceTint, onSurfaceTintChange) = rememberPreference(LiquidGlassSurfaceTintKey, defaultValue = 0)
    val (surfaceOpacity, onSurfaceOpacityChange) = rememberPreference(LiquidGlassSurfaceOpacityKey, defaultValue = 0.1f)
    val (glassTextColor, onGlassTextColorChange) = rememberPreference(LiquidGlassTextColorKey, defaultValue = 0xFFFFFFFF.toInt())
    
    val (glassPlayer, onGlassPlayerChange) = rememberPreference(LiquidGlassPlayerEnabledKey, defaultValue = true)
    val (glassMiniPlayer, onGlassMiniPlayerChange) = rememberPreference(LiquidGlassMiniPlayerEnabledKey, defaultValue = true)
    val (glassNavBar, onGlassNavBarChange) = rememberPreference(LiquidGlassNavBarEnabledKey, defaultValue = true)

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp),
    ) {
        Spacer(Modifier.height(16.dp))

        Material3SettingsGroup(scrollState = scrollState, title = "Navigation Bar Style", items = buildList {
            add(
                Material3SettingsItem(
                    icon = painterResource(R.drawable.tab),
                    title = { Text("Floating navigation bar") },
                    description = { Text("iOS style floating tab bar that shrinks while scrolling") },
                    trailingContent = {
                        Switch(
                            checked = floatingNavBar,
                            onCheckedChange = onFloatingNavBarChange
                        )
                    },
                    onClick = { onFloatingNavBarChange(!floatingNavBar) }
                )
            )
        })

        Spacer(Modifier.height(24.dp))

        Material3SettingsGroup(scrollState = scrollState, title = "Liquid Glass (Beta)", items = buildList {
            add(
                Material3SettingsItem(
                    icon = painterResource(R.drawable.done),
                    title = { Text("Enable Liquid Glass") },
                    description = { Text("Rendering liquid glass is performance heavy and may reduce smoothness and battery life on some devices") },
                    trailingContent = {
                        Switch(
                            checked = liquidGlassEnabled,
                            onCheckedChange = onLiquidGlassEnabledChange
                        )
                    },
                    onClick = { onLiquidGlassEnabledChange(!liquidGlassEnabled) }
                )
            )
        })

        if (liquidGlassEnabled) {
            Spacer(Modifier.height(24.dp))

            Material3SettingsGroup(scrollState = scrollState, title = "Effects", items = buildList {
                add(
                    Material3SettingsItem(
                        icon = painterResource(R.drawable.tune),
                        title = { Text("Vibrancy") },
                        description = { Text("Boost saturation of the glass backdrop") },
                        trailingContent = {
                            Slider(
                                value = vibrancy,
                                onValueChange = onVibrancyChange,
                                valueRange = 0f..2f,
                                modifier = Modifier.width(120.dp)
                            )
                        }
                    )
                )
                add(
                    Material3SettingsItem(
                        icon = painterResource(R.drawable.tune),
                        title = { Text("Blur Radius") },
                        description = { Text("Amount of blur on the glass surface") },
                        trailingContent = {
                            Slider(
                                value = blurRadius,
                                onValueChange = onBlurRadiusChange,
                                valueRange = 0f..100f,
                                modifier = Modifier.width(120.dp)
                            )
                        }
                    )
                )
                add(
                    Material3SettingsItem(
                        icon = painterResource(R.drawable.tune),
                        title = { Text("Lens Refraction Height") },
                        trailingContent = {
                            Slider(
                                value = refractionHeight,
                                onValueChange = onRefractionHeightChange,
                                valueRange = 0f..1f,
                                modifier = Modifier.width(120.dp)
                            )
                        }
                    )
                )
                add(
                    Material3SettingsItem(
                        icon = painterResource(R.drawable.tune),
                        title = { Text("Lens Refraction Amount") },
                        trailingContent = {
                            Slider(
                                value = refractionAmount,
                                onValueChange = onRefractionAmountChange,
                                valueRange = 0f..1f,
                                modifier = Modifier.width(120.dp)
                            )
                        }
                    )
                )
                add(
                    Material3SettingsItem(
                        icon = painterResource(R.drawable.tune),
                        title = { Text("Chromatic Aberration") },
                        trailingContent = {
                            Switch(
                                checked = chromaticAberration,
                                onCheckedChange = onChromaticAberrationChange
                            )
                        },
                        onClick = { onChromaticAberrationChange(!chromaticAberration) }
                    )
                )
                add(
                    Material3SettingsItem(
                        icon = painterResource(R.drawable.tune),
                        title = { Text("Depth Effect") },
                        trailingContent = {
                            Switch(
                                checked = depthEffect,
                                onCheckedChange = onDepthEffectChange
                            )
                        },
                        onClick = { onDepthEffectChange(!depthEffect) }
                    )
                )
            })

            Spacer(Modifier.height(24.dp))

            Material3SettingsGroup(scrollState = scrollState, title = "Appearance", items = buildList {
                add(
                    Material3SettingsItem(
                        icon = painterResource(R.drawable.palette),
                        title = { Text("Surface Tint") },
                        description = { Text("Tint color applied to the glass surface") },
                        onClick = { /* Implement Color Picker if needed */ }
                    )
                )
                add(
                    Material3SettingsItem(
                        icon = painterResource(R.drawable.tune),
                        title = { Text("Surface Opacity") },
                        description = { Text("Opacity of the glass tint overlay for readability") },
                        trailingContent = {
                            Slider(
                                value = surfaceOpacity,
                                onValueChange = onSurfaceOpacityChange,
                                valueRange = 0f..1f,
                                modifier = Modifier.width(120.dp)
                            )
                        }
                    )
                )
                add(
                    Material3SettingsItem(
                        icon = painterResource(R.drawable.palette),
                        title = { Text("Glass Text Color") },
                        description = { Text("Text color used on glass surfaces") },
                        onClick = { /* Implement Color Picker if needed */ }
                    )
                )
            })

            Spacer(Modifier.height(24.dp))

            Material3SettingsGroup(scrollState = scrollState, title = "Per Component", items = buildList {
                add(
                    Material3SettingsItem(
                        icon = painterResource(R.drawable.music_note),
                        title = { Text("Glass Player") },
                        trailingContent = {
                            Switch(checked = glassPlayer, onCheckedChange = onGlassPlayerChange)
                        },
                        onClick = { onGlassPlayerChange(!glassPlayer) }
                    )
                )
                add(
                    Material3SettingsItem(
                        icon = painterResource(R.drawable.music_note),
                        title = { Text("Glass Mini Player") },
                        trailingContent = {
                            Switch(checked = glassMiniPlayer, onCheckedChange = onGlassMiniPlayerChange)
                        },
                        onClick = { onGlassMiniPlayerChange(!glassMiniPlayer) }
                    )
                )
                add(
                    Material3SettingsItem(
                        icon = painterResource(R.drawable.tab),
                        title = { Text("Glass Navigation Bar") },
                        trailingContent = {
                            Switch(checked = glassNavBar, onCheckedChange = onGlassNavBarChange)
                        },
                        onClick = { onGlassNavBarChange(!glassNavBar) }
                    )
                )
            })
        }
        
        Spacer(Modifier.height(100.dp)) // Extra space for bottom bar
    }
}
