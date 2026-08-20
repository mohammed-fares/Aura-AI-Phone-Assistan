package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.entity.ActionType
import com.example.data.local.entity.TelemetryLogEntity
import com.example.data.local.entity.TelemetrySeverity
import com.example.data.local.entity.TelemetryType
import com.example.ui.MainViewModel
import com.example.ui.components.InsightCard
import com.example.ui.theme.PolishBackground
import com.example.ui.theme.PolishCritical
import com.example.ui.theme.PolishCriticalContainer
import com.example.ui.theme.PolishPrimary
import com.example.ui.theme.PolishPrimaryContainer
import com.example.ui.theme.PolishSecondaryContainer
import com.example.ui.theme.PolishSuccess
import com.example.ui.theme.PolishSuccessContainer
import com.example.ui.theme.PolishSurfaceBorder
import com.example.ui.theme.PolishSurfaceElevated
import com.example.ui.theme.PolishTextMuted
import com.example.ui.theme.PolishTextPrimary
import com.example.ui.theme.PolishTextSecondary
import com.example.ui.theme.PolishWarning
import com.example.ui.theme.PolishWarningContainer
import com.example.util.LocalizationManager
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ActivityArchiveScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val logs by viewModel.telemetryLogs.collectAsStateWithLifecycle()
    val insights by viewModel.insights.collectAsStateWithLifecycle()
    val selectedFilter by viewModel.selectedLogFilter.collectAsStateWithLifecycle()
    val isAuditing by viewModel.isProcessingAi.collectAsStateWithLifecycle()
    val appLang by viewModel.appLanguage.collectAsStateWithLifecycle()
    val lastBgAction by viewModel.lastBackgroundStatus.collectAsStateWithLifecycle()
    val isBackgroundActive by viewModel.isBackgroundServiceActive.collectAsStateWithLifecycle()

    val isAr = LocalizationManager.getEffectiveLanguage(appLang) == "ar"
    fun s(ar: String, en: String): String = if (isAr) ar else en

    val filteredLogs = if (selectedFilter == null) logs else logs.filter { it.type == selectedFilter }
    val totalCount = logs.size
    val autonomousActionsCount = logs.count {
        it.aiAudited || it.type == TelemetryType.AI_INFERENCE || it.title.contains("خلفي") || it.title.contains("تلقائي") || it.title.contains("تنفيذ") || it.title.contains("Auto") || it.title.contains("Autonomous")
    }
    val aiAuditedCount = logs.count { it.aiAudited }
    val securityOptimalCount = logs.count { it.severity == TelemetrySeverity.OPTIMAL || it.type == TelemetryType.SECURITY_AUDIT }

    val filterOptions = listOf(
        FilterOption(s("الكل", "All"), null, totalCount),
        FilterOption(s("الأفعال الذاتية 🤖", "Autonomous 🤖"), TelemetryType.AI_INFERENCE, logs.count { it.type == TelemetryType.AI_INFERENCE }),
        FilterOption(s("الأوامر الصوتية 🎙️", "Voice 🎙️"), TelemetryType.VOICE_COMMAND, logs.count { it.type == TelemetryType.VOICE_COMMAND }),
        FilterOption(s("فحص الأمان 🛡️", "Security 🛡️"), TelemetryType.SECURITY_AUDIT, logs.count { it.type == TelemetryType.SECURITY_AUDIT }),
        FilterOption(s("النظام والأداء ⚡", "System ⚡"), TelemetryType.SYSTEM_PERFORMANCE, logs.count { it.type == TelemetryType.SYSTEM_PERFORMANCE }),
        FilterOption(s("الشبكة 🌐", "Network 🌐"), TelemetryType.NETWORK_TRAFFIC, logs.count { it.type == TelemetryType.NETWORK_TRAFFIC }),
        FilterOption(s("البطارية 🔋", "Battery 🔋"), TelemetryType.BATTERY_POWER, logs.count { it.type == TelemetryType.BATTERY_POWER }),
        FilterOption(s("حركات اللمس 👆", "Touch 👆"), TelemetryType.TOUCH_GESTURE, logs.count { it.type == TelemetryType.TOUCH_GESTURE })
    )

    val infiniteTransition = rememberInfiniteTransition(label = "pulse_trans")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(PolishBackground)
            .padding(horizontal = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // ==========================================
        // 1. TOP STATISTICAL OVERVIEW CARDS
        // ==========================================
        item {
            Spacer(modifier = Modifier.height(8.dp))

            // Main Header Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("archive_header_card"),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = PolishSurfaceElevated),
                border = BorderStroke(1.dp, PolishSurfaceBorder)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    // Header Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(PolishPrimaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.History,
                                    contentDescription = null,
                                    tint = PolishPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = s("سجل الأرشفة والعمليات الذاتية", "Autonomous Action Log & Archive"),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = PolishTextPrimary,
                                    fontSize = 15.sp
                                )
                                Text(
                                    text = s("مراجعة وتدقيق أنشطة المساعد في الخلفية", "Review & audit background AI operations"),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = PolishTextSecondary,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        Button(
                            onClick = { viewModel.runAiAudit() },
                            enabled = !isAuditing,
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PolishPrimary),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            modifier = Modifier
                                .height(34.dp)
                                .testTag("audit_logs_now_button")
                        ) {
                            if (isAuditing) {
                                CircularProgressIndicator(
                                    color = Color.White,
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = s("جاري التدقيق...", "Auditing..."),
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = s("تدقيق AI", "AI Audit"),
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // 4 Stat Metric Mini-Cards
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ArchiveMetricBadge(
                            title = s("إجمالي السجلات", "Total Logs"),
                            value = "$totalCount",
                            icon = Icons.Default.Storage,
                            tint = PolishPrimary,
                            modifier = Modifier.weight(1f)
                        )
                        ArchiveMetricBadge(
                            title = s("أفعال ذاتية", "Autonomous"),
                            value = "$autonomousActionsCount",
                            icon = Icons.Default.SmartToy,
                            tint = Color(0xFF10B981),
                            modifier = Modifier.weight(1f)
                        )
                        ArchiveMetricBadge(
                            title = s("مدققة بـ AI", "Audited"),
                            value = "$aiAuditedCount",
                            icon = Icons.Default.Psychology,
                            tint = Color(0xFF8B5CF6),
                            modifier = Modifier.weight(1f)
                        )
                        ArchiveMetricBadge(
                            title = s("آمن ومثالي", "Optimal"),
                            value = "$securityOptimalCount",
                            icon = Icons.Default.Verified,
                            tint = Color(0xFF06B6D4),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Live Background AI Service Monitor Strip
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (isBackgroundActive) Color(0xFF10B981).copy(alpha = 0.10f) else PolishSecondaryContainer,
                        border = BorderStroke(1.dp, if (isBackgroundActive) Color(0xFF10B981).copy(alpha = 0.35f) else PolishSurfaceBorder)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (isBackgroundActive) Color(0xFF10B981).copy(alpha = pulseAlpha)
                                                else Color(0xFF94A3B8)
                                            )
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (isBackgroundActive) s("المساعد يسجل ويراقب في الخلفية 24/7", "AI Active 24/7 Monitoring in Background") else s("الخدمة الخلفية متوقفة حالياً", "Background service is idle"),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isBackgroundActive) Color(0xFF10B981) else PolishTextSecondary,
                                        fontSize = 11.sp
                                    )
                                }

                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (isBackgroundActive) Color(0xFF10B981).copy(alpha = 0.2f) else PolishSurfaceElevated
                                ) {
                                    Text(
                                        text = if (isBackgroundActive) s("مستمر ✓", "Online ✓") else s("متوقف", "Offline"),
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isBackgroundActive) Color(0xFF10B981) else PolishTextSecondary,
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            if (!lastBgAction.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = s("آخر نشاط: ", "Latest Activity: ") + lastBgAction,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = PolishTextSecondary,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        // ==========================================
        // 2. AI BEHAVIORAL INSIGHTS SECTION
        // ==========================================
        if (insights.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Analytics,
                            contentDescription = null,
                            tint = PolishPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = s("رؤى الذكاء الاصطناعي وأنماط السلوك", "AI Behavioral Insights"),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = PolishTextPrimary,
                            fontSize = 14.sp
                        )
                    }
                    Text(
                        text = "${insights.size} " + s("تحليلات مكتشفة", "insights"),
                        style = MaterialTheme.typography.labelSmall,
                        color = PolishTextMuted,
                        fontSize = 11.sp
                    )
                }
            }

            items(insights, key = { it.id }) { insight ->
                InsightCard(
                    insight = insight,
                    onApply = { viewModel.executeAction(ActionType.BATTERY_OPTIMIZATION) }
                )
            }
        }

        // ==========================================
        // 3. CATEGORY & SEVERITY FILTER BAR
        // ==========================================
        item {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = s("سجل الأحداث والعمليات الذاتية", "Autonomous Activity Stream") + " (${filteredLogs.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = PolishTextPrimary,
                        fontSize = 14.sp
                    )
                    if (selectedFilter != null) {
                        Surface(
                            onClick = { viewModel.setLogFilter(null) },
                            shape = RoundedCornerShape(8.dp),
                            color = PolishSecondaryContainer
                        ) {
                            Text(
                                text = s("إظهار الكل ↺", "Reset Filter ↺"),
                                style = MaterialTheme.typography.labelSmall,
                                color = PolishPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Filter Chips Horizontal Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    filterOptions.forEach { opt ->
                        val isSelected = selectedFilter == opt.type
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.setLogFilter(opt.type) },
                            label = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = opt.label,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) Color.White else PolishTextSecondary
                                    )
                                    if (opt.count > 0) {
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = if (isSelected) Color.White.copy(alpha = 0.25f) else PolishSecondaryContainer
                                        ) {
                                            Text(
                                                text = "${opt.count}",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isSelected) Color.White else PolishPrimary,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                            )
                                        }
                                    }
                                }
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = PolishPrimary,
                                containerColor = PolishSurfaceElevated
                            ),
                            border = BorderStroke(
                                1.dp,
                                if (isSelected) PolishPrimary else PolishSurfaceBorder
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.testTag("filter_chip_${opt.label}")
                        )
                    }
                }
            }
        }

        // ==========================================
        // 4. POLISHED LOG ITEMS STREAM
        // ==========================================
        if (filteredLogs.isEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = PolishSurfaceElevated),
                    border = BorderStroke(1.dp, PolishSurfaceBorder)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = null,
                            tint = PolishTextMuted,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = s("لا توجد سجلات مطابقة لهذا التصنيف", "No activity logs in this category"),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = PolishTextSecondary,
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = s("قم بإصدار أوامر صوتية أو تشغيل فحص الأمان لتوثيق العمليات", "Speak commands or run security scans to populate records"),
                            style = MaterialTheme.typography.bodySmall,
                            color = PolishTextMuted,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        } else {
            items(filteredLogs, key = { it.id }) { log ->
                ArchiveLogItemCard(
                    log = log,
                    isAr = isAr,
                    onExecuteAgain = { actionType, payload ->
                        viewModel.executeAction(actionType, payload)
                    }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

data class FilterOption(
    val label: String,
    val type: TelemetryType?,
    val count: Int
)

@Composable
fun ArchiveMetricBadge(
    title: String,
    value: String,
    icon: ImageVector,
    tint: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = PolishBackground,
        border = BorderStroke(1.dp, PolishSurfaceBorder),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = tint,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = PolishTextPrimary,
                fontSize = 15.sp
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = PolishTextSecondary,
                fontSize = 9.sp,
                maxLines = 1
            )
        }
    }
}

@Composable
fun ArchiveLogItemCard(
    log: TelemetryLogEntity,
    isAr: Boolean,
    onExecuteAgain: (ActionType, String?) -> Unit,
    modifier: Modifier = Modifier
) {
    val (severityBadgeBg, severityTextColor, severityLabel) = when (log.severity) {
        TelemetrySeverity.OPTIMAL -> Triple(PolishSuccessContainer, PolishSuccess, if (isAr) "مثالي ✓" else "Optimal ✓")
        TelemetrySeverity.INFO -> Triple(PolishSecondaryContainer, PolishPrimary, if (isAr) "معلومات ℹ" else "Info ℹ")
        TelemetrySeverity.WARNING -> Triple(PolishWarningContainer, PolishWarning, if (isAr) "تحذير ⚠️" else "Warning ⚠️")
        TelemetrySeverity.CRITICAL -> Triple(PolishCriticalContainer, PolishCritical, if (isAr) "حرج 🚨" else "Critical 🚨")
    }

    val typeIcon = when (log.type) {
        TelemetryType.VOICE_COMMAND -> Icons.Default.Mic
        TelemetryType.TOUCH_GESTURE -> Icons.Default.Sensors
        TelemetryType.NETWORK_TRAFFIC -> Icons.Default.Wifi
        TelemetryType.BATTERY_POWER -> Icons.Default.BatteryChargingFull
        TelemetryType.COMMUNICATION -> Icons.Default.Phone
        TelemetryType.SYSTEM_PERFORMANCE -> Icons.Default.Memory
        TelemetryType.SECURITY_AUDIT -> Icons.Default.Security
        TelemetryType.AI_INFERENCE -> Icons.Default.SmartToy
    }

    val typeLabel = when (log.type) {
        TelemetryType.VOICE_COMMAND -> if (isAr) "أمر صوتي" else "Voice Command"
        TelemetryType.TOUCH_GESTURE -> if (isAr) "حركة لمس" else "Touch Gesture"
        TelemetryType.NETWORK_TRAFFIC -> if (isAr) "حركة شبكة" else "Network Traffic"
        TelemetryType.BATTERY_POWER -> if (isAr) "طاقة وبطارية" else "Battery Power"
        TelemetryType.COMMUNICATION -> if (isAr) "اتصال ورسائل" else "Communication"
        TelemetryType.SYSTEM_PERFORMANCE -> if (isAr) "أداء النظام" else "System Perf"
        TelemetryType.SECURITY_AUDIT -> if (isAr) "فحص أمني" else "Security Audit"
        TelemetryType.AI_INFERENCE -> if (isAr) "تنفيذ ذاتي" else "Autonomous AI"
    }

    val formattedTime = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(log.timestamp))

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("log_item_${log.id}"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = PolishSurfaceElevated),
        border = BorderStroke(1.dp, PolishSurfaceBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // Header Row: Type Icon, Title, Severity Badge, and Timestamp
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(severityBadgeBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = typeIcon,
                        contentDescription = null,
                        tint = severityTextColor,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = log.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = PolishTextPrimary,
                            fontSize = 13.sp,
                            modifier = Modifier.weight(1f)
                        )

                        Text(
                            text = formattedTime,
                            style = MaterialTheme.typography.labelSmall,
                            color = PolishTextMuted,
                            fontSize = 10.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    // Sub-pills: Category and Severity
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = PolishSecondaryContainer
                        ) {
                            Text(
                                text = typeLabel,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = PolishPrimary,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = severityBadgeBg
                        ) {
                            Text(
                                text = severityLabel,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = severityTextColor,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Log description text
            Text(
                text = log.description,
                style = MaterialTheme.typography.bodySmall,
                color = PolishTextSecondary,
                fontSize = 12.sp,
                lineHeight = 16.sp
            )

            // AI Audited & Annotation Container
            if (log.aiAudited && !log.aiAnnotation.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = PolishSecondaryContainer.copy(alpha = 0.7f),
                    border = BorderStroke(1.dp, PolishPrimary.copy(alpha = 0.25f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = PolishPrimary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = (if (isAr) "تدقيق الذكاء الاصطناعي: " else "AI Audit: ") + log.aiAnnotation,
                            style = MaterialTheme.typography.labelSmall,
                            color = PolishPrimary,
                            fontWeight = FontWeight.Medium,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }
    }
}
