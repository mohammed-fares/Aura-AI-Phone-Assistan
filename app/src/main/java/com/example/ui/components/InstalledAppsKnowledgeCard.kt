package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.InstalledAppEntity
import com.example.ui.theme.PolishPrimary
import com.example.ui.theme.PolishPrimaryContainer
import com.example.ui.theme.PolishSecondary
import com.example.ui.theme.PolishSuccess
import com.example.ui.theme.PolishSuccessContainer
import com.example.ui.theme.PolishSurface
import com.example.ui.theme.PolishSurfaceBorder
import com.example.ui.theme.PolishSurfaceElevated
import com.example.ui.theme.PolishTextPrimary
import com.example.ui.theme.PolishTextSecondary

@Composable
fun InstalledAppsKnowledgeCard(
    apps: List<InstalledAppEntity>,
    isIndexing: Boolean,
    isAr: Boolean,
    onRefreshApps: () -> Unit,
    onLaunchApp: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    fun s(ar: String, en: String): String = if (isAr) ar else en

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("installed_apps_knowledge_card"),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = PolishSurfaceElevated),
        border = BorderStroke(1.dp, PolishSurfaceBorder)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(PolishPrimaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Apps,
                            contentDescription = null,
                            tint = PolishPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = s("معرفة وتدريب الذكاء على تطبيقات الهاتف", "AI Installed Apps Intelligence Hub"),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = PolishTextPrimary
                        )
                        Text(
                            text = if (isIndexing) s("جاري فحص وفهم تطبيقات الهاتف...", "Scanning & indexing installed apps...")
                            else s("تم استيراد وفهم ${apps.size} تطبيق للتحكم الذاتي المباشر", "Imported & learned ${apps.size} apps for autonomous control"),
                            style = MaterialTheme.typography.bodySmall,
                            color = PolishTextSecondary,
                            fontSize = 11.sp
                        )
                    }
                }

                IconButton(onClick = onRefreshApps, enabled = !isIndexing) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh Apps",
                        tint = PolishPrimary
                    )
                }
            }

            if (isIndexing) {
                Spacer(modifier = Modifier.height(10.dp))
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = PolishPrimary,
                    trackColor = PolishSurfaceBorder
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Quick App Badges Horizontal Row
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(apps.take(15)) { app ->
                    Surface(
                        onClick = { onLaunchApp(app.packageName) },
                        shape = RoundedCornerShape(12.dp),
                        color = PolishSurface,
                        border = BorderStroke(1.dp, PolishSurfaceBorder)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(PolishSuccess)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isAr && app.appNameArabic.isNotBlank()) app.appNameArabic else app.appName,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = PolishTextPrimary,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Expand/Collapse Details Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = PolishSuccessContainer
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = PolishSuccess,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = s("جاهز لاستقبال كافة أوامر هذه التطبيقات", "Ready for any voice commands"),
                            color = PolishSuccess,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Button(
                    onClick = { expanded = !expanded },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                    modifier = Modifier.height(30.dp)
                ) {
                    Text(
                        text = if (expanded) s("إخفاء التفاصيل", "Hide Details") else s("استعراض الآليات الذكية (${apps.size})", "View Mechanics (${apps.size})"),
                        color = PolishPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Icon(
                        imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        tint = PolishPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Expanded Mechanics & Execution Details
            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    apps.forEach { app ->
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = PolishSurface,
                            border = BorderStroke(1.dp, PolishSurfaceBorder),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = if (isAr && app.appNameArabic.isNotBlank()) "${app.appNameArabic} (${app.appName})" else app.appName,
                                            fontWeight = FontWeight.Bold,
                                            color = PolishTextPrimary,
                                            fontSize = 13.sp
                                        )
                                        Text(
                                            text = app.packageName,
                                            color = PolishTextSecondary,
                                            fontSize = 10.sp
                                        )
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = PolishPrimaryContainer
                                    ) {
                                        Text(
                                            text = app.category,
                                            color = PolishPrimary,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                if (app.aiMechanicsDescription.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "💡 " + app.aiMechanicsDescription,
                                        color = PolishTextSecondary,
                                        fontSize = 11.sp,
                                        lineHeight = 15.sp
                                    )
                                }

                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "🗣️ " + s("الكلمات الدلالية الصوتية: ", "Voice Triggers: ") + app.keywords,
                                    color = PolishSecondary,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
