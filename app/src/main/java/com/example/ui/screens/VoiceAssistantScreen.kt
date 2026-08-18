package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.VolumeUp
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
import com.example.ui.components.NeuralOrbVisualizer
import com.example.ui.theme.PolishBackground
import com.example.ui.theme.PolishGlow
import com.example.ui.theme.PolishOnPrimaryContainer
import com.example.ui.theme.PolishOnSecondaryContainer
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
fun VoiceAssistantScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val isListening by viewModel.voiceEngine.isListening.collectAsStateWithLifecycle()
    val isSpeaking by viewModel.voiceEngine.isSpeaking.collectAsStateWithLifecycle()
    val audioLevel by viewModel.voiceEngine.rmsAudioLevel.collectAsStateWithLifecycle()
    val liveRecognizedText by viewModel.voiceEngine.lastRecognizedText.collectAsStateWithLifecycle()
    val isProcessing by viewModel.isProcessingAi.collectAsStateWithLifecycle()
    val config by viewModel.assistantConfig.collectAsStateWithLifecycle()
    val conversation by viewModel.conversation.collectAsStateWithLifecycle()

    var textInput by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current
    val listState = rememberLazyListState()

    LaunchedEffect(conversation.size) {
        if (conversation.isNotEmpty()) {
            listState.animateScrollToItem(conversation.size - 1)
        }
    }

    val quickActionPrompts = listOf(
        "اتصل برقم سريع",
        "خلي الهاتف صامت",
        "افحص أداء المعالج والذاكرة",
        "وفر استهلاك البطارية",
        "احفظ ملاحظة صوتية",
        "لخص نشاط الهاتف وتدقيق الأمان"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(PolishBackground)
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(14.dp))

        // Assistant Persona & Status Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = PolishSurfaceElevated,
                border = BorderStroke(1.dp, PolishSurfaceBorder)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (isListening || isSpeaking) PolishPrimary else PolishSuccess)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "المساعد الذكي: ${config.assistantName}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = PolishTextPrimary
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(24.dp),
                color = PolishSecondaryContainer,
                border = BorderStroke(1.dp, PolishSurfaceBorder)
            ) {
                Text(
                    text = config.preferredDialect,
                    style = MaterialTheme.typography.labelSmall,
                    color = PolishPrimary,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Central Animated Neural Orb
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            NeuralOrbVisualizer(
                isListening = isListening,
                isSpeaking = isSpeaking,
                isProcessing = isProcessing,
                audioLevel = audioLevel,
                onClick = { viewModel.toggleVoiceListening() },
                size = 150.dp
            )
        }

        Text(
            text = when {
                isListening -> "جاري الاستماع لصوتك... تحدث الآن بأي لهجة"
                isSpeaking -> "المساعد يتحدث ويجيب عليك..."
                isProcessing -> "الذكاء الاصطناعي يحلل طلبك ويحوله لإجراء..."
                else -> "المس الدائرة وتحدث بصوتك مباشرة"
            },
            style = MaterialTheme.typography.bodySmall,
            color = if (isListening) PolishPrimary else PolishTextSecondary,
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp
        )

        if (isListening && liveRecognizedText.isNotBlank()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "« $liveRecognizedText »",
                style = MaterialTheme.typography.bodyMedium,
                color = PolishTextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Quick Suggestion Prompts Scroll
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            quickActionPrompts.forEach { prompt ->
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = PolishSurfaceElevated,
                    border = BorderStroke(1.dp, PolishSurfaceBorder),
                    modifier = Modifier.testTag("quick_prompt_$prompt")
                ) {
                    Row(
                        modifier = Modifier
                            .background(PolishSurfaceElevated)
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = { viewModel.handleUserVoiceInput(prompt) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                        ) {
                            Text(
                                text = "✦ $prompt",
                                style = MaterialTheme.typography.labelSmall,
                                color = PolishPrimary,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Conversation & Action History List
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
                    onExecuteAgain = { action, payload ->
                        viewModel.executeAction(action, payload)
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Input & Voice Action Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = textInput,
                onValueChange = { textInput = it },
                placeholder = {
                    Text(
                        text = "اكتب أو اضغط على المايك للتحدث...",
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

            // Mic Trigger Button
            IconButton(
                onClick = { viewModel.toggleVoiceListening() },
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(PolishPrimary)
                    .testTag("mic_toggle_button")
            ) {
                Icon(
                    imageVector = if (isListening) Icons.Default.Mic else Icons.Default.MicOff,
                    contentDescription = "استماع صوتي",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
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
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(PolishPrimary)
                        .testTag("send_query_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "إرسال",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ConversationBubble(
    message: ConversationMessage,
    onExecuteAgain: (ActionType, String?) -> Unit
) {
    val isUser = message.isUser
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.Start else Arrangement.End
    ) {
        Card(
            shape = RoundedCornerShape(
                topStart = 20.dp,
                topEnd = 20.dp,
                bottomStart = if (isUser) 6.dp else 20.dp,
                bottomEnd = if (isUser) 20.dp else 6.dp
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
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isUser) "أنت (صوتياً)" else "المساعد الذكي",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isUser) PolishOnPrimaryContainer else PolishPrimary,
                        fontSize = 11.sp
                    )
                    if (!message.detectedDialect.isNullOrBlank()) {
                        Text(
                            text = message.detectedDialect,
                            style = MaterialTheme.typography.labelSmall,
                            color = PolishTextMuted,
                            fontSize = 10.sp
                        )
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = message.text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isUser) PolishOnPrimaryContainer else PolishTextPrimary,
                    fontSize = 13.sp
                )
                if (message.actionType != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = PolishSuccessContainer,
                        border = BorderStroke(1.dp, PolishSuccess.copy(alpha = 0.3f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(PolishSuccess)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "تم تنفيذ الإجراء: ${message.actionType.name}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = PolishSuccess,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 11.sp
                                )
                            }
                            Button(
                                onClick = { onExecuteAgain(message.actionType, message.actionPayload) },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = PolishPrimary),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                                    horizontal = 10.dp,
                                    vertical = 2.dp
                                ),
                                modifier = Modifier.height(28.dp)
                            ) {
                                Text("إعادة", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}
