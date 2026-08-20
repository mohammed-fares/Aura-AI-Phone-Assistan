package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.ActionType
import com.example.ui.ExecutionStep
import com.example.ui.LiveExecutionPlan
import com.example.ui.theme.PolishBackground
import com.example.ui.theme.PolishGlow
import com.example.ui.theme.PolishPrimary
import com.example.ui.theme.PolishPrimaryContainer
import com.example.ui.theme.PolishSuccess
import com.example.ui.theme.PolishSurfaceBorder
import com.example.ui.theme.PolishSurfaceElevated
import com.example.ui.theme.PolishTextMuted
import com.example.ui.theme.PolishTextPrimary
import com.example.ui.theme.PolishTextSecondary

@Composable
fun AutonomousExecutionVisualizer(
    plan: LiveExecutionPlan,
    isArabic: Boolean,
    onDismiss: () -> Unit,
    onReturnHome: () -> Unit,
    onEndCall: () -> Unit,
    onReplay: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    fun s(ar: String, en: String): String = if (isArabic) ar else en

    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = PolishSurfaceElevated),
        border = BorderStroke(
            1.5.dp,
            Brush.horizontalGradient(
                listOf(
                    PolishPrimary.copy(alpha = 0.8f),
                    PolishGlow.copy(alpha = 0.6f),
                    PolishPrimary.copy(alpha = 0.8f)
                )
            )
        ),
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .testTag("autonomous_execution_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header with AI Agent Badge & Dismiss
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .scale(pulseScale)
                            .clip(CircleShape)
                            .background(if (plan.isRunning) PolishGlow else PolishSuccess)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (plan.isRunning) s("⚡ محاكي التنفيذ الذاتي المباشر", "⚡ Live Autonomous Agent Running")
                        else s("✓ تم التنفيذ على الهاتف بنجاح", "✓ Executed Successfully on Phone"),
                        color = if (plan.isRunning) PolishGlow else PolishSuccess,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(28.dp).testTag("dismiss_plan_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Dismiss",
                        tint = PolishTextMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Action Banner Card
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = PolishBackground,
                border = BorderStroke(1.dp, PolishSurfaceBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(PolishPrimaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = getActionIcon(plan.actionType),
                            contentDescription = null,
                            tint = PolishPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = getActionFriendlyTitle(plan.actionType, plan.actionPayload, isArabic),
                            color = PolishTextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        if (!plan.actionPayload.isNullOrBlank()) {
                            Text(
                                text = plan.actionPayload,
                                color = PolishTextSecondary,
                                fontSize = 12.sp,
                                maxLines = 1
                            )
                        }
                    }

                    if (plan.isRunning) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = PolishPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(PolishSuccess.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = PolishSuccess,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Live Step Tracker
            Text(
                text = s("خطوات التنفيذ التلقائي بالذكاء الاصطناعي:", "Autonomous AI Execution Pipeline:"),
                color = PolishTextMuted,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(8.dp))

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                plan.steps.forEachIndexed { idx, step ->
                    StepItemRow(
                        step = step,
                        isArabic = isArabic,
                        pulseScale = pulseScale
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Status message
            if (plan.statusMessage.isNotBlank()) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = PolishSurfaceElevated,
                    border = BorderStroke(1.dp, PolishSurfaceBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = plan.statusMessage,
                        color = PolishTextSecondary,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Interactive Controls Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (plan.actionType == ActionType.CALL_CONTACT) {
                    Button(
                        onClick = onEndCall,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f).testTag("end_call_button")
                    ) {
                        Icon(Icons.Default.CallEnd, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = s("إنهاء المكالمة 📵", "End Call 📵"), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                OutlinedButton(
                    onClick = onReturnHome,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, PolishSurfaceBorder),
                    modifier = Modifier.weight(1f).testTag("return_home_button")
                ) {
                    Icon(Icons.Default.Home, contentDescription = null, tint = PolishTextPrimary, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = s("الرئيسية 🏠", "Home 🏠"), color = PolishTextPrimary, fontSize = 12.sp)
                }

                OutlinedButton(
                    onClick = onReplay,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, PolishSurfaceBorder),
                    modifier = Modifier.weight(1f).testTag("replay_action_button")
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, tint = PolishPrimary, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = s("إعادة 🔄", "Replay 🔄"), color = PolishPrimary, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun StepItemRow(
    step: ExecutionStep,
    isArabic: Boolean,
    pulseScale: Float
) {
    val containerBg = when {
        step.isActive -> PolishPrimary.copy(alpha = 0.15f)
        step.isCompleted -> PolishSuccess.copy(alpha = 0.08f)
        else -> PolishBackground.copy(alpha = 0.5f)
    }

    val borderColor = when {
        step.isActive -> PolishPrimary.copy(alpha = 0.6f)
        step.isCompleted -> PolishSuccess.copy(alpha = 0.4f)
        else -> PolishSurfaceBorder
    }

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = containerBg,
        border = BorderStroke(1.dp, borderColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon / Number
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(
                        when {
                            step.isCompleted -> PolishSuccess
                            step.isActive -> PolishPrimary
                            else -> PolishSurfaceBorder
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (step.isCompleted) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                } else if (step.isActive) {
                    Icon(
                        imageVector = Icons.Default.TouchApp,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(14.dp).scale(pulseScale)
                    )
                } else {
                    Text(
                        text = "${step.stepIndex + 1}",
                        color = PolishTextMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = step.title,
                    color = if (step.isActive) PolishPrimary else PolishTextPrimary,
                    fontWeight = if (step.isActive) FontWeight.Bold else FontWeight.Medium,
                    fontSize = 13.sp
                )
                Text(
                    text = step.description,
                    color = PolishTextSecondary,
                    fontSize = 11.sp
                )
            }

            if (step.isActive) {
                Text(
                    text = if (isArabic) "جاري التنفيذ..." else "Active...",
                    color = PolishGlow,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

private fun getActionIcon(actionType: ActionType): ImageVector {
    return when (actionType) {
        ActionType.CALL_CONTACT -> Icons.Default.Call
        ActionType.END_CALL -> Icons.Default.CallEnd
        ActionType.SEND_MESSAGE -> Icons.Default.Message
        ActionType.SEND_EMAIL -> Icons.Default.Message
        ActionType.OPEN_APP -> Icons.Default.PlayArrow
        ActionType.CLOSE_APP, ActionType.RETURN_HOME -> Icons.Default.Home
        ActionType.TOGGLE_FLASHLIGHT -> Icons.Default.FlashlightOn
        ActionType.SET_VOLUME, ActionType.TOGGLE_SILENT_MODE -> Icons.Default.VolumeUp
        ActionType.SYSTEM_SECURITY_SCAN -> Icons.Default.Security
        ActionType.OPEN_SETTINGS -> Icons.Default.Settings
        else -> Icons.Default.TouchApp
    }
}

private fun getActionFriendlyTitle(actionType: ActionType, payload: String?, isArabic: Boolean): String {
    return when (actionType) {
        ActionType.CALL_CONTACT -> if (isArabic) "إجراء مكالمة هاتفية" else "Initiating Phone Call"
        ActionType.END_CALL -> if (isArabic) "إنهاء المكالمة الهاتفية" else "Ending Active Call"
        ActionType.SEND_MESSAGE -> if (isArabic) "إرسال رسالة SMS" else "Sending SMS Message"
        ActionType.SEND_EMAIL -> if (isArabic) "إرسال بريد إلكتروني" else "Composing Email"
        ActionType.OPEN_APP -> if (isArabic) "تشغيل تطبيق ${payload ?: ""}" else "Launching App ${payload ?: ""}"
        ActionType.CLOSE_APP, ActionType.RETURN_HOME -> if (isArabic) "العودة للشاشة الرئيسية" else "Returning to Home Screen"
        ActionType.OPEN_CAMERA -> if (isArabic) "فتح كاميرا الهاتف" else "Opening Camera"
        ActionType.OPEN_GALLERY -> if (isArabic) "فتح استوديو الصور" else "Opening Media Gallery"
        ActionType.OPEN_CALCULATOR -> if (isArabic) "تشغيل الآلة الحاسبة" else "Opening Calculator"
        ActionType.OPEN_MAPS -> if (isArabic) "فتح الخرائط والملاحة" else "Opening Navigation Maps"
        ActionType.OPEN_BROWSER -> if (isArabic) "فتح متصفح الويب" else "Opening Web Browser"
        ActionType.SET_ALARM -> if (isArabic) "ضبط الساعة والمنبه" else "Setting Alarm / Timer"
        ActionType.WEB_SEARCH -> if (isArabic) "البحث في Google" else "Google Web Search"
        ActionType.SET_VOLUME -> if (isArabic) "التحكم بمستوى الصوت" else "Sound Volume Adjusted"
        ActionType.TOGGLE_SILENT_MODE -> if (isArabic) "تبديل الوضع الصامت" else "Toggling Silent Mode"
        ActionType.TOGGLE_FLASHLIGHT -> if (isArabic) "التحكم في كشاف الهاتف" else "Toggling Flashlight (Torch)"
        ActionType.OPEN_SETTINGS -> if (isArabic) "فتح إعدادات الهاتف" else "Opening Device Settings"
        ActionType.OPEN_WIFI_SETTINGS -> if (isArabic) "فتح إعدادات Wi-Fi" else "Opening Wi-Fi Settings"
        ActionType.OPEN_BLUETOOTH_SETTINGS -> if (isArabic) "فتح إعدادات البلوتوث" else "Opening Bluetooth Settings"
        ActionType.OPEN_DISPLAY_SETTINGS -> if (isArabic) "فتح إعدادات الشاشة" else "Opening Display Settings"
        ActionType.OPEN_BATTERY_SETTINGS -> if (isArabic) "فتح إعدادات البطارية" else "Opening Battery Settings"
        ActionType.OPEN_SECURITY_SETTINGS -> if (isArabic) "فتح لوحة الأمان" else "Opening Security Settings"
        ActionType.SYSTEM_SECURITY_SCAN -> if (isArabic) "فحص الأمان ومكافحة الاختراق" else "Deep Security & Vulnerability Scan"
        ActionType.SEND_WHATSAPP_MESSAGE -> if (isArabic) "إرسال رسالة واتساب تلقائياً" else "Autonomous WhatsApp Message"
        ActionType.SEND_MESSENGER_MESSAGE -> if (isArabic) "إرسال رسالة ماسنجر خاصة" else "Autonomous Messenger Message"
        ActionType.POST_FACEBOOK -> if (isArabic) "نشر بوست على فيسبوك" else "Publishing Facebook Post"
        ActionType.COMMENT_ON_SCREEN -> if (isArabic) "التعليق على المحتوى المعروض" else "Autonomous Screen Comment"
        ActionType.GLOBAL_BACK -> if (isArabic) "الرجوع للخلف" else "Navigating Back"
        ActionType.OPEN_RECENTS -> if (isArabic) "عرض التطبيقات المفتوحة" else "Opening Recent Tasks"
        ActionType.OPEN_NOTIFICATIONS -> if (isArabic) "فتح لوحة الإشعارات" else "Opening Notifications"
        ActionType.SCROLL_UP -> if (isArabic) "التمرير لأعلى الشاشة" else "Scrolling Up"
        ActionType.SCROLL_DOWN -> if (isArabic) "التمرير لأسفل الشاشة" else "Scrolling Down"
        ActionType.CLICK_SCREEN_ELEMENT -> if (isArabic) "الضغط على عنصر في الشاشة" else "Autonomous Screen Tap"
        ActionType.TAKE_SCREENSHOT -> if (isArabic) "التقاط صورة للشاشة" else "Capturing Screenshot"
        ActionType.LOCAL_NETWORK_SCAN -> if (isArabic) "تدقيق الشبكة المحلية وبث الشاشة" else "LAN Nodes & Stream Audit"
        ActionType.DEVICE_DIAGNOSTIC -> if (isArabic) "تشخيص أداء المعالج والذاكرة" else "Hardware Diagnostics"
        ActionType.BATTERY_OPTIMIZATION -> if (isArabic) "تحسين استهلاك الطاقة" else "Battery Optimization"
        ActionType.NETWORK_AUDIT -> if (isArabic) "تدقيق وتشفير الشبكة" else "Network Encryption Audit"
        ActionType.VOICE_NOTE -> if (isArabic) "حفظ ملاحظة صوتية" else "Saving Voice Note"
        ActionType.REMOTE_LOCK_ALERT -> if (isArabic) "إشعار القفل الأمني" else "Security Lock Alert"
        ActionType.AI_SUMMARIZE_ACTIVITY -> if (isArabic) "استخراج تقرير النشاط الذكي" else "AI Telemetry Summary"
        else -> if (isArabic) "تنفيذ الأمر الذكي" else "Executing Smart Action"
    }
}
