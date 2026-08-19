package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Hearing
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.VolumeMute
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.entity.ActionType
import com.example.ui.MainViewModel
import com.example.ui.theme.PolishBackground
import com.example.ui.theme.PolishPrimary
import com.example.ui.theme.PolishPrimaryContainer
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

@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val config by viewModel.assistantConfig.collectAsStateWithLifecycle()
    val isEnrolling by viewModel.isEnrollingVoiceprint.collectAsStateWithLifecycle()
    val enrollmentStep by viewModel.enrollmentStep.collectAsStateWithLifecycle()

    var assistantNameInput by remember(config.assistantName) { mutableStateOf(config.assistantName) }
    var selectedDialect by remember(config.preferredDialect) { mutableStateOf(config.preferredDialect) }
    var dialectDropdownExpanded by remember { mutableStateOf(false) }

    var voiceFeedback by remember(config.voiceFeedbackEnabled) { mutableStateOf(config.voiceFeedbackEnabled) }
    var autoContinuousListening by remember(config.autoContinuousListening) { mutableStateOf(config.autoContinuousListening) }
    var biometricVoiceprintEnabled by remember(config.biometricVoiceprintEnabled) { mutableStateOf(config.biometricVoiceprintEnabled) }
    var securityThreatAlerts by remember(config.securityThreatScanAutoAlerts) { mutableStateOf(config.securityThreatScanAutoAlerts) }
    var localNetworkMonitoring by remember(config.localNetworkMonitoringEnabled) { mutableStateOf(config.localNetworkMonitoringEnabled) }

    var speechPitch by remember(config.ttsPitch) { mutableFloatStateOf(config.ttsPitch) }
    var speechSpeed by remember(config.ttsSpeed) { mutableFloatStateOf(config.ttsSpeed) }

    var showAddShortcut by remember { mutableStateOf(false) }
    var newShortcutTitle by remember { mutableStateOf("") }
    var newShortcutPhrase by remember { mutableStateOf("") }

    val dialectOptions = listOf(
        "العربية (لهجات متعددة)",
        "اللهجة الشامية (سورية، لبنان، فلسطين، الأردن)",
        "اللهجة المصرية",
        "اللهجة الخليجية",
        "اللهجة المغاربية",
        "اللغة العربية الفصحى",
        "English (US / UK)"
    )

    val enrollmentPhrases = listOf(
        "أنا المالك المعتمد لهذا الهاتف والمتحكم به",
        "تفعيل التحكم الذاتي وتنفيذ كافة الأوامر بدون لمس",
        "حماية الهاتف وتدقيق الأمان ومراقبة الشبكة المحلية"
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

            // 1. Biometric Voiceprint Enrollment Card (Crucial for User Voiceprint intent)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("voiceprint_settings_card"),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = PolishSurfaceElevated),
                border = BorderStroke(1.dp, PolishSurfaceBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(if (config.voiceprintEnrolled) Color(0xFF10B981).copy(alpha = 0.15f) else PolishPrimaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Fingerprint,
                                contentDescription = null,
                                tint = if (config.voiceprintEnrolled) Color(0xFF10B981) else PolishPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "بصمة صوت المستخدم (Biometric Voiceprint)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = PolishTextPrimary
                            )
                            Text(
                                text = if (config.voiceprintEnrolled) "بصمة صوت المالك مسجلة بنجاح ✓" else "لم يتم تسجيل بصمة صوت المالك بعد",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (config.voiceprintEnrolled) Color(0xFF10B981) else PolishTextSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "يتم حفظ بصمة صوتك وتحليل الترددات الحيوية لضمان عدم تنفيذ الأوامر التنفيذية والتحكم بالهاتف إلا من خلال صوت المالك الحقيقي حصراً.",
                        style = MaterialTheme.typography.bodySmall,
                        color = PolishTextSecondary,
                        fontSize = 12.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "حظر الأوامر الصوتية لغير المالك",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = PolishTextPrimary
                        )
                        Switch(
                            checked = biometricVoiceprintEnabled,
                            onCheckedChange = {
                                biometricVoiceprintEnabled = it
                                viewModel.updateAssistantConfig(config.copy(biometricVoiceprintEnabled = it))
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Color(0xFF10B981),
                                uncheckedTrackColor = PolishSurfaceBorder
                            ),
                            modifier = Modifier.testTag("switch_biometric_strict")
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (!isEnrolling) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = { viewModel.startVoiceprintEnrollment() },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .testTag("btn_start_voiceprint_enroll"),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = PolishPrimary)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Mic,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (config.voiceprintEnrolled) "إعادة تسجيل بصمة الصوت" else "تسجيل بصمة صوت جديدة",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            if (config.voiceprintEnrolled) {
                                OutlinedButton(
                                    onClick = { viewModel.resetVoiceprintProfile() },
                                    shape = RoundedCornerShape(14.dp),
                                    border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.5f)),
                                    modifier = Modifier.height(44.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Reset",
                                        tint = Color(0xFFEF4444),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    } else {
                        // Active 3-step enrollment wizard
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = PolishSurface,
                            border = BorderStroke(1.dp, PolishPrimary.copy(alpha = 0.3f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp)
                            ) {
                                Text(
                                    text = "خطوة تسجيل البصمة (${enrollmentStep + 1} من 3)",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PolishPrimary
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "انقر على زر التسجيل وتحدث بالعبارة التالية بوضوح:",
                                    fontSize = 11.sp,
                                    color = PolishTextSecondary
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = PolishPrimaryContainer.copy(alpha = 0.5f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "« ${enrollmentPhrases.getOrElse(enrollmentStep) { "" }} »",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = PolishTextPrimary,
                                        modifier = Modifier.padding(12.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = { viewModel.recordEnrollmentSample(enrollmentStep) },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(42.dp)
                                            .testTag("btn_record_sample"),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                                    ) {
                                        Icon(Icons.Default.Mic, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("تسجيل العينة ${enrollmentStep + 1}", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }
                                    OutlinedButton(
                                        onClick = { viewModel.cancelVoiceprintEnrollment() },
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.height(42.dp)
                                    ) {
                                        Text("إلغاء", fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 2. Control Mode & Behavior (Addressing: Silent execution & always-on listening)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("autonomous_control_card"),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = PolishSurfaceElevated),
                border = BorderStroke(1.dp, PolishSurfaceBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "نمط التحكم الذاتي وتفويض الأوامر",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = PolishTextPrimary
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Hands-Free Auto-Listening Switch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "الاستماع الدائم التلقائي فور فتح التطبيق",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = PolishTextPrimary
                            )
                            Text(
                                text = "الهاتف يعمل ضمن الأوامر الصوتية مباشرة بدون الحاجة للضغط على أي أيقونة",
                                style = MaterialTheme.typography.bodySmall,
                                color = PolishTextSecondary,
                                fontSize = 11.sp
                            )
                        }
                        Switch(
                            checked = autoContinuousListening,
                            onCheckedChange = {
                                autoContinuousListening = it
                                viewModel.updateAssistantConfig(config.copy(autoContinuousListening = it))
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Color(0xFF10B981),
                                uncheckedTrackColor = PolishSurfaceBorder
                            ),
                            modifier = Modifier.testTag("switch_auto_continuous_listening")
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Persistent Background Foreground Service (When Screen is off or App is Closed)
                    val isBackgroundRunning by viewModel.isBackgroundServiceActive.collectAsStateWithLifecycle()
                    val context = androidx.compose.ui.platform.LocalContext.current
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "تشغيل المساعد كخدمة دائمة في الخلفية (Foreground Service)",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = PolishTextPrimary
                            )
                            Text(
                                text = "الاستماع وتنفيذ الأوامر حتى لو كان التطبيق مغلقاً أو الشاشة مقفلة",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isBackgroundRunning) Color(0xFF10B981) else PolishTextSecondary,
                                fontSize = 11.sp
                            )
                        }
                        Switch(
                            checked = isBackgroundRunning,
                            onCheckedChange = {
                                viewModel.toggleBackgroundService(context)
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Color(0xFF10B981),
                                uncheckedTrackColor = PolishSurfaceBorder
                            ),
                            modifier = Modifier.testTag("switch_background_service")
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Silent Execution (Disable speech) - Explicit user preference
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "الرد الصوتي الناطق (TTS Voice)",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = PolishTextPrimary
                            )
                            Text(
                                text = if (!voiceFeedback) "التنفيذ صامت بالنيابة عنك (موصى به)" else "المساعد يتحدث صوتياً",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (!voiceFeedback) Color(0xFF10B981) else PolishTextSecondary,
                                fontSize = 11.sp
                            )
                        }
                        Switch(
                            checked = voiceFeedback,
                            onCheckedChange = {
                                voiceFeedback = it
                                viewModel.updateAssistantConfig(config.copy(voiceFeedbackEnabled = it))
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = PolishPrimary,
                                uncheckedTrackColor = PolishSurfaceBorder
                            ),
                            modifier = Modifier.testTag("switch_voice_feedback")
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Local Network Monitoring Switch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "مراقبة الشبكة المحلية ومشاركات الشاشة",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = PolishTextPrimary
                            )
                            Text(
                                text = "أرشفة كافة البيانات والمشاركات والأجهزة المتصلة على نفس شبكة Wi-Fi",
                                style = MaterialTheme.typography.bodySmall,
                                color = PolishTextSecondary,
                                fontSize = 11.sp
                            )
                        }
                        Switch(
                            checked = localNetworkMonitoring,
                            onCheckedChange = {
                                localNetworkMonitoring = it
                                viewModel.updateAssistantConfig(config.copy(localNetworkMonitoringEnabled = it))
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = PolishSecondary,
                                uncheckedTrackColor = PolishSurfaceBorder
                            ),
                            modifier = Modifier.testTag("switch_lan_monitoring")
                        )
                    }
                }
            }
        }

        // 3. AI Persona & Identity Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("assistant_identity_card"),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = PolishSurfaceElevated),
                border = BorderStroke(1.dp, PolishSurfaceBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "تخصيص هوية المساعد واللهجة",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = PolishTextPrimary
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "اسم المساعد (نداء التنبيه):",
                        style = MaterialTheme.typography.labelSmall,
                        color = PolishTextSecondary,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = assistantNameInput,
                        onValueChange = { assistantNameInput = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("assistant_name_input"),
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = PolishSurface,
                            unfocusedContainerColor = PolishSurface,
                            focusedBorderColor = PolishPrimary,
                            unfocusedBorderColor = PolishSurfaceBorder,
                            focusedTextColor = PolishTextPrimary,
                            unfocusedTextColor = PolishTextPrimary
                        ),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "اللهجة واللغة المفضلة:",
                        style = MaterialTheme.typography.labelSmall,
                        color = PolishTextSecondary,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Box {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = PolishSurface,
                            border = BorderStroke(1.dp, PolishSurfaceBorder),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { dialectDropdownExpanded = true }
                                .padding(vertical = 4.dp)
                                .testTag("dialect_picker_trigger")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = selectedDialect,
                                    color = PolishTextPrimary,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = "تغيير ▼",
                                    color = PolishPrimary,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        DropdownMenu(
                            expanded = dialectDropdownExpanded,
                            onDismissRequest = { dialectDropdownExpanded = false },
                            modifier = Modifier
                                .background(PolishSurfaceElevated)
                                .testTag("dialect_dropdown_menu")
                        ) {
                            dialectOptions.forEach { dialect ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = dialect,
                                            color = if (dialect == selectedDialect) PolishPrimary else PolishTextPrimary,
                                            fontWeight = if (dialect == selectedDialect) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    onClick = {
                                        selectedDialect = dialect
                                        dialectDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            viewModel.updateAssistantConfig(
                                config.copy(
                                    assistantName = assistantNameInput.ifBlank { "نور" },
                                    preferredDialect = selectedDialect,
                                    voiceFeedbackEnabled = voiceFeedback,
                                    autoContinuousListening = autoContinuousListening,
                                    biometricVoiceprintEnabled = biometricVoiceprintEnabled,
                                    localNetworkMonitoringEnabled = localNetworkMonitoring,
                                    securityThreatScanAutoAlerts = securityThreatAlerts,
                                    ttsPitch = speechPitch,
                                    ttsSpeed = speechSpeed
                                )
                            )
                        },
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PolishPrimary),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("save_identity_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "حفظ إعدادات الهوية والتحكم",
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // 4. Add Custom Voice Shortcut Section
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("create_shortcut_card"),
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
                            text = "إنشاء اختصار تحكم صوتي مخصص",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = PolishTextPrimary
                        )
                        Button(
                            onClick = { showAddShortcut = !showAddShortcut },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PolishPrimary),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                                horizontal = 12.dp,
                                vertical = 6.dp
                            ),
                            modifier = Modifier.testTag("toggle_add_shortcut_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (showAddShortcut) "إغلاق" else "إضافة",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    if (showAddShortcut) {
                        Spacer(modifier = Modifier.height(14.dp))
                        OutlinedTextField(
                            value = newShortcutTitle,
                            onValueChange = { newShortcutTitle = it },
                            label = { Text("اسم الاختصار (مثال: تشغيل الكشاف)") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_shortcut_title"),
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = PolishSurface,
                                unfocusedContainerColor = PolishSurface,
                                focusedBorderColor = PolishPrimary,
                                unfocusedBorderColor = PolishSurfaceBorder,
                                focusedTextColor = PolishTextPrimary,
                                unfocusedTextColor = PolishTextPrimary
                            ),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedTextField(
                            value = newShortcutPhrase,
                            onValueChange = { newShortcutPhrase = it },
                            label = { Text("العبارة الصوتية للتشغيل (مثال: شغل الضوء يا نور)") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_shortcut_phrase"),
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = PolishSurface,
                                unfocusedContainerColor = PolishSurface,
                                focusedBorderColor = PolishPrimary,
                                unfocusedBorderColor = PolishSurfaceBorder,
                                focusedTextColor = PolishTextPrimary,
                                unfocusedTextColor = PolishTextPrimary
                            ),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(14.dp))
                        Button(
                            onClick = {
                                if (newShortcutTitle.isNotBlank() && newShortcutPhrase.isNotBlank()) {
                                    viewModel.addCustomShortcut(
                                        title = newShortcutTitle,
                                        triggerPhrase = newShortcutPhrase,
                                        actionType = ActionType.TOGGLE_FLASHLIGHT,
                                        payload = ""
                                    )
                                    newShortcutTitle = ""
                                    newShortcutPhrase = ""
                                    showAddShortcut = false
                                }
                            },
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PolishPrimary),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .testTag("submit_custom_shortcut_btn")
                        ) {
                            Text(
                                text = "حفظ الاختصار في الذكاء الاصطناعي",
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}
