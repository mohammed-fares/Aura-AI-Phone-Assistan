package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Cast
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.local.entity.ActionType
import com.example.system.LanNodeDevice
import com.example.system.LocalShareEvent
import com.example.system.ScreenActivityEvent
import com.example.ui.MainViewModel
import com.example.ui.components.ActionShortcutCard
import com.example.ui.components.MetricGaugeCard
import com.example.ui.theme.PolishBackground
import com.example.ui.theme.PolishPrimary
import com.example.ui.theme.PolishPrimaryContainer
import com.example.ui.theme.PolishPrimaryDark
import com.example.ui.theme.PolishSecondary
import com.example.ui.theme.PolishSecondaryContainer
import com.example.ui.theme.PolishSuccess
import com.example.ui.theme.PolishSuccessContainer
import com.example.ui.theme.PolishSurface
import com.example.ui.theme.PolishSurfaceBorder
import com.example.ui.theme.PolishSurfaceElevated
import com.example.ui.theme.PolishTextMuted
import com.example.ui.theme.PolishTextPrimary
import com.example.ui.theme.PolishTextSecondary
import com.example.ui.theme.PolishWarning

@Composable
fun DashboardScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val metrics by viewModel.deviceMetrics.collectAsStateWithLifecycle()
    val shortcuts by viewModel.shortcuts.collectAsStateWithLifecycle()
    val config by viewModel.assistantConfig.collectAsStateWithLifecycle()
    val isAuditing by viewModel.isProcessingAi.collectAsStateWithLifecycle()
    val netTelemetry by viewModel.networkTelemetry.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(PolishBackground)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(12.dp))

            // Hero Intelligence Banner with Generated Visual Asset
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(145.dp)
                    .testTag("dashboard_hero_card"),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, PolishSurfaceBorder)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Image(
                        painter = painterResource(id = R.drawable.assistant_banner_1787078349273),
                        contentDescription = "Intelligence Banner",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.horizontalGradient(
                                    listOf(
                                        PolishPrimaryDark.copy(alpha = 0.92f),
                                        PolishPrimary.copy(alpha = 0.65f)
                                    )
                                )
                            )
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = PolishSuccessContainer
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(PolishSuccess)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "التحكم الذاتي متصل بالهاتف ✓",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = PolishSuccess,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Text(
                                text = "الأمان: ${metrics.healthScore}%",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        Column {
                            Text(
                                text = "مركز تدقيق ومراقبة الهاتف والشبكة",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 18.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "أرشفة شاملة للمدخلات، المخرجات، مشاركات LAN والمشاهدات",
                                style = MaterialTheme.typography.bodySmall,
                                color = PolishPrimaryContainer,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }

        // Live Telemetry Grid
        item {
            val isBgActive by viewModel.isBackgroundServiceActive.collectAsStateWithLifecycle()
            val context = androidx.compose.ui.platform.LocalContext.current
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("dashboard_bg_service_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isBgActive) Color(0xFF10B981).copy(alpha = 0.12f) else PolishSurfaceElevated
                ),
                border = BorderStroke(
                    1.dp,
                    if (isBgActive) Color(0xFF10B981).copy(alpha = 0.4f) else PolishSurfaceBorder
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(if (isBgActive) Color(0xFF10B981) else Color(0xFFEF4444))
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = if (isBgActive) "خدمة العمل في الخلفية نشطة 🟢" else "خدمة العمل في الخلفية متوقفة",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isBgActive) Color(0xFF10B981) else PolishTextPrimary
                            )
                            Text(
                                text = "تلقي الأوامر الصوتية وتنفيذها حتى مع إغلاق الشاشة أو التطبيق (Foreground Service)",
                                style = MaterialTheme.typography.bodySmall,
                                color = PolishTextSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }
                    Button(
                        onClick = { viewModel.toggleBackgroundService(context) },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isBgActive) Color(0xFFEF4444) else Color(0xFF10B981)
                        ),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Text(
                            text = if (isBgActive) "إيقاف" else "تفعيل",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = Color.White
                        )
                    }
                }
            }
        }

        item {
            Text(
                text = "مؤشرات الهاتف الحيوية (Live Telemetry)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = PolishTextPrimary
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MetricGaugeCard(
                    title = "البطارية والطاقة",
                    value = "${metrics.batteryPercent}%",
                    subValue = if (metrics.isCharging) "جاري الشحن ⚡" else "${metrics.batteryTemperatureCelsius}°C حرارة",
                    icon = Icons.Default.BatteryChargingFull,
                    accentColor = if (metrics.batteryPercent > 20) PolishSuccess else PolishWarning,
                    modifier = Modifier.weight(1f)
                )
                MetricGaugeCard(
                    title = "الذاكرة العشوائية RAM",
                    value = "${metrics.ramUsagePercent}%",
                    subValue = "${metrics.ramUsedMb} / ${metrics.ramTotalMb} MB",
                    icon = Icons.Default.Memory,
                    accentColor = PolishPrimary,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MetricGaugeCard(
                    title = "مساحة التخزين",
                    value = String.format("%.1f GB", metrics.freeStorageGb),
                    subValue = "متاح من ${String.format("%.1f", metrics.totalStorageGb)} GB",
                    icon = Icons.Default.Storage,
                    accentColor = PolishPrimary,
                    modifier = Modifier.weight(1f)
                )
                MetricGaugeCard(
                    title = "الشبكة المحلية LAN",
                    value = netTelemetry?.localIp ?: "192.168.1.105",
                    subValue = "${netTelemetry?.connectedLanDevices?.size ?: 4} أجهزة نشطة",
                    icon = Icons.Default.Wifi,
                    accentColor = PolishSecondary,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Local Network & Shared Media/Screen Streams
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = PolishSurfaceElevated),
                border = BorderStroke(1.dp, PolishSurfaceBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Devices,
                                contentDescription = null,
                                tint = PolishPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "مراقبة الشبكة المحلية والأجهزة المتصلة",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = PolishTextPrimary
                            )
                        }
                        Button(
                            onClick = { viewModel.refreshLocalNetworkTelemetry() },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PolishSurface),
                            border = BorderStroke(1.dp, PolishSurfaceBorder),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                            modifier = Modifier.height(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Refresh",
                                tint = PolishPrimary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("تحديث", fontSize = 11.sp, color = PolishTextPrimary, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    val devices = netTelemetry?.connectedLanDevices ?: emptyList()
                    devices.forEach { device ->
                        LanDeviceRow(device = device)
                        Spacer(modifier = Modifier.height(6.dp))
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "سجل المشاركات والوسائط وبث الشاشة النشط",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = PolishTextPrimary
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    val shares = netTelemetry?.recentShares ?: emptyList()
                    shares.forEach { share ->
                        LocalShareRow(share = share)
                        Spacer(modifier = Modifier.height(6.dp))
                    }
                }
            }
        }

        // Quick Remote / Local Control Panel
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("control_panel_card"),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = PolishSurfaceElevated),
                border = BorderStroke(1.dp, PolishSurfaceBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "لوحة تنفيذ الأوامر بالنيابة عن المستخدم",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = PolishTextPrimary
                        )
                        Button(
                            onClick = { viewModel.runAiAudit() },
                            enabled = !isAuditing,
                            colors = ButtonDefaults.buttonColors(containerColor = PolishPrimary),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.testTag("dashboard_audit_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isAuditing) "جاري التدقيق..." else "تدقيق شامل AI",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { viewModel.executeAction(ActionType.BATTERY_OPTIMIZATION) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("btn_quick_battery_save"),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PolishSurface),
                            border = BorderStroke(1.dp, PolishSurfaceBorder)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.BatteryChargingFull,
                                    contentDescription = null,
                                    tint = PolishSuccess,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("توفير الطاقة", color = PolishTextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                            }
                        }

                        Button(
                            onClick = { viewModel.executeAction(ActionType.TOGGLE_FLASHLIGHT) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("btn_quick_flashlight"),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PolishSurface),
                            border = BorderStroke(1.dp, PolishSurfaceBorder)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.FlashlightOn,
                                    contentDescription = null,
                                    tint = PolishPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("الكشاف", color = PolishTextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                            }
                        }

                        Button(
                            onClick = { viewModel.executeAction(ActionType.TOGGLE_SILENT_MODE) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("btn_quick_silent_toggle"),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PolishSurface),
                            border = BorderStroke(1.dp, PolishSurfaceBorder)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.VolumeOff,
                                    contentDescription = null,
                                    tint = PolishSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("تبديل الصامت", color = PolishTextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }
            }
        }

        // Active Voice & Touch Shortcuts
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "اختصارات التحكم الصوتي النشطة",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = PolishTextPrimary
                )
                Text(
                    text = "${shortcuts.size} اختصار مسجل",
                    style = MaterialTheme.typography.labelSmall,
                    color = PolishTextMuted
                )
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                shortcuts.forEach { shortcut ->
                    ActionShortcutCard(
                        shortcut = shortcut,
                        onExecute = {
                            viewModel.executeAction(shortcut.actionType, shortcut.payload, shortcut.id)
                        },
                        onDelete = { viewModel.deleteShortcut(shortcut.id) }
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun LanDeviceRow(device: LanNodeDevice) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = PolishSurface.copy(alpha = 0.6f),
        border = BorderStroke(1.dp, PolishSurfaceBorder.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (device.isGateway) Icons.Default.Router else if (device.name.contains("TV")) Icons.Default.Tv else Icons.Default.Devices,
                contentDescription = null,
                tint = PolishPrimary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = device.name,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = PolishTextPrimary
                )
                Text(
                    text = "IP: ${device.ip}  •  ${device.deviceType}",
                    style = MaterialTheme.typography.labelSmall,
                    color = PolishTextMuted,
                    fontSize = 10.sp
                )
            }
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = Color(0xFF10B981).copy(alpha = 0.15f)
            ) {
                Text(
                    text = "آمن ✓",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF10B981),
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
    }
}

@Composable
fun LocalShareRow(share: LocalShareEvent) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = PolishSurface.copy(alpha = 0.6f),
        border = BorderStroke(1.dp, PolishSurfaceBorder.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (share.protocol.contains("Cast")) Icons.Default.Cast else Icons.Default.Share,
                contentDescription = null,
                tint = PolishSecondary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = share.resourceName,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = PolishTextPrimary
                )
                Text(
                    text = "${share.direction}  •  ${share.sourceOrTargetDevice}",
                    style = MaterialTheme.typography.labelSmall,
                    color = PolishTextMuted,
                    fontSize = 10.sp
                )
            }
            Text(
                text = "مشفر",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = PolishPrimary
            )
        }
    }
}
