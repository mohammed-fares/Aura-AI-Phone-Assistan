package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.MainViewModel
import com.example.ui.components.PermissionRowCard
import com.example.ui.theme.PolishBackground
import com.example.ui.theme.PolishPrimary
import com.example.ui.theme.PolishPrimaryContainer
import com.example.ui.theme.PolishSecondaryContainer
import com.example.ui.theme.PolishSuccess
import com.example.ui.theme.PolishSuccessContainer
import com.example.ui.theme.PolishSurfaceBorder
import com.example.ui.theme.PolishSurfaceElevated
import com.example.ui.theme.PolishTextPrimary
import com.example.ui.theme.PolishTextSecondary
import com.example.util.LocalizationManager

@Composable
fun PermissionsScreen(
    viewModel: MainViewModel,
    onRequestAllPermissions: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val permissions by viewModel.permissionsState.collectAsStateWithLifecycle()
    val appLang by viewModel.appLanguage.collectAsStateWithLifecycle()
    val isAccessibilityConnected by viewModel.isAccessibilityConnected.collectAsStateWithLifecycle()

    val isAr = LocalizationManager.getEffectiveLanguage(appLang) == "ar"
    fun s(ar: String, en: String): String = if (isAr) ar else en

    val grantedCount = permissions.count { it.isGranted }
    val totalCount = permissions.size.coerceAtLeast(1)
    val progress = grantedCount.toFloat() / totalCount

    val singlePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) {
        onRequestAllPermissions()
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(PolishBackground)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(12.dp))

            // Permission Overview Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("permissions_hub_card"),
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
                                    imageVector = Icons.Default.Shield,
                                    contentDescription = null,
                                    tint = PolishPrimary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = s("مركز صلاحيات وأذونات الهاتف", "Permissions & Access Center"),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = PolishTextPrimary
                                )
                                Text(
                                    text = s("تمكين الوصول لكافة وظائف الهاتف ذاتياً", "Enable access to phone functions"),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = PolishTextSecondary,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = if (grantedCount == totalCount) PolishSuccessContainer else PolishSecondaryContainer,
                            border = BorderStroke(1.dp, if (grantedCount == totalCount) PolishSuccess else PolishSurfaceBorder)
                        ) {
                            Text(
                                text = "$grantedCount " + s("من", "of") + " $totalCount " + s("نشطة", "granted"),
                                style = MaterialTheme.typography.labelSmall,
                                color = if (grantedCount == totalCount) PolishSuccess else PolishPrimary,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = if (grantedCount == totalCount) PolishSuccess else PolishPrimary,
                        trackColor = PolishSurfaceBorder
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = onRequestAllPermissions,
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PolishPrimary),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("grant_all_permissions_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (grantedCount == totalCount) s("كافة الأذونات ممنوحة وجاهزة", "All Permissions Granted ✓") else s("منح وتفعيل كافة الصلاحيات الآن", "Grant All Permissions Now"),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }

        // Accessibility Service (Autonomous UI Automation) Permission Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("accessibility_permission_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = PolishSurfaceElevated),
                border = BorderStroke(1.dp, if (isAccessibilityConnected) Color(0xFF10B981).copy(alpha = 0.5f) else Color(0xFFF59E0B).copy(alpha = 0.4f))
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
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(if (isAccessibilityConnected) Color(0xFF10B981).copy(alpha = 0.15f) else Color(0xFFF59E0B).copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.TouchApp,
                                    contentDescription = null,
                                    tint = if (isAccessibilityConnected) Color(0xFF10B981) else Color(0xFFF59E0B),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = s("خدمة إمكانية الوصول والتنفيذ الذاتي", "Accessibility & Autonomous Service"),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = PolishTextPrimary
                                )
                                Text(
                                    text = if (isAccessibilityConnected) s("مفعلة وتعمل بنجاح ✓", "Enabled & Active ✓") else s("مطلوبة لكتابة الرسائل والنقر في التطبيقات", "Required for typing & clicking in apps"),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (isAccessibilityConnected) Color(0xFF10B981) else Color(0xFFF59E0B),
                                    fontSize = 11.sp
                                )
                            }
                        }

                        Button(
                            onClick = { viewModel.openAccessibilitySettings(context) },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isAccessibilityConnected) Color(0xFF10B981) else Color(0xFFF59E0B)
                            ),
                            modifier = Modifier.height(36.dp)
                        ) {
                            Icon(imageVector = Icons.Default.OpenInNew, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (isAccessibilityConnected) s("مفعلة", "Active") else s("تفعيل", "Enable"), fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        item {
            Text(
                text = s("قائمة الصلاحيات والوصول للنظام", "System Permissions & Access"),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = PolishTextPrimary
            )
        }

        items(permissions, key = { it.permission }) { info ->
            PermissionRowCard(
                info = info,
                onRequest = { singlePermissionLauncher.launch(info.permission) }
            )
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = PolishSurfaceElevated),
                border = BorderStroke(1.dp, PolishSurfaceBorder)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "🔒 " + s("الخصوصية والأمان الفائق", "Privacy & Local Processing"),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = PolishPrimary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = s("تطبيق المساعد لا يقوم ببيع بياناتك، ويتم تدقيق وأرشفة الأحداث محلياً لحماية خصوصيتك مع تشفير تام للاتصالات.", "The assistant processes data locally on-device and respects your privacy."),
                        style = MaterialTheme.typography.bodySmall,
                        color = PolishTextSecondary,
                        fontSize = 11.sp
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
