package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.AuraApplication
import com.example.data.local.entity.ActionShortcutEntity
import com.example.data.local.entity.ActionType
import com.example.data.local.entity.AssistantConfigEntity
import com.example.data.local.entity.BehaviorInsightEntity
import com.example.data.local.entity.TelemetryLogEntity
import com.example.data.local.entity.TelemetrySeverity
import com.example.data.local.entity.TelemetryType
import com.example.data.remote.ParsedVoiceAction
import com.example.system.DeviceMetrics
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class AppPermissionInfo(
    val permission: String,
    val title: String,
    val description: String,
    val isGranted: Boolean,
    val iconName: String
)

data class ConversationMessage(
    val id: Long = System.currentTimeMillis(),
    val text: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
    val actionType: ActionType? = null,
    val actionPayload: String? = null,
    val detectedDialect: String? = null,
    val isExecuted: Boolean = false
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val auraApp = getApplication<AuraApplication>()
    private val repository = auraApp.repository
    private val telemetryManager = auraApp.telemetryManager
    val voiceEngine = auraApp.voiceSpeechEngine
    private val actionEngine = auraApp.actionExecutionEngine

    // Data from Room
    val telemetryLogs: StateFlow<List<TelemetryLogEntity>> = repository.telemetryLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val shortcuts: StateFlow<List<ActionShortcutEntity>> = repository.shortcuts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val insights: StateFlow<List<BehaviorInsightEntity>> = repository.insights
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val assistantConfig: StateFlow<AssistantConfigEntity> = repository.config
        .combine(MutableStateFlow(AssistantConfigEntity())) { config, default ->
            config ?: default
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AssistantConfigEntity())

    // Live Device Metrics
    private val _deviceMetrics = MutableStateFlow(telemetryManager.getLiveMetrics())
    val deviceMetrics: StateFlow<DeviceMetrics> = _deviceMetrics.asStateFlow()

    // Conversation & Action State
    private val _conversation = MutableStateFlow<List<ConversationMessage>>(
        listOf(
            ConversationMessage(
                text = "مرحباً بك! أنا مساعدك الذكي الذاتي (نور). يمكنك التحدث معي بأي لغة أو لهجة وسأقوم بفهم طلبك وتنفيذه فوراً دون الحاجة للمس الهاتف.",
                isUser = false
            )
        )
    )
    val conversation: StateFlow<List<ConversationMessage>> = _conversation.asStateFlow()

    private val _isProcessingAi = MutableStateFlow(false)
    val isProcessingAi: StateFlow<Boolean> = _isProcessingAi.asStateFlow()

    private val _systemStatusNotice = MutableStateFlow<String?>(null)
    val systemStatusNotice: StateFlow<String?> = _systemStatusNotice.asStateFlow()

    private val _selectedLogFilter = MutableStateFlow<TelemetryType?>(null)
    val selectedLogFilter: StateFlow<TelemetryType?> = _selectedLogFilter.asStateFlow()

    // Permission List
    private val _permissionsState = MutableStateFlow<List<AppPermissionInfo>>(emptyList())
    val permissionsState: StateFlow<List<AppPermissionInfo>> = _permissionsState.asStateFlow()

    init {
        startTelemetryRefresher()
    }

    private fun startTelemetryRefresher() {
        viewModelScope.launch(Dispatchers.IO) {
            while (true) {
                _deviceMetrics.value = telemetryManager.getLiveMetrics()
                delay(3000)
            }
        }
    }

    fun updatePermissionsState(grantedMap: Map<String, Boolean>) {
        val permissions = listOf(
            AppPermissionInfo(
                permission = android.Manifest.permission.RECORD_AUDIO,
                title = "الميكروفون والتعرف الصوتي",
                description = "للإصغاء للأوامر الصوتية وتحويل ما تريده إلى إجراءات بدون لمس.",
                isGranted = grantedMap[android.Manifest.permission.RECORD_AUDIO] == true,
                iconName = "mic"
            ),
            AppPermissionInfo(
                permission = android.Manifest.permission.READ_PHONE_STATE,
                title = "حالة الهاتف والاتصالات",
                description = "لأرشفة وتدقيق حالة المكالمات والشبكة ومراقبة الاستجابة.",
                isGranted = grantedMap[android.Manifest.permission.READ_PHONE_STATE] == true,
                iconName = "phone"
            ),
            AppPermissionInfo(
                permission = android.Manifest.permission.READ_CONTACTS,
                title = "جهات الاتصال",
                description = "لإجراء المكالمات وإرسال الرسائل الصوتية للأسماء المطلوبة فوراً.",
                isGranted = grantedMap[android.Manifest.permission.READ_CONTACTS] == true,
                iconName = "contacts"
            ),
            AppPermissionInfo(
                permission = android.Manifest.permission.POST_NOTIFICATIONS,
                title = "الإشعارات والتنبيهات",
                description = "لإرسال تنبيهات التدقيق والتحذيرات الذكية الصادرة من الداشبورد.",
                isGranted = grantedMap[android.Manifest.permission.POST_NOTIFICATIONS] == true,
                iconName = "notifications"
            ),
            AppPermissionInfo(
                permission = android.Manifest.permission.ACCESS_FINE_LOCATION,
                title = "الموقع الجغرافي والبيئة",
                description = "لتكييف الأوامر الصوتية الذكية بناءً على موقعك ونمط تحركاتك.",
                isGranted = grantedMap[android.Manifest.permission.ACCESS_FINE_LOCATION] == true,
                iconName = "location"
            )
        )
        _permissionsState.value = permissions
    }

    fun toggleVoiceListening() {
        if (voiceEngine.isListening.value) {
            voiceEngine.stopListening()
        } else {
            actionEngine.vibratePhone(50L)
            voiceEngine.startListening(
                onResult = { recognizedText ->
                    handleUserVoiceInput(recognizedText)
                },
                onError = { errorMsg ->
                    _systemStatusNotice.value = errorMsg
                }
            )
        }
    }

    fun handleUserVoiceInput(rawInput: String) {
        if (rawInput.isBlank()) return

        val userMsg = ConversationMessage(
            text = rawInput,
            isUser = true
        )
        _conversation.value = _conversation.value + userMsg

        viewModelScope.launch {
            _isProcessingAi.value = true
            try {
                val parsed = repository.processVoiceCommand(rawInput)
                val assistantMsg = ConversationMessage(
                    text = parsed.responseSpeechText,
                    isUser = false,
                    actionType = parsed.actionType,
                    actionPayload = parsed.actionPayload,
                    detectedDialect = parsed.detectedDialect
                )
                _conversation.value = _conversation.value + assistantMsg

                // Speak response if enabled
                val currentConfig = assistantConfig.value
                if (currentConfig.voiceFeedbackEnabled) {
                    voiceEngine.speak(
                        text = parsed.responseSpeechText,
                        pitch = currentConfig.ttsPitch,
                        speed = currentConfig.ttsSpeed
                    )
                }

                // Execute action automatically if parsed
                parsed.actionType?.let { action ->
                    executeAction(action, parsed.actionPayload)
                }
            } catch (e: Exception) {
                _systemStatusNotice.value = "حدث خطأ في معالجة الأمر: ${e.message}"
            } finally {
                _isProcessingAi.value = false
            }
        }
    }

    fun executeAction(actionType: ActionType, payload: String? = null, shortcutId: Long? = null) {
        actionEngine.executeAction(actionType, payload) { feedback ->
            _systemStatusNotice.value = feedback
            viewModelScope.launch {
                repository.logTelemetry(
                    type = TelemetryType.TOUCH_GESTURE,
                    title = "تنفيذ إجراء: $actionType",
                    description = feedback,
                    severity = TelemetrySeverity.OPTIMAL,
                    aiAudited = true
                )
                shortcutId?.let { repository.recordShortcutExecution(it) }
            }
        }
    }

    fun runAiAudit() {
        viewModelScope.launch {
            _isProcessingAi.value = true
            actionEngine.vibratePhone(100L)
            try {
                val result = repository.runAiAuditAndArchiving()
                _systemStatusNotice.value = "اكتمل التدقيق الشامل بواسطة الذكاء الاصطناعي بنجاح."
                val auditMsg = ConversationMessage(
                    text = "تقرير التدقيق الذكي:\n${result.healthSummary}",
                    isUser = false,
                    actionType = ActionType.AI_SUMMARIZE_ACTIVITY
                )
                _conversation.value = _conversation.value + auditMsg
                if (assistantConfig.value.voiceFeedbackEnabled) {
                    voiceEngine.speak(result.healthSummary)
                }
            } catch (e: Exception) {
                _systemStatusNotice.value = "تعذر إكمال التدقيق: ${e.message}"
            } finally {
                _isProcessingAi.value = false
            }
        }
    }

    fun updateAssistantConfig(newConfig: AssistantConfigEntity) {
        viewModelScope.launch {
            repository.updateConfig(newConfig)
            _systemStatusNotice.value = "تم حفظ إعدادات المساعد (${newConfig.assistantName}) وتحديث نمط الاستماع."
        }
    }

    fun addCustomShortcut(title: String, triggerPhrase: String, actionType: ActionType, payload: String) {
        viewModelScope.launch {
            val shortcut = ActionShortcutEntity(
                title = title,
                triggerVoicePhrase = triggerPhrase,
                actionType = actionType,
                payload = payload
            )
            repository.saveShortcut(shortcut)
            _systemStatusNotice.value = "تمت إضافة الاختصار الصوتي بنجاح."
        }
    }

    fun deleteShortcut(id: Long) {
        viewModelScope.launch {
            repository.deleteShortcut(id)
            _systemStatusNotice.value = "تم حذف الاختصار."
        }
    }

    fun setLogFilter(filter: TelemetryType?) {
        _selectedLogFilter.value = filter
    }

    fun clearStatusNotice() {
        _systemStatusNotice.value = null
    }

    override fun onCleared() {
        super.onCleared()
        voiceEngine.destroy()
    }
}
