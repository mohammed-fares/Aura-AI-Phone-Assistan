package com.example.ui.screens

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
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.KeyboardVoice
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import com.example.ui.theme.PolishGlow
import com.example.ui.theme.PolishOnPrimaryContainer
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

    var assistantNameInput by remember(config.assistantName) { mutableStateOf(config.assistantName) }
    var selectedDialect by remember(config.preferredDialect) { mutableStateOf(config.preferredDialect) }
    var dialectDropdownExpanded by remember { mutableStateOf(false) }

    var voiceFeedback by remember(config.voiceFeedbackEnabled) { mutableStateOf(config.voiceFeedbackEnabled) }
    var lowResourceMode by remember(config.lowResourceMode) { mutableStateOf(config.lowResourceMode) }
    var shakeGestureEnabled by remember(config.shakeGestureActionEnabled) { mutableStateOf(config.shakeGestureActionEnabled) }

    var speechPitch by remember(config.ttsPitch) { mutableFloatStateOf(config.ttsPitch) }
    var speechSpeed by remember(config.ttsSpeed) { mutableFloatStateOf(config.ttsSpeed) }

    // New Shortcut Modal/Inline state
    var showAddShortcut by remember { mutableStateOf(false) }
    var newShortcutTitle by remember { mutableStateOf("") }
    var newShortcutPhrase by remember { mutableStateOf("") }
    var newShortcutType by remember { mutableStateOf(ActionType.CALL_CONTACT) }

    val dialectOptions = listOf(
        "العربية (لهجات متعددة)",
        "اللهجة الشامية (سورية، لبنان، فلسطين، الأردن)",
        "اللهجة المصرية",
        "اللهجة الخليجية",
        "اللهجة المغاربية",
        "اللغة العربية الفصحى",
        "English (US / UK)"
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

            // AI Persona & Identity Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("assistant_identity_card"),
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
                                .background(PolishPrimaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = PolishPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "تخصيص هوية المساعد الذكي",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = PolishTextPrimary
                            )
                            Text(
                                text = "تسمية المساعد واختيار لغة ولهجة التفاهم المفضلة",
                                style = MaterialTheme.typography.bodySmall,
                                color = PolishTextSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

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

                    Spacer(modifier = Modifier.height(14.dp))

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
                                    lowResourceMode = lowResourceMode,
                                    shakeGestureActionEnabled = shakeGestureEnabled,
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
                            text = "حفظ إعدادات الهوية والتعلم الذاتي",
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Voice Engine & Resource Optimization Settings
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("preferences_card"),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = PolishSurfaceElevated),
                border = BorderStroke(1.dp, PolishSurfaceBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "محرك الصوت والأداء والموارد",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = PolishTextPrimary
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Resource Saving Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "وضع توفير موارد الهاتف والبطارية",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = PolishTextPrimary
                            )
                            Text(
                                text = "تدقيق البيانات عند الطلب وتجنب المعالجة الزائدة في الخلفية",
                                style = MaterialTheme.typography.bodySmall,
                                color = PolishTextSecondary,
                                fontSize = 11.sp
                            )
                        }
                        Switch(
                            checked = lowResourceMode,
                            onCheckedChange = {
                                lowResourceMode = it
                                viewModel.updateAssistantConfig(config.copy(lowResourceMode = it))
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = PolishSuccess,
                                uncheckedTrackColor = PolishSurfaceBorder
                            ),
                            modifier = Modifier.testTag("switch_low_resource")
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Voice Feedback Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "الرد الصوتي التلقائي (TTS)",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = PolishTextPrimary
                            )
                            Text(
                                text = "تحدث المساعد بنطق صوتي طبيعي باللغة العربية",
                                style = MaterialTheme.typography.bodySmall,
                                color = PolishTextSecondary,
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

                    // Gesture / Shake Action
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "محول اللمس والإيماءات لصوت",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = PolishTextPrimary
                            )
                            Text(
                                text = "تحويل تفاعلات اليد وهز الهاتف إلى أوامر صوتية فورية",
                                style = MaterialTheme.typography.bodySmall,
                                color = PolishTextSecondary,
                                fontSize = 11.sp
                            )
                        }
                        Switch(
                            checked = shakeGestureEnabled,
                            onCheckedChange = {
                                shakeGestureEnabled = it
                                viewModel.updateAssistantConfig(config.copy(shakeGestureActionEnabled = it))
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = PolishPrimary,
                                uncheckedTrackColor = PolishSurfaceBorder
                            ),
                            modifier = Modifier.testTag("switch_gesture_action")
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Pitch & Speed Sliders
                    Text(
                        text = "سرعة التحدث: ${String.format("%.1f", speechSpeed)}x",
                        style = MaterialTheme.typography.labelSmall,
                        color = PolishTextSecondary
                    )
                    Slider(
                        value = speechSpeed,
                        onValueChange = { speechSpeed = it },
                        onValueChangeFinished = {
                            viewModel.updateAssistantConfig(config.copy(ttsSpeed = speechSpeed))
                        },
                        valueRange = 0.7f..1.5f,
                        colors = SliderDefaults.colors(
                            thumbColor = PolishPrimary,
                            activeTrackColor = PolishPrimary,
                            inactiveTrackColor = PolishSurfaceBorder
                        ),
                        modifier = Modifier.testTag("slider_speech_speed")
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "طبقة ونغمة الصوت: ${String.format("%.1f", speechPitch)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = PolishTextSecondary
                    )
                    Slider(
                        value = speechPitch,
                        onValueChange = { speechPitch = it },
                        onValueChangeFinished = {
                            viewModel.updateAssistantConfig(config.copy(ttsPitch = speechPitch))
                        },
                        valueRange = 0.7f..1.4f,
                        colors = SliderDefaults.colors(
                            thumbColor = PolishPrimary,
                            activeTrackColor = PolishPrimary,
                            inactiveTrackColor = PolishSurfaceBorder
                        ),
                        modifier = Modifier.testTag("slider_speech_pitch")
                    )
                }
            }
        }

        // Add Custom Voice Shortcut Section
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
                            text = "إنشاء اختصار صوتي مخصص",
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
                            label = { Text("اسم الاختصار (مثال: تشغيل وضع الهدوء)") },
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
                            label = { Text("العبارة الصوتية للتشغيل (مثال: هدوء يا نور)") },
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
                                        actionType = ActionType.TOGGLE_SILENT_MODE,
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

