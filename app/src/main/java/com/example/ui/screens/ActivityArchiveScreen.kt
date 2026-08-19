package com.example.ui.screens

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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.entity.ActionType
import com.example.data.local.entity.TelemetryType
import com.example.ui.MainViewModel
import com.example.ui.components.InsightCard
import com.example.ui.components.TelemetryLogItemCard
import com.example.ui.theme.PolishBackground
import com.example.ui.theme.PolishPrimary
import com.example.ui.theme.PolishPrimaryContainer
import com.example.ui.theme.PolishSecondaryContainer
import com.example.ui.theme.PolishSurfaceBorder
import com.example.ui.theme.PolishSurfaceElevated
import com.example.ui.theme.PolishTextMuted
import com.example.ui.theme.PolishTextPrimary
import com.example.ui.theme.PolishTextSecondary
import com.example.util.LocalizationManager

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

    val isAr = LocalizationManager.getEffectiveLanguage(appLang) == "ar"
    fun s(ar: String, en: String): String = if (isAr) ar else en

    val filteredLogs = if (selectedFilter == null) logs else logs.filter { it.type == selectedFilter }

    val filterOptions = listOf(
        Pair(s("الكل", "All"), null),
        Pair(s("الأوامر الصوتية", "Voice"), TelemetryType.VOICE_COMMAND),
        Pair(s("الشبكة والاتصالات", "Network"), TelemetryType.NETWORK_TRAFFIC),
        Pair(s("البطارية والطاقة", "Battery"), TelemetryType.BATTERY_POWER),
        Pair(s("الأداء والذاكرة", "System"), TelemetryType.SYSTEM_PERFORMANCE),
        Pair(s("تدقيق الذكاء الاصطناعي", "AI Audit"), TelemetryType.AI_INFERENCE),
        Pair(s("حركات اللمس", "Touch"), TelemetryType.TOUCH_GESTURE)
    )

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(PolishBackground)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(12.dp))

            // Archive Header & Resource Consciousness Notice
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("archive_header_card"),
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
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(PolishPrimaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Storage,
                                    contentDescription = null,
                                    tint = PolishPrimary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = s("قاعدة بيانات وتدقيق الهاتف", "System Audit & Archive"),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = PolishTextPrimary
                                )
                                Text(
                                    text = s("أرشفة وتحليل المدخلات وسلوكيات الاستخدام", "Archive and analysis of system inputs"),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = PolishTextSecondary,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        Button(
                            onClick = { viewModel.runAiAudit() },
                            enabled = !isAuditing,
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PolishPrimary),
                            modifier = Modifier.testTag("audit_logs_now_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isAuditing) s("جاري التدقيق...", "Auditing...") else s("تدقيق AI", "AI Audit"),
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = PolishSecondaryContainer,
                        border = BorderStroke(1.dp, PolishSurfaceBorder)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = PolishPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = s("حماية موارد الهاتف: يتم تدقيق البيانات فور تدفقها وتطهير السجلات القديمة تلقائياً دون استهلاك الذاكرة أو طاقة المعالج.", "Resource Guard: Telemetry streams are processed in real-time with zero overhead."),
                                style = MaterialTheme.typography.bodySmall,
                                color = PolishTextPrimary,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        }

        // Behavior Insights from AI
        if (insights.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = s("رؤى الذكاء الاصطناعي وأنماط السلوك", "AI Behavioral Insights"),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = PolishTextPrimary
                    )
                    Text(
                        text = "${insights.size} " + s("تحليلات مكتشفة", "insights found"),
                        style = MaterialTheme.typography.labelSmall,
                        color = PolishTextMuted
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

        // Telemetry Logs Header & Filter Chips
        item {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = s("سجل أحداث ومدخلات الهاتف", "System Event Stream") + " (${filteredLogs.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = PolishTextPrimary
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    filterOptions.forEach { (label, type) ->
                        val isSelected = selectedFilter == type
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.setLogFilter(type) },
                            label = {
                                Text(
                                    text = label,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) Color.White else PolishTextSecondary
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = PolishPrimary,
                                containerColor = PolishSurfaceElevated
                            ),
                            border = BorderStroke(
                                1.dp,
                                if (isSelected) PolishPrimary else PolishSurfaceBorder
                            ),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.testTag("filter_chip_$label")
                        )
                    }
                }
            }
        }

        // Log Items List
        if (filteredLogs.isEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = PolishSurfaceElevated),
                    border = BorderStroke(1.dp, PolishSurfaceBorder)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = s("لا توجد سجلات مطابقة لهذا الفلتر", "No logs matching current filter"),
                            style = MaterialTheme.typography.bodyMedium,
                            color = PolishTextMuted
                        )
                    }
                }
            }
        } else {
            items(filteredLogs, key = { it.id }) { log ->
                TelemetryLogItemCard(log = log)
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
