@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package iad1tya.echo.music.ui.screens.settings

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.ripple
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import iad1tya.echo.music.BuildConfig
import iad1tya.echo.music.LocalPlayerAwareWindowInsets
import iad1tya.echo.music.R
import iad1tya.echo.music.ui.component.IconButton
import iad1tya.echo.music.ui.utils.backToMain

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
    onBack: (() -> Unit)? = null,
    highlightKey: String? = null
) {
    val uriHandler = LocalUriHandler.current
    val context = LocalContext.current

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = Color.Black,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.about),
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = { onBack?.invoke() ?: navController.navigateUp() },
                        onLongClick = navController::backToMain,
                    ) {
                        Icon(
                            painterResource(R.drawable.arrow_back), 
                            contentDescription = null,
                            tint = Color.White
                        )
                    }
                },
                windowInsets = TopAppBarDefaults.windowInsets,
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = Color.Black,
                    scrolledContainerColor = Color(0xFF0B0B0F),
                    titleContentColor = Color.White
                ),
                scrollBehavior = scrollBehavior,
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(
                    LocalPlayerAwareWindowInsets.current.only(
                        WindowInsetsSides.Horizontal,
                    ),
                ),
            contentPadding = PaddingValues(
                start = 16.dp,
                top = innerPadding.calculateTopPadding() + 8.dp,
                end = 16.dp,
                bottom = androidx.compose.foundation.layout.WindowInsets.systemBars.asPaddingValues().calculateBottomPadding() + 32.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item { AboutAppCard() }

            item {
                AboutSectionCard(title = "Summary") {
                    Text(
                        text = "MelodyX is a high-performance, premium music streaming experience built for audiophiles. With a focus on speed, elegant design, and seamless integration, it delivers ad-free music with advanced features like Melody Brain AI and word-by-word synchronized lyrics.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.padding(16.dp),
                        textAlign = TextAlign.Start
                    )
                }
            }

            item {
                AboutSectionCard(title = "Developer") {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Created by KITU",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF8B5CF6)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "A passionate developer with over 2.5 years of expertise in Python, Java, and C++. KITU focuses on building efficient, user-centric applications with cutting-edge technology and modern design principles.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }
                    AboutDivider()
                    AboutActionRow(
                        icon = painterResource(R.drawable.ic_instagram_new),
                        title = "Instagram",
                        subtitle = "@codexkitu",
                        onClick = { uriHandler.openUri("https://www.instagram.com/codexkitu?igsh=czEyN245czRqaTlx") },
                    )
                    AboutDivider()
                    AboutActionRow(
                        icon = painterResource(R.drawable.ic_x_new),
                        title = "X (Twitter)",
                        subtitle = "@KAUSHIKBORav",
                        onClick = { uriHandler.openUri("https://x.com/KAUSHIKBORav") },
                    )
                    AboutDivider()
                    AboutActionRow(
                        icon = painterResource(R.drawable.github),
                        title = "GitHub",
                        subtitle = "kituontop69-cell",
                        onClick = { uriHandler.openUri("https://github.com/kituontop69-cell") },
                    )
                }
            }
        }
    }
}

@Composable
private fun AboutAppCard() {
    val infiniteTransition = rememberInfiniteTransition(label = "premium_visuals")
    
    // Slow 18-second rotation for the overall gradient
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(18000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    // Pulse animation traveling around the border
    val pulseProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse"
    )

    val electricPurple = Color(0xFF8B5CF6)
    val neonBlue = Color(0xFF3B82F6)
    val cyan = Color(0xFF06B6D4)
    val magenta = Color(0xFFD946EF)
    
    val premiumColors = listOf(electricPurple, neonBlue, cyan, magenta, electricPurple)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        // Soft, diffused ambient glow (30% opacity, 8px blur equivalent)
        Canvas(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .height(190.dp)
                .graphicsLayer { rotationZ = rotation }
                .blur(8.dp)
                .alpha(0.35f)
        ) {
            drawRoundRect(
                brush = Brush.sweepGradient(premiumColors),
                style = Stroke(width = 12.dp.toPx()),
                cornerRadius = CornerRadius(30.dp.toPx())
            )
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .drawWithContent {
                    drawContent()
                    
                    // Very thin 1.5px glowing border
                    val strokeWidth = 1.5.dp.toPx()
                    drawRoundRect(
                        brush = Brush.sweepGradient(
                            colors = premiumColors,
                            center = center
                        ),
                        style = Stroke(width = strokeWidth),
                        cornerRadius = CornerRadius(28.dp.toPx())
                    )
                    
                    // Traveling light pulse effect
                    val pulseBrush = Brush.sweepGradient(
                        0f to Color.Transparent,
                        pulseProgress to Color.White.copy(alpha = 0.4f),
                        (pulseProgress + 0.1f).coerceAtMost(1f) to Color.Transparent,
                        center = center
                    )
                    drawRoundRect(
                        brush = pulseBrush,
                        style = Stroke(width = strokeWidth * 1.5f),
                        cornerRadius = CornerRadius(28.dp.toPx())
                    )
                },
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF0B0B0F), // Matte Black
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        ) {
            // Content with a glassmorphism feel (subtle gradient overlay)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.White.copy(alpha = 0.03f), Color.Transparent)
                        )
                    )
                    .padding(vertical = 32.dp, horizontal = 24.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    var isEasterEggActive by remember { mutableStateOf(false) }
                    val flipRotation by animateFloatAsState(
                        targetValue = if (isEasterEggActive) 180f else 0f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        ),
                        label = "flip"
                    )
                    
                    val interactionSource = remember { MutableInteractionSource() }
                    val isPressed by interactionSource.collectIsPressedAsState()
                    val scale by animateFloatAsState(
                        targetValue = if (isPressed) 0.92f else 1f,
                        animationSpec = spring(stiffness = Spring.StiffnessMedium),
                        label = "scale"
                    )

                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .graphicsLayer {
                                rotationY = flipRotation
                                scaleX = scale
                                scaleY = scale
                                cameraDistance = 15f * density
                            }
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.2f))
                            .clickable(
                                interactionSource = interactionSource,
                                indication = null,
                                onClick = { isEasterEggActive = !isEasterEggActive }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (flipRotation <= 90f) {
                            Image(
                                painter = painterResource(R.drawable.ic_launcher_nobg),
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            coil3.compose.AsyncImage(
                                model = "https://avatars.githubusercontent.com/u/147871321?v=4",
                                contentDescription = null,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .graphicsLayer { rotationY = 180f },
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop
                            )
                        }
                    }
                    
                    Spacer(Modifier.height(4.dp))
                    
                    Text(
                        text = if (flipRotation <= 90f) "MELODY X" else "Created by KITU",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        letterSpacing = 0.5.sp
                    )
                    
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color.White.copy(alpha = 0.05f),
                            border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.1f))
                        ) {
                            Text(
                                text = BuildConfig.VERSION_NAME,
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.6f),
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AboutSectionCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF8B5CF6).copy(alpha = 0.8f),
            modifier = Modifier.padding(start = 8.dp),
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF0B0B0F),
            ),
            border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.05f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        ) {
            Column(
                modifier = Modifier.padding(vertical = 4.dp),
                content = content,
            )
        }
    }
}

@Composable
private fun AboutActionRow(
    icon: Painter,
    title: String,
    subtitle: String? = null,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessHigh),
        label = "rowScale",
    )
    val tint = Color(0xFF8B5CF6)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 6.dp, vertical = 2.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(RoundedCornerShape(20.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(),
                onClick = onClick,
            ),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Surface(
                modifier = Modifier.size(40.dp),
                shape = RoundedCornerShape(12.dp),
                color = tint.copy(alpha = 0.12f),
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Icon(
                        painter = icon,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = tint.copy(alpha = 0.9f),
                    )
                }
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                subtitle?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.5f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Icon(
                painter = painterResource(R.drawable.arrow_forward),
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = Color.White.copy(alpha = 0.3f),
            )
        }
    }
}

@Composable
private fun AboutDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(start = 72.dp, end = 20.dp),
        thickness = 0.5.dp,
        color = Color.White.copy(alpha = 0.05f),
    )
}
