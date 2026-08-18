package com.example.data.repository

import com.example.data.local.AppDatabase
import com.example.data.local.entity.ActionShortcutEntity
import com.example.data.local.entity.ActionType
import com.example.data.local.entity.AssistantConfigEntity
import com.example.data.local.entity.BehaviorInsightEntity
import com.example.data.local.entity.TelemetryLogEntity
import com.example.data.local.entity.TelemetrySeverity
import com.example.data.local.entity.TelemetryType
import com.example.data.remote.AuditAnalysisResult
import com.example.data.remote.GeminiService
import com.example.data.remote.ParsedVoiceAction
import kotlinx.coroutines.flow.Flow

class AssistantRepository(
    private val database: AppDatabase,
    private val geminiService: GeminiService
) {
    val telemetryLogs: Flow<List<TelemetryLogEntity>> = database.telemetryDao().getAllLogs()
    val shortcuts: Flow<List<ActionShortcutEntity>> = database.shortcutDao().getAllShortcuts()
    val insights: Flow<List<BehaviorInsightEntity>> = database.insightDao().getAllInsights()
    val config: Flow<AssistantConfigEntity?> = database.configDao().getConfigFlow()

    suspend fun logTelemetry(
        type: TelemetryType,
        title: String,
        description: String,
        severity: TelemetrySeverity = TelemetrySeverity.INFO,
        aiAudited: Boolean = false,
        aiAnnotation: String? = null
    ) {
        val entity = TelemetryLogEntity(
            type = type,
            title = title,
            description = description,
            severity = severity,
            aiAudited = aiAudited,
            aiAnnotation = aiAnnotation
        )
        database.telemetryDao().insertLog(entity)
        // Keep database lean and resource-conscious
        database.telemetryDao().purgeOldLogs()
    }

    suspend fun processVoiceCommand(rawVoiceInput: String): ParsedVoiceAction {
        val currentConfig = database.configDao().getConfig() ?: AssistantConfigEntity()
        val parsed = geminiService.interpretVoiceCommand(
            userQuery = rawVoiceInput,
            assistantName = currentConfig.assistantName,
            preferredDialect = currentConfig.preferredDialect
        )

        // Log to telemetry
        logTelemetry(
            type = TelemetryType.VOICE_COMMAND,
            title = "أمر صوتي: $rawVoiceInput",
            description = "تم تفسير الأمر إلى: ${parsed.understoodText} (${parsed.detectedDialect})",
            severity = TelemetrySeverity.OPTIMAL,
            aiAudited = true,
            aiAnnotation = parsed.responseSpeechText
        )

        return parsed
    }

    suspend fun runAiAuditAndArchiving(): AuditAnalysisResult {
        val currentConfig = database.configDao().getConfig() ?: AssistantConfigEntity()
        val recentLogs = database.telemetryDao().getRecentLogsForAi()
        val auditResult = geminiService.auditDeviceLogs(recentLogs, currentConfig.assistantName)

        // Save generated insights
        if (auditResult.insights.isNotEmpty()) {
            database.insightDao().insertInsights(auditResult.insights)
        }

        // Log audit event
        logTelemetry(
            type = TelemetryType.AI_INFERENCE,
            title = "تدقيق الذكاء الاصطناعي الشامل",
            description = auditResult.healthSummary,
            severity = TelemetrySeverity.OPTIMAL,
            aiAudited = true,
            aiAnnotation = "تم تحديث لوحة التحكم وتدقيق استهلاك الموارد"
        )

        database.configDao().saveConfig(
            currentConfig.copy(lastAuditTimestamp = System.currentTimeMillis())
        )

        return auditResult
    }

    suspend fun saveShortcut(shortcut: ActionShortcutEntity): Long {
        return database.shortcutDao().insertShortcut(shortcut)
    }

    suspend fun recordShortcutExecution(id: Long) {
        database.shortcutDao().incrementExecution(id)
    }

    suspend fun deleteShortcut(id: Long) {
        database.shortcutDao().deleteShortcut(id)
    }

    suspend fun updateConfig(config: AssistantConfigEntity) {
        database.configDao().saveConfig(config)
    }

    suspend fun markInsightApplied(id: Long) {
        database.insightDao().markApplied(id)
    }

    suspend fun seedInitialDataIfEmpty() {
        val existingConfig = database.configDao().getConfig()
        if (existingConfig == null) {
            database.configDao().saveConfig(AssistantConfigEntity())
        }

        val initialLogs = listOf(
            TelemetryLogEntity(
                timestamp = System.currentTimeMillis() - 120000,
                type = TelemetryType.SYSTEM_PERFORMANCE,
                title = "تهيئة محرك الذكاء الاصطناعي",
                description = "تم تحميل النواة الذكية للتعرف على الصوت والإيماءات في خلفية الهاتف بكفاءة عالية.",
                severity = TelemetrySeverity.OPTIMAL,
                aiAudited = true,
                aiAnnotation = "الموارد مستقرة ومحسنة"
            ),
            TelemetryLogEntity(
                timestamp = System.currentTimeMillis() - 90000,
                type = TelemetryType.NETWORK_TRAFFIC,
                title = "تدقيق اتصال الشبكة والداشبورد",
                description = "قناة الاتصال باللوحة الرئيسية مشفرة ومحمية بنظام أمان ثنائي.",
                severity = TelemetrySeverity.INFO,
                aiAudited = true
            ),
            TelemetryLogEntity(
                timestamp = System.currentTimeMillis() - 60000,
                type = TelemetryType.BATTERY_POWER,
                title = "تحليل دورة الطاقة",
                description = "مستوى البطارية مستقر والاستهلاك أقل من 1.2% في الساعة أثناء تفعيل الذكاء الاصطناعي.",
                severity = TelemetrySeverity.OPTIMAL,
                aiAudited = true
            ),
            TelemetryLogEntity(
                timestamp = System.currentTimeMillis() - 30000,
                type = TelemetryType.TOUCH_GESTURE,
                title = "معايرة محول اللمس إلى صوت",
                description = "جاهز لتحويل كافة تفاعلات المستخدم والأوامر اللمسية إلى إجراءات صوتية تلقائية.",
                severity = TelemetrySeverity.INFO,
                aiAudited = false
            )
        )
        database.telemetryDao().insertLogs(initialLogs)

        val defaultShortcuts = listOf(
            ActionShortcutEntity(
                title = "اتصال سريع بالطوارئ أو الأهل",
                triggerVoicePhrase = "اتصل بأمي أو الطوارئ",
                actionType = ActionType.CALL_CONTACT,
                payload = "الأهل",
                dialect = "شامي / مصري / خليجي",
                executionCount = 5
            ),
            ActionShortcutEntity(
                title = "الوضع الصامت الفوري",
                triggerVoicePhrase = "اكتم الصوت أو خلي التلفون صامت",
                actionType = ActionType.TOGGLE_SILENT_MODE,
                payload = "silent",
                dialect = "عام",
                executionCount = 8
            ),
            ActionShortcutEntity(
                title = "فحص وتحسين موارد الهاتف",
                triggerVoicePhrase = "افحص البطارية والذاكرة ونظف الرام",
                actionType = ActionType.BATTERY_OPTIMIZATION,
                payload = "optimize",
                dialect = "عام",
                executionCount = 12
            ),
            ActionShortcutEntity(
                title = "مذكرة صوتية سريعة بالذكاء الاصطناعي",
                triggerVoicePhrase = "احفظ ملاحظة صوتية جديدة",
                actionType = ActionType.VOICE_NOTE,
                payload = "note",
                dialect = "تلقائي",
                executionCount = 3
            )
        )
        for (shortcut in defaultShortcuts) {
            database.shortcutDao().insertShortcut(shortcut)
        }

        // Seed initial AI audit insight
        val initialInsights = listOf(
            BehaviorInsightEntity(
                timestamp = System.currentTimeMillis(),
                category = "كفاءة الموارد",
                title = "نظام الحفظ الذكي دون تكرار",
                summary = "يقوم التطبيق بفهم وتدقيق البيانات فور تدفقها مع الحفاظ على أقل استهلاك لموارد الهاتف.",
                recommendation = "الإبقاء على تفعيل المعالجة السريعة لضمان أداء سلس وفوري.",
                score = 98
            ),
            BehaviorInsightEntity(
                timestamp = System.currentTimeMillis() - 3600000,
                category = "التحول من اللمس إلى الصوت",
                title = "مستوى التفاعل الصوتي الذاتي",
                summary = "تم تفعيل محرك الاستماع الصوتي الذاتي لتمكينك من إعطاء الأوامر بصوتك دون الحاجة للمس الشاشة.",
                recommendation = "استخدم اسم المساعد للتحدث إليه في أي وقت وبأي لهجة.",
                score = 95
            )
        )
        database.insightDao().insertInsights(initialInsights)
    }
}
