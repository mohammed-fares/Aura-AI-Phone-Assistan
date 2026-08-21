package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.ActionType
import com.example.ui.PendingCommandReview
import com.example.ui.theme.PolishBackground
import com.example.ui.theme.PolishGlow
import com.example.ui.theme.PolishOnPrimaryContainer
import com.example.ui.theme.PolishPrimary
import com.example.ui.theme.PolishPrimaryContainer
import com.example.ui.theme.PolishSuccess
import com.example.ui.theme.PolishSurfaceBorder
import com.example.ui.theme.PolishSurfaceElevated
import com.example.ui.theme.PolishTextMuted
import com.example.ui.theme.PolishTextPrimary
import com.example.ui.theme.PolishTextSecondary

/**
 * Human-in-the-loop Command Review & Intent Correction Component
 * Displays the AI's parsed command as an editable text field before execution,
 * allowing the user to review, edit parameters, switch actions, and correct intent.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PendingCommandReviewCard(
    review: PendingCommandReview,
    isArabic: Boolean,
    onPayloadChange: (String) -> Unit,
    onActionTypeChange: (ActionType) -> Unit,
    onConfirmExecute: () -> Unit,
    onCancel: () -> Unit,
    onTeachAsSynonym: (word: String, canonical: String, action: ActionType) -> Unit,
    modifier: Modifier = Modifier
) {
    fun s(ar: String, en: String): String = if (isArabic) ar else en

    var showActionPicker by remember { mutableStateOf(false) }

    val commonActions: List<Pair<ActionType, String>> = listOf(
        ActionType.CLOSE_APP to s("إغلاق التطبيق 🛑", "Close App 🛑"),
        ActionType.OPEN_APP to s("فتح تطبيق 📱", "Open App 📱"),
        ActionType.CALL_CONTACT to s("اتصال هاتف 📞", "Call Contact 📞"),
        ActionType.SEND_MESSAGE to s("إرسال رسالة SMS 💬", "Send SMS 💬"),
        ActionType.SEND_WHATSAPP_MESSAGE to s("إرسال واتساب 🟢", "Send WhatsApp 🟢"),
        ActionType.POST_FACEBOOK to s("نشر فيسبوك 📘", "Post Facebook 📘"),
        ActionType.READ_SCREEN_TEXT to s("قراءة الشاشة 👁️", "Read Screen 👁️"),
        ActionType.SUMMARIZE_SCREEN to s("تلخيص الشاشة 📊", "Summarize Screen 📊"),
        ActionType.TYPE_ON_SCREEN to s("كتابة بالشاشة ✍️", "Type on Screen ✍️"),
        ActionType.CLICK_SCREEN_ELEMENT to s("نقر عنصر 👆", "Click Element 👆"),
        ActionType.GLOBAL_BACK to s("زر الرجوع 🔙", "Go Back 🔙"),
        ActionType.RETURN_HOME to s("الرئيسية 🏠", "Return Home 🏠"),
        ActionType.TOGGLE_FLASHLIGHT to s("كشاف الهاتف 🔦", "Flashlight 🔦"),
        ActionType.OPEN_CAMERA to s("الكاميرا 📷", "Camera 📷"),
        ActionType.WEB_SEARCH to s("بحث جوجل 🔍", "Google Search 🔍"),
        ActionType.SYSTEM_SECURITY_SCAN to s("فحص الأمان 🛡️", "Security Scan 🛡️")
    )

    val payloadLabel = when (review.editableActionType) {
        ActionType.CALL_CONTACT -> s("رقم الهاتف أو اسم جهة الاتصال", "Phone Number or Contact Name")
        ActionType.SEND_MESSAGE, ActionType.SEND_WHATSAPP_MESSAGE -> s("نص الرسالة أو المستلم", "Message Text / Recipient")
        ActionType.POST_FACEBOOK, ActionType.COMMENT_ON_SCREEN -> s("نص المنشور أو التعليق", "Post / Comment Content")
        ActionType.OPEN_APP, ActionType.CLOSE_APP -> s("اسم التطبيق أو الحزمة", "App Name or Package")
        ActionType.TYPE_ON_SCREEN -> s("النص المراد كتابته ذاتياً على الشاشة", "Text to Type Autonomously")
        ActionType.CLICK_SCREEN_ELEMENT -> s("اسم الزر أو العنصر للنقر عليه", "Element / Button Text to Click")
        ActionType.WEB_SEARCH -> s("كلمات البحث في جوجل", "Search Query")
        ActionType.SEND_EMAIL -> s("عنوان البريد الإلكتروني أو الرسالة", "Email Address or Body")
        else -> s("البيانات البرمجية والمعاملات (Payload)", "Command Parameters (Payload)")
    }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = PolishSurfaceElevated),
        border = BorderStroke(1.5.dp, PolishPrimary.copy(alpha = 0.7f)),
        modifier = modifier
            .fillMaxWidth()
            .testTag("pending_command_review_card")
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // 1. Header with Badge & Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(PolishPrimary.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = null,
                            tint = PolishPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = s("مراجعة وتعديل الأمر البرمجي ⚡", "Review & Edit Intent Parameters ⚡"),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = PolishTextPrimary,
                            fontSize = 13.sp
                        )
                        Text(
                            text = s("يمكنك تعديل أي خطأ برمجي أو أمر قبل تنفيذه على الهاتف", "Modify any parameter or target action before execution"),
                            style = MaterialTheme.typography.bodySmall,
                            color = PolishTextSecondary,
                            fontSize = 10.sp
                        )
                    }
                }

                IconButton(
                    onClick = onCancel,
                    modifier = Modifier.size(28.dp).testTag("btn_cancel_review")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Cancel",
                        tint = PolishTextMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // 2. Vector Synonym / Dialect Match Banner (if matched)
            if (review.matchedSynonymWord != null || review.matchedClusterTitle != null) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = PolishPrimaryContainer.copy(alpha = 0.35f),
                    border = BorderStroke(1.dp, PolishPrimary.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Psychology,
                                contentDescription = null,
                                tint = PolishPrimary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (review.matchedSynonymWord != null) {
                                    s("قاموس المرادفات: تم التعرف على '${review.matchedSynonymWord}'", "Synonym Match: '${review.matchedSynonymWord}'")
                                } else {
                                    s("مجموعة الأوامر: ${review.matchedClusterTitle}", "Command Cluster: ${review.matchedClusterTitle}")
                                },
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = PolishOnPrimaryContainer
                            )
                        }

                        Text(
                            text = "${(review.similarityScore * 100).toInt()}% " + s("دقة", "Match"),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = PolishPrimary
                        )
                    }
                }
            }

            // 3. Raw Spoken Text Box
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = PolishBackground,
                border = BorderStroke(1.dp, PolishSurfaceBorder)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = s("الصوت المسموع:", "Heard Voice:"),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = PolishTextSecondary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "\"${review.rawSpokenText}\"",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = PolishTextPrimary
                    )
                }
            }

            // 4. Action Type Selector Header + Chip
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = s("نوع الأمر التنفيذي المستهدف:", "Target Execution Action:"),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = PolishTextSecondary
                    )
                    Surface(
                        onClick = { showActionPicker = !showActionPicker },
                        shape = RoundedCornerShape(8.dp),
                        color = PolishPrimary.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, PolishPrimary.copy(alpha = 0.4f)),
                        modifier = Modifier.testTag("btn_change_action_type")
                    ) {
                        Text(
                            text = if (showActionPicker) s("إخفاء الخيارات ▲", "Hide ▲") else s("تغيير الأمر البرمجي ▼", "Change Action ▼"),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = PolishPrimary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                // Current Action Pill
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = PolishSuccess.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, PolishSuccess.copy(alpha = 0.4f))
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
                            text = review.editableActionType.name,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = PolishSuccess
                        )
                    }
                }

                // Dropdown / Chips List for selecting another action
                AnimatedVisibility(visible = showActionPicker) {
                    FlowRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        commonActions.forEach { (action, label) ->
                            val isSelected = review.editableActionType == action
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    onActionTypeChange(action)
                                    showActionPicker = false
                                },
                                label = { Text(label, fontSize = 10.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = PolishPrimary,
                                    selectedLabelColor = Color.White,
                                    containerColor = PolishSurfaceElevated,
                                    labelColor = PolishTextPrimary
                                ),
                                modifier = Modifier.testTag("chip_action_${action.name}")
                            )
                        }
                    }
                }
            }

            // 5. Editable Parameter Field (TextField)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = s("معاملات الأمر (قابلة للتعديل والتحرير):", "Command Payload (Editable):"),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = PolishTextSecondary
                )

                OutlinedTextField(
                    value = review.editablePayload,
                    onValueChange = onPayloadChange,
                    placeholder = {
                        Text(
                            text = payloadLabel,
                            fontSize = 11.sp,
                            color = PolishTextMuted
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("review_editable_payload_input"),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = PolishBackground,
                        unfocusedContainerColor = PolishBackground,
                        focusedBorderColor = PolishPrimary,
                        unfocusedBorderColor = PolishSurfaceBorder,
                        focusedTextColor = PolishTextPrimary,
                        unfocusedTextColor = PolishTextPrimary
                    ),
                    maxLines = 3
                )
            }

            // 6. Action Execution & Learning Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Execute Button
                Button(
                    onClick = onConfirmExecute,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PolishPrimary),
                    modifier = Modifier
                        .weight(1.3f)
                        .height(44.dp)
                        .testTag("btn_confirm_execute_intent")
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = s("تنفيذ الأمر المعتمد ⚡", "Execute Verified Intent ⚡"),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                // Teach as Synonym Button
                OutlinedButton(
                    onClick = {
                        onTeachAsSynonym(
                            review.rawSpokenText,
                            review.editablePayload.ifBlank { review.editableActionType.name },
                            review.editableActionType
                        )
                    },
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, PolishGlow),
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .testTag("btn_teach_intent_as_synonym")
                ) {
                    Icon(
                        imageVector = Icons.Default.Psychology,
                        contentDescription = null,
                        tint = PolishPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = s("حفظ كمرادف 🧠", "Learn Synonym 🧠"),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = PolishPrimary
                    )
                }
            }
        }
    }
}
