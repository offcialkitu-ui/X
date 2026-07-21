package iad1tya.echo.music.ui.screens.settings

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import iad1tya.echo.music.BuildConfig
import iad1tya.echo.music.LocalPlayerAwareWindowInsets
import iad1tya.echo.music.R
import iad1tya.echo.music.constants.DownloadOnlyOnWifiKey
import iad1tya.echo.music.ui.component.IconButton
import iad1tya.echo.music.ui.component.Material3SettingsGroup
import iad1tya.echo.music.ui.component.Material3SettingsItem
import iad1tya.echo.music.utils.rememberPreference
import iad1tya.echo.music.echomusic.updater.*

import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Wifi

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpdateSettings(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
    highlightKey: String? = null
) {
    val scrollState = rememberScrollState()
    val context = LocalContext.current
    
    // Preferences
    var autoUpdateEnabled by remember { mutableStateOf(getAutoUpdateCheckSetting(context)) }
    var notificationsEnabled by remember { mutableStateOf(getUpdateNotificationsSetting(context)) }
    var downloadOnlyOnWifi by rememberPreference(DownloadOnlyOnWifiKey, false)
    
    var apkCount by remember { mutableStateOf(getDownloadedApkCount(context)) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.update_settings_title)) },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(painterResource(R.drawable.arrow_back), contentDescription = null)
                    }
                },
                scrollBehavior = scrollBehavior
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp)
        ) {
            Material3SettingsGroup(
                scrollState = scrollState,
                title = "Update Preferences",
                items = listOf(
                    Material3SettingsItem(
                        icon = painterResource(R.drawable.update),
                        title = { Text("Check for Updates") },
                        description = { Text("Check manually for the latest version") },
                        onClick = { navController.navigate("update") }
                    ),
                    Material3SettingsItem(
                        icon = painterResource(R.drawable.update),
                        title = { Text("Automatic Update Check") },
                        description = { Text("Check for updates in the background") },
                        trailingContent = {
                            Switch(
                                checked = autoUpdateEnabled,
                                onCheckedChange = { 
                                    autoUpdateEnabled = it
                                    saveAutoUpdateCheckSetting(context, it)
                                }
                            )
                        }
                    ),
                    Material3SettingsItem(
                        icon = painterResource(R.drawable.notification),
                        title = { Text("Update Notifications") },
                        description = { Text("Get notified when a new version is ready") },
                        trailingContent = {
                            Switch(
                                checked = notificationsEnabled,
                                onCheckedChange = { 
                                    notificationsEnabled = it
                                    saveUpdateNotificationsSetting(context, it)
                                }
                            )
                        }
                    ),
                    Material3SettingsItem(
                        icon = rememberVectorPainter(Icons.Rounded.Wifi),
                        title = { Text("Download only on Wi-Fi") },
                        description = { Text("Save mobile data by restricting downloads") },
                        trailingContent = {
                            Switch(
                                checked = downloadOnlyOnWifi,
                                onCheckedChange = { downloadOnlyOnWifi = it }
                            )
                        }
                    )
                )
            )

            Spacer(Modifier.height(16.dp))

            Material3SettingsGroup(
                scrollState = scrollState,
                title = "App Information",
                items = listOf(
                    Material3SettingsItem(
                        icon = painterResource(R.drawable.info),
                        title = { Text("Current Version") },
                        description = { Text("v${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})") }
                    ),
                    Material3SettingsItem(
                        icon = painterResource(R.drawable.delete),
                        title = { Text("Clear Downloaded Updates") },
                        description = { Text(if (apkCount > 0) "$apkCount APKs found" else "No downloaded files") },
                        onClick = {
                            if (apkCount > 0) {
                                clearDownloadedApks(context)
                                apkCount = 0
                                Toast.makeText(context, "Cleared successfully", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                )
            )
            
            Spacer(Modifier.windowInsetsPadding(LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Bottom)))
        }
    }
}
