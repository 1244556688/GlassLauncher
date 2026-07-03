package com.example

import android.content.Context
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AppSettingsAlt
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.BatteryStd
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothDisabled
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardVoice
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarOutline
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.GlassCard
import com.example.ui.components.glassmorphic
import com.example.ui.theme.CyanGlow
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.IceBlue
import com.example.ui.theme.MagentaGlow
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.ObsidianDark
import com.example.ui.theme.SpaceNavy
import com.example.ui.theme.SunsetOrange

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Setup state holder
        val viewModel = LauncherViewModel(applicationContext)

        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = ObsidianDark
                ) {
                    GlassLauncherMainScreen(viewModel = viewModel)
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GlassLauncherMainScreen(viewModel: LauncherViewModel) {
    val context = LocalContext.current
    val currentTheme by viewModel.selectedTheme.collectAsState()
    
    // Manage long press / options modal
    var selectedAppForOptions by remember { mutableStateOf<AppItem?>(null) }
    val dockPackageNames by viewModel.dockPackageNames.collectAsState()

    Box(modifier = Modifier.fillMaxSize()) {
        
        // 1. Dynamic Wallpaper Layer
        WallpaperBackground(theme = currentTheme)

        // 2. Main Content Grid (Adaptive landscape vs portrait)
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            val isLandscape = maxWidth > maxHeight

            if (isLandscape) {
                // Tablet Landscape Layout
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    // Left Column: Control Center & Widgets (32% width)
                    Column(
                        modifier = Modifier
                            .weight(0.32f)
                            .fillMaxHeight(),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        ClockWidget(viewModel = viewModel)
                        QuickControlsWidget(viewModel = viewModel)
                        SystemTelemetryWidget(viewModel = viewModel)
                        ThemeCustomizerWidget(viewModel = viewModel)
                    }

                    // Right Column: Apps Grid & Dock (68% width)
                    Column(
                        modifier = Modifier
                            .weight(0.68f)
                            .fillMaxHeight(),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Search bar
                        SearchBarWidget(viewModel = viewModel)

                        // Apps Drawer Area
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                        ) {
                            AppsGrid(
                                viewModel = viewModel,
                                onAppLongClick = { selectedAppForOptions = it },
                                gridCells = GridCells.Adaptive(minSize = 92.dp)
                            )
                        }

                        // Bottom Pinned Dock
                        DockWidget(
                            viewModel = viewModel,
                            onAppLongClick = { selectedAppForOptions = it }
                        )
                    }
                }
            } else {
                // Portrait Tablet/Phone Layout
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Top Row/Carousel: Clock & Telemetry
                    ClockWidget(viewModel = viewModel)

                    // Compact settings & customizer row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(modifier = Modifier.weight(1f)) {
                            SystemTelemetryWidget(viewModel = viewModel, compact = true)
                        }
                        Box(modifier = Modifier.weight(1f)) {
                            QuickControlsWidget(viewModel = viewModel, compact = true)
                        }
                    }

                    // Search bar
                    SearchBarWidget(viewModel = viewModel)

                    // Apps Grid (Expanded)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                        AppsGrid(
                            viewModel = viewModel,
                            onAppLongClick = { selectedAppForOptions = it },
                            gridCells = GridCells.Fixed(4)
                        )
                    }

                    // Dock
                    DockWidget(
                        viewModel = viewModel,
                        onAppLongClick = { selectedAppForOptions = it }
                    )
                }
            }
        }

        // 3. App Options Glass Dialogue overlay (Long press app menu)
        selectedAppForOptions?.let { app ->
            val isDocked = dockPackageNames.contains(app.packageName)
            AppActionsDialog(
                app = app,
                isDocked = isDocked,
                onDismiss = { selectedAppForOptions = null },
                onToggleDock = {
                    viewModel.toggleDockApp(app.packageName)
                    selectedAppForOptions = null
                },
                onOpenSettings = {
                    viewModel.openAppInfo(app.packageName)
                    selectedAppForOptions = null
                },
                onUninstall = {
                    viewModel.uninstallApp(app.packageName)
                    selectedAppForOptions = null
                }
            )
        }
    }
}

// Wallpaper backgrounds with smooth glowing effects
@Composable
fun WallpaperBackground(theme: WallpaperTheme) {
    Box(modifier = Modifier.fillMaxSize()) {
        when (theme) {
            WallpaperTheme.SPACE_NEBULA -> {
                // High fidelity generated image background
                Image(
                    painter = painterResource(id = R.drawable.img_glass_bg),
                    contentDescription = "Cosmic Glassmorphism Backdrop",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                
                // Subtle rotating light mesh on top
                val infiniteTransition = rememberInfiniteTransition(label = "nebula")
                val angle by infiniteTransition.animateFloat(
                    initialValue = 0f,
                    targetValue = 360f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(120000, easing = LinearEasing),
                        repeatMode = RepeatMode.Restart
                    ),
                    label = "nebula_angle"
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .rotate(angle)
                        .scale(1.4f)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    CyanGlow.copy(alpha = 0.08f),
                                    Color.Transparent,
                                    MagentaGlow.copy(alpha = 0.05f),
                                    Color.Transparent
                                )
                            )
                        )
                )
            }
            WallpaperTheme.CYAN_AURORA -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    SpaceNavy,
                                    Color(0xFF00333A),
                                    Color(0xFF031E20)
                                )
                            )
                        )
                )
                // Aurora flow
                val infiniteTransition = rememberInfiniteTransition(label = "aurora")
                val shiftY by infiniteTransition.animateFloat(
                    initialValue = -100f,
                    targetValue = 100f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(8000, easing = LinearEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "aurora_shift"
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .offset(y = shiftY.dp)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    EmeraldGreen.copy(alpha = 0.12f),
                                    CyanGlow.copy(alpha = 0.08f),
                                    Color.Transparent
                                ),
                                radius = 900f
                            )
                        )
                )
            }
            WallpaperTheme.OBSIDIAN_DEEP -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFF1B1F38),
                                    ObsidianDark,
                                    Color.Black
                                ),
                                radius = 1200f
                            )
                        )
                )
                // Add simple futuristic tech grid
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.015f),
                                    Color.Transparent
                                )
                            )
                        )
                )
            }
            WallpaperTheme.CYBER_SUNSET -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFF2D0938),
                                    Color(0xFF4C0E35),
                                    Color(0xFF441B08),
                                    ObsidianDark
                                )
                            )
                        )
                )
                // Neon glow sun
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    SunsetOrange.copy(alpha = 0.15f),
                                    MagentaGlow.copy(alpha = 0.10f),
                                    Color.Transparent
                                ),
                                radius = 800f
                            )
                        )
                )
            }
        }
    }
}

// Widget: Glass Clock and Calendar
@Composable
fun ClockWidget(viewModel: LauncherViewModel) {
    val time by viewModel.currentTime.collectAsState()
    val date by viewModel.currentDate.collectAsState()

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        elevation = 12.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "GLASS OS",
                    color = CyanGlow,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 3.sp
                )
                
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(CyanGlow.copy(alpha = 0.15f))
                        .border(1.dp, CyanGlow.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "ONLINE",
                        color = CyanGlow,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = time.ifEmpty { "00:00:00" },
                color = Color.White,
                fontSize = 42.sp,
                fontWeight = FontWeight.W800,
                fontFamily = FontFamily.Monospace,
                letterSpacing = (-1).sp,
                modifier = Modifier.testTag("clock_time_text")
            )

            Text(
                text = date.ifEmpty { "Welcome back" },
                color = IceBlue.copy(alpha = 0.8f),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.5.sp
            )
        }
    }
}

// Widget: Systems Controls, toggles WiFi, Bluetooth, etc.
@Composable
fun QuickControlsWidget(viewModel: LauncherViewModel, compact: Boolean = false) {
    val context = LocalContext.current
    val isWifiEnabled by viewModel.wifiEnabled.collectAsState()
    val isBtEnabled by viewModel.bluetoothEnabled.collectAsState()
    val volume by viewModel.systemVolume.collectAsState()
    val brightness by viewModel.systemBrightness.collectAsState()

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        elevation = 10.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(if (compact) 12.dp else 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (!compact) {
                Text(
                    text = "CONTROL DECK",
                    color = IceBlue.copy(alpha = 0.5f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 2.sp
                )
            }

            // Quick Toggle Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // WiFi Button
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(54.dp)
                        .glassmorphic(
                            shape = RoundedCornerShape(12.dp),
                            backgroundColor = if (isWifiEnabled) CyanGlow.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.05f),
                            borderColor = if (isWifiEnabled) CyanGlow.copy(alpha = 0.4f) else Color.White.copy(alpha = 0.12f)
                        )
                        .clickable {
                            viewModel.wifiEnabled.value = !isWifiEnabled
                        }
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = if (isWifiEnabled) Icons.Filled.Wifi else Icons.Filled.WifiOff,
                            contentDescription = "WiFi",
                            tint = if (isWifiEnabled) CyanGlow else Color.White.copy(alpha = 0.5f),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isWifiEnabled) "WIFI" else "OFF",
                            color = if (isWifiEnabled) Color.White else Color.White.copy(alpha = 0.5f),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                // Bluetooth Button
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(54.dp)
                        .glassmorphic(
                            shape = RoundedCornerShape(12.dp),
                            backgroundColor = if (isBtEnabled) MagentaGlow.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.05f),
                            borderColor = if (isBtEnabled) MagentaGlow.copy(alpha = 0.4f) else Color.White.copy(alpha = 0.12f)
                        )
                        .clickable {
                            viewModel.bluetoothEnabled.value = !isBtEnabled
                        }
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = if (isBtEnabled) Icons.Filled.Bluetooth else Icons.Filled.BluetoothDisabled,
                            contentDescription = "Bluetooth",
                            tint = if (isBtEnabled) MagentaGlow else Color.White.copy(alpha = 0.5f),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isBtEnabled) "BT" else "OFF",
                            color = if (isBtEnabled) Color.White else Color.White.copy(alpha = 0.5f),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                // System Settings button
                IconButton(
                    onClick = { viewModel.openSettings(Settings.ACTION_SETTINGS) },
                    modifier = Modifier
                        .size(54.dp)
                        .glassmorphic(
                            shape = RoundedCornerShape(12.dp),
                            backgroundColor = Color.White.copy(alpha = 0.05f),
                            borderColor = Color.White.copy(alpha = 0.15f)
                        )
                        .testTag("settings_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Settings,
                        contentDescription = "System Settings",
                        tint = Color.White.copy(alpha = 0.8f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Slider parameters (only shown in full non-compact widget)
            if (!compact) {
                // Brightness slider
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("BRIGHTNESS", color = Color.White.copy(alpha = 0.6f), fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        Text("${(brightness * 100).toInt()}%", color = CyanGlow, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    }
                    Slider(
                        value = brightness,
                        onValueChange = { viewModel.systemBrightness.value = it },
                        colors = SliderDefaults.colors(
                            activeTrackColor = CyanGlow,
                            inactiveTrackColor = Color.White.copy(alpha = 0.1f),
                            thumbColor = CyanGlow
                        ),
                        modifier = Modifier.height(24.dp)
                    )
                }

                // Volume slider
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("SYSTEM VOLUME", color = Color.White.copy(alpha = 0.6f), fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        Text("${(volume * 100).toInt()}%", color = MagentaGlow, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    }
                    Slider(
                        value = volume,
                        onValueChange = { viewModel.systemVolume.value = it },
                        colors = SliderDefaults.colors(
                            activeTrackColor = MagentaGlow,
                            inactiveTrackColor = Color.White.copy(alpha = 0.1f),
                            thumbColor = MagentaGlow
                        ),
                        modifier = Modifier.height(24.dp)
                    )
                }
            }
        }
    }
}

// Widget: RAM/Memory & Battery monitors
@Composable
fun SystemTelemetryWidget(viewModel: LauncherViewModel, compact: Boolean = false) {
    val ramUsageString by viewModel.ramUsage.collectAsState()
    val ramPct by viewModel.ramPercentage.collectAsState()
    val batteryPct by viewModel.batteryLevel.collectAsState()
    val isCharging by viewModel.isBatteryCharging.collectAsState()

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        elevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(if (compact) 12.dp else 16.dp),
            verticalArrangement = Arrangement.spacedBy(if (compact) 8.dp else 12.dp)
        ) {
            if (!compact) {
                Text(
                    text = "SYSTEM TELEMETRY",
                    color = IceBlue.copy(alpha = 0.5f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 2.sp
                )
            }

            // RAM usage details
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.Memory,
                    contentDescription = "RAM Usage",
                    tint = CyanGlow,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("MEMORY BUFFER", color = Color.White.copy(alpha = 0.8f), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        Text("${(ramPct * 100).toInt()}%", color = CyanGlow, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    // Glass Progress bar
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.1f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(ramPct)
                                .clip(CircleShape)
                                .background(
                                    Brush.horizontalGradient(
                                        colors = listOf(CyanGlow, MagentaGlow)
                                    )
                                )
                        )
                    }
                }
            }

            // Battery details
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val batteryIcon = when {
                    isCharging -> Icons.Filled.BatteryChargingFull
                    batteryPct < 20 -> Icons.Filled.BatteryAlert
                    else -> Icons.Filled.BatteryStd
                }
                val batteryColor = when {
                    isCharging -> EmeraldGreen
                    batteryPct < 20 -> SunsetOrange
                    else -> CyanGlow
                }

                Icon(
                    imageVector = batteryIcon,
                    contentDescription = "Battery Status",
                    tint = batteryColor,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(if (isCharging) "DOCK POWER: CHARGING" else "BATTERY POWER", color = Color.White.copy(alpha = 0.8f), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        Text("$batteryPct%", color = batteryColor, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    // Glass Progress bar
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.1f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(batteryPct / 100f)
                                .clip(CircleShape)
                                .background(batteryColor)
                        )
                    }
                }
            }
        }
    }
}

// Widget: Theme customizer for changing dynamic background theme instantly
@Composable
fun ThemeCustomizerWidget(viewModel: LauncherViewModel) {
    val activeTheme by viewModel.selectedTheme.collectAsState()

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        elevation = 6.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.Palette,
                    contentDescription = "Theme Customizer",
                    tint = MagentaGlow,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "THEME SELECTOR",
                    color = IceBlue.copy(alpha = 0.5f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 2.sp
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                WallpaperTheme.values().forEach { theme ->
                    val isActive = activeTheme == theme
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isActive) MagentaGlow.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.05f))
                            .border(
                                1.dp,
                                if (isActive) MagentaGlow.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.12f),
                                RoundedCornerShape(8.dp)
                            )
                            .clickable { viewModel.updateTheme(theme) }
                            .padding(vertical = 8.dp, horizontal = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = theme.displayName.substringBefore(" ("),
                            color = if (isActive) Color.White else Color.White.copy(alpha = 0.6f),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            fontFamily = FontFamily.Monospace,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

// Widget: App search bar
@Composable
fun SearchBarWidget(viewModel: LauncherViewModel) {
    val query by viewModel.searchQuery.collectAsState()

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        backgroundColor = Color.White.copy(alpha = 0.05f),
        borderColor = Color.White.copy(alpha = 0.15f),
        elevation = 10.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.Search,
                contentDescription = "Search Apps",
                tint = CyanGlow,
                modifier = Modifier.size(22.dp)
            )
            
            TextField(
                value = query,
                onValueChange = { viewModel.setSearchQuery(it) },
                placeholder = {
                    Text(
                        "Search installer files and application packets...",
                        color = Color.White.copy(alpha = 0.4f),
                        fontSize = 13.sp,
                        fontFamily = FontFamily.SansSerif
                    )
                },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    cursorColor = CyanGlow
                ),
                singleLine = true,
                modifier = Modifier
                    .weight(1f)
                    .testTag("search_bar")
            )

            if (query.isNotEmpty()) {
                IconButton(onClick = { viewModel.setSearchQuery("") }) {
                    Icon(
                        imageVector = Icons.Filled.Refresh,
                        contentDescription = "Clear Search",
                        tint = Color.White.copy(alpha = 0.6f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            } else {
                Icon(
                    imageVector = Icons.Filled.KeyboardVoice,
                    contentDescription = "Voice Search",
                    tint = Color.White.copy(alpha = 0.4f),
                    modifier = Modifier
                        .size(18.dp)
                        .clickable { /* Trigger voice if available */ }
                )
            }
        }
    }
}

// Widget: Primary workspace Apps list grid
@Composable
fun AppsGrid(
    viewModel: LauncherViewModel,
    onAppLongClick: (AppItem) -> Unit,
    gridCells: GridCells
) {
    val apps by viewModel.filteredApps.collectAsState()

    if (apps.isEmpty()) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Filled.AppSettingsAlt,
                contentDescription = "No apps found",
                tint = Color.White.copy(alpha = 0.2f),
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "NO COMPATIBLE PACKETS DETECTED",
                color = Color.White.copy(alpha = 0.4f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 1.5.sp
            )
        }
    } else {
        LazyVerticalGrid(
            columns = gridCells,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(apps, key = { it.packageName }) { app ->
                AppTile(
                    app = app,
                    onClick = { viewModel.launchApp(app) },
                    onLongClick = { onAppLongClick(app) }
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppTile(
    app: AppItem,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .background(Color.White.copy(alpha = 0.02f))
            .border(1.dp, Color.White.copy(alpha = 0.04f), RoundedCornerShape(16.dp))
            .padding(vertical = 12.dp, horizontal = 8.dp)
            .testTag("app_tile_${app.packageName}"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // App icon holder with neon backdrop ring
        Box(
            modifier = Modifier
                .size(54.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.05f),
                            Color.Transparent
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            if (app.iconBitmap != null) {
                Image(
                    bitmap = app.iconBitmap,
                    contentDescription = app.label,
                    modifier = Modifier.size(44.dp)
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(CyanGlow.copy(alpha = 0.15f))
                        .border(1.dp, CyanGlow, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = app.label.take(1).uppercase(),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Label
        Text(
            text = app.label,
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 2.dp)
        )
    }
}

// Widget: Pinned bottom launcher deck (DOCK)
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DockWidget(
    viewModel: LauncherViewModel,
    onAppLongClick: (AppItem) -> Unit
) {
    val dockApps by viewModel.dockApps.collectAsState()

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp),
        shape = RoundedCornerShape(24.dp),
        backgroundColor = Color.White.copy(alpha = 0.05f),
        borderColor = Color.White.copy(alpha = 0.18f),
        elevation = 16.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (dockApps.isEmpty()) {
                // Friendly drag and drop tips
                Text(
                    text = "LONG-PRESS ANY MAIN APP PACKET TO ADD TO DOCK",
                    color = Color.White.copy(alpha = 0.35f),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                )
            } else {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    dockApps.take(10).forEach { app ->
                        Box(
                            modifier = Modifier
                                .size(58.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .combinedClickable(
                                    onClick = { viewModel.launchApp(app) },
                                    onLongClick = { onAppLongClick(app) }
                                )
                                .background(Color.White.copy(alpha = 0.04f))
                                .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(14.dp))
                                .testTag("dock_app_${app.packageName}"),
                            contentAlignment = Alignment.Center
                        ) {
                            if (app.iconBitmap != null) {
                                Image(
                                    bitmap = app.iconBitmap,
                                    contentDescription = app.label,
                                    modifier = Modifier.size(38.dp)
                                )
                            } else {
                                Text(
                                    text = app.label.take(1).uppercase(),
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// Dialog Component: Pop-up overlay when long pressing apps to add/remove pin, view app info, or uninstall
@Composable
fun AppActionsDialog(
    app: AppItem,
    isDocked: Boolean,
    onDismiss: () -> Unit,
    onToggleDock: () -> Unit,
    onOpenSettings: () -> Unit,
    onUninstall: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.4f))
            .clickable { onDismiss() },
        contentAlignment = Alignment.Center
    ) {
        GlassCard(
            modifier = Modifier
                .widthIn(max = 320.dp)
                .padding(24.dp)
                .clickable(enabled = false) {}, // Prevent dismiss click propagate
            shape = RoundedCornerShape(24.dp),
            backgroundColor = Color(0xFF0F121C).copy(alpha = 0.9f),
            borderColor = CyanGlow.copy(alpha = 0.3f),
            elevation = 24.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // App identity display
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.05f))
                        .border(1.dp, CyanGlow.copy(alpha = 0.4f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (app.iconBitmap != null) {
                        Image(
                            bitmap = app.iconBitmap,
                            contentDescription = app.label,
                            modifier = Modifier.size(44.dp)
                        )
                    } else {
                        Text(
                            text = app.label.take(1).uppercase(),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = app.label,
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = app.packageName,
                        color = IceBlue.copy(alpha = 0.5f),
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Menu list items
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Toggle dock item
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(alpha = 0.04f))
                            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                            .clickable { onToggleDock() }
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isDocked) Icons.Filled.StarOutline else Icons.Filled.Star,
                            contentDescription = null,
                            tint = CyanGlow,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = if (isDocked) "UNPIN FROM DOCK" else "PIN TO DOCK",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    // Open app settings details
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(alpha = 0.04f))
                            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                            .clickable { onOpenSettings() }
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Info,
                            contentDescription = null,
                            tint = IceBlue,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "APP CONFIG DETAILS",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    // Uninstall app details
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(alpha = 0.04f))
                            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                            .clickable { onUninstall() }
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Delete,
                            contentDescription = null,
                            tint = MagentaGlow,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "UNINSTALL APPLICATION",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }
    }
}
