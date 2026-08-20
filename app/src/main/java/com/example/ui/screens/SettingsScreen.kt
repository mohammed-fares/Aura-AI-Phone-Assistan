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
import androidx.compose.material.icons.filled.AccessibilityNew
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Hearing
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.VolumeMute
import androidx.compose.material.icons.filled.VolumeOff
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
import androidx.compose.ui.platform.LocalContext
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
import com.example.util.LocalizationManager

@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val config by viewModel.assistantConfig.collectAsStateWithLifecycle()
    val isEnrolling by viewModel.isEnrollingVoiceprint.collectAsStateWithLifecycle()
    val enrollmentStep by viewModel.enrollmentStep.collectAsStateWithLifecycle()
    val appLang by viewModel.appLanguage.collectAsStateWithLifecycle()
    val isAccessibilityConnected by viewModel.isAccessibilityConnected.collectAsStateWithLifecycle()
    val isBackgroundRunning by viewModel.isBackgroundServiceActive.collectAsStateWithLifecycle()

    val isAr = LocalizationManager.getEffectiveLanguage(appLang) == "ar"
    fun s(ar: String, en: String): String = if (isAr) ar else en

    var assistantNameInput by remember(config.assistantName) { mutableStateOf(config.assistantName) }
    var selectedDialect by remember(config.preferredDialect) { mutableStateOf(config.preferredDialect) }
    var dialectDropdownExpanded by remember { mutableStateOf(false) }

    var voiceFeedback by remember(config.voiceFeedbackEnabled) { mutableStateOf(config.voiceFeedbackEnabled) }
    var muteAllAppSounds by remember(config.muteAllAppSounds) { mutableStateOf(config.muteAllAppSounds) }
    var muteMicBleeps by remember(config.muteMicBleepsAndSystemSounds) { mutableStateOf(config.muteMicBleepsAndSystemSounds) }
    var autonomousUiInteractions by remember(config.autonomousUiInteractions) { mutableStateOf(config.autonomousUiInteractions) }
    var keepMicOpenContinuously by remember(config.keepMicOpenContinuously) { mutableStateOf(config.keepMicOpenContinuously) }
    var autoContinuousListening by remember(config.autoContinuousListening) { mutableStateOf(config.autoContinuousListening) }
    var biometricVoiceprintEnabled by remember(config.biometricVoiceprintEnabled) { mutableStateOf(config.biometricVoiceprintEnabled) }
    var securityThreatAlerts by remember(config.securityThreatScanAutoAlerts) { mutableStateOf(config.securityThreatScanAutoAlerts) }
    var localNetworkMonitoring by remember(config.localNetworkMonitoringEnabled) { mutableStateOf(config.localNetworkMonitoringEnabled) }
    var wakeWordOnlyMode by remember(config.wakeWordOnlyMode) { mutableStateOf(config.wakeWordOnlyMode) }
    var customWakeWordInput by remember(config.customWakeWord) { mutableStateOf(config.customWakeWord) }

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

    val enrollmentPhrases = if (isAr) listOf(
        "أنا المالك المعتمد لهذا الهاتف والمتحكم به",
        "تفعيل التحكم الذاتي وتنفيذ كافة الأوامر بدون لمس",
        "حماية الهاتف وتدقيق الأمان ومراقبة الشبكة المحلية"
    ) else listOf(
        "I am the authorized owner of this phone",
        "Activate autonomous hands-free phone control",
        "System security verified and protected"
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

            // 0. Language Selector (Arabic / English / System)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("language_settings_card"),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = PolishSurfaceElevated),
                border = BorderStroke(1.dp, PolishSurfaceBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(PolishPrimaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Language,
                                contentDescription = null,
                                tint = PolishPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = s("لغة واجهة التطبيق", "App Interface Language"),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = PolishTextPrimary
                            )
                            Text(
                                text = s("اختر اللغة الافتراضية لعرض التطبيق", "Select the UI display language"),
                                style = MaterialTheme.typography.bodySmall,
                                color = PolishTextSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val languages = listOf(
                            Triple("system", "تلقائي (لغة النظام)", "System Default"),
                            Triple("ar", "العربية", "Arabic"),
                            Triple("en", "English", "English")
                        )

                        languages.forEach { (code, labelAr, labelEn) ->
                            val isSelected = appLang == code
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = if (isSelected) PolishPrimaryContainer else PolishSurface,
                                border = BorderStroke(
                                    1.dp,
                                    if (isSelected) PolishPrimary else PolishSurfaceBorder
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { viewModel.setAppLanguage(code) }
                            ) {
                                Row(
                                    modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = PolishPrimary,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                    }
                                    Text(
                                        text = if (isAr) labelAr else labelEn,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) PolishPrimary else PolishTextSecondary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 1. Autonomous Accessibility & UI Execution Card (CRITICAL USER FEATURE)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("accessibility_service_card"),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = PolishSurfaceElevated),
                border = BorderStroke(1.dp, if (isAccessibilityConnected) Color(0xFF10B981).copy(alpha = 0.5f) else Color(0xFFF59E0B).copy(alpha = 0.4f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(if (isAccessibilityConnected) Color(0xFF10B981).copy(alpha = 0.15f) else Color(0xFFF59E0B).copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.TouchApp,
                                contentDescription = null,
                                tint = if (isAccessibilityConnected) Color(0xFF10B981) else Color(0xFFF59E0B),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = s("التحكم الذاتي الفعلي والدخول للتطبيقات", "Autonomous UI Automation & App Access"),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = PolishTextPrimary
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(if (isAccessibilityConnected) Color(0xFF10B981) else Color(0xFFF59E0B))
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isAccessibilityConnected)
                                        s("خدمة الوصول متصلة وجاهزة للتنفيذ الذاتي ✅", "Accessibility Service Active & Connected ✅")
                                    else
                                        s("خدمة الوصول غير مفعلة (يتطلب تفعيلها للضغط والكتابة)", "Accessibility Service Disabled (Tap to enable)"),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isAccessibilityConnected) Color(0xFF10B981) else Color(0xFFF59E0B)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = s(
                            "يتيح للمساعد فتح التطبيقات، كتابة الرسائل تلقائياً (مثل 'مرحبا')، الضغط على زر الإرسال، وإغلاق التطبيق والعودة للشاشة الرئيسية للهاتف تماماً كما يفعل المستخدم باليد.",
                            "Enables the assistant to open apps, type message text autonomously, tap the send button, close the app, and return home hands-free."
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = PolishTextSecondary,
                        fontSize = 12.sp,
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = s("تفعيل التنفيذ الذاتي والتنقل بين الصفحات", "Enable Autonomous Cross-App Actions"),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = PolishTextPrimary
                            )
                            Text(
                                text = s("الكتابة والنقر والرجوع التلقائي للشاشة الرئيسية", "Auto-typing, clicking, and returning to home screen"),
                                style = MaterialTheme.typography.bodySmall,
                                color = if (autonomousUiInteractions) Color(0xFF10B981) else PolishTextSecondary,
                                fontSize = 11.sp
                            )
                        }
                        Switch(
                            checked = autonomousUiInteractions,
                            onCheckedChange = {
                                autonomousUiInteractions = it
                                viewModel.updateAssistantConfig(config.copy(autonomousUiInteractions = it))
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Color(0xFF10B981),
                                uncheckedTrackColor = PolishSurfaceBorder
                            ),
                            modifier = Modifier.testTag("switch_autonomous_ui")
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = { viewModel.openAccessibilitySettings(context) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .testTag("btn_open_accessibility_settings"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isAccessibilityConnected) PolishSurface else PolishPrimary
                        ),
                        border = if (isAccessibilityConnected) BorderStroke(1.dp, PolishSurfaceBorder) else null
                    ) {
                        Icon(
                            imageVector = if (isAccessibilityConnected) Icons.Default.Check else Icons.Default.OpenInNew,
                            contentDescription = null,
                            tint = if (isAccessibilityConnected) Color(0xFF10B981) else Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isAccessibilityConnected)
                                s("إدارة إعدادات خدمة الوصول (مفعلة)", "Manage Accessibility Service (Enabled)")
                            else
                                s("فتح إعدادات الهاتف لتفعيل خدمة الوصول (Aura AI)", "Open Android Settings to Enable Aura AI Service"),
                            color = if (isAccessibilityConnected) PolishTextPrimary else Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        // 2. Audio & Sound Controls Card (NEW USER FEATURE)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("audio_and_sounds_card"),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = PolishSurfaceElevated),
                border = BorderStroke(1.dp, PolishSurfaceBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(PolishSecondaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (muteMicBleeps && muteAllAppSounds) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                                contentDescription = null,
                                tint = PolishSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = s("التحكم الكامل بأصوات المايك والهاتف", "Microphone & App Sound Controls"),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = PolishTextPrimary
                            )
                            Text(
                                text = s("إلغاء وتشغيل الأصوات ونغمات فتح وإغلاق المايك", "Toggle or mute mic open/close bleeps and app sounds"),
                                style = MaterialTheme.typography.bodySmall,
                                color = PolishTextSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 1. Mute Mic Open/Close Bleeps & System Chimes
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = s("إلغاء أصوات فتح وإغلاق المايك (صمت تام)", "Mute Mic Open/Close Bleeps & Chimes"),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = PolishTextPrimary
                            )
                            Text(
                                text = if (muteMicBleeps)
                                    s("تم كتم نغمات وأصوات المايك والنظام عند بدء الاستماع ✓", "Mic start/stop chimes & system bleeps suppressed ✓")
                                else
                                    s("أصوات ونغمات فتح المايك تصدر بشكل طبيعي", "Standard mic chime sounds are audible"),
                                style = MaterialTheme.typography.bodySmall,
                                color = if (muteMicBleeps) Color(0xFF10B981) else PolishTextSecondary,
                                fontSize = 11.sp
                            )
                        }
                        Switch(
                            checked = muteMicBleeps,
                            onCheckedChange = {
                                muteMicBleeps = it
                                viewModel.updateAssistantConfig(config.copy(muteMicBleepsAndSystemSounds = it))
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Color(0xFF10B981),
                                uncheckedTrackColor = PolishSurfaceBorder
                            ),
                            modifier = Modifier.testTag("switch_mute_mic_bleeps")
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // 2. Mute All App Sounds
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = s("كتم وتوقيف كافة أصوات التطبيق", "Mute All App Sounds & Audio"),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = PolishTextPrimary
                            )
                            Text(
                                text = if (muteAllAppSounds)
                                    s("تم كتم كافة الأصوات والردود الصوتية ليعمل التطبيق بصمت تام ✓", "All TTS speech and sounds muted for total silent operation ✓")
                                else
                                    s("أصوات التطبيق والردود الصوتية مفعلة", "App sounds and voice feedback are enabled"),
                                style = MaterialTheme.typography.bodySmall,
                                color = if (muteAllAppSounds) Color(0xFF10B981) else PolishTextSecondary,
                                fontSize = 11.sp
                            )
                        }
                        Switch(
                            checked = muteAllAppSounds,
                            onCheckedChange = {
                                muteAllAppSounds = it
                                viewModel.updateAssistantConfig(config.copy(muteAllAppSounds = it))
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Color(0xFF10B981),
                                uncheckedTrackColor = PolishSurfaceBorder
                            ),
                            modifier = Modifier.testTag("switch_mute_all_sounds")
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // 3. Continuous Open Microphone (No Repeated Open/Close)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = s("إبقاء المايك مفتوحاً باستمرار (دون فتح وإغلاق متكرر)", "Continuous Open Microphone (No cycling)"),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = PolishTextPrimary
                            )
                            Text(
                                text = s("استمرار فتح قناة المايك دائماً لالتقاط الأوامر بسلاسة بدون انقطاع", "Keeps audio hardware open continuously for seamless hands-free capture"),
                                style = MaterialTheme.typography.bodySmall,
                                color = if (keepMicOpenContinuously) Color(0xFF10B981) else PolishTextSecondary,
                                fontSize = 11.sp
                            )
                        }
                        Switch(
                            checked = keepMicOpenContinuously,
                            onCheckedChange = {
                                keepMicOpenContinuously = it
                                viewModel.updateAssistantConfig(config.copy(keepMicOpenContinuously = it))
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Color(0xFF10B981),
                                uncheckedTrackColor = PolishSurfaceBorder
                            ),
                            modifier = Modifier.testTag("switch_keep_mic_open")
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // 4. Hands-Free Auto-Listening Switch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = s("الاستماع التلقائي الدائم فور تشغيل التطبيق", "Hands-Free Auto-Listening on Open"),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = PolishTextPrimary
                            )
                            Text(
                                text = s("الهاتف يستمع وينفذ الأوامر مباشرة دون لمس أي زر", "Listens and executes commands immediately without touching"),
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
                }
            }
        }

        // 3. Background Execution & Services Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("background_service_card"),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = PolishSurfaceElevated),
                border = BorderStroke(1.dp, PolishSurfaceBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = s("خدمات الخلفية وتوفير الطاقة", "Background Services & Power"),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = PolishTextPrimary
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = s("تشغيل المساعد كخدمة دائمة في الخلفية (Foreground Service)", "Run in Background (Foreground Service)"),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = PolishTextPrimary
                            )
                            Text(
                                text = s("الاستماع وتنفيذ الأوامر حتى لو كان التطبيق مغلقاً أو الشاشة مقفلة", "Executes commands even when app is closed or screen is off"),
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

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = s("مراقبة الشبكة المحلية ومشاركات الشاشة", "Local LAN & Screen Stream Audit"),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = PolishTextPrimary
                            )
                            Text(
                                text = s("أرشفة كافة البيانات والمشاركات والأجهزة المتصلة على نفس شبكة Wi-Fi", "Monitor connected LAN nodes, media streams, and screen shares"),
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

        // 4. Biometric Voiceprint Security Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("voiceprint_security_card"),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = PolishSurfaceElevated),
                border = BorderStroke(1.dp, PolishSurfaceBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(PolishPrimaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Fingerprint,
                                contentDescription = null,
                                tint = PolishPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = s("بصمة الصوت البيومترية للمالك", "Owner Biometric Voiceprint"),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = PolishTextPrimary
                            )
                            Text(
                                text = s("حماية الهاتف وتنفيذ الأوامر فقط بصوت صاحب الهاتف", "Restricts executive actions strictly to your registered voice"),
                                style = MaterialTheme.typography.bodySmall,
                                color = PolishTextSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = s("التحقق الصوتي البيومتري الإجباري", "Enforce Biometric Voiceprint"),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = PolishTextPrimary
                            )
                            Text(
                                text = if (config.voiceprintEnrolled)
                                    s("البصمة مسجلة ونشطة (أمان 100%)", "Voiceprint enrolled & active")
                                else
                                    s("لم يتم تسجيل بصمة صوت المالك بعد", "No voiceprint enrolled"),
                                style = MaterialTheme.typography.bodySmall,
                                color = if (config.voiceprintEnrolled) Color(0xFF10B981) else Color(0xFFF59E0B),
                                fontSize = 11.sp
                            )
                        }
                        Switch(
                            checked = biometricVoiceprintEnabled,
                            onCheckedChange = {
                                biometricVoiceprintEnabled = it
                                viewModel.updateAssistantConfig(config.copy(biometricVoiceprintEnabled = it))
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = PolishPrimary,
                                uncheckedTrackColor = PolishSurfaceBorder
                            ),
                            modifier = Modifier.testTag("switch_biometric_voiceprint")
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    if (!isEnrolling) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { viewModel.startVoiceprintEnrollment() },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .testTag("btn_enroll_voiceprint"),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = PolishPrimary)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Mic,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (config.voiceprintEnrolled) s("إعادة تسجيل البصمة", "Re-enroll Voiceprint") else s("تسجيل بصمة صوت جديدة", "Enroll Voiceprint"),
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }

                            if (config.voiceprintEnrolled) {
                                OutlinedButton(
                                    onClick = { viewModel.resetVoiceprintProfile() },
                                    shape = RoundedCornerShape(14.dp),
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
                                    text = s("خطوة تسجيل البصمة (${enrollmentStep + 1} من 3)", "Enrollment Step (${enrollmentStep + 1} of 3)"),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PolishPrimary
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = s("انقر على زر التسجيل وتحدث بالعبارة التالية بوضوح:", "Tap record and speak the phrase clearly:"),
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
                                        Text(s("تسجيل العينة ${enrollmentStep + 1}", "Record Sample ${enrollmentStep + 1}"), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }
                                    OutlinedButton(
                                        onClick = { viewModel.cancelVoiceprintEnrollment() },
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.height(42.dp)
                                    ) {
                                        Text(s("إلغاء", "Cancel"), fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 5. AI Persona & Identity Card
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
                        text = s("تخصيص هوية المساعد واللهجة", "Assistant Persona & Identity"),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = PolishTextPrimary
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = s("اسم المساعد (نداء التنبيه):", "Assistant Name:"),
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
                            .testTag("input_assistant_name"),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PolishPrimary,
                            unfocusedBorderColor = PolishSurfaceBorder,
                            focusedTextColor = PolishTextPrimary,
                            unfocusedTextColor = PolishTextPrimary
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = s("اللهجة المفضلة للاستجابة:", "Preferred Dialect:"),
                        style = MaterialTheme.typography.labelSmall,
                        color = PolishTextSecondary,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Box(modifier = Modifier.fillMaxWidth()) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = PolishSurface,
                            border = BorderStroke(1.dp, PolishSurfaceBorder),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { dialectDropdownExpanded = true }
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = selectedDialect,
                                    fontSize = 13.sp,
                                    color = PolishTextPrimary
                                )
                                Text(text = "▼", fontSize = 10.sp, color = PolishTextSecondary)
                            }
                        }

                        DropdownMenu(
                            expanded = dialectDropdownExpanded,
                            onDismissRequest = { dialectDropdownExpanded = false },
                            modifier = Modifier.background(PolishSurfaceElevated)
                        ) {
                            dialectOptions.forEach { opt ->
                                DropdownMenuItem(
                                    text = { Text(opt, color = PolishTextPrimary, fontSize = 13.sp) },
                                    onClick = {
                                        selectedDialect = opt
                                        dialectDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = s("نداء التنبيه المخصص الإضافي (كلمة الاستيقاظ):", "Additional Custom Wake-Word:"),
                        style = MaterialTheme.typography.labelSmall,
                        color = PolishTextSecondary,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = customWakeWordInput,
                        onValueChange = { customWakeWordInput = it },
                        placeholder = { Text(s("مثال: يا أورا، يا ذكاء، رفيقي", "e.g. Aura, Hey Assistant"), fontSize = 12.sp, color = PolishTextMuted) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_custom_wake_word"),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PolishPrimary,
                            unfocusedBorderColor = PolishSurfaceBorder,
                            focusedTextColor = PolishTextPrimary,
                            unfocusedTextColor = PolishTextPrimary
                        )
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = s("الاستيقاظ والتنفيذ فقط عند النداء بالاسم", "Wake-Word Only Mode"),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = PolishTextPrimary
                            )
                            Text(
                                text = s("يتجاهل أي حديث عام حتى تناديه باسمه (مثل: يا ${assistantNameInput.ifBlank { "أورا" }})", "Ignores background talk until called by name"),
                                style = MaterialTheme.typography.bodySmall,
                                color = PolishTextSecondary,
                                fontSize = 11.sp
                            )
                        }
                        Switch(
                            checked = wakeWordOnlyMode,
                            onCheckedChange = {
                                wakeWordOnlyMode = it
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Color(0xFF10B981),
                                uncheckedTrackColor = PolishSurfaceBorder
                            ),
                            modifier = Modifier.testTag("switch_wake_word_only")
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Safe Audio Source Assurance
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF064E3B).copy(alpha = 0.3f),
                        border = BorderStroke(1.dp, Color(0xFF059669).copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                tint = Color(0xFF34D399),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = s(
                                    "🔒 حماية وأمان فائق: المساعد محمي تماماً من استماع الفيديوهات المسجلة أو المشغلة على الهاتف لضمان عدم تنفيذ أوامر غير مقصودة.",
                                    "🔒 Verified Audio Isolation: Ignores internal media/video playback to prevent unintended execution."
                                ),
                                fontSize = 10.sp,
                                color = Color(0xFFA7F3D0),
                                lineHeight = 14.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = {
                            viewModel.updateAssistantConfig(
                                config.copy(
                                    assistantName = assistantNameInput.ifBlank { "AURA" },
                                    customWakeWord = customWakeWordInput.trim(),
                                    wakeWordOnlyMode = wakeWordOnlyMode,
                                    preferredDialect = selectedDialect
                                )
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .testTag("btn_save_persona"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PolishPrimary)
                    ) {
                        Text(s("حفظ التغييرات", "Save Identity"), fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}
