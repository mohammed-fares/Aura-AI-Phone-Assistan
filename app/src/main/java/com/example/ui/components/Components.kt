package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.Warning
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
import com.example.data.local.entity.ActionShortcutEntity
import com.example.data.local.entity.BehaviorInsightEntity
import com.example.data.local.entity.TelemetryLogEntity
import com.example.data.local.entity.TelemetrySeverity
import com.example.data.local.entity.TelemetryType
import com.example.ui.AppPermissionInfo
import com.example.ui.theme.PolishCritical
import com.example.ui.theme.PolishCriticalContainer
import com.example.ui.theme.PolishGlow
import com.example.ui.theme.PolishPrimary
import com.example.ui.theme.PolishPrimaryContainer
import com.example.ui.theme.PolishSecondary
import com.example.ui.theme.PolishSecondaryContainer
import com.example.ui.theme.PolishSuccess
import com.example.ui.theme.PolishSuccessContainer
import com.example.ui.theme.PolishSurface
import com.example.ui.theme.PolishSurfaceBorder
import com.example.ui.theme.PolishSurfaceBorderDark
import com.example.ui.theme.PolishSurfaceElevated
import com.example.ui.theme.PolishTextMuted
import com.example.ui.theme.PolishTextPrimary
import com.example.ui.theme.PolishTextSecondary
import com.example.ui.theme.PolishWarning
import com.example.ui.theme.PolishWarningContainer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun MetricGaugeCard(
    title: String,
    value: String,
    subValue: String,
    icon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.testTag("metric_card_$title"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = PolishSurfaceElevated),
        border = BorderStroke(1.dp, PolishSurfaceBorder)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = PolishPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    letterSpacing = 0.5.sp
                )
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(PolishSecondaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = PolishPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = PolishTextPrimary,
                fontSize = 22.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subValue,
                style = MaterialTheme.typography.bodySmall,
                color = PolishTextSecondary,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
fun TelemetryLogItemCard(
    log: TelemetryLogEntity,
    modifier: Modifier = Modifier
) {
    val (severityBadgeBg, severityTextColor) = when (log.severity) {
        TelemetrySeverity.OPTIMAL -> Pair(PolishSuccessContainer, PolishSuccess)
        TelemetrySeverity.INFO -> Pair(PolishSecondaryContainer, PolishPrimary)
        TelemetrySeverity.WARNING -> Pair(PolishWarningContainer, PolishWarning)
        TelemetrySeverity.CRITICAL -> Pair(PolishCriticalContainer, PolishCritical)
    }

    val typeIcon = when (log.type) {
        TelemetryType.VOICE_COMMAND -> Icons.Default.Mic
        TelemetryType.TOUCH_GESTURE -> Icons.Default.Sensors
        TelemetryType.NETWORK_TRAFFIC -> Icons.Default.Wifi
        TelemetryType.BATTERY_POWER -> Icons.Default.BatteryChargingFull
        TelemetryType.COMMUNICATION -> Icons.Default.Phone
        TelemetryType.SYSTEM_PERFORMANCE -> Icons.Default.Memory
        TelemetryType.SECURITY_AUDIT -> Icons.Default.Security
        TelemetryType.AI_INFERENCE -> Icons.Default.CheckCircle
    }

    val formattedTime = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(log.timestamp))

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("log_item_${log.id}"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = PolishSurfaceElevated),
        border = BorderStroke(1.dp, PolishSurfaceBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(severityBadgeBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = typeIcon,
                    contentDescription = null,
                    tint = severityTextColor,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = log.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = PolishTextPrimary,
                        fontSize = 14.sp
                    )
                    Text(
                        text = formattedTime,
                        style = MaterialTheme.typography.labelSmall,
                        color = PolishTextMuted,
                        fontSize = 11.sp
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = log.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = PolishTextSecondary,
                    fontSize = 12.sp
                )
                if (log.aiAudited && !log.aiAnnotation.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = PolishSecondaryContainer,
                        border = BorderStroke(1.dp, PolishSurfaceBorder)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "تدقيق الذكاء الاصطناعي: ${log.aiAnnotation}",
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
}

@Composable
fun InsightCard(
    insight: BehaviorInsightEntity,
    onApply: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("insight_card_${insight.id}"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = PolishSurface),
        border = BorderStroke(1.dp, PolishSurfaceBorder)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = PolishSecondaryContainer
                ) {
                    Text(
                        text = insight.category,
                        color = PolishPrimary,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = "دقة الفهم: ${insight.score}%",
                    style = MaterialTheme.typography.labelSmall,
                    color = PolishSuccess,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = insight.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = PolishTextPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = insight.summary,
                style = MaterialTheme.typography.bodySmall,
                color = PolishTextSecondary
            )
            if (insight.recommendation.isNotBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = PolishSurfaceElevated,
                    border = BorderStroke(1.dp, PolishSurfaceBorder)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "💡 التوصية: ${insight.recommendation}",
                            style = MaterialTheme.typography.bodySmall,
                            color = PolishPrimary,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.weight(1f),
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PermissionRowCard(
    info: AppPermissionInfo,
    onRequest: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("permission_row_${info.title}"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = PolishSurfaceElevated
        ),
        border = BorderStroke(
            1.dp,
            if (info.isGranted) PolishSuccess.copy(alpha = 0.3f) else PolishSurfaceBorder
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(
                        if (info.isGranted) PolishSuccessContainer
                        else PolishSecondaryContainer
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when (info.iconName) {
                        "mic" -> Icons.Default.Mic
                        "phone" -> Icons.Default.Phone
                        "contacts" -> Icons.Default.Call
                        "notifications" -> Icons.Default.Notifications
                        else -> Icons.Default.Security
                    },
                    contentDescription = null,
                    tint = if (info.isGranted) PolishSuccess else PolishPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = info.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = PolishTextPrimary,
                    fontSize = 14.sp
                )
                Text(
                    text = info.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = PolishTextSecondary,
                    fontSize = 12.sp
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            if (info.isGranted) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = PolishSuccessContainer,
                    border = BorderStroke(1.dp, PolishSuccess.copy(alpha = 0.4f))
                ) {
                    Text(
                        text = "مفعل ✓",
                        color = PolishSuccess,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            } else {
                Button(
                    onClick = onRequest,
                    colors = ButtonDefaults.buttonColors(containerColor = PolishPrimary),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.testTag("grant_permission_${info.title}")
                ) {
                    Text(
                        text = "منح الإذن",
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun ActionShortcutCard(
    shortcut: ActionShortcutEntity,
    onExecute: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("shortcut_card_${shortcut.id}"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = PolishSurfaceElevated),
        border = BorderStroke(1.dp, PolishSurfaceBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(PolishSecondaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "تشغيل",
                    tint = PolishPrimary,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = shortcut.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = PolishTextPrimary,
                    fontSize = 14.sp
                )
                Text(
                    text = "صيغة النداء: \"${shortcut.triggerVoicePhrase}\"",
                    style = MaterialTheme.typography.bodySmall,
                    color = PolishPrimary,
                    fontWeight = FontWeight.Medium,
                    fontSize = 12.sp
                )
                Text(
                    text = "مرات التنفيذ: ${shortcut.executionCount}",
                    style = MaterialTheme.typography.labelSmall,
                    color = PolishTextMuted,
                    fontSize = 10.sp
                )
            }
            Button(
                onClick = onExecute,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PolishPrimary),
                modifier = Modifier.testTag("execute_shortcut_${shortcut.id}")
            ) {
                Text("تنفيذ", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

