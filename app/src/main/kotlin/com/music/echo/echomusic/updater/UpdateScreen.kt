package iad1tya.echo.music.echomusic.updater

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import iad1tya.echo.music.R
import iad1tya.echo.music.ui.component.IconButton
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpdateScreen(
    navController: NavController,
    viewModel: UpdateViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val downloadState by viewModel.downloadState.collectAsState()
    val context = LocalContext.current
    var lastCheckedTime by remember { mutableStateOf("Never") }
    var showInstallDialog by remember { mutableStateOf(false) }

    val cyanAccent = Color(0xFF5CE1E6)
    val cyanButtonBg = Color(0xFF80DEEA)
    val darkBg = Color(0xFF0F1417)
    val darkCardBg = Color(0xFF1B2228)
    val darkBorder = Color(0xFF2E363F)
    val darkProgressTrack = Color(0xFF1E262C)

    // Check updates & observe download state
    LaunchedEffect(Unit) {
        viewModel.checkForUpdates(context, silent = false)
        lastCheckedTime = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
    }

    Scaffold(
        containerColor = darkBg,
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(
                        onClick = { navController.navigateUp() },
                        modifier = Modifier.padding(start = 8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF1F2429)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.arrow_back),
                                contentDescription = "Back",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(darkBg)
        ) {
            when (val state = uiState) {
                is UpdateUiState.Available -> {
                    val info = state.updateInfo

                    // Auto-trigger observe on state load
                    LaunchedEffect(info.versionName) {
                        viewModel.observeDownloadProgress(context, info.versionName)
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 24.dp)
                    ) {
                        Spacer(Modifier.height(8.dp))

                        // Title: New update vX.X.X
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "New update ",
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "v${info.versionName}",
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                                color = cyanAccent
                            )
                        }

                        Spacer(Modifier.height(16.dp))

                        // Metadata info
                        val formattedDate = formatReleaseDate(info.releaseDate)
                        Text(
                            text = "Released on: $formattedDate",
                            fontSize = 14.sp,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                        if (info.apkSize.isNotBlank()) {
                            Text(
                                text = "Size: ${info.apkSize}",
                                fontSize = 14.sp,
                                color = Color.White.copy(alpha = 0.7f)
                            )
                        }

                        Spacer(Modifier.height(24.dp))

                        // Scrollable Release Notes / Changelog
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState())
                        ) {
                            val changelogLines = remember(info) { buildChangelogList(info) }
                            if (changelogLines.isNotEmpty()) {
                                changelogLines.forEach { line ->
                                    if (line.isHeader) {
                                        Spacer(Modifier.height(12.dp))
                                        Text(
                                            text = line.text,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                        Spacer(Modifier.height(4.dp))
                                    } else {
                                        Text(
                                            text = line.text,
                                            fontSize = 14.sp,
                                            color = Color.White.copy(alpha = 0.85f),
                                            lineHeight = 20.sp,
                                            modifier = Modifier.padding(vertical = 2.dp)
                                        )
                                    }
                                }
                            } else {
                                Text(
                                    text = "- Performance improvements and minor bug fixes.",
                                    fontSize = 14.sp,
                                    color = Color.White.copy(alpha = 0.85f),
                                    lineHeight = 20.sp
                                )
                            }
                        }

                        // Downloading Progress Bar (Screenshot 3)
                        if (downloadState is DownloadState.Downloading) {
                            val progress = (downloadState as DownloadState.Downloading).progress
                            Spacer(Modifier.height(12.dp))
                            LinearProgressIndicator(
                                progress = progress,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(2.dp)),
                                color = cyanAccent,
                                trackColor = darkProgressTrack
                            )
                            Spacer(Modifier.height(12.dp))
                        } else {
                            Spacer(Modifier.height(16.dp))
                        }

                        // Bottom Action Buttons (Later & Update/Install/Progress)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 24.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Left Button: Later
                            Button(
                                onClick = { navController.navigateUp() },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(54.dp),
                                shape = RoundedCornerShape(50),
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                                border = BorderStroke(1.dp, darkBorder)
                            ) {
                                Text(
                                    text = "Later",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White
                                )
                            }

                            // Right Button: Dynamic action based on downloadState
                            when (val dState = downloadState) {
                                is DownloadState.Downloading -> {
                                    val percent = (dState.progress * 100).toInt()
                                    Button(
                                        onClick = { },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(54.dp),
                                        shape = RoundedCornerShape(50),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF222930))
                                    ) {
                                        Text(
                                            text = "$percent%",
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = cyanAccent
                                        )
                                    }
                                }
                                is DownloadState.Completed -> {
                                    Button(
                                        onClick = { showInstallDialog = true },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(54.dp),
                                        shape = RoundedCornerShape(50),
                                        colors = ButtonDefaults.buttonColors(containerColor = cyanButtonBg)
                                    ) {
                                        Text(
                                            text = "Install",
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF0F1417)
                                        )
                                    }
                                }
                                else -> {
                                    Button(
                                        onClick = { viewModel.startDownload(context, info) },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(54.dp),
                                        shape = RoundedCornerShape(50),
                                        colors = ButtonDefaults.buttonColors(containerColor = cyanButtonBg)
                                    ) {
                                        Text(
                                            text = "Update",
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF0F1417)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // In-App Confirmation Dialog (Screenshot 5)
                    if (showInstallDialog) {
                        AlertDialog(
                            onDismissRequest = { showInstallDialog = false },
                            containerColor = darkCardBg,
                            shape = RoundedCornerShape(24.dp),
                            title = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Image(
                                        painter = painterResource(R.mipmap.ic_launcher),
                                        contentDescription = null,
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                    )
                                    Spacer(Modifier.width(12.dp))
                                    Text(
                                        text = "Echo Music",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp
                                    )
                                }
                            },
                            text = {
                                Text(
                                    text = "Do you want to update this app?",
                                    color = Color.White.copy(alpha = 0.85f),
                                    fontSize = 15.sp
                                )
                            },
                            dismissButton = {
                                TextButton(onClick = { showInstallDialog = false }) {
                                    Text(
                                        text = "Cancel",
                                        color = cyanAccent,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp
                                    )
                                }
                            },
                            confirmButton = {
                                TextButton(
                                    onClick = {
                                        showInstallDialog = false
                                        if (downloadState is DownloadState.Completed) {
                                            val filePath = (downloadState as DownloadState.Completed).fileUri
                                            viewModel.installApk(context, filePath)
                                        }
                                    }
                                ) {
                                    Text(
                                        text = "Update",
                                        color = cyanAccent,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp
                                    )
                                }
                            }
                        )
                    }
                }
                else -> {
                    // Up to date or loading state
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        if (state is UpdateUiState.Checking) {
                            CircularProgressIndicator(color = cyanAccent)
                            Spacer(Modifier.height(16.dp))
                            Text(
                                text = "Checking for updates...",
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 15.sp
                            )
                        } else {
                            Icon(
                                painter = painterResource(R.drawable.check),
                                contentDescription = null,
                                modifier = Modifier.size(72.dp),
                                tint = Color(0xFF10B981)
                            )
                            Spacer(Modifier.height(16.dp))
                            Text(
                                text = "YOU ARE ON THE LATEST VERSION",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                textAlign = TextAlign.Center
                            )
                        }

                        if (state is UpdateUiState.Error) {
                            Spacer(Modifier.height(16.dp))
                            Text(
                                text = state.message,
                                color = Color.Red.copy(alpha = 0.8f),
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
}

private data class ChangelogLine(val text: String, val isHeader: Boolean = false)

private fun buildChangelogList(info: UpdateInfo): List<ChangelogLine> {
    val list = mutableListOf<ChangelogLine>()
    val changelog = info.changelog

    if (changelog.features.isNotEmpty()) {
        changelog.features.forEach { list.add(ChangelogLine("- $it")) }
    }
    if (changelog.improvements.isNotEmpty()) {
        changelog.improvements.forEach { list.add(ChangelogLine("- $it")) }
    }
    if (changelog.bugFixes.isNotEmpty()) {
        changelog.bugFixes.forEach { list.add(ChangelogLine("- $it")) }
    }

    return list
}

private fun formatReleaseDate(rawDate: String): String {
    if (rawDate.isBlank()) return "Recently"
    return try {
        if (rawDate.contains("T")) {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault())
            val outputFormat = SimpleDateFormat("d MMMM yyyy, h:mm a", Locale.getDefault())
            val parsed = inputFormat.parse(rawDate)
            if (parsed != null) outputFormat.format(parsed) else rawDate
        } else if (rawDate.matches(Regex("\\d{4}-\\d{2}-\\d{2}"))) {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val outputFormat = SimpleDateFormat("d MMMM yyyy", Locale.getDefault())
            val parsed = inputFormat.parse(rawDate)
            if (parsed != null) outputFormat.format(parsed) else rawDate
        } else {
            rawDate
        }
    } catch (e: Exception) {
        rawDate
    }
}
