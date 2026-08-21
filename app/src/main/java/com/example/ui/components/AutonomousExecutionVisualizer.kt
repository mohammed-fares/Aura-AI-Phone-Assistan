package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.ActionType
import com.example.system.ProgrammaticCommandDescriptor
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
    onUpdateAndReExecute: ((ActionType, String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val clipboardManager = LocalClipboardManager.current
    var showProgrammaticDetails by remember { mutableStateOf(false) }
    var isEditingCommand by remember { mutableStateOf(false) }
    var editedPayload by remember(plan.id, plan.actionPayload) { mutableStateOf(plan.actionPayload ?: "") }
    var selectedActionType by remember(plan.id, plan.actionType) { mutableStateOf(plan.actionType) }
    var copyNotice by remember { mutableStateOf<String?>(null) }

    val programmaticCmd = remember(plan.actionType, plan.actionPayload) {
        plan.programmaticCommand ?: ProgrammaticCommandDescriptor.describe(plan.actionType, plan.actionPayload)
    }

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
                        text = if (plan.isRunning) s("⚡ محاكي ومحرك التنفيذ البرمجي الذاتي", "⚡ Live Programmatic Autonomous Agent")
                        else s("✓ تم تنفيذ الأمر البرمجي على الهاتف بنجاح", "✓ Code Executed Successfully on Phone"),
                        color = if (plan.isRunning) PolishGlow else PolishSuccess,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
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
                            fontSize = 14.sp
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

            Spacer(modifier = Modifier.height(10.dp))

            // Programmatic Code & Edit Buttons Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                OutlinedButton(
                    onClick = { showProgrammaticDetails = !showProgrammaticDetails },
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, if (showProgrammaticDetails) PolishPrimary else PolishSurfaceBorder),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (showProgrammaticDetails) PolishPrimary.copy(alpha = 0.12f) else Color.Transparent
                    ),
                    modifier = Modifier.weight(1f).testTag("btn_inspect_programmatic_code")
                ) {
                    Icon(
                        imageVector = Icons.Default.Code,
                        contentDescription = null,
                        tint = if (showProgrammaticDetails) PolishPrimary else PolishTextSecondary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (showProgrammaticDetails) s("إخفاء الكود 💻", "Hide Code 💻") else s("عرض الكود البرمجي 💻", "Inspect Code 💻"),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (showProgrammaticDetails) PolishPrimary else PolishTextSecondary
                    )
                }

                OutlinedButton(
                    onClick = { isEditingCommand = !isEditingCommand },
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, if (isEditingCommand) PolishGlow else PolishSurfaceBorder),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (isEditingCommand) PolishGlow.copy(alpha = 0.12f) else Color.Transparent
                    ),
                    modifier = Modifier.weight(1f).testTag("btn_edit_command_parameters")
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = null,
                        tint = if (isEditingCommand) PolishGlow else PolishTextSecondary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isEditingCommand) s("إلغاء التعديل ✏️", "Cancel Edit ✏️") else s("تعديل الأمر وتصحيحه 🛠️", "Edit & Fix 🛠️"),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isEditingCommand) PolishGlow else PolishTextSecondary
                    )
                }
            }

            // =========================================================================
            // INLINE PROGRAMMATIC CODE INSPECTOR
            // =========================================================================
            AnimatedVisibility(visible = showProgrammaticDetails) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFF0F172A),
                        border = BorderStroke(1.dp, Color(0xFF334155)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Terminal,
                                        contentDescription = null,
                                        tint = Color(0xFF38BDF8),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = s("الكود البرمجي المنفذ على الهاتف (Kotlin/OS)", "Executed OS Intent & Kotlin Code"),
                                        color = Color(0xFF38BDF8),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                IconButton(
                                    onClick = {
                                        clipboardManager.setText(AnnotatedString(programmaticCmd.kotlinCodeSnippet))
                                        copyNotice = if (isArabic) "تم نسخ الكود البرمجي! 📋" else "Copied Kotlin code!"
                                    },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ContentCopy,
                                        contentDescription = "Copy",
                                        tint = Color(0xFF94A3B8),
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            // Kotlin Snippet Box
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFF020617),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = programmaticCmd.kotlinCodeSnippet,
                                    color = Color(0xFFA7F3D0),
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    lineHeight = 16.sp,
                                    modifier = Modifier.padding(10.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // ADB Shell Terminal Command
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = s("أمر ADB Shell المباشر:", "ADB Shell Command:"),
                                    color = Color(0xFF94A3B8),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium
                                )

                                IconButton(
                                    onClick = {
                                        clipboardManager.setText(AnnotatedString(programmaticCmd.adbShellCommand))
                                        copyNotice = if (isArabic) "تم نسخ أمر ADB Shell! 📋" else "Copied ADB command!"
                                    },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ContentCopy,
                                        contentDescription = "Copy ADB",
                                        tint = Color(0xFF38BDF8),
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFF1E293B),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "$ ${programmaticCmd.adbShellCommand}",
                                    color = Color(0xFFFDE047),
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.sp,
                                    modifier = Modifier.padding(8.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            // Target Component / Hook
                            Text(
                                text = "🎯 Hook: ${programmaticCmd.targetComponent} | Action: ${programmaticCmd.intentAction}",
                                color = Color(0xFF64748B),
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace
                            )

                            if (copyNotice != null) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = copyNotice!!,
                                    color = Color(0xFF10B981),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // =========================================================================
            // INLINE COMMAND & PARAMETER EDITOR (FOR FIXING ANY BUGS OR CUSTOMIZING)
            // =========================================================================
            AnimatedVisibility(visible = isEditingCommand) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = PolishBackground,
                        border = BorderStroke(1.dp, PolishGlow.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Build,
                                    contentDescription = null,
                                    tint = PolishGlow,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = s("تعديل المعاملات البرمجية وإصلاح الخطأ 🛠️", "Edit Command Parameters & Fix Bugs 🛠️"),
                                    color = PolishTextPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Quick Action Switcher Chips
                            Text(
                                text = s("نوع الأمر البرمجي:", "Action Type:"),
                                color = PolishTextSecondary,
                                fontSize = 10.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                val commonActions = listOf(
                                    ActionType.CALL_CONTACT,
                                    ActionType.SEND_MESSAGE,
                                    ActionType.SEND_WHATSAPP_MESSAGE,
                                    ActionType.READ_SCREEN_TEXT,
                                    ActionType.SUMMARIZE_SCREEN,
                                    ActionType.TYPE_ON_SCREEN,
                                    ActionType.OPEN_APP,
                                    ActionType.WEB_SEARCH,
                                    ActionType.TOGGLE_FLASHLIGHT,
                                    ActionType.SET_VOLUME,
                                    ActionType.OPEN_SETTINGS,
                                    ActionType.RETURN_HOME
                                )
                                commonActions.forEach { act ->
                                    val isSel = selectedActionType == act
                                    Surface(
                                        onClick = { selectedActionType = act },
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isSel) PolishPrimary else PolishSurfaceElevated,
                                        border = BorderStroke(1.dp, if (isSel) PolishPrimary else PolishSurfaceBorder)
                                    ) {
                                        Text(
                                            text = act.name,
                                            fontSize = 9.sp,
                                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSel) Color.White else PolishTextSecondary,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Payload / Parameters Input
                            Text(
                                text = s("المعامل / المحتوى (رقم، نص، اسم تطبيق، جملة للكتابة):", "Payload / Parameters (Number, SMS, App Name, Text to Type):"),
                                color = PolishTextSecondary,
                                fontSize = 10.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))

                            OutlinedTextField(
                                value = editedPayload,
                                onValueChange = { editedPayload = it },
                                placeholder = {
                                    Text(
                                        text = s("أدخل المعامل البرمجي الجديد...", "Enter new parameter / payload..."),
                                        fontSize = 11.sp,
                                        color = PolishTextMuted
                                    )
                                },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("input_edit_command_payload"),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = PolishPrimary,
                                    unfocusedBorderColor = PolishSurfaceBorder
                                )
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            // Save & Execute Button
                            Button(
                                onClick = {
                                    isEditingCommand = false
                                    onUpdateAndReExecute?.invoke(selectedActionType, editedPayload)
                                },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = PolishPrimary),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("btn_save_reexecute_command")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = s("حفظ المعاملات وإعادة التنفيذ المباشر ⚡", "Apply Changes & Execute Now ⚡"),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

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
                plan.steps.forEachIndexed { _, step ->
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
        ActionType.READ_SCREEN_TEXT -> Icons.Default.Visibility
        ActionType.SUMMARIZE_SCREEN -> Icons.Default.AutoAwesome
        ActionType.TYPE_ON_SCREEN -> Icons.Default.Keyboard
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
        ActionType.READ_SCREEN_TEXT -> if (isArabic) "قراءة نصوص الشاشة المعروضة" else "Reading Screen Content"
        ActionType.SUMMARIZE_SCREEN -> if (isArabic) "تلخيص الشاشة بالذكاء الاصطناعي" else "AI Screen Summarization"
        ActionType.TYPE_ON_SCREEN -> if (isArabic) "الكتابة في حقل الشاشة النشط" else "Typing in Active Field"
        ActionType.TAKE_SCREENSHOT -> if (isArabic) "التقاط صورة للشاشة" else "Capturing Screenshot"
        ActionType.LOCAL_NETWORK_SCAN -> if (isArabic) "تدقيق الشبكة المحلية وبث الشاشة" else "LAN Nodes & Stream Audit"
        ActionType.DEVICE_DIAGNOSTIC -> if (isArabic) "تشخيص أداء المعالج والذاكرة" else "Hardware Diagnostics"
        ActionType.BATTERY_OPTIMIZATION -> if (isArabic) "تحسين استهلاك الطاقة" else "Battery Optimization"
        ActionType.NETWORK_AUDIT -> if (isArabic) "تدقيق وتشفير الشبكة" else "Network Encryption Audit"
        ActionType.VOICE_NOTE -> if (isArabic) "حفظ ملاحظة صوتية" else "Saving Voice Note"
        ActionType.REMOTE_LOCK_ALERT -> if (isArabic) "إشعار القفل الأمني" else "Security Lock Alert"
        ActionType.AI_SUMMARIZE_ACTIVITY -> if (isArabic) "استخراج تقرير النشاط الذكي" else "AI Telemetry Summary"
    }
}
