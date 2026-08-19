package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.entity.ActionType
import com.example.ui.ConversationMessage
import com.example.ui.MainViewModel
import com.example.ui.components.AutonomousExecutionVisualizer
import com.example.ui.components.NeuralOrbVisualizer
import com.example.ui.theme.PolishBackground
import com.example.ui.theme.PolishGlow
import com.example.ui.theme.PolishOnPrimaryContainer
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
import com.example.util.LocalizationManager

@Composable
fun VoiceAssistantScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val isListening by viewModel.voiceEngine.isListening.collectAsStateWithLifecycle()
    val isSpeaking by viewModel.voiceEngine.isSpeaking.collectAsStateWithLifecycle()
    val audioLevel by viewModel.voiceEngine.rmsAudioLevel.collectAsStateWithLifecycle()
    val liveRecognizedText by viewModel.voiceEngine.lastRecognizedText.collectAsStateWithLifecycle()
    val isProcessing by viewModel.isProcessingAi.collectAsStateWithLifecycle()
    val config by viewModel.assistantConfig.collectAsStateWithLifecycle()
    val conversation by viewModel.conversation.collectAsStateWithLifecycle()
    val isBackgroundActive by viewModel.isBackgroundServiceActive.collectAsStateWithLifecycle()
    val appLang by viewModel.appLanguage.collectAsStateWithLifecycle()
    val isUsingFallback by viewModel.voiceEngine.isUsingFallbackAcousticEngine.collectAsStateWithLifecycle()
    val activeExecutionPlan by viewModel.activeExecutionPlan.collectAsStateWithLifecycle()
    val isAccessibilityConnected by viewModel.isAccessibilityConnected.collectAsStateWithLifecycle()

    val isAr = LocalizationManager.getEffectiveLanguage(appLang) == "ar"
    fun s(ar: String, en: String): String = if (isAr) ar else en

    var textInput by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current
    val listState = rememberLazyListState()

    LaunchedEffect(conversation.size) {
        if (conversation.isNotEmpty()) {
            listState.animateScrollToItem(conversation.size - 1)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(PolishBackground)
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(10.dp))

        // 1. Background Execution Card
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isBackgroundActive) Color(0xFF10B981).copy(alpha = 0.12f) else PolishSurfaceElevated
            ),
            border = BorderStroke(
                1.dp,
                if (isBackgroundActive) Color(0xFF10B981).copy(alpha = 0.5f) else PolishSurfaceBorder
            ),
            modifier = Modifier.fillMaxWidth().testTag("background_service_banner")
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(if (isBackgroundActive) Color(0xFF10B981) else Color(0xFFEF4444))
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = if (isBackgroundActive) s("العمل في الخلفية نشط 🟢", "Background Execution Active 🟢") else s("العمل في الخلفية متوقف", "Background Service Paused"),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isBackgroundActive) Color(0xFF10B981) else PolishTextPrimary,
                            fontSize = 12.sp
                        )
                        Text(
                            text = if (isBackgroundActive) s("يستمع وينفذ حتى لو أغلقت الشاشة أو التطبيق", "Listens and acts even when locked or app closed") else s("اضغط لتشغيل الاستماع أثناء قفل الشاشة", "Tap to keep active in background"),
                            style = MaterialTheme.typography.bodySmall,
                            color = PolishTextSecondary,
                            fontSize = 10.sp
                        )
                    }
                }

                Button(
                    onClick = { viewModel.toggleBackgroundService(context) },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isBackgroundActive) Color(0xFFEF4444) else Color(0xFF10B981)
                    ),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.height(32.dp).testTag("btn_toggle_background_service")
                ) {
                    Text(
                        text = if (isBackgroundActive) s("إيقاف", "Stop") else s("تشغيل في الخلفية", "Start in BG"),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 2. Hands-Free Banner, Sound Status & Biometric Voiceprint Status
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = if (isListening) Color(0xFF10B981).copy(alpha = 0.15f) else PolishSurfaceElevated,
                border = BorderStroke(1.dp, if (isListening) Color(0xFF10B981) else PolishSurfaceBorder)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(9.dp)
                            .clip(CircleShape)
                            .background(if (isListening) Color(0xFF10B981) else Color(0xFF94A3B8))
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isListening) {
                            if (isUsingFallback) s("استماع مدمج 🎙️", "In-App Acoustic 🎙️")
                            else s("مايك مستمر 🟢", "Continuous Mic 🟢")
                        } else s("متوقف", "Paused"),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isListening) Color(0xFF10B981) else PolishTextPrimary,
                        fontSize = 11.sp
                    )
                }
            }

            // Quick Mute Status Pill
            Surface(
                onClick = {
                    viewModel.updateAssistantConfig(config.copy(muteAllAppSounds = !config.muteAllAppSounds))
                },
                shape = RoundedCornerShape(20.dp),
                color = if (config.muteAllAppSounds) Color(0xFFEF4444).copy(alpha = 0.15f) else PolishSurfaceElevated,
                border = BorderStroke(1.dp, if (config.muteAllAppSounds) Color(0xFFEF4444).copy(alpha = 0.5f) else PolishSurfaceBorder),
                modifier = Modifier.testTag("pill_quick_mute_toggle")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (config.muteAllAppSounds) Icons.Default.VolumeOff else Icons.Default.Settings,
                        contentDescription = "Sound Status",
                        tint = if (config.muteAllAppSounds) Color(0xFFEF4444) else PolishTextSecondary,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (config.muteAllAppSounds) s("أصوات مكتومة 🔇", "Sounds Muted 🔇") else s("صوت مفعّل 🔊", "Sound Active 🔊"),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (config.muteAllAppSounds) Color(0xFFEF4444) else PolishTextSecondary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                }
            }

            // Accessibility Automation Quick Status Pill
            Surface(
                onClick = {
                    if (!isAccessibilityConnected) {
                        viewModel.openAccessibilitySettings(context)
                    } else {
                        viewModel.toggleAutonomousUiInteractions()
                    }
                },
                shape = RoundedCornerShape(20.dp),
                color = if (isAccessibilityConnected && config.autonomousUiInteractions) Color(0xFF10B981).copy(alpha = 0.15f) else PolishSurfaceElevated,
                border = BorderStroke(1.dp, if (isAccessibilityConnected && config.autonomousUiInteractions) Color(0xFF10B981).copy(alpha = 0.5f) else PolishSurfaceBorder),
                modifier = Modifier.testTag("pill_accessibility_status")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.TouchApp,
                        contentDescription = "Autonomous Access",
                        tint = if (isAccessibilityConnected && config.autonomousUiInteractions) Color(0xFF10B981) else PolishTextSecondary,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isAccessibilityConnected && config.autonomousUiInteractions) s("تحكم ذاتي 🤖", "Auto UI 🤖") else s("تحكم يدوي", "Manual UI"),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isAccessibilityConnected && config.autonomousUiInteractions) Color(0xFF10B981) else PolishTextSecondary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(20.dp),
                color = PolishSecondaryContainer,
                border = BorderStroke(1.dp, PolishSurfaceBorder)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Fingerprint,
                        contentDescription = "Voiceprint",
                        tint = PolishPrimary,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (config.voiceprintEnrolled) s("بصمة معتمدة ✓", "Verified ✓") else s("بصمة عامة", "Open Voice"),
                        style = MaterialTheme.typography.labelSmall,
                        color = PolishPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 2. Central Neural Wave Orb
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            NeuralOrbVisualizer(
                isListening = isListening,
                isSpeaking = isSpeaking,
                isProcessing = isProcessing,
                audioLevel = audioLevel,
                onClick = { viewModel.toggleVoiceListening() },
                size = 135.dp
            )
        }

        Text(
            text = when {
                isListening -> s("تحدث بأي أمر بصوتك وسينفذه الهاتف فوراً بالنيابة عنك", "Speak any command and the phone will execute it hands-free")
                isProcessing -> s("جاري تحليل الأمر والتحكم بالهاتف بصمت...", "Processing command and executing...")
                else -> s("اضغط لإعادة تفعيل وضع الاستماع الدائم", "Tap to activate hands-free listening")
            },
            style = MaterialTheme.typography.bodySmall,
            color = if (isListening) PolishPrimary else PolishTextSecondary,
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp
        )

        if (liveRecognizedText.isNotBlank()) {
            Spacer(modifier = Modifier.height(4.dp))
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = PolishSurfaceElevated,
                border = BorderStroke(1.dp, PolishSurfaceBorder)
            ) {
                Text(
                    text = "« $liveRecognizedText »",
                    style = MaterialTheme.typography.bodyMedium,
                    color = PolishPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 3. Autonomous Quick Phone Action Shortcuts
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            QuickPhoneActionButton(
                icon = Icons.Default.Call,
                label = s("اتصال هاتف", "Call"),
                onClick = { viewModel.executeAction(ActionType.CALL_CONTACT, "0000000") }
            )
            QuickPhoneActionButton(
                icon = Icons.Default.CallEnd,
                label = s("إنهاء المكالمة", "End Call"),
                onClick = { viewModel.executeEndCall() }
            )
            QuickPhoneActionButton(
                icon = Icons.Default.Message,
                label = s("رسائل SMS", "SMS"),
                onClick = { viewModel.executeAction(ActionType.SEND_MESSAGE) }
            )
            QuickPhoneActionButton(
                icon = Icons.Default.Home,
                label = s("الرئيسية / إغلاق", "Home / Exit"),
                onClick = { viewModel.executeReturnHome() }
            )
            QuickPhoneActionButton(
                icon = Icons.Default.Email,
                label = s("ايميل", "Email"),
                onClick = { viewModel.executeAction(ActionType.SEND_EMAIL) }
            )
            QuickPhoneActionButton(
                icon = Icons.Default.Security,
                label = s("فحص الأمان", "Security Scan"),
                onClick = {
                    viewModel.executeAction(ActionType.SYSTEM_SECURITY_SCAN)
                    viewModel.startFullSecurityScan()
                }
            )
            QuickPhoneActionButton(
                icon = Icons.Default.PhotoLibrary,
                label = s("الصور / المعرض", "Gallery"),
                onClick = { viewModel.executeAction(ActionType.OPEN_GALLERY) }
            )
            QuickPhoneActionButton(
                icon = Icons.Default.Calculate,
                label = s("آلة حاسبة", "Calculator"),
                onClick = { viewModel.executeAction(ActionType.OPEN_CALCULATOR) }
            )
            QuickPhoneActionButton(
                icon = Icons.Default.Map,
                label = s("الخرائط", "Maps"),
                onClick = { viewModel.executeAction(ActionType.OPEN_MAPS) }
            )
            QuickPhoneActionButton(
                icon = Icons.Default.FlashlightOn,
                label = s("الكشاف", "Flashlight"),
                onClick = { viewModel.executeAction(ActionType.TOGGLE_FLASHLIGHT) }
            )
            QuickPhoneActionButton(
                icon = Icons.Default.CameraAlt,
                label = s("الكاميرا", "Camera"),
                onClick = { viewModel.executeAction(ActionType.OPEN_CAMERA) }
            )
            QuickPhoneActionButton(
                icon = Icons.Default.VolumeOff,
                label = s("الصامت", "Silent"),
                onClick = { viewModel.executeAction(ActionType.TOGGLE_SILENT_MODE) }
            )
            QuickPhoneActionButton(
                icon = Icons.Default.Wifi,
                label = s("Wi-Fi", "Wi-Fi"),
                onClick = { viewModel.executeAction(ActionType.OPEN_WIFI_SETTINGS) }
            )
            QuickPhoneActionButton(
                icon = Icons.Default.Settings,
                label = s("الإعدادات", "Settings"),
                onClick = { viewModel.executeAction(ActionType.OPEN_SETTINGS) }
            )
        }

        // Live Autonomous Phone Execution Visualizer
        activeExecutionPlan?.let { plan ->
            Spacer(modifier = Modifier.height(6.dp))
            AutonomousExecutionVisualizer(
                plan = plan,
                isArabic = isAr,
                onDismiss = { viewModel.dismissExecutionPlan() },
                onReturnHome = { viewModel.executeReturnHome() },
                onEndCall = { viewModel.executeEndCall() },
                onReplay = { viewModel.executeAction(plan.actionType, plan.actionPayload) }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 4. Conversation & Autonomous Action Feeds
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(conversation, key = { it.id }) { msg ->
                ConversationBubble(
                    message = msg,
                    isAr = isAr,
                    onExecuteAgain = { action, payload ->
                        viewModel.executeAction(action, payload)
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // 5. Input Text Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = textInput,
                onValueChange = { textInput = it },
                placeholder = {
                    Text(
                        text = s("اكتب أو تحدث مباشرة بدون لمس...", "Type or speak hands-free..."),
                        style = MaterialTheme.typography.bodySmall,
                        color = PolishTextMuted,
                        fontSize = 12.sp
                    )
                },
                modifier = Modifier
                    .weight(1f)
                    .testTag("voice_assistant_text_input"),
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = PolishSurfaceElevated,
                    unfocusedContainerColor = PolishSurfaceElevated,
                    focusedBorderColor = PolishPrimary,
                    unfocusedBorderColor = PolishSurfaceBorder,
                    focusedTextColor = PolishTextPrimary,
                    unfocusedTextColor = PolishTextPrimary
                ),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = {
                    if (textInput.isNotBlank()) {
                        viewModel.handleUserVoiceInput(textInput)
                        textInput = ""
                        focusManager.clearFocus()
                    }
                })
            )

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick = { viewModel.toggleVoiceListening() },
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(if (isListening) Color(0xFF10B981) else PolishPrimary)
                    .testTag("mic_toggle_button")
            ) {
                Icon(
                    imageVector = if (isListening) Icons.Default.Mic else Icons.Default.MicOff,
                    contentDescription = "استماع صوتي",
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }

            if (textInput.isNotBlank()) {
                Spacer(modifier = Modifier.width(6.dp))
                IconButton(
                    onClick = {
                        viewModel.handleUserVoiceInput(textInput)
                        textInput = ""
                        focusManager.clearFocus()
                    },
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(PolishPrimary)
                        .testTag("send_query_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "إرسال",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun QuickPhoneActionButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = PolishSurfaceElevated,
        border = BorderStroke(1.dp, PolishSurfaceBorder),
        modifier = Modifier.testTag("quick_action_$label")
    ) {
        Button(
            onClick = onClick,
            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = PolishPrimary,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = PolishTextPrimary,
                fontWeight = FontWeight.SemiBold,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
fun ConversationBubble(
    message: ConversationMessage,
    isAr: Boolean,
    onExecuteAgain: (ActionType, String?) -> Unit
) {
    val isUser = message.isUser
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.Start else Arrangement.End
    ) {
        Card(
            shape = RoundedCornerShape(
                topStart = 18.dp,
                topEnd = 18.dp,
                bottomStart = if (isUser) 4.dp else 18.dp,
                bottomEnd = if (isUser) 18.dp else 4.dp
            ),
            colors = CardDefaults.cardColors(
                containerColor = if (isUser) PolishPrimaryContainer else PolishSurfaceElevated
            ),
            border = BorderStroke(
                1.dp,
                if (isUser) PolishGlow else PolishSurfaceBorder
            ),
            modifier = Modifier.fillMaxWidth(0.92f)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isUser) (if (isAr) "أنت (صوت المالك)" else "You (Owner Voice)") else (if (isAr) "التحكم الذاتي" else "Autonomous Agent"),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isUser) PolishOnPrimaryContainer else PolishPrimary,
                        fontSize = 11.sp
                    )
                    if (isUser && message.biometricVerified) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFF10B981).copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = if (isAr) "بصمة موثقة (${message.biometricConfidence}%) ✓" else "Voiceprint verified (${message.biometricConfidence}%) ✓",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF10B981),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = message.text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isUser) PolishOnPrimaryContainer else PolishTextPrimary,
                    fontSize = 13.sp
                )
                if (message.actionType != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = PolishSuccessContainer,
                        border = BorderStroke(1.dp, PolishSuccess.copy(alpha = 0.3f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(PolishSuccess)
                                    )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isAr) "تم تنفيذ الإجراء: ${message.actionType.name}" else "Action executed: ${message.actionType.name}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = PolishSuccess,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 10.sp
                                )
                            }
                            Button(
                                onClick = { onExecuteAgain(message.actionType, message.actionPayload) },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = PolishPrimary),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                                    horizontal = 8.dp,
                                    vertical = 2.dp
                                ),
                                modifier = Modifier.height(26.dp)
                            ) {
                                Text(if (isAr) "إعادة" else "Retry", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}
