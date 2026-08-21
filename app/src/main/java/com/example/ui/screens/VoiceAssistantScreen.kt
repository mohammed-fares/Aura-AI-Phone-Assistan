package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.Hearing
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
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
import com.example.ui.components.PendingCommandReviewCard
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

enum class ActionCategory(val titleAr: String, val titleEn: String) {
    COMMUNICATION("اتصال ورسائل 📞", "Comms & Calls 📞"),
    SCREEN_AI("رؤية الشاشة وتحليلها 👁️", "Screen Perception 👁️"),
    SYNONYM_LEARNING("تعلم اللهجات والمرادفات 🧠", "Dialect Learning 🧠"),
    SYSTEM_APPS("تطبيقات ونظام 📱", "Apps & System 📱"),
    PHONE_TOOLS("أدوات وتحكم ⚙️", "Tools & Control ⚙️")
}

@Composable
fun VoiceAssistantScreen(
    viewModel: MainViewModel,
    onRequestPermissions: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
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
    val pendingCommandReview by viewModel.pendingCommandReview.collectAsStateWithLifecycle()
    val requireReviewBeforeExecution by viewModel.requireReviewBeforeExecution.collectAsStateWithLifecycle()

    val isAr = LocalizationManager.getEffectiveLanguage(appLang) == "ar"
    fun s(ar: String, en: String): String = if (isAr) ar else en

    var selectedCategory by remember { mutableStateOf(ActionCategory.COMMUNICATION) }
    var textInput by remember { mutableStateOf("") }
    var showAddSynonymDialog by remember { mutableStateOf(false) }
    var customSynonymWord by remember { mutableStateOf("") }
    var customSynonymCanonical by remember { mutableStateOf("") }
    val learnedSynonymsList by viewModel.learnedSynonyms.collectAsStateWithLifecycle()
    val focusManager = LocalFocusManager.current
    val listState = rememberLazyListState()

    val infiniteTransition = rememberInfiniteTransition(label = "pulse_trans")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    LaunchedEffect(conversation.size) {
        if (conversation.isNotEmpty()) {
            listState.animateScrollToItem(conversation.size - 1)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(PolishBackground)
            .padding(horizontal = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(10.dp))

        // ==========================================
        // 1. UNIFIED SYSTEM HEALTH & STATUS BANNER
        // ==========================================
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isBackgroundActive) Color(0xFF10B981).copy(alpha = 0.10f) else PolishSurfaceElevated
            ),
            border = BorderStroke(
                1.dp,
                if (isBackgroundActive) Color(0xFF10B981).copy(alpha = 0.45f) else PolishSurfaceBorder
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("voice_system_header_card")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                // Header Top Row: Status Title & Background Toggle
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
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isBackgroundActive && isListening) Color(0xFF10B981).copy(alpha = pulseAlpha)
                                    else if (isBackgroundActive) Color(0xFF10B981)
                                    else Color(0xFFF59E0B)
                                )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = if (isBackgroundActive) {
                                    s("المساعد نشط ويعمل بالخلفية 🟢", "AI Assistant: Active & Listening 🟢")
                                } else {
                                    s("وضع الاستماع المحلي فقط 🟡", "In-App Listening Only 🟡")
                                },
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (isBackgroundActive) Color(0xFF10B981) else PolishTextPrimary,
                                fontSize = 12.sp
                            )
                            Text(
                                text = if (isBackgroundActive) {
                                    s("يستمع وينفذ الأوامر دون لمس الشاشة 24/7", "Listens & executes hands-free 24/7")
                                } else {
                                    s("اضغط لتشغيل الاستماع الدائم بالخلفية", "Tap to enable 24/7 persistent background")
                                },
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
                        modifier = Modifier
                            .height(30.dp)
                            .testTag("btn_toggle_background_service")
                    ) {
                        Text(
                            text = if (isBackgroundActive) s("إيقاف الخدمة", "Stop Service") else s("تشغيل دائم", "Enable 24/7"),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Header Bottom Pills: Mic, Auto-UI, Mute, Voiceprint
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Continuous Mic Status Pill
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (isListening) Color(0xFF10B981).copy(alpha = 0.15f) else PolishSurfaceElevated,
                        border = BorderStroke(1.dp, if (isListening) Color(0xFF10B981).copy(alpha = 0.4f) else PolishSurfaceBorder)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(if (isListening) Color(0xFF10B981) else Color(0xFF94A3B8))
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isListening) s("مايك مستمر 🎙️", "Continuous Mic 🎙️") else s("المايك متوقف", "Mic Idle"),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isListening) Color(0xFF10B981) else PolishTextSecondary
                            )
                        }
                    }

                    // Permissions & Access Center Pill
                    Surface(
                        onClick = { onRequestPermissions() },
                        shape = RoundedCornerShape(16.dp),
                        color = PolishPrimaryContainer.copy(alpha = 0.25f),
                        border = BorderStroke(1.dp, PolishPrimary.copy(alpha = 0.5f)),
                        modifier = Modifier.testTag("pill_grant_permissions_shortcut")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = null,
                                tint = PolishPrimary,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = s("صلاحيات الهاتف 🛡️", "Permissions 🛡️"),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = PolishPrimary
                            )
                        }
                    }

                    // Autonomous UI Automation Pill
                    Surface(
                        onClick = {
                            if (!isAccessibilityConnected) {
                                viewModel.openAccessibilitySettings(context)
                            } else {
                                viewModel.toggleAutonomousUiInteractions()
                            }
                        },
                        shape = RoundedCornerShape(16.dp),
                        color = if (isAccessibilityConnected && config.autonomousUiInteractions) Color(0xFF10B981).copy(alpha = 0.15f) else PolishSurfaceElevated,
                        border = BorderStroke(1.dp, if (isAccessibilityConnected && config.autonomousUiInteractions) Color(0xFF10B981).copy(alpha = 0.4f) else PolishSurfaceBorder),
                        modifier = Modifier.testTag("pill_accessibility_status")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.TouchApp,
                                contentDescription = null,
                                tint = if (isAccessibilityConnected && config.autonomousUiInteractions) Color(0xFF10B981) else PolishTextSecondary,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isAccessibilityConnected && config.autonomousUiInteractions) s("تحكم ذاتي 🤖", "Auto-UI 🤖") else s("تحكم يدوي", "Manual UI"),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isAccessibilityConnected && config.autonomousUiInteractions) Color(0xFF10B981) else PolishTextSecondary
                            )
                        }
                    }

                    // Audio Feedback Mute Pill
                    Surface(
                        onClick = {
                            viewModel.updateAssistantConfig(config.copy(muteAllAppSounds = !config.muteAllAppSounds))
                        },
                        shape = RoundedCornerShape(16.dp),
                        color = if (config.muteAllAppSounds) Color(0xFFEF4444).copy(alpha = 0.15f) else PolishSurfaceElevated,
                        border = BorderStroke(1.dp, if (config.muteAllAppSounds) Color(0xFFEF4444).copy(alpha = 0.4f) else PolishSurfaceBorder),
                        modifier = Modifier.testTag("pill_quick_mute_toggle")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (config.muteAllAppSounds) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                                contentDescription = null,
                                tint = if (config.muteAllAppSounds) Color(0xFFEF4444) else PolishTextSecondary,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (config.muteAllAppSounds) s("كتم الرد 🔇", "Muted 🔇") else s("صوت ناطق 🔊", "Speech On 🔊"),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (config.muteAllAppSounds) Color(0xFFEF4444) else PolishTextSecondary
                            )
                        }
                    }

                    // Intent Review Mode Pill (Human-in-the-loop)
                    Surface(
                        onClick = { viewModel.toggleRequireReviewBeforeExecution() },
                        shape = RoundedCornerShape(16.dp),
                        color = if (requireReviewBeforeExecution) PolishPrimary.copy(alpha = 0.15f) else PolishSurfaceElevated,
                        border = BorderStroke(1.dp, if (requireReviewBeforeExecution) PolishPrimary.copy(alpha = 0.4f) else PolishSurfaceBorder),
                        modifier = Modifier.testTag("pill_intent_review_mode_toggle")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = null,
                                tint = if (requireReviewBeforeExecution) PolishPrimary else PolishTextSecondary,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (requireReviewBeforeExecution) s("مراجعة الأوامر 📝", "Review Mode 📝") else s("تنفيذ فوري ⚡", "Instant Mode ⚡"),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (requireReviewBeforeExecution) PolishPrimary else PolishTextSecondary
                            )
                        }
                    }

                    // Voiceprint Verification Pill
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = PolishSecondaryContainer,
                        border = BorderStroke(1.dp, PolishSurfaceBorder)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Fingerprint,
                                contentDescription = null,
                                tint = PolishPrimary,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (config.voiceprintEnrolled) s("بصمة المالك ✓", "Owner Voice ✓") else s("بصمة عامة", "Open Voice"),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = PolishPrimary
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // ==========================================
        // 2. CENTRAL NEURAL ORB & INTERACTIVE VOICE HUB
        // ==========================================
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 2.dp),
            contentAlignment = Alignment.Center
        ) {
            NeuralOrbVisualizer(
                isListening = isListening,
                isSpeaking = isSpeaking,
                isProcessing = isProcessing,
                audioLevel = audioLevel,
                onClick = { viewModel.toggleVoiceListening() },
                size = 125.dp
            )
        }

        Text(
            text = when {
                isListening -> s("تحدث بأي أمر وسينفذه الهاتف تلقائياً بالنيابة عنك", "Speak any command and your phone executes it hands-free")
                isProcessing -> s("جاري تحليل الأمر والتنفيذ الذاتي في الهاتف...", "Analyzing command and executing autonomously...")
                else -> s("اضغط على الهالة لتفعيل وضع الاستماع الدائم", "Tap the orb to start continuous listening")
            },
            style = MaterialTheme.typography.bodySmall,
            color = if (isListening) PolishPrimary else PolishTextSecondary,
            fontWeight = FontWeight.Medium,
            fontSize = 11.sp
        )

        // Live recognized speech badge
        AnimatedVisibility(visible = liveRecognizedText.isNotBlank()) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = PolishSurfaceElevated,
                border = BorderStroke(1.dp, PolishPrimary.copy(alpha = 0.5f)),
                modifier = Modifier.padding(top = 4.dp)
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

        // ==========================================
        // 3. CATEGORIZED QUICK ACTION PALETTE
        // ==========================================
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            ActionCategory.values().forEach { cat ->
                val isSelected = selectedCategory == cat
                Surface(
                    onClick = { selectedCategory = cat },
                    shape = RoundedCornerShape(14.dp),
                    color = if (isSelected) PolishPrimary else PolishSurfaceElevated,
                    border = BorderStroke(1.dp, if (isSelected) PolishPrimary else PolishSurfaceBorder)
                ) {
                    Text(
                        text = if (isAr) cat.titleAr else cat.titleEn,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) Color.White else PolishTextSecondary,
                        fontSize = 10.sp,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Category Action Chips Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            when (selectedCategory) {
                ActionCategory.COMMUNICATION -> {
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
                        icon = Icons.Default.Email,
                        label = s("إيميل", "Email"),
                        onClick = { viewModel.executeAction(ActionType.SEND_EMAIL) }
                    )
                }
                ActionCategory.SCREEN_AI -> {
                    QuickPhoneActionButton(
                        icon = Icons.Default.Visibility,
                        label = s("قراءة الشاشة", "Read Screen"),
                        onClick = { viewModel.readScreenNow() }
                    )
                    QuickPhoneActionButton(
                        icon = Icons.Default.AutoAwesome,
                        label = s("تلخيص الشاشة AI", "Summarize AI"),
                        onClick = { viewModel.summarizeScreenNow() }
                    )
                    QuickPhoneActionButton(
                        icon = Icons.Default.Keyboard,
                        label = s("كتابة بالشاشة", "Type Text"),
                        onClick = { viewModel.typeTextOnScreen("تمت المعالجة الذاتية") }
                    )
                    QuickPhoneActionButton(
                        icon = Icons.Default.CameraAlt,
                        label = s("لقطة شاشة", "Screenshot"),
                        onClick = { viewModel.executeAction(ActionType.TAKE_SCREENSHOT) }
                    )
                    QuickPhoneActionButton(
                        icon = Icons.Default.Message,
                        label = s("تعليق بالشاشة", "Screen Comment"),
                        onClick = { viewModel.executeAction(ActionType.COMMENT_ON_SCREEN) }
                    )
                }
                ActionCategory.SYNONYM_LEARNING -> {
                    QuickPhoneActionButton(
                        icon = Icons.Default.Add,
                        label = s("➕ علم كلمة جديدة", "➕ Teach Word"),
                        onClick = { showAddSynonymDialog = true }
                    )
                    QuickPhoneActionButton(
                        icon = Icons.Default.Psychology,
                        label = s("علم: اطفي = اغلق", "Teach: اطفي"),
                        onClick = { viewModel.learnNewSynonym("اطفي", "اغلق", ActionType.CLOSE_APP) }
                    )
                    QuickPhoneActionButton(
                        icon = Icons.Default.School,
                        label = s("علم: سكر = اغلق", "Teach: سكر"),
                        onClick = { viewModel.learnNewSynonym("سكر", "اغلق", ActionType.CLOSE_APP) }
                    )
                    QuickPhoneActionButton(
                        icon = Icons.Default.Psychology,
                        label = s("علم: بند = اغلق", "Teach: بند"),
                        onClick = { viewModel.learnNewSynonym("بند", "اغلق", ActionType.CLOSE_APP) }
                    )
                    QuickPhoneActionButton(
                        icon = Icons.Default.CallEnd,
                        label = s("علم: الغي = إنهاء", "Teach: الغي"),
                        onClick = { viewModel.learnNewSynonym("الغي", "إنهاء", ActionType.END_CALL) }
                    )
                    QuickPhoneActionButton(
                        icon = Icons.Default.Psychology,
                        label = s("علم: فركش = اغلق", "Teach: فركش"),
                        onClick = { viewModel.learnNewSynonym("فركش", "اغلق", ActionType.CLOSE_APP) }
                    )
                    QuickPhoneActionButton(
                        icon = Icons.Default.FlashlightOn,
                        label = s("علم: ولّع = شغل", "Teach: ولع"),
                        onClick = { viewModel.learnNewSynonym("ولع", "شغل", ActionType.TOGGLE_FLASHLIGHT) }
                    )
                }
                ActionCategory.SYSTEM_APPS -> {
                    QuickPhoneActionButton(
                        icon = Icons.Default.Share,
                        label = s("بوست فيسبوك", "Post FB"),
                        onClick = { viewModel.executeAction(ActionType.POST_FACEBOOK) }
                    )
                    QuickPhoneActionButton(
                        icon = Icons.Default.Message,
                        label = s("تعليق بالشاشة", "Comment"),
                        onClick = { viewModel.executeAction(ActionType.COMMENT_ON_SCREEN) }
                    )
                    QuickPhoneActionButton(
                        icon = Icons.Default.Home,
                        label = s("الرئيسية", "Home"),
                        onClick = { viewModel.executeReturnHome() }
                    )
                    QuickPhoneActionButton(
                        icon = Icons.Default.ArrowBack,
                        label = s("رجوع", "Back"),
                        onClick = { viewModel.executeAction(ActionType.GLOBAL_BACK) }
                    )
                    QuickPhoneActionButton(
                        icon = Icons.Default.Close,
                        label = s("إغلاق التطبيق", "Close App"),
                        onClick = { viewModel.executeAction(ActionType.CLOSE_APP) }
                    )
                }
                ActionCategory.PHONE_TOOLS -> {
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
                        icon = Icons.Default.PhotoLibrary,
                        label = s("المعرض", "Gallery"),
                        onClick = { viewModel.executeAction(ActionType.OPEN_GALLERY) }
                    )
                    QuickPhoneActionButton(
                        icon = Icons.Default.Calculate,
                        label = s("الحاسبة", "Calc"),
                        onClick = { viewModel.executeAction(ActionType.OPEN_CALCULATOR) }
                    )
                    QuickPhoneActionButton(
                        icon = Icons.Default.Map,
                        label = s("الخرائط", "Maps"),
                        onClick = { viewModel.executeAction(ActionType.OPEN_MAPS) }
                    )
                    QuickPhoneActionButton(
                        icon = Icons.Default.Wifi,
                        label = s("واي فاي", "Wi-Fi"),
                        onClick = { viewModel.executeAction(ActionType.OPEN_WIFI_SETTINGS) }
                    )
                    QuickPhoneActionButton(
                        icon = Icons.Default.Security,
                        label = s("فحص الأمان", "Security Scan"),
                        onClick = {
                            viewModel.executeAction(ActionType.SYSTEM_SECURITY_SCAN)
                            viewModel.startFullSecurityScan()
                        }
                    )
                }
            }
        }

        // Human-in-the-loop Pending Command Review & Verification Card
        pendingCommandReview?.let { review ->
            Spacer(modifier = Modifier.height(6.dp))
            PendingCommandReviewCard(
                review = review,
                isArabic = isAr,
                onPayloadChange = { viewModel.updatePendingCommandPayload(it) },
                onActionTypeChange = { viewModel.updatePendingCommandActionType(it) },
                onConfirmExecute = { viewModel.confirmAndExecutePendingCommand() },
                onCancel = { viewModel.cancelPendingCommand() },
                onTeachAsSynonym = { word, canonical, action ->
                    viewModel.learnNewSynonym(word, canonical, action)
                    viewModel.confirmAndExecutePendingCommand()
                }
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
                onReplay = { viewModel.executeAction(plan.actionType, plan.actionPayload) },
                onUpdateAndReExecute = { action, payload ->
                    viewModel.updateAndReExecutePlan(action, payload)
                }
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // ==========================================
        // 4. CONVERSATION & AUTONOMOUS ACTION STREAM
        // ==========================================
        if (conversation.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Hearing,
                        contentDescription = null,
                        tint = PolishTextMuted,
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = s("المساعد الصوتي في وضع الاستعداد الدائم", "Assistant is always listening and ready"),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = PolishTextSecondary,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = s(
                            "جرب أن تقول بصوتك:\n• 'اتصل بمحمد'\n• 'انشر بوست على فيسبوك'\n• 'افتح الكاميرا' أو 'شغل الكشاف'",
                            "Try speaking commands like:\n• 'Call John'\n• 'Post on Facebook'\n• 'Open Camera' or 'Turn on flashlight'"
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = PolishTextMuted,
                        fontSize = 11.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(conversation, key = { it.id }) { msg ->
                    ConversationBubble(
                        message = msg,
                        isAr = isAr,
                        onExecuteAgain = { action, payload ->
                            viewModel.executeAction(action, payload)
                        },
                        onEditReview = { messageToReview ->
                            viewModel.openReviewForMessage(messageToReview)
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Quick Suggestion Chips Row for Instant Testing
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val suggestions = if (isAr) listOf(
                "🔦 شغل الكشاف",
                "📷 افتح الكاميرا",
                "🛡️ فحص الأمان",
                "📞 اتصل بمحمد",
                "📘 انشر بوست فيسبوك",
                "🏠 الشاشة الرئيسية",
                "🔙 رجوع للخلف",
                "🔍 ابحث في جوجل"
            ) else listOf(
                "🔦 Flashlight",
                "📷 Open Camera",
                "🛡️ Security Scan",
                "📞 Call Contact",
                "📘 Post Facebook",
                "🏠 Home Screen",
                "🔙 Go Back",
                "🔍 Web Search"
            )

            suggestions.forEach { prompt ->
                Surface(
                    onClick = {
                        val cleanCommand = prompt.substringAfter(" ").trim()
                        viewModel.handleUserVoiceInput(cleanCommand)
                    },
                    shape = RoundedCornerShape(12.dp),
                    color = PolishSurfaceElevated,
                    border = BorderStroke(1.dp, PolishSurfaceBorder)
                ) {
                    Text(
                        text = prompt,
                        style = MaterialTheme.typography.labelSmall,
                        color = PolishTextSecondary,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // ==========================================
        // 5. UNIFIED INPUT & MIC DOCK BAR
        // ==========================================
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = textInput,
                onValueChange = { textInput = it },
                placeholder = {
                    Text(
                        text = s("اكتب أمراً أو تحدث بصوتك مباشرة...", "Type a command or speak directly..."),
                        style = MaterialTheme.typography.bodySmall,
                        color = PolishTextMuted,
                        fontSize = 11.sp
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

            Spacer(modifier = Modifier.width(6.dp))

            IconButton(
                onClick = { viewModel.toggleVoiceListening() },
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(if (isListening) Color(0xFF10B981) else PolishPrimary)
                    .testTag("mic_toggle_button")
            ) {
                Icon(
                    imageVector = if (isListening) Icons.Default.Mic else Icons.Default.MicOff,
                    contentDescription = "استماع صوتي",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }

            if (textInput.isNotBlank()) {
                Spacer(modifier = Modifier.width(4.dp))
                IconButton(
                    onClick = {
                        viewModel.handleUserVoiceInput(textInput)
                        textInput = ""
                        focusManager.clearFocus()
                    },
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(PolishPrimary)
                        .testTag("send_query_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "إرسال",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // ==========================================
        // 6. ADD CUSTOM SYNONYM / DIALECT DIALOG
        // ==========================================
        if (showAddSynonymDialog) {
            AlertDialog(
                onDismissRequest = { showAddSynonymDialog = false },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Psychology,
                            contentDescription = null,
                            tint = PolishPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = s("تعليم الذكاء الاصطناعي مرادف جديد 🧠", "Teach AI New Dialect Synonym 🧠"),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = s(
                                "علّم المساعد الصوتي كيف يفهم لهجتك المحلية، مثلاً: كلمة 'اطفي' أو 'سكر' تعني 'اغلق'، أو 'ولّع' تعني 'شغل'.",
                                "Teach the assistant your dialect vocabulary. E.g.: 'اطفي' means 'اغلق', or 'ولع' means 'شغل'."
                            ),
                            fontSize = 11.sp,
                            color = PolishTextSecondary
                        )

                        OutlinedTextField(
                            value = customSynonymWord,
                            onValueChange = { customSynonymWord = it },
                            label = { Text(s("الكلمة / العبارة بلهجتك (مثلاً: اطفي)", "Dialect Word (e.g. اطفي)")) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = customSynonymCanonical,
                            onValueChange = { customSynonymCanonical = it },
                            label = { Text(s("المعنى الفصيح أو الأمر (مثلاً: اغلق)", "Standard Meaning (e.g. اغلق)")) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (customSynonymWord.isNotBlank() && customSynonymCanonical.isNotBlank()) {
                                viewModel.learnNewSynonym(
                                    phrase = customSynonymWord.trim(),
                                    canonical = customSynonymCanonical.trim()
                                )
                                customSynonymWord = ""
                                customSynonymCanonical = ""
                                showAddSynonymDialog = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PolishPrimary)
                    ) {
                        Text(s("تدريب وحفظ 🧠", "Train & Save 🧠"), color = Color.White, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    OutlinedButton(onClick = { showAddSynonymDialog = false }) {
                        Text(s("إلغاء", "Cancel"))
                    }
                }
            )
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
        shape = RoundedCornerShape(14.dp),
        color = PolishSurfaceElevated,
        border = BorderStroke(1.dp, PolishSurfaceBorder),
        modifier = Modifier.testTag("quick_action_$label")
    ) {
        Button(
            onClick = onClick,
            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 5.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = PolishPrimary,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(5.dp))
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
    onExecuteAgain: (ActionType, String?) -> Unit,
    onEditReview: (ConversationMessage) -> Unit = {}
) {
    val isUser = message.isUser
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.Start else Arrangement.End
    ) {
        Card(
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isUser) 4.dp else 16.dp,
                bottomEnd = if (isUser) 16.dp else 4.dp
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
            Column(modifier = Modifier.padding(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isUser) (if (isAr) "صوت المالك 👤" else "Owner Voice 👤") else (if (isAr) "المساعد الذكي 🤖" else "Autonomous Agent 🤖"),
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
                                text = if (isAr) "بصمة معتمدة (${message.biometricConfidence}%) ✓" else "Voiceprint (${message.biometricConfidence}%) ✓",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF10B981),
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = message.text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isUser) PolishOnPrimaryContainer else PolishTextPrimary,
                    fontSize = 12.sp
                )
                if (message.actionType != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = PolishSuccessContainer,
                        border = BorderStroke(1.dp, PolishSuccess.copy(alpha = 0.3f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(PolishSuccess)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isAr) "الأمر: ${message.actionType.name}" else "Intent: ${message.actionType.name}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = PolishSuccess,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 10.sp
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Button(
                                    onClick = { onEditReview(message) },
                                    shape = RoundedCornerShape(6.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = PolishPrimary.copy(alpha = 0.85f)),
                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                                        horizontal = 6.dp,
                                        vertical = 2.dp
                                    ),
                                    modifier = Modifier.height(24.dp)
                                ) {
                                    Text(
                                        text = if (isAr) "تعديل 📝" else "Edit 📝",
                                        color = Color.White,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Button(
                                    onClick = { onExecuteAgain(message.actionType, message.actionPayload) },
                                    shape = RoundedCornerShape(6.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = PolishPrimary),
                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                                        horizontal = 6.dp,
                                        vertical = 2.dp
                                    ),
                                    modifier = Modifier.height(24.dp)
                                ) {
                                    Text(
                                        text = if (isAr) "إعادة ⚡" else "Retry ⚡",
                                        color = Color.White,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
