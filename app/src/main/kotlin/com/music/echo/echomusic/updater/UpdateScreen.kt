package iad1tya.echo.music.echomusic.updater

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpdateScreen(
    navController: NavController,
    viewModel: UpdateViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var lastCheckedTime by remember { mutableStateOf("Never") }

    // 5-second auto-refresh loop
    LaunchedEffect(Unit) {
        var isFirstCheck = true
        while (true) {
            viewModel.checkForUpdates(silent = !isFirstCheck)
            isFirstCheck = false
            lastCheckedTime = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
            delay(5000L)
        }
    }

    Scaffold(
        containerColor = Color.Black,
        topBar = {
            TopAppBar(
                title = { Text("App Update", color = Color.White, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(painterResource(R.drawable.arrow_back), contentDescription = null, tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color.Black)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            when (val state = uiState) {
                is UpdateUiState.Available -> {
                    Icon(
                        painter = painterResource(R.drawable.new_release),
                        contentDescription = null,
                        modifier = Modifier.size(100.dp),
                        tint = Color(0xFF8B5CF6)
                    )
                    Spacer(Modifier.height(24.dp))
                    Text(
                        text = "NEW VERSION AVAILABLE",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        letterSpacing = 2.sp
                    )
                    Text(
                        text = "v${state.updateInfo.versionName}",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color(0xFF8B5CF6).copy(alpha = 0.8f),
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(32.dp))
                    Button(
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(state.updateInfo.apkUrl))
                            context.startActivity(intent)
                        },
                        modifier = Modifier.fillMaxWidth().height(64.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B5CF6)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("UPDATE NOW", fontSize = 20.sp, fontWeight = FontWeight.Black)
                    }
                }
                else -> {
                    // Show "Up to date" or "Checking" state
                    val isChecking = state is UpdateUiState.Checking || state is UpdateUiState.Idle
                    
                    if (isChecking && lastCheckedTime == "Never") {
                        CircularProgressIndicator(color = Color(0xFF8B5CF6))
                    } else {
                        Icon(
                            painter = painterResource(R.drawable.check),
                            contentDescription = null,
                            modifier = Modifier.size(80.dp),
                            tint = Color(0xFF10B981).copy(alpha = 0.6f)
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = "YOU ARE ON THE LATEST VERSION",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center
                        )
                    }
                    
                    Spacer(Modifier.height(48.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (state is UpdateUiState.Checking) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Color.Gray)
                            Spacer(Modifier.width(8.dp))
                        }
                        Text(
                            text = "Last checked: $lastCheckedTime (Refreshing every 5s)",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.Gray
                        )
                    }
                }
            }
            
            if (uiState is UpdateUiState.Error) {
                Spacer(Modifier.height(16.dp))
                Text(
                    text = (uiState as UpdateUiState.Error).message,
                    color = Color.Red.copy(alpha = 0.6f),
                    style = MaterialTheme.typography.labelSmall,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
