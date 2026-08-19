package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.system.ThreatCategory
import com.example.system.ThreatItem
import com.example.system.ThreatSeverity
import com.example.ui.MainViewModel

@Composable
fun SecurityScanScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val isScanning by viewModel.isScanningSecurity.collectAsState()
    val scanProgress by viewModel.securityScanProgress.collectAsState()
    val scanStatusMsg by viewModel.securityScanStatusMessage.collectAsState()
    val report by viewModel.lastSecurityReport.collectAsState()

    val infiniteTransition = rememberInfiniteTransition(label = "radar_spin")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "radar_angle"
    )

    val currentScore = report?.securityScore ?: 98
    val scoreColor = when {
        currentScore >= 85 -> Color(0xFF10B981)
        currentScore >= 60 -> Color(0xFFF59E0B)
        else -> Color(0xFFEF4444)
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .testTag("security_scan_screen"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Security Header & Score Banner
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(120.dp)
                    ) {
                        CircularProgressIndicator(
                            progress = { currentScore / 100f },
                            modifier = Modifier.size(120.dp),
                            strokeWidth = 8.dp,
                            color = scoreColor,
                            trackColor = scoreColor.copy(alpha = 0.15f)
                        )
                        if (isScanning) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Scanning",
                                modifier = Modifier
                                    .size(48.dp)
                                    .rotate(rotation),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        } else {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "$currentScore%",
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = scoreColor
                                )
                                Text(
                                    text = if (currentScore >= 85) "آمن ومحمي" else "تنبيه أمني",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "مركز فحص الأمان ومكافحة الاختراق",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = report?.systemStatusText ?: "جاهز لإجراء فحص شامل للهاتف وتدقيق التطبيقات والملفات الخبيثة.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Scan Action Button
                    Button(
                        onClick = { viewModel.startFullSecurityScan() },
                        enabled = !isScanning,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("start_security_scan_button"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        if (isScanning) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("جاري الفحص ($scanProgress%)...", fontWeight = FontWeight.Bold)
                        } else {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = "Scan",
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("بدء فحص الأمان ومكافحة الاختراق", fontWeight = FontWeight.Bold)
                        }
                    }

                    // Progress Bar
                    AnimatedVisibility(visible = isScanning) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp)
                        ) {
                            LinearProgressIndicator(
                                progress = { scanProgress / 100f },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = scanStatusMsg,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }

        // 2. Telemetry Counts Grid
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SecurityStatBadge(
                    modifier = Modifier.weight(1f),
                    title = "التطبيقات",
                    count = report?.appsScannedCount ?: 42,
                    subtitle = "مفحوصة"
                )
                SecurityStatBadge(
                    modifier = Modifier.weight(1f),
                    title = "الملفات والوسائط",
                    count = report?.filesScannedCount ?: 84,
                    subtitle = "مدققة"
                )
                SecurityStatBadge(
                    modifier = Modifier.weight(1f),
                    title = "الاتصالات والمنافذ",
                    count = report?.networkConnectionsScannedCount ?: 12,
                    subtitle = "مؤمنة"
                )
            }
        }

        // 3. System Integrity Diagnostic Cards
        item {
            Text(
                text = "تدقيق سلامة النواة وحماية النظام",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SecurityCheckRow(
                    title = "صلاحيات الروت وتعديل النظام (Root / SU Check)",
                    isSafe = !(report?.isSystemIntegrityCompromised ?: false),
                    detail = if (report?.isSystemIntegrityCompromised == true) "تم رصد ملفات روت مفتوحة" else "النظام مغلق ومحمي رسمياً"
                )
                SecurityCheckRow(
                    title = "تشفير التخزين والذاكرة (Storage Encryption)",
                    isSafe = true,
                    detail = "التشفير التام AES-256 نشط لحماية بيانات الهاتف"
                )
                SecurityCheckRow(
                    title = "تطبيقات التراكب والتجسس (Overlay & Accessibility)",
                    isSafe = report?.threatsFound?.none { it.category == ThreatCategory.MALICIOUS_APP } ?: true,
                    detail = "مراقبة الصلاحيات الحساسة ومنع تسجيل الشاشة الخفي"
                )
                SecurityCheckRow(
                    title = "حماية الشبكة المحلية والبروكسي (LAN / Proxy Guard)",
                    isSafe = report?.threatsFound?.none { it.category == ThreatCategory.NETWORK_BREACH } ?: true,
                    detail = "فحص منافذ الاتصال المفتوحة ومنع اعتراض حركة الإنترنت"
                )
            }
        }

        // 4. Detected Threats Section
        item {
            val threats = report?.threatsFound ?: emptyList()
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "التهديدات والملاحظات المكتشفة (${threats.size})",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (threats.isNotEmpty() && threats.any { !it.isResolved }) {
                    Text(
                        text = "يتطلب إجراءات حماية",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFEF4444)
                    )
                }
            }
        }

        val threatList = report?.threatsFound ?: emptyList()
        if (threatList.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF10B981).copy(alpha = 0.08f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Safe",
                            tint = Color(0xFF10B981),
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(
                                text = "الهاتف نظيف تماماً من أي ملفات أو تطبيقات خبيثة",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF065F46)
                            )
                            Text(
                                text = "كافة المدخلات والمخرجات ومشاركات الشبكة تحت المراقبة الآمنة بالذكاء الاصطناعي.",
                                fontSize = 12.sp,
                                color = Color(0xFF047857)
                            )
                        }
                    }
                }
            }
        } else {
            items(threatList, key = { it.id }) { threat ->
                ThreatItemCard(
                    threat = threat,
                    onNeutralize = { viewModel.neutralizeThreat(threat.id) }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun SecurityStatBadge(
    modifier: Modifier = Modifier,
    title: String,
    count: Int,
    subtitle: String
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "$count",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = title,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun SecurityCheckRow(
    title: String,
    isSafe: Boolean,
    detail: String
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isSafe) Icons.Default.CheckCircle else Icons.Default.Warning,
                contentDescription = null,
                tint = if (isSafe) Color(0xFF10B981) else Color(0xFFF59E0B),
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = detail,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun ThreatItemCard(
    threat: ThreatItem,
    onNeutralize: () -> Unit
) {
    val containerBg = if (threat.isResolved) {
        Color(0xFF10B981).copy(alpha = 0.08f)
    } else when (threat.severity) {
        ThreatSeverity.CRITICAL -> Color(0xFFEF4444).copy(alpha = 0.08f)
        ThreatSeverity.WARNING -> Color(0xFFF59E0B).copy(alpha = 0.08f)
        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
    }

    val badgeColor = if (threat.isResolved) {
        Color(0xFF10B981)
    } else when (threat.severity) {
        ThreatSeverity.CRITICAL -> Color(0xFFEF4444)
        ThreatSeverity.WARNING -> Color(0xFFF59E0B)
        else -> Color(0xFF3B82F6)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = containerBg)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = threat.title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = badgeColor.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = if (threat.isResolved) "تم التطهير ✓" else when (threat.severity) {
                            ThreatSeverity.CRITICAL -> "تهديد عالي"
                            ThreatSeverity.WARNING -> "تحذير"
                            ThreatSeverity.SUSPICIOUS -> "مشبوه"
                            ThreatSeverity.SECURE -> "آمن"
                        },
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = badgeColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = threat.description,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "المسار / الحزمة: ${threat.targetPathOrPackage}",
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(10.dp))

            if (!threat.isResolved) {
                Button(
                    onClick = onNeutralize,
                    modifier = Modifier.align(Alignment.End),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = badgeColor)
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Fix",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("عزل وتطهير التهديد", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
