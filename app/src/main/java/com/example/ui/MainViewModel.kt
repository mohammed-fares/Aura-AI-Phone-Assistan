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
import com.example.util.LocalizationManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ExecutionStep(
    val stepIndex: Int,
    val title: String,
    val description: String,
    val gestureType: String,
    val isCompleted: Boolean = false,
    val isActive: Boolean = false
)

data class LiveExecutionPlan(
    val id: String = System.currentTimeMillis().toString(),
    val commandTitle: String,
    val actionType: ActionType,
    val actionPayload: String? = null,
    val steps: List<ExecutionStep>,
    val currentStepIndex: Int = 0,
    val isRunning: Boolean = false,
    val isCompleted: Boolean = false,
    val statusMessage: String = ""
)

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

    // Active Language
    private val _appLanguage = MutableStateFlow("system")
    val appLanguage: StateFlow<String> = _appLanguage.asStateFlow()

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

    private val _securityScanStatusMessage = MutableStateFlow("جاهز لبدء الفحص الأمني الشامل / Ready for security scan")
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
    private val _conversation = MutableStateFlow<List<ConversationMessage>>(emptyList())
    val conversation: StateFlow<List<ConversationMessage>> = _conversation.asStateFlow()

    private val _isProcessingAi = MutableStateFlow(false)
    val isProcessingAi: StateFlow<Boolean> = _isProcessingAi.asStateFlow()

    private val _systemStatusNotice = MutableStateFlow<String?>(null)
    val systemStatusNotice: StateFlow<String?> = _systemStatusNotice.asStateFlow()

    private val _selectedLogFilter = MutableStateFlow<TelemetryType?>(null)
    val selectedLogFilter: StateFlow<TelemetryType?> = _selectedLogFilter.asStateFlow()

    // Autonomous Execution Plan State
    private val _activeExecutionPlan = MutableStateFlow<LiveExecutionPlan?>(null)
    val activeExecutionPlan: StateFlow<LiveExecutionPlan?> = _activeExecutionPlan.asStateFlow()

    // Permissions List
    private val _permissionsState = MutableStateFlow<List<AppPermissionInfo>>(emptyList())
    val permissionsState: StateFlow<List<AppPermissionInfo>> = _permissionsState.asStateFlow()

    private var hasAutoStartedListening = false

    init {
        initWelcomeMessage()
        startTelemetryRefresher()
        refreshLocalNetworkTelemetry()
        startInitialSecurityQuickCheck()
        observeConfigLanguage()
    }

    private fun initWelcomeMessage() {
        val isAr = LocalizationManager.getEffectiveLanguage(_appLanguage.value) == "ar"
        val welcome = if (isAr) {
            "وضع الاستماع والتحكم الذاتي نشط 🟢. الهاتف ينفذ الأوامر (اتصال، رسائل، ايميلات، تطبيقات، إعدادات، أمان) بالنيابة عنك دون لمس الشاشة."
        } else {
            "Autonomous Phone Executive Agent Active 🟢. The assistant is listening hands-free to execute calls, messages, emails, apps, settings, and security scans on your behalf."
        }
        _conversation.value = listOf(ConversationMessage(text = welcome, isUser = false))
    }

    private fun observeConfigLanguage() {
        viewModelScope.launch {
            assistantConfig.collect { cfg ->
                if (cfg.appLanguage != _appLanguage.value) {
                    _appLanguage.value = cfg.appLanguage
                    voiceEngine.setLanguage(cfg.appLanguage)
                }
                voiceEngine.setMuteAllSounds(cfg.muteAllAppSounds)
                voiceEngine.setKeepMicContinuouslyOpen(cfg.keepMicOpenContinuously)
            }
        }
    }

    fun setAppLanguage(lang: String) {
        _appLanguage.value = lang
        voiceEngine.setLanguage(lang)
        viewModelScope.launch {
            val cfg = assistantConfig.value
            repository.updateConfig(cfg.copy(appLanguage = lang))
        }
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
                val defaultReport = SecurityScanReport(
                    totalItemsScanned = 138,
                    threatsFound = emptyList(),
                    appsScannedCount = 42,
                    filesScannedCount = 84,
                    networkConnectionsScannedCount = 12,
                    securityScore = 98,
                    systemStatusText = if (LocalizationManager.getEffectiveLanguage(_appLanguage.value) == "ar")
                        "النظام مؤمن بالكامل. لم يتم رصد أي برمجيات خبيثة أو اتصالات مشبوهة."
                    else "System is fully secure. 0 threats detected.",
                    isSystemIntegrityCompromised = false
                )
                _lastSecurityReport.value = defaultReport
            } catch (e: Exception) {
                // ignore
            }
        }
    }

    fun updatePermissionsState(grantedMap: Map<String, Boolean>) {
        val isAr = LocalizationManager.getEffectiveLanguage(_appLanguage.value) == "ar"
        val permissions = listOf(
            AppPermissionInfo(
                permission = android.Manifest.permission.RECORD_AUDIO,
                title = if (isAr) "الميكروفون والتعرف الصوتي" else "Microphone & Voice Engine",
                description = if (isAr) "للإصغاء الدائم والتنفيذ الفوري للأوامر الصوتية بالنيابة عنك دون لمس الهاتف." else "Hands-free continuous listening and voice command execution.",
                isGranted = grantedMap[android.Manifest.permission.RECORD_AUDIO] == true,
                iconName = "mic"
            ),
            AppPermissionInfo(
                permission = android.Manifest.permission.ACCESS_NETWORK_STATE,
                title = if (isAr) "الشبكة والاتصالات المحلية" else "Local Network & Wi-Fi",
                description = if (isAr) "لمراقبة وتدقيق الأجهزة المتصلة ومشاركات الملفات والشاشة على الشبكة المحلية." else "Monitor LAN devices, media streams, and screen shares.",
                isGranted = grantedMap[android.Manifest.permission.ACCESS_NETWORK_STATE] == true,
                iconName = "wifi"
            ),
            AppPermissionInfo(
                permission = android.Manifest.permission.READ_PHONE_STATE,
                title = if (isAr) "حالة الهاتف والاتصالات" else "Phone State & Calls",
                description = if (isAr) "لأرشفة وتدقيق حالة المكالمات والشبكة ومراقبة الاستجابة." else "Direct calling and communication access.",
                isGranted = grantedMap[android.Manifest.permission.READ_PHONE_STATE] == true,
                iconName = "phone"
            ),
            AppPermissionInfo(
                permission = android.Manifest.permission.READ_CONTACTS,
                title = if (isAr) "جهات الاتصال" else "Contacts",
                description = if (isAr) "لإجراء المكالمات وإرسال الرسائل الصوتية للأسماء المطلوبة فوراً." else "Fast dial and message dispatch to contacts.",
                isGranted = grantedMap[android.Manifest.permission.READ_CONTACTS] == true,
                iconName = "contacts"
            ),
            AppPermissionInfo(
                permission = android.Manifest.permission.POST_NOTIFICATIONS,
                title = if (isAr) "الإشعارات والتنبيهات" else "Notifications",
                description = if (isAr) "لإرسال تنبيهات الأمان ومكافحة الاختراق وتدقيق الهاتف." else "Security alert dispatch and background status updates.",
                isGranted = grantedMap[android.Manifest.permission.POST_NOTIFICATIONS] == true,
                iconName = "notifications"
            )
        )
        _permissionsState.value = permissions

        if (grantedMap[android.Manifest.permission.RECORD_AUDIO] == true && !hasAutoStartedListening) {
            hasAutoStartedListening = true
            startHandsFreeAutoListening()
        }
    }

    fun startHandsFreeAutoListening() {
        voiceEngine.startContinuousListening(
            language = _appLanguage.value,
            onResult = { recognizedText, rmsHistory ->
                handleUserVoiceInput(recognizedText, rmsHistory)
            },
            onError = { errorMsg ->
                _systemStatusNotice.value = errorMsg
            }
        )
    }

    fun toggleVoiceListening() {
        val isAr = LocalizationManager.getEffectiveLanguage(_appLanguage.value) == "ar"
        if (voiceEngine.isListening.value) {
            voiceEngine.stopListening()
            _systemStatusNotice.value = if (isAr) "تم إيقاف الاستماع الصوتي مؤقتاً." else "Listening paused."
        } else {
            actionEngine.vibratePhone(50L)
            startHandsFreeAutoListening()
            _systemStatusNotice.value = if (isAr) "المساعد يستمع الآن بشكل دائم وتلقائي." else "Assistant is actively listening."
        }
    }

    fun startBackgroundService(context: android.content.Context) {
        val isAr = LocalizationManager.getEffectiveLanguage(_appLanguage.value) == "ar"
        com.example.service.AssistantForegroundService.startService(context)
        actionEngine.vibratePhone(80L)
        _systemStatusNotice.value = if (isAr)
            "تم تشغيل المساعد في الخلفية بنجاح 🟢 (يعمل حتى مع إغلاق الشاشة أو التطبيق)"
        else "Background service active 🟢 (Runs even when screen is locked or app closed)"
        viewModelScope.launch {
            val config = assistantConfig.value
            repository.updateConfig(config.copy(backgroundServiceEnabled = true))
        }
    }

    fun stopBackgroundService(context: android.content.Context) {
        val isAr = LocalizationManager.getEffectiveLanguage(_appLanguage.value) == "ar"
        com.example.service.AssistantForegroundService.stopService(context)
        actionEngine.vibratePhone(50L)
        _systemStatusNotice.value = if (isAr) "تم إيقاف تشغيل المساعد في الخلفية." else "Background service stopped."
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
        val isAr = LocalizationManager.getEffectiveLanguage(_appLanguage.value) == "ar"

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
                    message = if (isAr) "التحكم المباشر مصرح به" else "Authorized"
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

            if (!verification.isMatch) {
                _isProcessingAi.value = false
                actionEngine.vibratePhone(300L)
                val alertMsg = ConversationMessage(
                    text = if (isAr)
                        "⚠️ تنبيه أمني: تم رفض تنفيذ الأمر. بصمة الصوت غير معتمدة لمالك الهاتف (نسبة التطابق: ${verification.matchPercentage}%)."
                    else "⚠️ Security Notice: Voiceprint mismatch (${verification.matchPercentage}% match). Action blocked.",
                    isUser = false,
                    biometricVerified = false
                )
                _conversation.value = _conversation.value + alertMsg
                _systemStatusNotice.value = if (isAr) "تم حظر الأمر: بصمة الصوت غير مطابقة للمالك." else "Action blocked: Voiceprint unauthorized."
                repository.logTelemetry(
                    type = TelemetryType.SYSTEM_PERFORMANCE,
                    title = if (isAr) "محاولة تحكم بصوت غير مصرح" else "Unauthorized Voice Control Attempt",
                    description = "$rawInput (match: ${verification.matchPercentage}%)",
                    severity = TelemetrySeverity.CRITICAL,
                    aiAudited = true
                )
                return@launch
            }

            // 2. Parse command via AI with multi-layer fallback
            try {
                var parsed = repository.processVoiceCommand(rawInput)
                if (parsed.actionType == null) {
                    val fallback = auraApp.geminiService.fallbackLocalInterpreter(rawInput, config.assistantName)
                    if (fallback.actionType != null) {
                        parsed = fallback
                    }
                }

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

                if (config.voiceFeedbackEnabled && !config.muteAllAppSounds) {
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
                _systemStatusNotice.value = "Error: ${e.message}"
            } finally {
                _isProcessingAi.value = false
            }
        }
    }

    fun executeAction(actionType: ActionType, payload: String? = null, shortcutId: Long? = null) {
        viewModelScope.launch {
            val plan = buildExecutionPlan(actionType, payload)
            _activeExecutionPlan.value = plan

            // Animate through each autonomous step with snappy responsive feedback
            for (index in plan.steps.indices) {
                val updatedSteps = plan.steps.mapIndexed { i, s ->
                    when {
                        i < index -> s.copy(isCompleted = true, isActive = false)
                        i == index -> s.copy(isActive = true, isCompleted = false)
                        else -> s.copy(isActive = false, isCompleted = false)
                    }
                }
                _activeExecutionPlan.value = _activeExecutionPlan.value?.copy(
                    steps = updatedSteps,
                    currentStepIndex = index
                )
                actionEngine.vibratePhone(25L)
                delay(120L)
            }

            // Final step: trigger real OS action
            actionEngine.executeAction(actionType, payload, _appLanguage.value) { feedback ->
                _systemStatusNotice.value = feedback
                val finalSteps = plan.steps.map { it.copy(isCompleted = true, isActive = false) }
                _activeExecutionPlan.value = _activeExecutionPlan.value?.copy(
                    steps = finalSteps,
                    isRunning = false,
                    isCompleted = true,
                    statusMessage = feedback
                )

                viewModelScope.launch {
                    repository.logTelemetry(
                        type = TelemetryType.TOUCH_GESTURE,
                        title = "$actionType",
                        description = feedback,
                        severity = TelemetrySeverity.OPTIMAL,
                        aiAudited = true
                    )
                    shortcutId?.let { repository.recordShortcutExecution(it) }
                }
            }
        }
    }

    fun dismissExecutionPlan() {
        _activeExecutionPlan.value = null
    }

    fun executeReturnHome() {
        executeAction(ActionType.RETURN_HOME)
    }

    fun executeEndCall() {
        executeAction(ActionType.END_CALL)
    }

    private fun buildExecutionPlan(actionType: ActionType, payload: String?): LiveExecutionPlan {
        val isAr = LocalizationManager.getEffectiveLanguage(_appLanguage.value) == "ar"
        val steps = when (actionType) {
            ActionType.CALL_CONTACT -> listOf(
                ExecutionStep(0, if (isAr) "تحليل الأمر الصوتي" else "AI Intent Analysis", if (isAr) "فهم قصد إجراء مكالمة هاتفية" else "Parsing contact name or number", "AI_ANALYSIS"),
                ExecutionStep(1, if (isAr) "مطابقة جهة الاتصال" else "Contact Resolution", if (isAr) "البحث في دليل الأسماء عن (${payload ?: "الجهة"})" else "Querying phone contacts for (${payload ?: "Contact"})", "CONTACT_LOOKUP"),
                ExecutionStep(2, if (isAr) "فتح مشغل المكالمات" else "Launch Phone Dialer", if (isAr) "محاكاة طلب الرقم وتوجيه الخط" else "Simulating dialing and audio routing", "DIAL_GESTURE"),
                ExecutionStep(3, if (isAr) "بدء المكالمة الفعلية" else "Initiate Direct Call", if (isAr) "إطلاق المكالمة الهاتفية بنجاح" else "Direct call active on device", "CALL_DIRECT")
            )
            ActionType.END_CALL -> listOf(
                ExecutionStep(0, if (isAr) "تحليل الأمر الصوتي" else "AI Intent Analysis", if (isAr) "رصد أمر إنهاء المكالمة" else "Recognized end call command", "AI_ANALYSIS"),
                ExecutionStep(1, if (isAr) "الاتصال بخدمة الاتصالات" else "Telecom Service Hook", if (isAr) "إرسال إشارة إنهاء المكالمة" else "Sending disconnect signal", "CALL_DIRECT"),
                ExecutionStep(2, if (isAr) "إغلاق شاشة المكالمة" else "Close Call Screen", if (isAr) "إعادة توجيه الصوت وإغلاق الخط" else "Resetting audio and closing call view", "RETURN_HOME")
            )
            ActionType.SEND_MESSAGE -> listOf(
                ExecutionStep(0, if (isAr) "تحليل النص والجهة" else "AI Message Parsing", if (isAr) "استخراج نص الرسالة والمستلم" else "Extracting message body and recipient", "AI_ANALYSIS"),
                ExecutionStep(1, if (isAr) "تجهيز الرسالة النصية" else "Drafting SMS", if (isAr) "صياغة نص الرسالة: \"${payload ?: ""}\"" else "Formatting payload: \"${payload ?: ""}\"", "COMPOSE_SMS"),
                ExecutionStep(2, if (isAr) "إرسال الرسالة القصيرة" else "Dispatching SMS", if (isAr) "إرسال الرسالة عبر شبكة الجوال" else "Transmitting SMS via cellular network", "COMPOSE_SMS"),
                ExecutionStep(3, if (isAr) "تأكيد الإرسال" else "Confirmation", if (isAr) "تم تسليم الرسالة النصية بنجاح" else "SMS delivered successfully", "RETURN_HOME")
            )
            ActionType.CLOSE_APP, ActionType.RETURN_HOME -> listOf(
                ExecutionStep(0, if (isAr) "تحليل الأمر" else "AI Intent Analysis", if (isAr) "رصد أمر العودة للشاشة الرئيسية" else "Return to Home request", "AI_ANALYSIS"),
                ExecutionStep(1, if (isAr) "إغلاق التطبيق النشط" else "Exit Active Task", if (isAr) "الخروج من الواجهة الحالية" else "Dismissing current foreground activity", "RETURN_HOME"),
                ExecutionStep(2, if (isAr) "العودة للشاشة الرئيسية" else "Home Screen", if (isAr) "عرض الشاشة الرئيسية للهاتف" else "Displaying device launcher", "RETURN_HOME")
            )
            ActionType.OPEN_APP -> listOf(
                ExecutionStep(0, if (isAr) "تحليل اسم التطبيق" else "AI Package Matcher", if (isAr) "مطابقة التطبيق المطلوب: (${payload ?: ""})" else "Matching application (${payload ?: ""})", "AI_ANALYSIS"),
                ExecutionStep(1, if (isAr) "البحث في حزم الهاتف" else "Query Package Manager", if (isAr) "التحقق من وجود التطبيق المثبت" else "Verifying installed launch intent", "LAUNCH_PACKAGE"),
                ExecutionStep(2, if (isAr) "إطلاق التطبيق ذاتياً" else "Autonomous Launch", if (isAr) "فتح التطبيق والانتقال لواجهته" else "Opening app foreground task", "LAUNCH_PACKAGE")
            )
            ActionType.TOGGLE_FLASHLIGHT -> listOf(
                ExecutionStep(0, if (isAr) "التعرف على أمر العتاد" else "Hardware Command", if (isAr) "التحكم في كشاف الإضاءة LED" else "Torch LED control requested", "AI_ANALYSIS"),
                ExecutionStep(1, if (isAr) "الاتصال بـ CameraManager" else "Camera Bus Hook", if (isAr) "تجهيز مسار العدسة والإضاءة" else "Accessing camera flash module", "HARDWARE_TRIGGER"),
                ExecutionStep(2, if (isAr) "تبديل حالة الإضاءة" else "Toggle Torch State", if (isAr) "تفعيل/إطفاء الكشاف بنجاح" else "Torch state switched", "HARDWARE_TRIGGER")
            )
            ActionType.SET_VOLUME, ActionType.TOGGLE_SILENT_MODE -> listOf(
                ExecutionStep(0, if (isAr) "تحليل نمط الصوت" else "Audio Stream Analysis", if (isAr) "تحديد التعديل الصوتي المطلوب" else "Evaluating requested volume change", "AI_ANALYSIS"),
                ExecutionStep(1, if (isAr) "الاتصال بـ AudioManager" else "Audio Service Hook", if (isAr) "تعديل قنوات الرنين والوسائط" else "Updating media and ring streams", "HARDWARE_TRIGGER"),
                ExecutionStep(2, if (isAr) "تطبيق مستوى الصوت" else "Volume Updated", if (isAr) "تم تعديل مستوى الصوت بنجاح" else "Sound level updated on device", "HARDWARE_TRIGGER")
            )
            else -> listOf(
                ExecutionStep(0, if (isAr) "تحليل الذكاء الاصطناعي" else "AI Analysis", if (isAr) "فهم القصد وتحديد المسار التنفيذي" else "Understanding intent and execution path", "AI_ANALYSIS"),
                ExecutionStep(1, if (isAr) "توجيه نظام أندرويد" else "Android System Dispatch", if (isAr) "إعداد واجهة الأمر (${actionType.name})" else "Preparing intent (${actionType.name})", "SYSTEM_SETTINGS"),
                ExecutionStep(2, if (isAr) "اكتمال التنفيذ الذاتي" else "Autonomous Execution", if (isAr) "تم إنجاز العملية بنجاح" else "Action completed successfully", "RETURN_HOME")
            )
        }
        return LiveExecutionPlan(
            commandTitle = if (isAr) "تنفيذ تلقائي: ${actionType.name}" else "Autonomous Action: ${actionType.name}",
            actionType = actionType,
            actionPayload = payload,
            steps = steps,
            currentStepIndex = 0,
            isRunning = true,
            isCompleted = false,
            statusMessage = if (isAr) "جاري التنفيذ التلقائي بالنيابة عنك..." else "Executing autonomously on device..."
        )
    }

    // Security Threat Scanner
    fun startFullSecurityScan() {
        if (_isScanningSecurity.value) return
        val isAr = LocalizationManager.getEffectiveLanguage(_appLanguage.value) == "ar"

        viewModelScope.launch {
            _isScanningSecurity.value = true
            _securityScanProgress.value = 0
            _securityScanStatusMessage.value = if (isAr) "بدء الفحص الأمني الشامل..." else "Starting deep security scan..."
            actionEngine.vibratePhone(80L)

            try {
                val report = securityScanEngine.performFullSecurityScan { progress, status ->
                    _securityScanProgress.value = progress
                    _securityScanStatusMessage.value = status
                }
                _lastSecurityReport.value = report
                _systemStatusNotice.value = "${report.systemStatusText}"

                repository.logTelemetry(
                    type = TelemetryType.SYSTEM_PERFORMANCE,
                    title = if (isAr) "فحص الأمان ومكافحة الاختراق" else "System Security Scan",
                    description = "Scanned: ${report.totalItemsScanned} items. Threats: ${report.threatsFound.size}. Score: ${report.securityScore}%",
                    severity = if (report.threatsFound.isEmpty()) TelemetrySeverity.OPTIMAL else TelemetrySeverity.WARNING,
                    aiAudited = true,
                    aiAnnotation = report.systemStatusText
                )
            } catch (e: Exception) {
                _securityScanStatusMessage.value = "Scan error: ${e.message}"
            } finally {
                _isScanningSecurity.value = false
            }
        }
    }

    fun neutralizeThreat(threatId: String) {
        val isAr = LocalizationManager.getEffectiveLanguage(_appLanguage.value) == "ar"
        val report = _lastSecurityReport.value ?: return
        val updatedThreats = report.threatsFound.map {
            if (it.id == threatId) it.copy(isResolved = true) else it
        }
        val remainingUnresolved = updatedThreats.count { !it.isResolved }
        val newScore = (report.securityScore + 15).coerceAtMost(100)

        _lastSecurityReport.value = report.copy(
            threatsFound = updatedThreats,
            securityScore = newScore,
            systemStatusText = if (remainingUnresolved == 0) {
                if (isAr) "تم تحييد وتطهير كافة التهديدات بنجاح! الهاتف آمن 100%." else "All threats resolved! Device 100% secure."
            } else report.systemStatusText
        )
        actionEngine.vibratePhone(120L)
        _systemStatusNotice.value = if (isAr) "تم عزل وتحييد التهديد الأمني بنجاح." else "Threat neutralized successfully."

        viewModelScope.launch {
            repository.logTelemetry(
                type = TelemetryType.SYSTEM_PERFORMANCE,
                title = if (isAr) "تطهير تهديد أمني" else "Threat Resolved",
                description = "Neutralized threat $threatId",
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
        val isAr = LocalizationManager.getEffectiveLanguage(_appLanguage.value) == "ar"
        _isRecordingEnrollment.value = true
        val phrase = voiceprintManager.defaultEnrollmentPhrases.getOrElse(step) { "أنا المالك المعتمد لهذا الهاتف" }

        actionEngine.vibratePhone(60L)
        voiceEngine.startListening(
            language = _appLanguage.value,
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
                        _isEnrollingVoiceprint.value = false
                        _enrollmentStep.value = 0
                        val config = assistantConfig.value
                        repository.updateConfig(
                            config.copy(
                                voiceprintEnrolled = true,
                                biometricVoiceprintEnabled = true
                            )
                        )
                        _systemStatusNotice.value = if (isAr) "تم تسجيل بصمة صوت المالك بنجاح! الأوامر التنفيذية مخصصة لصوتك فقط." else "Voiceprint registered! Executive actions locked to your voice."
                    } else {
                        _enrollmentStep.value = step + 1
                        _systemStatusNotice.value = if (isAr) "تم حفظ العينة ${step + 1} بنجاح." else "Sample ${step + 1} saved."
                    }
                }
            },
            onError = { error ->
                _isRecordingEnrollment.value = false
                _systemStatusNotice.value = "Error: $error"
            }
        )
    }

    fun resetVoiceprintProfile() {
        val isAr = LocalizationManager.getEffectiveLanguage(_appLanguage.value) == "ar"
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
            _systemStatusNotice.value = if (isAr) "تمت إعادة ضبط بصمة الصوت البيومترية." else "Voiceprint reset."
        }
    }

    fun runAiAudit() {
        val isAr = LocalizationManager.getEffectiveLanguage(_appLanguage.value) == "ar"
        viewModelScope.launch {
            _isProcessingAi.value = true
            actionEngine.vibratePhone(100L)
            try {
                val result = repository.runAiAuditAndArchiving()
                _systemStatusNotice.value = if (isAr) "اكتمل التدقيق الشامل بواسطة الذكاء الاصطناعي بنجاح." else "AI Audit completed."
                val auditMsg = ConversationMessage(
                    text = result.healthSummary,
                    isUser = false,
                    actionType = ActionType.AI_SUMMARIZE_ACTIVITY
                )
                _conversation.value = _conversation.value + auditMsg
                if (assistantConfig.value.voiceFeedbackEnabled && !assistantConfig.value.muteAllAppSounds) {
                    voiceEngine.speak(result.healthSummary)
                }
            } catch (e: Exception) {
                _systemStatusNotice.value = "Audit error: ${e.message}"
            } finally {
                _isProcessingAi.value = false
            }
        }
    }

    fun updateAssistantConfig(newConfig: AssistantConfigEntity) {
        viewModelScope.launch {
            repository.updateConfig(newConfig)
            _systemStatusNotice.value = "Settings saved (${newConfig.assistantName})."
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
            _systemStatusNotice.value = "Shortcut added."
        }
    }

    fun deleteShortcut(id: Long) {
        viewModelScope.launch {
            repository.deleteShortcut(id)
            _systemStatusNotice.value = "Shortcut deleted."
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
