package iad1tya.echo.music.ui.screens.storyshare

import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun StoryShareScreen(vm: StoryShareViewModel, onResult: (ShareDestination) -> Unit) {
    val state by vm.state.collectAsState()
    val context = LocalContext.current
    val activity = remember(context) { context as ComponentActivity }

    when (val s = state) {
        StoryShareUiState.Loading -> Box(Modifier.fillMaxSize(), Alignment.Center) {
            CircularProgressIndicator()   // <300ms budget; swap for shimmer skeleton
        }
        is StoryShareUiState.Preview -> Column(
            Modifier.fillMaxSize().background(Color(0xFF0E0E12)).padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Live preview — the EXACT same composable the exporter renders
            Box(
                Modifier.weight(1f).aspectRatio(9f / 16f)
                    .clip(RoundedCornerShape(20.dp)),
            ) { StoryCard(s.data, s.palette, s.selected, s.blurredArt) }

            Spacer(Modifier.height(16.dp))
            Text("Choose your vibe", color = Color.White,
                fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(Modifier.height(10.dp))

            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(StoryTemplate.entries.size) { i ->
                    val t = StoryTemplate.entries[i]
                    val selected = t == s.selected
                    Text(
                        t.label,
                        Modifier.clip(RoundedCornerShape(20.dp))
                            .background(if (selected) Color(0xFF7C4DFF) else Color.White.copy(alpha = 0.08f))
                            .clickable { vm.selectTemplate(t) }
                            .padding(horizontal = 18.dp, vertical = 10.dp),
                        color = Color.White, fontSize = 14.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            Button(
                onClick = { vm.export(activity, onResult) },
                enabled = !s.exporting,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C4DFF)),
            ) {
                Text(if (s.exporting) "Polishing your story…" else "Share to Stories",
                    fontWeight = FontWeight.Bold)
            }
        }
    }
}
