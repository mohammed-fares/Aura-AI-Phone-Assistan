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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Storage
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
import com.example.ui.MainViewModel
import com.example.ui.components.ActionShortcutCard
import com.example.ui.components.MetricGaugeCard
import com.example.ui.theme.PolishBackground
import com.example.ui.theme.PolishCritical
import com.example.ui.theme.PolishGlow
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
                                        text = "الداشبورد متصل بالهاتف ✓",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = PolishSuccess,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Text(
                                text = "درجة الأمان: ${metrics.healthScore}%",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        Column {
                            Text(
                                text = "مركز التحكم الذاتي بالهاتف",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 18.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "مراقبة مباشرة للمدخلات، المخرجات، الاتصالات وكفاءة الموارد",
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
                    title = "حالة الاتصال",
                    value = if (metrics.isInternetAvailable) "متصل وآمن" else "محلي",
                    subValue = metrics.networkType,
                    icon = Icons.Default.Wifi,
                    accentColor = PolishSecondary,
                    modifier = Modifier.weight(1f)
                )
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
                            text = "لوحة تنفيذ الأوامر السريعة",
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
                            onClick = { viewModel.executeAction(ActionType.DEVICE_DIAGNOSTIC) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("btn_quick_ram_clean"),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PolishSurface),
                            border = BorderStroke(1.dp, PolishSurfaceBorder)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.Speed,
                                    contentDescription = null,
                                    tint = PolishPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("تحسين الذاكرة", color = PolishTextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Medium)
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
                    text = "اختصارات الصوت واللمس النشطة",
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
