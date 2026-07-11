package iad1tya.echo.music.echomusic.updater

import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.work.WorkInfo
import androidx.work.WorkManager
import coil3.compose.AsyncImage
import iad1tya.echo.music.R
import iad1tya.echo.music.ui.component.IconButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpdateScreen(
    navController: NavController,
    viewModel: UpdateViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var downloadState by remember { mutableStateOf<DownloadState>(DownloadState.Idle) }

    LaunchedEffect(Unit) {
        viewModel.checkForUpdates()
        WorkManager.getInstance(context)
            .getWorkInfosForUniqueWorkLiveData("update_download")
            .observeForever { workInfos ->
                val workInfo = workInfos?.firstOrNull() ?: return@observeForever
                when (workInfo.state) {
                    WorkInfo.State.RUNNING -> {
                        val progress = workInfo.progress.getFloat("progress", 0f)
                        val speed = workInfo.progress.getString("speed") ?: "0 MB/s"
                        val eta = workInfo.progress.getString("eta") ?: "--:--"
                        val downloaded = workInfo.progress.getLong("downloaded", 0L)
                        val total = workInfo.progress.getLong("total", 0L)
                        downloadState = DownloadState.Downloading(progress, speed, eta, downloaded, total)
                    }
                    WorkInfo.State.SUCCEEDED -> {
                        val path = workInfo.outputData.getString("file_path")
                        if (path != null) downloadState = DownloadState.Completed(path)
                    }
                    WorkInfo.State.FAILED -> downloadState = DownloadState.Failed("Download failed")
                    else -> {}
                }
            }
    }

    Scaffold(
        containerColor = Color.Black,
        topBar = {
            TopAppBar(
                title = { Text("MELODY X Update", color = Color.White, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(painterResource(R.drawable.arrow_back), contentDescription = null, tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding).background(Color.Black)) {
            when (val state = uiState) {
                is UpdateUiState.Checking -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Color(0xFF8B5CF6))
                    }
                }
                is UpdateUiState.Available -> {
                    UpdateAvailableContent(
                        info = state.updateInfo,
                        downloadState = downloadState,
                        onUpdate = { viewModel.startDownload(context, state.updateInfo) },
                        onInstall = { path -> viewModel.installApk(context, path) },
                        onLater = { navController.navigateUp() }
                    )
                }
                is UpdateUiState.UpToDate -> UpToDateContent(state.currentVersion)
                is UpdateUiState.Error -> ErrorContent(state.message, onRetry = { viewModel.checkForUpdates() })
                else -> {}
            }
        }
    }
}

@Composable
private fun UpdateAvailableContent(
    info: UpdateInfo,
    downloadState: DownloadState,
    onUpdate: () -> Unit,
    onInstall: (String) -> Unit,
    onLater: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp)) {
        Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("New Update Available", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = Color.White)
            Text("v${info.versionName}", style = MaterialTheme.typography.titleLarge, color = Color(0xFF8B5CF6), fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            Text("${info.releaseDate} • ${info.apkSize}", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
        }

        Spacer(Modifier.height(24.dp))

        LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth()) {
            item {
                ChangelogCategory("✨ New Features", info.changelog.features, Color(0xFF8B5CF6))
                ChangelogCategory("🛠️ Improvements", info.changelog.improvements, Color(0xFF10B981))
                ChangelogCategory("🐛 Bug Fixes", info.changelog.bugFixes, Color(0xFFEF4444))
            }
        }

        if (downloadState is DownloadState.Downloading) {
            DownloadProgressCard(downloadState)
            Spacer(Modifier.height(16.dp))
        }

        Row(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp, top = 8.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            if (!info.forceUpdate && downloadState !is DownloadState.Downloading) {
                Button(onClick = onLater, modifier = Modifier.weight(1f).height(56.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A1A1A)), shape = RoundedCornerShape(24.dp)) {
                    Text("Later", color = Color.White)
                }
            }
            val buttonText = when (downloadState) {
                is DownloadState.Downloading -> "Downloading..."
                is DownloadState.Completed -> "Install Now"
                else -> "Update Now"
            }
            Button(
                onClick = { if (downloadState is DownloadState.Completed) onInstall(downloadState.fileUri) else onUpdate() },
                enabled = downloadState !is DownloadState.Downloading,
                modifier = Modifier.weight(1f).height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B5CF6)),
                shape = RoundedCornerShape(24.dp)
            ) { Text(buttonText, color = Color.White, fontWeight = FontWeight.Bold) }
        }
    }
}

@Composable
private fun DownloadProgressCard(state: DownloadState.Downloading) {
    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF121212)), shape = RoundedCornerShape(24.dp), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Downloading...", color = Color.White, style = MaterialTheme.typography.bodyMedium)
                Text(state.speed, color = Color(0xFF8B5CF6), fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(12.dp))
            LinearProgressIndicator(
                progress = { state.progress },
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                color = Color(0xFF8B5CF6),
                trackColor = Color(0xFF1A1A1A)
            )
            Spacer(Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("${(state.progress * 100).toInt()}% complete", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                Text("ETA: ${state.remainingTime}", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun ChangelogCategory(title: String, items: List<String>, accentColor: Color) {
    if (items.isEmpty()) return
    Column(modifier = Modifier.padding(bottom = 20.dp)) {
        Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = accentColor)
        Spacer(Modifier.height(10.dp))
        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF121212)), shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                items.forEach { item ->
                    Row(modifier = Modifier.padding(vertical = 4.dp)) {
                        Text("•", color = accentColor, modifier = Modifier.padding(end = 12.dp), fontWeight = FontWeight.Bold)
                        Text(item, color = Color.White.copy(alpha = 0.85f), lineHeight = 20.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun UpToDateContent(version: String) {
    Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Icon(painterResource(R.drawable.check), contentDescription = null, modifier = Modifier.size(80.dp), tint = Color(0xFF10B981))
        Spacer(Modifier.height(16.dp))
        Text("You're all set!", style = MaterialTheme.typography.headlineSmall, color = Color.White, fontWeight = FontWeight.Bold)
        Text("v$version is the latest version.", color = Color.Gray)
    }
}

@Composable
private fun ErrorContent(message: String, onRetry: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Icon(painterResource(R.drawable.error), contentDescription = null, modifier = Modifier.size(80.dp), tint = Color(0xFFEF4444))
        Spacer(Modifier.height(16.dp))
        Text("Check Failed", style = MaterialTheme.typography.headlineSmall, color = Color.White, fontWeight = FontWeight.Bold)
        Text(message, color = Color.Gray, textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 32.dp))
        Spacer(Modifier.height(24.dp))
        Button(onClick = onRetry, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A1A1A)), shape = RoundedCornerShape(16.dp)) {
            Text("Retry", color = Color.White)
        }
    }
}
