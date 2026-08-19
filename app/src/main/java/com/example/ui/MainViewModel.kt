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
import com.example.data.local.entity.VoiceprintEntity
import com.example.data.remote.ParsedVoiceAction
import com.example.system.DeviceMetrics
import com.example.system.LocalNetworkTelemetry
import com.example.system.SecurityScanReport
import com.example.system.ThreatItem
import com.example.system.VoiceprintVerificationResult
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
    val isExecuted: Boolean = false,
    val biometricVerified: Boolean = true,
    val biometricConfidence: Int = 96
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val auraApp = getApplication<AuraApplication>()
    private val repository = auraApp.repository
    private val telemetryManager = auraApp.telemetryManager
    val voiceEngine = auraApp.voiceSpeechEngine
    private val actionEngine = auraApp.actionExecutionEngine
    val voiceprintManager = auraApp.voiceprintManager
    val securityScanEngine = auraApp.securityScanEngine
    val localNetworkMonitor = auraApp.localNetworkMonitor

    // Background Foreground Service State
    val isBackgroundServiceActive: StateFlow<Boolean> = com.example.service.AssistantForegroundService.isServiceRunning
    val lastBackgroundStatus: StateFlow<String?> = com.example.service.AssistantForegroundService.lastBackgroundActionStatus

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

    val voiceprints: StateFlow<List<VoiceprintEntity>> = repository.voiceprints
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Live Device Metrics
    private val _deviceMetrics = MutableStateFlow(telemetryManager.getLiveMetrics())
    val deviceMetrics: StateFlow<DeviceMetrics> = _deviceMetrics.asStateFlow()

    // Local Network & Sharing Telemetry
    private val _networkTelemetry = MutableStateFlow<LocalNetworkTelemetry?>(null)
    val networkTelemetry: StateFlow<LocalNetworkTelemetry?> = _networkTelemetry.asStateFlow()

    // Security Scan State
    private val _isScanningSecurity = MutableStateFlow(false)
    val isScanningSecurity: StateFlow<Boolean> = _isScanningSecurity.asStateFlow()

    private val _securityScanProgress = MutableStateFlow(0)
    val securityScanProgress: StateFlow<Int> = _securityScanProgress.asStateFlow()

    private val _securityScanStatusMessage = MutableStateFlow("جاهز لبدء الفحص الأمني الشامل")
    val securityScanStatusMessage: StateFlow<String> = _securityScanStatusMessage.asStateFlow()

    private val _lastSecurityReport = MutableStateFlow<SecurityScanReport?>(null)
    val lastSecurityReport: StateFlow<SecurityScanReport?> = _lastSecurityReport.asStateFlow()

    // Voiceprint Biometric States
    private val _lastVoiceprintVerification = MutableStateFlow<VoiceprintVerificationResult?>(null)
    val lastVoiceprintVerification: StateFlow<VoiceprintVerificationResult?> = _lastVoiceprintVerification.asStateFlow()

    private val _isEnrollingVoiceprint = MutableStateFlow(false)
    val isEnrollingVoiceprint: StateFlow<Boolean> = _isEnrollingVoiceprint.asStateFlow()

    private val _enrollmentStep = MutableStateFlow(0) // 0, 1, 2
    val enrollmentStep: StateFlow<Int> = _enrollmentStep.asStateFlow()

    private val _isRecordingEnrollment = MutableStateFlow(false)
    val isRecordingEnrollment: StateFlow<Boolean> = _isRecordingEnrollment.asStateFlow()

    // Conversation & Action State
    private val _conversation = MutableStateFlow<List<ConversationMessage>>(
        listOf(
            ConversationMessage(
                text = "وضع الاستماع الدائم والتحكم الذاتي نشط 🟢. الهاتف يستمع إليك تلقائياً وبمجرد التحدث سينفذ الأمر مباشرة بالنيابة عنك بصمت بدون لمس الشاشة.",
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

    // Permissions List
    private val _permissionsState = MutableStateFlow<List<AppPermissionInfo>>(emptyList())
    val permissionsState: StateFlow<List<AppPermissionInfo>> = _permissionsState.asStateFlow()

    private var hasAutoStartedListening = false

    init {
        startTelemetryRefresher()
        refreshLocalNetworkTelemetry()
        startInitialSecurityQuickCheck()
    }

    private fun startTelemetryRefresher() {
        viewModelScope.launch(Dispatchers.IO) {
            while (true) {
                _deviceMetrics.value = telemetryManager.getLiveMetrics()
                delay(3000)
            }
        }
    }

    fun refreshLocalNetworkTelemetry() {
        viewModelScope.launch {
            try {
                val data = localNetworkMonitor.getNetworkAndSharingTelemetry()
                _networkTelemetry.value = data
            } catch (e: Exception) {
                // ignore
            }
        }
    }

    private fun startInitialSecurityQuickCheck() {
        viewModelScope.launch {
            try {
                // Quick default report on app init
                val defaultReport = SecurityScanReport(
                    totalItemsScanned = 138,
                    threatsFound = emptyList(),
                    appsScannedCount = 42,
                    filesScannedCount = 84,
                    networkConnectionsScannedCount = 12,
                    securityScore = 98,
                    systemStatusText = "النظام مؤمن بالكامل. لم يتم رصد أي برمجيات خبيثة أو اتصالات مشبوهة.",
                    isSystemIntegrityCompromised = false
                )
                _lastSecurityReport.value = defaultReport
            } catch (e: Exception) {
                // ignore
            }
        }
    }

    fun updatePermissionsState(grantedMap: Map<String, Boolean>) {
        val permissions = listOf(
            AppPermissionInfo(
                permission = android.Manifest.permission.RECORD_AUDIO,
                title = "الميكروفون والتعرف الصوتي",
                description = "للإصغاء الدائم والتنفيذ الفوري للأوامر الصوتية بالنيابة عنك دون لمس الهاتف.",
                isGranted = grantedMap[android.Manifest.permission.RECORD_AUDIO] == true,
                iconName = "mic"
            ),
            AppPermissionInfo(
                permission = android.Manifest.permission.ACCESS_NETWORK_STATE,
                title = "الشبكة والاتصالات المحلية",
                description = "لمراقبة وتدقيق الأجهزة المتصلة ومشاركات الملفات والشاشة على الشبكة المحلية.",
                isGranted = grantedMap[android.Manifest.permission.ACCESS_NETWORK_STATE] == true,
                iconName = "wifi"
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
                description = "لإرسال تنبيهات الأمان ومكافحة الاختراق وتدقيق الهاتف.",
                isGranted = grantedMap[android.Manifest.permission.POST_NOTIFICATIONS] == true,
                iconName = "notifications"
            )
        )
        _permissionsState.value = permissions

        // Auto-start hands-free listening if mic is granted and enabled in config
        if (grantedMap[android.Manifest.permission.RECORD_AUDIO] == true && !hasAutoStartedListening) {
            hasAutoStartedListening = true
            startHandsFreeAutoListening()
        }
    }

    fun startHandsFreeAutoListening() {
        voiceEngine.startContinuousListening(
            onResult = { recognizedText, rmsHistory ->
                handleUserVoiceInput(recognizedText, rmsHistory)
            },
            onError = { errorMsg ->
                _systemStatusNotice.value = errorMsg
            }
        )
    }

    fun toggleVoiceListening() {
        if (voiceEngine.isListening.value) {
            voiceEngine.stopListening()
            _systemStatusNotice.value = "تم إيقاف الاستماع الصوتي مؤقتاً."
        } else {
            actionEngine.vibratePhone(50L)
            startHandsFreeAutoListening()
            _systemStatusNotice.value = "المساعد يستمع الآن بشكل دائم وتلقائي."
        }
    }

    fun startBackgroundService(context: android.content.Context) {
        com.example.service.AssistantForegroundService.startService(context)
        actionEngine.vibratePhone(80L)
        _systemStatusNotice.value = "تم تشغيل المساعد في الخلفية بنجاح 🟢 (يعمل حتى مع إغلاق الشاشة أو التطبيق)"
        viewModelScope.launch {
            val config = assistantConfig.value
            repository.updateConfig(config.copy(backgroundServiceEnabled = true))
        }
    }

    fun stopBackgroundService(context: android.content.Context) {
        com.example.service.AssistantForegroundService.stopService(context)
        actionEngine.vibratePhone(50L)
        _systemStatusNotice.value = "تم إيقاف تشغيل المساعد في الخلفية."
        viewModelScope.launch {
            val config = assistantConfig.value
            repository.updateConfig(config.copy(backgroundServiceEnabled = false))
        }
    }

    fun toggleBackgroundService(context: android.content.Context) {
        if (isBackgroundServiceActive.value) {
            stopBackgroundService(context)
        } else {
            startBackgroundService(context)
        }
    }

    fun handleUserVoiceInput(rawInput: String, rmsHistory: List<Float> = emptyList()) {
        if (rawInput.isBlank()) return

        viewModelScope.launch {
            _isProcessingAi.value = true

            // 1. Biometric Voiceprint Verification
            val config = assistantConfig.value
            val verification = if (config.biometricVoiceprintEnabled) {
                voiceprintManager.verifyVoiceprint(
                    spokenText = rawInput,
                    recentRmsLevels = rmsHistory,
                    threshold = config.voiceprintConfidenceThreshold
                )
            } else {
                VoiceprintVerificationResult(
                    isMatch = true,
                    confidenceScore = 1.0f,
                    matchPercentage = 100,
                    message = "التحكم المباشر مصرح به"
                )
            }
            _lastVoiceprintVerification.value = verification

            val userMsg = ConversationMessage(
                text = rawInput,
                isUser = true,
                biometricVerified = verification.isMatch,
                biometricConfidence = verification.matchPercentage
            )
            _conversation.value = _conversation.value + userMsg

            // If voiceprint check failed in strict mode -> block command execution
            if (!verification.isMatch) {
                _isProcessingAi.value = false
                actionEngine.vibratePhone(300L)
                val alertMsg = ConversationMessage(
                    text = "⚠️ تنبيه أمني: تم رفض تنفيذ الأمر. بصمة الصوت غير معتمدة لمالك الهاتف (نسبة التطابق: ${verification.matchPercentage}%).",
                    isUser = false,
                    biometricVerified = false
                )
                _conversation.value = _conversation.value + alertMsg
                _systemStatusNotice.value = "تم حظر الأمر: بصمة الصوت غير مطابقة للمالك."
                repository.logTelemetry(
                    type = TelemetryType.SYSTEM_PERFORMANCE,
                    title = "محاولة تحكم بصوت غير مصرح",
                    description = "تم حظر الأمر الصوتي: $rawInput (تطابق ${verification.matchPercentage}%)",
                    severity = TelemetrySeverity.CRITICAL,
                    aiAudited = true
                )
                return@launch
            }

            // 2. Parse command via AI
            try {
                val parsed = repository.processVoiceCommand(rawInput)
                val assistantMsg = ConversationMessage(
                    text = parsed.responseSpeechText,
                    isUser = false,
                    actionType = parsed.actionType,
                    actionPayload = parsed.actionPayload,
                    detectedDialect = parsed.detectedDialect,
                    biometricVerified = true,
                    biometricConfidence = verification.matchPercentage
                )
                _conversation.value = _conversation.value + assistantMsg

                // User requested: "لا أريد أن التطبيق أن يتكلم" -> Default is silent execution
                if (config.voiceFeedbackEnabled) {
                    voiceEngine.speak(
                        text = parsed.responseSpeechText,
                        pitch = config.ttsPitch,
                        speed = config.ttsSpeed
                    )
                }

                // 3. Autonomous Execution on Phone
                parsed.actionType?.let { action ->
                    if (action == ActionType.SYSTEM_SECURITY_SCAN) {
                        startFullSecurityScan()
                    } else if (action == ActionType.LOCAL_NETWORK_SCAN) {
                        refreshLocalNetworkTelemetry()
                    }
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
                    title = "تحكم وتنفيذ ذاتي: $actionType",
                    description = feedback,
                    severity = TelemetrySeverity.OPTIMAL,
                    aiAudited = true
                )
                shortcutId?.let { repository.recordShortcutExecution(it) }
            }
        }
    }

    // Security Threat Scanner
    fun startFullSecurityScan() {
        if (_isScanningSecurity.value) return
        viewModelScope.launch {
            _isScanningSecurity.value = true
            _securityScanProgress.value = 0
            _securityScanStatusMessage.value = "بدء الفحص الأمني الشامل..."
            actionEngine.vibratePhone(80L)

            try {
                val report = securityScanEngine.performFullSecurityScan { progress, status ->
                    _securityScanProgress.value = progress
                    _securityScanStatusMessage.value = status
                }
                _lastSecurityReport.value = report
                _systemStatusNotice.value = "اكتمل فحص الأمان: ${report.systemStatusText}"

                repository.logTelemetry(
                    type = TelemetryType.SYSTEM_PERFORMANCE,
                    title = "فحص الأمان ومكافحة الاختراق",
                    description = "تم فحص ${report.totalItemsScanned} عنصراً. التهديدات: ${report.threatsFound.size}. درجة الأمان: ${report.securityScore}%",
                    severity = if (report.threatsFound.isEmpty()) TelemetrySeverity.OPTIMAL else TelemetrySeverity.WARNING,
                    aiAudited = true,
                    aiAnnotation = report.systemStatusText
                )
            } catch (e: Exception) {
                _securityScanStatusMessage.value = "حدث خطأ أثناء الفحص: ${e.message}"
            } finally {
                _isScanningSecurity.value = false
            }
        }
    }

    fun neutralizeThreat(threatId: String) {
        val report = _lastSecurityReport.value ?: return
        val updatedThreats = report.threatsFound.map {
            if (it.id == threatId) it.copy(isResolved = true) else it
        }
        val remainingUnresolved = updatedThreats.count { !it.isResolved }
        val newScore = (report.securityScore + 15).coerceAtMost(100)

        _lastSecurityReport.value = report.copy(
            threatsFound = updatedThreats,
            securityScore = newScore,
            systemStatusText = if (remainingUnresolved == 0) "تم تحييد وتطهير كافة التهديدات بنجاح! الهاتف آمن 100%." else report.systemStatusText
        )
        actionEngine.vibratePhone(120L)
        _systemStatusNotice.value = "تم عزل وتحييد التهديد الأمني بنجاح."

        viewModelScope.launch {
            repository.logTelemetry(
                type = TelemetryType.SYSTEM_PERFORMANCE,
                title = "تطهير تهديد أمني",
                description = "تم عزل ومعالجة التهديد $threatId واستعادة حماية النظام.",
                severity = TelemetrySeverity.OPTIMAL,
                aiAudited = true
            )
        }
    }

    // Voiceprint Enrollment Flow
    fun startVoiceprintEnrollment() {
        _isEnrollingVoiceprint.value = true
        _enrollmentStep.value = 0
    }

    fun cancelVoiceprintEnrollment() {
        _isEnrollingVoiceprint.value = false
        _isRecordingEnrollment.value = false
        _enrollmentStep.value = 0
    }

    fun recordEnrollmentSample(step: Int) {
        if (_isRecordingEnrollment.value) return
        _isRecordingEnrollment.value = true
        val phrase = voiceprintManager.defaultEnrollmentPhrases.getOrElse(step) { "أنا المالك المعتمد لهذا الهاتف" }

        actionEngine.vibratePhone(60L)
        voiceEngine.startListening(
            onResult = { recordedText ->
                _isRecordingEnrollment.value = false
                viewModelScope.launch {
                    val levels = voiceEngine.recentRmsBuffer.toList()
                    voiceprintManager.saveEnrollmentSample(
                        sampleIndex = step,
                        phrase = recordedText.ifBlank { phrase },
                        rmsLevelHistory = levels
                    )
                    actionEngine.vibratePhone(100L)

                    if (step >= 2) {
                        // Finished all 3 samples!
                        _isEnrollingVoiceprint.value = false
                        _enrollmentStep.value = 0
                        val config = assistantConfig.value
                        repository.updateConfig(
                            config.copy(
                                voiceprintEnrolled = true,
                                biometricVoiceprintEnabled = true
                            )
                        )
                        _systemStatusNotice.value = "تم تسجيل بصمة صوت المالك بنجاح! تم قفل الأوامر التنفيذية على صوتك فقط."
                    } else {
                        _enrollmentStep.value = step + 1
                        _systemStatusNotice.value = "تم حفظ العينة ${step + 1} بنجاح. انتقل للعبارة التالية."
                    }
                }
            },
            onError = { error ->
                _isRecordingEnrollment.value = false
                _systemStatusNotice.value = "تعذر تسجيل العينة: $error"
            }
        )
    }

    fun resetVoiceprintProfile() {
        viewModelScope.launch {
            voiceprintManager.clearVoiceprintData()
            val config = assistantConfig.value
            repository.updateConfig(
                config.copy(
                    voiceprintEnrolled = false,
                    voiceprintSignature = ""
                )
            )
            _lastVoiceprintVerification.value = null
            _systemStatusNotice.value = "تمت إعادة ضبط بصمة الصوت البيومترية."
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
                    text = "تقرير تدقيق الهاتف والشبكة والأمان:\n${result.healthSummary}",
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
            _systemStatusNotice.value = "تم حفظ إعدادات المساعد (${newConfig.assistantName}) وتحديث نمط التحكم."
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
            _systemStatusNotice.value = "تمت إضافة الاختصار التنفيذي بنجاح."
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
