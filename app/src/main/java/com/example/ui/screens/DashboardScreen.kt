package com.example.ui.screens

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.ToggleOff
import androidx.compose.material.icons.filled.ToggleOn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.AppInfoItem
import com.example.ui.viewmodel.BoosterViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: BoosterViewModel,
    onNavigateToAppList: () -> Unit,
    onNavigateToStats: () -> Unit
) {
    val context = LocalContext.current

    val isServiceActive by viewModel.isServiceActive.collectAsState()
    val boostedAppsList by viewModel.combinedAppList.collectAsState()
    val addedBoostedApps = boostedAppsList.filter { it.isBoosted }

    val usagePermission by viewModel.isUsageAccessGranted.collectAsState()
    val overlayPermission by viewModel.isOverlayAccessGranted.collectAsState()
    val dndPermission by viewModel.isDndAccessGranted.collectAsState()

    val ramInfo by viewModel.systemRamUsage.collectAsState()
    val isCleaning by viewModel.isCleaningRam.collectAsState()
    val cleanedAmount by viewModel.cleanedRamAmount.collectAsState()

    var showCleanedAlert by remember { mutableStateOf(false) }
    var selectedAppToConfigure by remember { mutableStateOf<AppInfoItem?>(null) }

    // Recheck permissions when we resume
    LaunchedEffect(Unit) {
        viewModel.checkPermissions()
        viewModel.checkServiceStatus()
        viewModel.refreshRamUsage()
    }

    if (cleanedAmount > 0 && !isCleaning) {
        showCleanedAlert = true
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Speed,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "GAME BOOSTER",
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = onNavigateToStats,
                        modifier = Modifier.testTag("stats_navigation_button")
                    ) {
                        Icon(
                            Icons.Default.BarChart,
                            contentDescription = "Statistik",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToAppList,
                modifier = Modifier
                    .testTag("add_game_fab")
                    .padding(bottom = 16.dp, end = 8.dp),
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.Black,
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Tambah Game")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("TAMBAH GAME", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            // RAM visual gauges section
            item {
                RamPerformanceMeter(
                    percentUsed = ramInfo.percentUsed,
                    usedMb = ramInfo.usedRam,
                    totalMb = ramInfo.totalRam,
                    isCleaning = isCleaning,
                    onCleanClick = { viewModel.cleanRam() }
                )
            }

            // Cleanup Result Banner
            item {
                AnimatedVisibility(visible = showCleanedAlert) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                        ),
                        shape = RoundedCornerShape(16.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        "Optimalisasi Selesai!",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        "Berhasil membersihkan $cleanedAmount MB RAM",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                                    )
                                }
                            }

                            IconButton(
                                onClick = { showCleanedAlert = false },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Close",
                                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                )
                            }
                        }
                    }
                }
            }

            // Foreground Service Control Card
            item {
                BoosterServiceControl(
                    isActive = isServiceActive,
                    onToggle = { viewModel.toggleBoosterService() }
                )
            }

            // Permissions alert card
            val hasAllPermissions = usagePermission && overlayPermission && dndPermission
            if (!hasAllPermissions) {
                item {
                    PermissionStatusCard(
                        usageGranted = usagePermission,
                        overlayGranted = overlayPermission,
                        dndGranted = dndPermission,
                        onGrantUsage = {
                            val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
                            context.startActivity(intent)
                        },
                        onGrantOverlay = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                val intent = Intent(
                                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                    Uri.parse("package:" + context.packageName)
                                )
                                context.startActivity(intent)
                            }
                        },
                        onGrantDnd = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                val intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
                                context.startActivity(intent)
                            }
                        }
                    )
                }
            }

            // Disclaimer Card for RAM cleaning limitations on modern devices
            item {
                RamDisclaimerCard()
            }

            // Header for Boosted Apps
            item {
                Text(
                    text = "Daftar Aplikasi yang Di-Boost (${addedBoostedApps.size})",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                )
            }

            if (addedBoostedApps.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp)
                            .clickable { onNavigateToAppList() },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(20.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.Layers,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                "Belum Ada Aplikasi Ditambahkan",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "Ketuk di sini atau klik tombol TAMBAH GAME di bawah untuk memilih aplikasi dan mengaktifkan booster otomatis.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                textAlign = TextAlign.Center,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            } else {
                items(addedBoostedApps) { app ->
                    BoostedAppCard(
                        app = app,
                        onClick = { selectedAppToConfigure = app }
                    )
                }
            }

            // Bottom space
            item {
                Spacer(modifier = Modifier.height(100.dp))
            }
        }
    }

    // Per-app Boost Mode Settings Dialog
    selectedAppToConfigure?.let { app ->
        ModeSelectionDialog(
            app = app,
            onDismiss = { selectedAppToConfigure = null },
            onSaveMode = { mode ->
                viewModel.updateBoostMode(app.packageName, app.label, mode)
                selectedAppToConfigure = null
            },
            onRemove = {
                viewModel.toggleBoostApp(app)
                selectedAppToConfigure = null
            }
        )
    }
}

@Composable
fun RamPerformanceMeter(
    percentUsed: Int,
    usedMb: Long,
    totalMb: Long,
    isCleaning: Boolean,
    onCleanClick: () -> Unit
) {
    val animatedPercent by animateFloatAsState(
        targetValue = percentUsed.toFloat(),
        animationSpec = tween(1000),
        label = "Ram Gauge"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "STATUS KINERJA SISTEM",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Gauge structure
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(160.dp)
            ) {
                // Secondary arc color
                val colorScheme = MaterialTheme.colorScheme
                Canvas(modifier = Modifier.size(150.dp)) {
                    drawArc(
                        color = colorScheme.outline.copy(alpha = 0.2f),
                        startAngle = -220f,
                        sweepAngle = 260f,
                        useCenter = false,
                        style = Stroke(width = 16.dp.toPx(), cap = StrokeCap.Round)
                    )

                    // Dynamic progress arc
                    val sweep = (animatedPercent / 100f) * 260f
                    drawArc(
                        brush = Brush.sweepGradient(
                            colors = listOf(
                                colorScheme.secondary,
                                colorScheme.primary
                            )
                        ),
                        startAngle = -220f,
                        sweepAngle = sweep,
                        useCenter = false,
                        style = Stroke(width = 16.dp.toPx(), cap = StrokeCap.Round)
                    )
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "${percentUsed}%",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Black,
                        color = if (percentUsed > 80) Color.Red else MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "RAM TERPAKAI",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "$usedMb MB / $totalMb MB",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Cleaner Button
            Button(
                onClick = onCleanClick,
                enabled = !isCleaning,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.Black
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("clean_ram_button")
            ) {
                if (isCleaning) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(
                            color = Color.Black,
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("MEMERIKSA CACHE...", fontWeight = FontWeight.Bold)
                    }
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Memory, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("BERSIHKAN RAM SEKARANG", fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun BoosterServiceControl(
    isActive: Boolean,
    onToggle: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(20.dp),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = if (isActive) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else Color.Transparent
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(
                            color = if (isActive) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.1f),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isActive) Icons.Default.FlashOn else Icons.Default.ToggleOff,
                        contentDescription = null,
                        tint = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column {
                    Text(
                        text = if (isActive) "Booster Otomatis: AKTIF" else "Booster Otomatis: NONAKTIF",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = if (isActive) "Memonitor HP Anda secara real-time" else "Nyalakan untuk optimasi otomatis saat main game",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            }

            Switch(
                checked = isActive,
                onCheckedChange = { onToggle() },
                modifier = Modifier.testTag("booster_service_switch"),
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.Black,
                    checkedTrackColor = MaterialTheme.colorScheme.primary,
                    uncheckedThumbColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    uncheckedTrackColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    }
}

@Composable
fun PermissionStatusCard(
    usageGranted: Boolean,
    overlayGranted: Boolean,
    dndGranted: Boolean,
    onGrantUsage: () -> Unit,
    onGrantOverlay: () -> Unit,
    onGrantDnd: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f)),
        shape = RoundedCornerShape(20.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Security,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "IZIN SISTEM DIPERLUKAN",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                "Untuk mendeteksi game berjalan dan menampilkan floating monitor, Anda perlu mengaktifkan izin berikut:",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                lineHeight = 16.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 1. Usage stats access
            PermissionItem(
                label = "Akses Data Penggunaan (Wajib mendeteksi game)",
                isGranted = usageGranted,
                onGrant = onGrantUsage,
                tag = "grant_usage_btn"
            )

            // 2. Alert window overlay
            PermissionItem(
                label = "Tampilkan di Atas Aplikasi Lain (Floating FPS)",
                isGranted = overlayGranted,
                onGrant = onGrantOverlay,
                tag = "grant_overlay_btn"
            )

            // 3. Do not disturb
            PermissionItem(
                label = "Jangan Ganggu / DND (Blokir Notifikasi)",
                isGranted = dndGranted,
                onGrant = onGrantDnd,
                tag = "grant_dnd_btn"
            )
        }
    }
}

@Composable
fun PermissionItem(
    label: String,
    isGranted: Boolean,
    onGrant: () -> Unit,
    tag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = if (isGranted) "✓ $label" else "✗ $label",
            fontSize = 11.sp,
            color = if (isGranted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
            modifier = Modifier.weight(1f)
        )

        if (!isGranted) {
            Text(
                text = "AKTIFKAN",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier
                    .clickable { onGrant() }
                    .padding(horizontal = 8.dp, vertical = 4.dp)
                    .testTag(tag)
            )
        }
    }
}

@Composable
fun RamDisclaimerCard() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                Icons.Default.NotificationsOff,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                modifier = Modifier
                    .size(18.dp)
                    .padding(top = 2.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    "Catatan RAM Cleaner Android Modern:",
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    "Sistem Android 10+ mengelola RAM secara mandiri. RAM Cleaner kami melakukan clearing background cache dan memberhentikan service tidak penting sesuai batasan API resmi untuk menjaga sisa RAM optimal.",
                    fontSize = 9.5.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    lineHeight = 14.sp
                )
            }
        }
    }
}

@Composable
fun BoostedAppCard(
    app: AppInfoItem,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clickable { onClick() }
            .testTag("boosted_card_${app.packageName}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            AppIconImage(
                drawable = app.icon,
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = app.label,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = app.packageName,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Mode label tag
                val modeLabel = when (app.boostMode) {
                    "PERFORMANCE" -> "Mode Performa ⚡"
                    "BATTERY" -> "Mode Hemat Baterai 🔋"
                    else -> "Mode Seimbang ⚙"
                }
                val modeColor = when (app.boostMode) {
                    "PERFORMANCE" -> MaterialTheme.colorScheme.primary
                    "BATTERY" -> MaterialTheme.colorScheme.secondary
                    else -> MaterialTheme.colorScheme.tertiary
                }

                Box(
                    modifier = Modifier
                        .background(
                            color = modeColor.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(6.dp)
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = modeLabel,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = modeColor
                    )
                }
            }

            IconButton(
                onClick = onClick,
                modifier = Modifier.testTag("configure_${app.packageName}")
            ) {
                Icon(
                    Icons.Default.Settings,
                    contentDescription = "Setting",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun ModeSelectionDialog(
    app: AppInfoItem,
    onDismiss: () -> Unit,
    onSaveMode: (String) -> Unit,
    onRemove: () -> Unit
) {
    var selectedMode by remember { mutableStateOf(app.boostMode) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Pengaturan Optimasi Game",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        },
        text = {
            Column {
                Text(
                    app.label,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                Text("Pilih mode pengoptimalan saat aplikasi ini berjalan:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))

                Spacer(modifier = Modifier.height(12.dp))

                // 1. Performance mode
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { selectedMode = "PERFORMANCE" }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = selectedMode == "PERFORMANCE",
                        onClick = { selectedMode = "PERFORMANCE" },
                        colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.primary)
                    )
                    Column(modifier = Modifier.padding(start = 8.dp)) {
                        Text("Mode Performa ⚡", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text("Prioritaskan CPU/RAM maksimal, nonaktifkan cache.", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                    }
                }

                // 2. Battery mode
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { selectedMode = "BATTERY" }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = selectedMode == "BATTERY",
                        onClick = { selectedMode = "BATTERY" },
                        colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.secondary)
                    )
                    Column(modifier = Modifier.padding(start = 8.dp)) {
                        Text("Mode Hemat Baterai 🔋", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text("Membatasi background processes, optimasi FPS agar awet.", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                    }
                }

                // 3. Balanced mode
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { selectedMode = "BALANCED" }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = selectedMode == "BALANCED",
                        onClick = { selectedMode = "BALANCED" },
                        colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.tertiary)
                    )
                    Column(modifier = Modifier.padding(start = 8.dp)) {
                        Text("Mode Seimbang ⚙", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text("Keseimbangan performa stabil dan konsumsi baterai aman.", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSaveMode(selectedMode) },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = Color.Black)
            ) {
                Text("Simpan", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            Row {
                TextButton(
                    onClick = onRemove,
                    colors = ButtonDefaults.textButtonColors(contentColor = Color.Red)
                ) {
                    Text("Hapus dari Booster", fontWeight = FontWeight.Bold)
                }

                TextButton(onClick = onDismiss) {
                    Text("Batal")
                }
            }
        },
        shape = RoundedCornerShape(24.dp),
        containerColor = MaterialTheme.colorScheme.surface
    )
}
