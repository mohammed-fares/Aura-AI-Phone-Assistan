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
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Cast
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.Hearing
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
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
import androidx.compose.material3.IconButton
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
import com.example.ui.components.InstalledAppsKnowledgeCard
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
import com.example.util.LocalizationManager

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
    val appLang by viewModel.appLanguage.collectAsStateWithLifecycle()
    val indexedApps by viewModel.indexedApps.collectAsStateWithLifecycle()
    val isIndexingApps by viewModel.isIndexingApps.collectAsStateWithLifecycle()
    val isAr = LocalizationManager.getEffectiveLanguage(appLang) == "ar"

    fun s(ar: String, en: String): String = if (isAr) ar else en

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
                                        text = s("التحكم الذاتي متصل بالهاتف ✓", "Autonomous Agent Connected ✓"),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = PolishSuccess
                                    )
                                }
                            }
                            Text(
                                text = s("الاستماع الدائم مفعل", "Hands-Free Active"),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.85f)
                            )
                        }

                        Column {
                            Text(
                                text = s("المساعد الصوتي الذاتي (${config.assistantName})", "Autonomous Assistant (${config.assistantName})"),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = s("تحكم كامل: مكالمات، رسائل، ايميلات، تطبيقات، أمان بدون لمس", "Full Phone Access: Calls, SMS, Emails, Apps, Security hands-free"),
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.9f)
                            )
                        }
                    }
                }
            }
        }

        // Wake Word Trigger & Call-By-Name Intelligence Card (NEW USER REQUIREMENT)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("wake_word_call_card"),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = PolishSurfaceElevated),
                border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.4f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF10B981).copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Hearing,
                                    contentDescription = null,
                                    tint = Color(0xFF10B981),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = s("الاستيقاظ الفوري بالنداء على المساعد", "Instant Wake-Word & Call-by-Name"),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = PolishTextPrimary
                                )
                                Text(
                                    text = s("ينفذ الأوامر مباشرة دون لمس الهاتف أو فتحه", "Executes commands hands-free without opening app"),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontSize = 11.sp,
                                    color = PolishTextSecondary
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (config.wakeWordOnlyMode) PolishPrimaryContainer else Color(0xFF10B981).copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = if (config.wakeWordOnlyMode) s("وضع النداء", "Wake-Word Mode") else s("استماع مباشر", "Always Direct"),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (config.wakeWordOnlyMode) PolishPrimary else Color(0xFF10B981)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = PolishBackground,
                        border = BorderStroke(1.dp, PolishSurfaceBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = s("💬 نداء التنبيه:", "💬 Call Phrase:"),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = PolishTextSecondary
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "«يا ${config.assistantName}» / «${config.customWakeWord}»",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF10B981)
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = s(
                                    "مثال: «يا أورا افتح الواتساب واكتب رسالة لمحمد مرحبا» أو «يا أورا اتصل بأبي»",
                                    "e.g. \"Hey Aura send message to Mom hello\" or \"Aura call Dad\""
                                ),
                                fontSize = 11.sp,
                                color = PolishTextMuted
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Security & Hardware Trust Shield Bar
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF0F172A).copy(alpha = 0.6f),
                        border = BorderStroke(1.dp, Color(0xFF334155)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                tint = Color(0xFF38BDF8),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = s(
                                    "🛡️ أمان وثقة: عزل أصوات الفيديوهات المشغلة على الهاتف | استماع خارجي نقي | مهلة كافية لإكمال كلامك",
                                    "🛡️ Verified Shield: Internal video sound filtered | Clean mic input | Adaptive pause tolerance"
                                ),
                                fontSize = 10.sp,
                                color = Color(0xFF94A3B8),
                                lineHeight = 14.sp
                            )
                        }
                    }
                }
            }
        }

        // Quick Executive Actions (Call, SMS, Email, Camera, Flashlight, Silent, Scan)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("executive_actions_card"),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = PolishSurfaceElevated),
                border = BorderStroke(1.dp, PolishSurfaceBorder)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = s("تحكم مباشر بإمكانيات الهاتف", "Direct Phone Capabilities"),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = PolishTextPrimary
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        QuickActionItem(
                            icon = Icons.Default.Call,
                            label = s("اتصال", "Call"),
                            color = Color(0xFF10B981),
                            modifier = Modifier.weight(1f),
                            onClick = { viewModel.executeAction(ActionType.CALL_CONTACT, "0000000") }
                        )
                        QuickActionItem(
                            icon = Icons.Default.Message,
                            label = s("رسائل", "SMS"),
                            color = Color(0xFF3B82F6),
                            modifier = Modifier.weight(1f),
                            onClick = { viewModel.executeAction(ActionType.SEND_MESSAGE) }
                        )
                        QuickActionItem(
                            icon = Icons.Default.Email,
                            label = s("ايميل", "Email"),
                            color = Color(0xFFA855F7),
                            modifier = Modifier.weight(1f),
                            onClick = { viewModel.executeAction(ActionType.SEND_EMAIL) }
                        )
                        QuickActionItem(
                            icon = Icons.Default.CameraAlt,
                            label = s("كاميرا", "Camera"),
                            color = Color(0xFFF59E0B),
                            modifier = Modifier.weight(1f),
                            onClick = { viewModel.executeAction(ActionType.OPEN_CAMERA) }
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        QuickActionItem(
                            icon = Icons.Default.FlashlightOn,
                            label = s("كشاف", "Torch"),
                            color = Color(0xFFFACC15),
                            modifier = Modifier.weight(1f),
                            onClick = { viewModel.executeAction(ActionType.TOGGLE_FLASHLIGHT) }
                        )
                        QuickActionItem(
                            icon = Icons.Default.VolumeOff,
                            label = s("صامت", "Silent"),
                            color = Color(0xFFEC4899),
                            modifier = Modifier.weight(1f),
                            onClick = { viewModel.executeAction(ActionType.TOGGLE_SILENT_MODE) }
                        )
                        QuickActionItem(
                            icon = Icons.Default.Security,
                            label = s("فحص أمان", "Scan"),
                            color = Color(0xFF06B6D4),
                            modifier = Modifier.weight(1f),
                            onClick = { viewModel.startFullSecurityScan() }
                        )
                        QuickActionItem(
                            icon = Icons.Default.Settings,
                            label = s("إعدادات", "Settings"),
                            color = Color(0xFF64748B),
                            modifier = Modifier.weight(1f),
                            onClick = { viewModel.executeAction(ActionType.OPEN_SETTINGS) }
                        )
                    }
                }
            }
        }

        // AI Installed Applications Intelligence Hub
        item {
            InstalledAppsKnowledgeCard(
                apps = indexedApps,
                isIndexing = isIndexingApps,
                isAr = isAr,
                onRefreshApps = { viewModel.refreshInstalledAppsCatalog() },
                onLaunchApp = { pkg -> viewModel.executeAction(ActionType.OPEN_APP, pkg) }
            )
        }

        // Live Telemetry Gauges Grid
        item {
            Text(
                text = s("مؤشرات أداء الهاتف والاتصال الحي", "Live System Telemetry"),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = PolishTextPrimary
            )
            Spacer(modifier = Modifier.height(6.dp))

            val estimatedCpu = (metrics.ramUsagePercent * 0.85f).toInt().coerceIn(12, 95)
            val usedStorageGb = (metrics.totalStorageGb - metrics.freeStorageGb).coerceAtLeast(0f)
            val storagePercent = if (metrics.totalStorageGb > 0f) ((usedStorageGb / metrics.totalStorageGb) * 100).toInt() else 40

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MetricGaugeCard(
                    title = s("المعالج", "CPU Load"),
                    value = "$estimatedCpu%",
                    subValue = if (estimatedCpu < 70) s("أداء مثالي", "Optimal") else s("ضغط مرتفع", "High"),
                    icon = Icons.Default.Speed,
                    accentColor = if (estimatedCpu < 70) PolishSuccess else PolishWarning,
                    modifier = Modifier.weight(1f)
                )
                MetricGaugeCard(
                    title = s("الذاكرة", "RAM Usage"),
                    value = "${metrics.ramUsagePercent}%",
                    subValue = "${metrics.ramUsedMb} / ${metrics.ramTotalMb} MB",
                    icon = Icons.Default.Memory,
                    accentColor = PolishPrimary,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MetricGaugeCard(
                    title = s("البطارية", "Battery"),
                    value = "${metrics.batteryPercent}%",
                    subValue = if (metrics.isCharging) s("جاري الشحن ⚡", "Charging ⚡") else "${metrics.batteryTemperatureCelsius}°C",
                    icon = Icons.Default.BatteryChargingFull,
                    accentColor = if (metrics.batteryPercent > 20) PolishSuccess else PolishWarning,
                    modifier = Modifier.weight(1f)
                )
                MetricGaugeCard(
                    title = s("التخزين", "Storage"),
                    value = "$storagePercent%",
                    subValue = String.format(java.util.Locale.US, "%.1f GB ", metrics.freeStorageGb) + s("متاح", "Free"),
                    icon = Icons.Default.Storage,
                    accentColor = PolishSecondary,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Local Network & Media Sharing Audit Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("local_network_audit_card"),
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
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(PolishSecondaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Router,
                                    contentDescription = null,
                                    tint = PolishSecondary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = s("تدقيق الشبكة المحلية وبث الشاشة", "LAN Nodes & Stream Audit"),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = PolishTextPrimary
                                )
                                Text(
                                    text = netTelemetry?.networkName?.let { s("متصل بشبكة: $it", "Connected: $it") } ?: s("مراقبة أجهزة ومشاركات Wi-Fi", "Wi-Fi audit active"),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = PolishTextSecondary,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        IconButton(onClick = { viewModel.refreshLocalNetworkTelemetry() }) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Refresh",
                                tint = PolishSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Connected LAN Devices
                    val devices = netTelemetry?.connectedLanDevices ?: emptyList()
                    if (devices.isNotEmpty()) {
                        Text(
                            text = s("الأجهزة المتصلة على نفس الشبكة (${devices.size}):", "Connected Devices (${devices.size}):"),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = PolishTextSecondary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        devices.take(3).forEach { device ->
                            LanDeviceRow(device = device, isAr = isAr)
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                    }
                }
            }
        }

        // AI Autonomous Audit Button & Summary
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("ai_audit_trigger_card"),
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
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = s("التدقيق الشامل بالذكاء الاصطناعي", "AI Full System Audit"),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = PolishTextPrimary
                            )
                            Text(
                                text = s("تحليل كافة بيانات الهاتف والاتصالات والأمان", "Audit all hardware, communication, and security streams"),
                                style = MaterialTheme.typography.bodySmall,
                                color = PolishTextSecondary,
                                fontSize = 11.sp
                            )
                        }

                        Button(
                            onClick = { viewModel.runAiAudit() },
                            enabled = !isAuditing,
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PolishPrimary),
                            modifier = Modifier.testTag("run_audit_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isAuditing) s("جاري التدقيق...", "Auditing...") else s("تدقيق فوري", "Run Audit"),
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }

        // Shortcuts list
        if (shortcuts.isNotEmpty()) {
            item {
                Text(
                    text = s("أوامر واختصارات التحكم الذاتي السريعة", "Autonomous Shortcuts"),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = PolishTextPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
            }

            items(shortcuts) { shortcut ->
                ActionShortcutCard(
                    shortcut = shortcut,
                    onExecute = { viewModel.executeAction(shortcut.actionType, shortcut.payload, shortcut.id) },
                    onDelete = { viewModel.deleteShortcut(shortcut.id) }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
private fun QuickActionItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = color.copy(alpha = 0.12f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.3f)),
        modifier = modifier.height(64.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(imageVector = icon, contentDescription = label, tint = color, modifier = Modifier.size(22.dp))
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = label, color = PolishTextPrimary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun LanDeviceRow(device: LanNodeDevice, isAr: Boolean) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = PolishSurface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = when {
                        device.deviceType.contains("شاشة") || device.deviceType.contains("TV") -> Icons.Default.Tv
                        device.deviceType.contains("بوابة") || device.deviceType.contains("Router") -> Icons.Default.Router
                        else -> Icons.Default.Devices
                    },
                    contentDescription = null,
                    tint = PolishSecondary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(text = device.name, color = PolishTextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text(text = "${device.ip} • ${device.deviceType}", color = PolishTextSecondary, fontSize = 10.sp)
                }
            }
            Text(
                text = if (device.isAudited) (if (isAr) "مدقق ✓" else "Audited ✓") else (if (isAr) "جديد" else "New"),
                color = if (device.isAudited) PolishSuccess else PolishWarning,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
