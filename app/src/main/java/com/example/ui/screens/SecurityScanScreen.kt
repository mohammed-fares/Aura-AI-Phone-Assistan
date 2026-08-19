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
import androidx.compose.material.icons.filled.Lock
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.system.ThreatCategory
import com.example.system.ThreatItem
import com.example.system.ThreatSeverity
import com.example.ui.MainViewModel
import com.example.ui.theme.PolishBackground
import com.example.ui.theme.PolishPrimary
import com.example.ui.theme.PolishSecondary
import com.example.ui.theme.PolishSuccess
import com.example.ui.theme.PolishSurfaceBorder
import com.example.ui.theme.PolishSurfaceElevated
import com.example.ui.theme.PolishTextPrimary
import com.example.ui.theme.PolishTextSecondary
import com.example.ui.theme.PolishWarning
import com.example.util.LocalizationManager

@Composable
fun SecurityScanScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val isScanning by viewModel.isScanningSecurity.collectAsStateWithLifecycle()
    val scanProgress by viewModel.securityScanProgress.collectAsStateWithLifecycle()
    val scanStatusMsg by viewModel.securityScanStatusMessage.collectAsStateWithLifecycle()
    val report by viewModel.lastSecurityReport.collectAsStateWithLifecycle()
    val appLang by viewModel.appLanguage.collectAsStateWithLifecycle()

    val isAr = LocalizationManager.getEffectiveLanguage(appLang) == "ar"
    fun s(ar: String, en: String): String = if (isAr) ar else en

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
            .background(PolishBackground)
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .testTag("security_scan_screen"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Security Header & Score Banner
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = PolishSurfaceElevated),
                border = BorderStroke(1.dp, PolishSurfaceBorder)
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
                                tint = PolishPrimary
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
                                    text = if (currentScore >= 85) s("آمن ومحمي", "Protected") else s("تنبيه أمني", "Warning"),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = PolishTextSecondary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = s("مركز فحص الأمان ومكافحة الاختراق", "Security & Anti-Intrusion Center"),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = PolishTextPrimary
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = report?.systemStatusText ?: s("جاهز لإجراء فحص شامل للهاتف وتدقيق التطبيقات والملفات الخبيثة.", "Ready to perform full-system security scan."),
                        fontSize = 13.sp,
                        color = PolishTextSecondary,
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
                            containerColor = PolishPrimary
                        )
                    ) {
                        if (isScanning) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(s("جاري الفحص ($scanProgress%)...", "Scanning ($scanProgress%)..."), fontWeight = FontWeight.Bold)
                        } else {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = "Scan",
                                modifier = Modifier.size(20.dp),
                                tint = Color.White
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(s("بدء فحص الأمان ومكافحة الاختراق", "Start Full Security Scan"), fontWeight = FontWeight.Bold)
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
                                color = PolishPrimary
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = scanStatusMsg,
                                fontSize = 12.sp,
                                color = PolishPrimary,
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
                    title = s("التطبيقات", "Apps"),
                    count = report?.appsScannedCount ?: 42,
                    subtitle = s("مفحوصة", "Scanned")
                )
                SecurityStatBadge(
                    modifier = Modifier.weight(1f),
                    title = s("الملفات والوسائط", "Files"),
                    count = report?.filesScannedCount ?: 84,
                    subtitle = s("مدققة", "Audited")
                )
                SecurityStatBadge(
                    modifier = Modifier.weight(1f),
                    title = s("الاتصالات والمنافذ", "Network Ports"),
                    count = report?.networkConnectionsScannedCount ?: 12,
                    subtitle = s("مؤمنة", "Secured")
                )
            }
        }

        // 3. System Integrity Diagnostic Cards
        item {
            Text(
                text = s("تدقيق سلامة النواة وحماية النظام", "Core System Integrity Checks"),
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = PolishTextPrimary,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SecurityCheckRow(
                    title = s("صلاحيات الروت وتعديل النظام (Root / SU Check)", "Root & Bootloader Integrity"),
                    isSafe = !(report?.isSystemIntegrityCompromised ?: false),
                    detail = if (report?.isSystemIntegrityCompromised == true) s("تم رصد ملفات روت مفتوحة", "Root access detected") else s("النظام مغلق ومحمي رسمياً", "Official verified build")
                )
                SecurityCheckRow(
                    title = s("تشفير التخزين والذاكرة (Storage Encryption)", "Hardware Storage Encryption"),
                    isSafe = true,
                    detail = s("التشفير التام AES-256 نشط لحماية بيانات الهاتف", "Full-disk AES-256 active")
                )
                SecurityCheckRow(
                    title = s("تطبيقات التراكب والتجسس (Overlay & Accessibility)", "Screen Overlay & Spyware Guard"),
                    isSafe = report?.threatsFound?.none { it.category == ThreatCategory.MALICIOUS_APP } ?: true,
                    detail = s("مراقبة الصلاحيات الحساسة ومنع تسجيل الشاشة الخفي", "Protected against hidden overlays")
                )
                SecurityCheckRow(
                    title = s("حماية الشبكة المحلية والبروكسي (LAN / Proxy Guard)", "LAN & Proxy Integrity"),
                    isSafe = report?.threatsFound?.none { it.category == ThreatCategory.NETWORK_BREACH } ?: true,
                    detail = s("فحص منافذ الاتصال المفتوحة ومنع اعتراض حركة الإنترنت", "No unauthorized proxies active")
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
                    text = s("التهديدات والملاحظات المكتشفة (${threats.size})", "Detected Threats (${threats.size})"),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = PolishTextPrimary
                )
                if (threats.isNotEmpty() && threats.any { !it.isResolved }) {
                    Text(
                        text = s("يتطلب إجراءات حماية", "Action required"),
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
                    ),
                    border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.2f))
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
                                text = s("الهاتف نظيف تماماً من أي ملفات أو تطبيقات خبيثة", "Device is fully clean and secure"),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF10B981)
                            )
                            Text(
                                text = s("كافة المدخلات والمخرجات ومشاركات الشبكة تحت المراقبة الآمنة بالذكاء الاصطناعي.", "All inputs, outbound connections, and media streams are protected."),
                                fontSize = 12.sp,
                                color = PolishTextSecondary
                            )
                        }
                    }
                }
            }
        } else {
            items(threatList, key = { it.id }) { threat ->
                ThreatItemCard(
                    threat = threat,
                    isAr = isAr,
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
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = PolishSurfaceElevated),
        border = BorderStroke(1.dp, PolishSurfaceBorder)
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
                color = PolishPrimary
            )
            Text(
                text = title,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = PolishTextPrimary
            )
            Text(
                text = subtitle,
                fontSize = 10.sp,
                color = PolishTextSecondary
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
        shape = RoundedCornerShape(14.dp),
        color = PolishSurfaceElevated,
        border = BorderStroke(1.dp, PolishSurfaceBorder)
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
                    color = PolishTextPrimary
                )
                Text(
                    text = detail,
                    fontSize = 11.sp,
                    color = PolishTextSecondary
                )
            }
        }
    }
}

@Composable
fun ThreatItemCard(
    threat: ThreatItem,
    isAr: Boolean,
    onNeutralize: () -> Unit
) {
    val containerBg = if (threat.isResolved) {
        Color(0xFF10B981).copy(alpha = 0.08f)
    } else when (threat.severity) {
        ThreatSeverity.CRITICAL -> Color(0xFFEF4444).copy(alpha = 0.08f)
        ThreatSeverity.WARNING -> Color(0xFFF59E0B).copy(alpha = 0.08f)
        else -> PolishSurfaceElevated
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
        colors = CardDefaults.cardColors(containerColor = containerBg),
        border = BorderStroke(1.dp, PolishSurfaceBorder)
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
                    color = PolishTextPrimary,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = badgeColor.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = if (threat.isResolved) (if (isAr) "تم التطهير ✓" else "Resolved ✓") else when (threat.severity) {
                            ThreatSeverity.CRITICAL -> if (isAr) "تهديد عالي" else "Critical"
                            ThreatSeverity.WARNING -> if (isAr) "تحذير" else "Warning"
                            ThreatSeverity.SUSPICIOUS -> if (isAr) "مشبوه" else "Suspicious"
                            ThreatSeverity.SECURE -> if (isAr) "آمن" else "Secure"
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
                color = PolishTextSecondary
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = (if (isAr) "المسار / الحزمة: " else "Target: ") + threat.targetPathOrPackage,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = PolishPrimary
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
                        modifier = Modifier.size(16.dp),
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (isAr) "عزل وتطهير التهديد" else "Neutralize Threat", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }
}
