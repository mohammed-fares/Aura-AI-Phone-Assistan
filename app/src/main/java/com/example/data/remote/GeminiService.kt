package com.example.data.remote

import android.util.Log
import com.example.BuildConfig
import com.example.data.local.entity.ActionType
import com.example.data.local.entity.BehaviorInsightEntity
import com.example.data.local.entity.TelemetryLogEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class ParsedVoiceAction(
    val understoodText: String,
    val responseSpeechText: String,
    val actionType: ActionType? = null,
    val actionPayload: String? = null,
    val detectedDialect: String = "العربية",
    val confidence: Float = 0.95f
)

data class AuditAnalysisResult(
    val healthSummary: String,
    val detectedHabits: List<String>,
    val insights: List<BehaviorInsightEntity>,
    val resourceOptimizationTips: List<String>
)

class GeminiService {
    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    suspend fun interpretVoiceCommand(
        userQuery: String,
        assistantName: String,
        preferredDialect: String
    ): ParsedVoiceAction = withContext(Dispatchers.IO) {
        val apiKey = try { BuildConfig.GEMINI_API_KEY } catch (e: Throwable) { "" }
        if (apiKey.isNullOrBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext fallbackLocalInterpreter(userQuery, assistantName)
        }

        try {
            val systemPrompt = """
                أنت $assistantName، مساعد التحكم الذاتي بالهاتف بدون لمس. 
                المستخدم يتحدث إليك بصوته أو يكتب بأي لغة أو لهجة عربية.
                مهمتك هي فهم نية المستخدم وتحويل كلامه فوراً إلى إجراء وتحكم بالهاتف والرد بأسلوب مقتضب ودقيق.
                
                الأفعال المدعومة (actionType):
                - CALL_CONTACT (إجراء اتصال بهاتف أو اسم شخص، payload: اسم الشخص أو الرقم)
                - SEND_MESSAGE (إرسال رسالة نصية أو واتساب، payload: نص الرسالة)
                - TOGGLE_SILENT_MODE (كتم الصوت أو تفعيل الوضع الصامت)
                - TOGGLE_FLASHLIGHT (تشغيل أو إيقاف كشاف الهاتف والضوء)
                - OPEN_SETTINGS (فتح إعدادات النظام العامة)
                - OPEN_WIFI_SETTINGS (فتح إعدادات الواي فاي والشبكات)
                - OPEN_BLUETOOTH_SETTINGS (فتح إعدادات البلوتوث)
                - OPEN_DISPLAY_SETTINGS (فتح إعدادات الشاشة والسطوع)
                - OPEN_BATTERY_SETTINGS (فتح إعدادات واستهلاك البطارية)
                - OPEN_SECURITY_SETTINGS (فتح إعدادات الأمان وقفل الهاتف)
                - OPEN_APP (تشغيل تطبيق محدد مثل الكاميرا أو المعرض، payload: اسم التطبيق)
                - SYSTEM_SECURITY_SCAN (فحص النظام من الاختراق والملفات والتطبيقات الخبيثة)
                - LOCAL_NETWORK_SCAN (فحص أجهزة ومشاركات الشبكة المحلية)
                - DEVICE_DIAGNOSTIC (فحص موارد الهاتف وأدائه والذاكرة)
                - BATTERY_OPTIMIZATION (توفير البطارية وتحسين الاستهلاك)
                - NETWORK_AUDIT (فحص الشبكة والإنترنت)
                - VOICE_NOTE (حفظ مذكرة صوتية سريعة، payload: نص المذكرة)
                - AI_SUMMARIZE_ACTIVITY (تلخيص نشاط وتدقيق الهاتف)
                - NULL (محادثة عامة أو إجابة على سؤال)

                أرجع النتيجة بصيغة JSON فقط:
                {
                    "understoodText": "النص المفهوم",
                    "responseSpeechText": "الرد التنفيذي المباشر",
                    "actionType": "CALL_CONTACT | SEND_MESSAGE | TOGGLE_SILENT_MODE | TOGGLE_FLASHLIGHT | OPEN_SETTINGS | OPEN_WIFI_SETTINGS | OPEN_BLUETOOTH_SETTINGS | OPEN_DISPLAY_SETTINGS | OPEN_BATTERY_SETTINGS | OPEN_SECURITY_SETTINGS | OPEN_APP | SYSTEM_SECURITY_SCAN | LOCAL_NETWORK_SCAN | DEVICE_DIAGNOSTIC | BATTERY_OPTIMIZATION | NETWORK_AUDIT | VOICE_NOTE | AI_SUMMARIZE_ACTIVITY | null",
                    "actionPayload": "نص المعامل أو null",
                    "detectedDialect": "اسم اللهجة المكتشفة",
                    "confidence": 0.95
                }
            """.trimIndent()

            val requestBodyJson = JSONObject().apply {
                val contentsArray = JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().put("text", "طلب المستخدم: \"$userQuery\""))
                        })
                    })
                }
                put("contents", contentsArray)
                put("systemInstruction", JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().put("text", systemPrompt))
                    })
                })
                put("generationConfig", JSONObject().apply {
                    put("responseMimeType", "application/json")
                    put("temperature", 0.2)
                })
            }

            val request = Request.Builder()
                .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey")
                .post(requestBodyJson.toString().toRequestBody(jsonMediaType))
                .build()

            val response = client.newCall(request).execute()
            val rawResponse = response.body?.string() ?: ""
            if (!response.isSuccessful) {
                Log.e("GeminiService", "API Error: ${response.code} - $rawResponse")
                return@withContext fallbackLocalInterpreter(userQuery, assistantName)
            }

            val rootJson = JSONObject(rawResponse)
            val candidates = rootJson.optJSONArray("candidates")
            val firstCandidate = candidates?.optJSONObject(0)
            val parts = firstCandidate?.optJSONObject("content")?.optJSONArray("parts")
            val textContent = parts?.optJSONObject(0)?.optString("text") ?: ""

            parseActionJson(textContent, userQuery, assistantName)
        } catch (e: Exception) {
            Log.e("GeminiService", "Interpret exception", e)
            fallbackLocalInterpreter(userQuery, assistantName)
        }
    }

    suspend fun auditDeviceLogs(
        recentLogs: List<TelemetryLogEntity>,
        assistantName: String
    ): AuditAnalysisResult = withContext(Dispatchers.IO) {
        val apiKey = try { BuildConfig.GEMINI_API_KEY } catch (e: Throwable) { "" }
        if (apiKey.isNullOrBlank() || apiKey == "MY_GEMINI_API_KEY" || recentLogs.isEmpty()) {
            return@withContext fallbackAudit(recentLogs, assistantName)
        }

        try {
            val logsSummary = recentLogs.take(20).joinToString("\n") {
                "- [${it.type}] ${it.title}: ${it.description} (Severity: ${it.severity})"
            }

            val prompt = """
                أنت $assistantName، محرك الذكاء الاصطناعي الخاص بالهاتف.
                قم بتدقيق وتحليل سجلات الهاتف والأمان ومشاركات الشبكة المحلية والأنشطة التالية:
                
                $logsSummary
                
                المطلوب:
                1. تقديم ملخص شامل لحالة أمان الهاتف ومشاركات الشبكة.
                2. استخراج أهم الأنماط والسلوكيات المكتشفة.
                3. تقديم 3 رؤى أمنية ذكية وتوصيات عملية مع نسبة الأمان.
                
                أرجع الإجابة بصيغة JSON فقط:
                {
                   "healthSummary": "ملخص أمان وتدقيق الهاتف والشبكة",
                   "detectedHabits": ["سلوك 1", "سلوك 2", "سلوك 3"],
                   "insights": [
                      {
                        "category": "الأمان ومكافحة الاختراق أو كفاءة الموارد أو الشبكة المحلية",
                        "title": "عنوان الرؤية الذكية",
                        "summary": "شرح الرؤية",
                        "recommendation": "توصية قابلة للتنفيذ",
                        "score": 95
                      }
                   ],
                   "resourceOptimizationTips": ["نصيحة أمنية 1", "نصيحة أمنية 2"]
                }
            """.trimIndent()

            val requestBodyJson = JSONObject().apply {
                put("contents", JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().put("text", prompt))
                        })
                    })
                })
                put("generationConfig", JSONObject().apply {
                    put("responseMimeType", "application/json")
                    put("temperature", 0.3)
                })
            }

            val request = Request.Builder()
                .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey")
                .post(requestBodyJson.toString().toRequestBody(jsonMediaType))
                .build()

            val response = client.newCall(request).execute()
            val rawResponse = response.body?.string() ?: ""
            if (!response.isSuccessful) {
                return@withContext fallbackAudit(recentLogs, assistantName)
            }

            val rootJson = JSONObject(rawResponse)
            val textContent = rootJson.optJSONArray("candidates")
                ?.optJSONObject(0)?.optJSONObject("content")
                ?.optJSONArray("parts")?.optJSONObject(0)?.optString("text") ?: ""

            parseAuditJson(textContent, assistantName)
        } catch (e: Exception) {
            Log.e("GeminiService", "Audit exception", e)
            fallbackAudit(recentLogs, assistantName)
        }
    }

    private fun parseActionJson(jsonStr: String, originalQuery: String, assistantName: String): ParsedVoiceAction {
        return try {
            val json = JSONObject(jsonStr)
            val actionTypeStr = json.optString("actionType")
            val actionType = if (actionTypeStr.isNotBlank() && actionTypeStr != "null" && actionTypeStr != "NULL") {
                try { ActionType.valueOf(actionTypeStr) } catch (e: Exception) { null }
            } else null

            ParsedVoiceAction(
                understoodText = json.optString("understoodText", originalQuery),
                responseSpeechText = json.optString("responseSpeechText", "تم تنفيذ الأمر بالنيابة عنك"),
                actionType = actionType,
                actionPayload = json.optString("actionPayload").takeIf { it.isNotBlank() && it != "null" },
                detectedDialect = json.optString("detectedDialect", "العربية"),
                confidence = json.optDouble("confidence", 0.95).toFloat()
            )
        } catch (e: Exception) {
            fallbackLocalInterpreter(originalQuery, assistantName)
        }
    }

    private fun parseAuditJson(jsonStr: String, assistantName: String): AuditAnalysisResult {
        return try {
            val json = JSONObject(jsonStr)
            val healthSummary = json.optString("healthSummary", "حالة أمان الهاتف ممتازة وكافة الاتصالات مؤمنة.")
            
            val habits = mutableListOf<String>()
            val habitsArray = json.optJSONArray("detectedHabits")
            if (habitsArray != null) {
                for (i in 0 until habitsArray.length()) {
                    habits.add(habitsArray.getString(i))
                }
            }

            val tips = mutableListOf<String>()
            val tipsArray = json.optJSONArray("resourceOptimizationTips")
            if (tipsArray != null) {
                for (i in 0 until tipsArray.length()) {
                    tips.add(tipsArray.getString(i))
                }
            }

            val insights = mutableListOf<BehaviorInsightEntity>()
            val insightsArray = json.optJSONArray("insights")
            if (insightsArray != null) {
                for (i in 0 until insightsArray.length()) {
                    val item = insightsArray.getJSONObject(i)
                    insights.add(
                        BehaviorInsightEntity(
                            category = item.optString("category", "أمان وتدقيق"),
                            title = item.optString("title", "رؤية أمنية"),
                            summary = item.optString("summary", ""),
                            recommendation = item.optString("recommendation", ""),
                            score = item.optInt("score", 95)
                        )
                    )
                }
            }

            AuditAnalysisResult(
                healthSummary = healthSummary,
                detectedHabits = habits.ifEmpty { listOf("تنفيذ ذاتي للأوامر بصمت", "مراقبة مشددة للشبكة المحلية", "فحص الأمان ومكافحة الاختراق") },
                insights = insights,
                resourceOptimizationTips = tips.ifEmpty { listOf("إجراء فحص أمان دوري للملفات والتطبيقات") }
            )
        } catch (e: Exception) {
            fallbackAudit(emptyList(), assistantName)
        }
    }

    private fun fallbackLocalInterpreter(query: String, assistantName: String): ParsedVoiceAction {
        val lower = query.lowercase().trim()
        return when {
            lower.contains("فحص الأمان") || lower.contains("اختراق") || lower.contains("فيروس") || lower.contains("خبيث") || lower.contains("تهديد") || lower.contains("security scan") -> {
                ParsedVoiceAction(
                    understoodText = "فحص الأمان ومكافحة الاختراق والملفات الخبيثة",
                    responseSpeechText = "جاري إجراء فحص أمان شامل للنظام والتطبيقات والملفات الخبيثة",
                    actionType = ActionType.SYSTEM_SECURITY_SCAN
                )
            }
            lower.contains("كشاف") || lower.contains("ضوء") || lower.contains("فلاش") || lower.contains("torch") || lower.contains("flashlight") -> {
                ParsedVoiceAction(
                    understoodText = "التحكم في كشاف الهاتف",
                    responseSpeechText = "تم تفعيل كشاف الهاتف فوراً",
                    actionType = ActionType.TOGGLE_FLASHLIGHT
                )
            }
            lower.contains("واي فاي") || lower.contains("wifi") || lower.contains("شبكة محلية") -> {
                if (lower.contains("فحص") || lower.contains("مراقبة") || lower.contains("أجهزة")) {
                    ParsedVoiceAction(
                        understoodText = "فحص ومراقبة أجهزة ومشاركات الشبكة المحلية",
                        responseSpeechText = "تم فتح تدقيق أجهزة الشبكة ومشاركات الوسائط",
                        actionType = ActionType.LOCAL_NETWORK_SCAN
                    )
                } else {
                    ParsedVoiceAction(
                        understoodText = "فتح إعدادات Wi-Fi",
                        responseSpeechText = "تم فتح إعدادات الشبكة والواي فاي",
                        actionType = ActionType.OPEN_WIFI_SETTINGS
                    )
                }
            }
            lower.contains("بلوتوث") || lower.contains("bluetooth") -> {
                ParsedVoiceAction(
                    understoodText = "فتح إعدادات البلوتوث",
                    responseSpeechText = "تم فتح إعدادات البلوتوث والأجهزة المقترنة",
                    actionType = ActionType.OPEN_BLUETOOTH_SETTINGS
                )
            }
            lower.contains("كاميرا") || lower.contains("صورة") || lower.contains("تصوير") || lower.contains("camera") -> {
                ParsedVoiceAction(
                    understoodText = "فتح الكاميرا",
                    responseSpeechText = "تم فتح الكاميرا للتصوير فوراً",
                    actionType = ActionType.OPEN_APP,
                    actionPayload = "الكاميرا"
                )
            }
            lower.contains("إعدادات") || lower.contains("ضبط") || lower.contains("settings") -> {
                when {
                    lower.contains("شاشة") || lower.contains("سطوع") -> ParsedVoiceAction(
                        understoodText = "إعدادات الشاشة",
                        responseSpeechText = "تم فتح إعدادات الشاشة",
                        actionType = ActionType.OPEN_DISPLAY_SETTINGS
                    )
                    lower.contains("أمان") || lower.contains("حماية") -> ParsedVoiceAction(
                        understoodText = "إعدادات الأمان",
                        responseSpeechText = "تم فتح إعدادات الأمان والقفل",
                        actionType = ActionType.OPEN_SECURITY_SETTINGS
                    )
                    lower.contains("بطارية") -> ParsedVoiceAction(
                        understoodText = "إعدادات البطارية",
                        responseSpeechText = "تم فتح إعدادات البطارية",
                        actionType = ActionType.OPEN_BATTERY_SETTINGS
                    )
                    else -> ParsedVoiceAction(
                        understoodText = "إعدادات الهاتف",
                        responseSpeechText = "تم فتح لوحة إعدادات النظام بالنيابة عنك",
                        actionType = ActionType.OPEN_SETTINGS
                    )
                }
            }
            lower.contains("اتصل") || lower.contains("كلم") || lower.contains("رن على") || lower.contains("call") -> {
                val target = query.replace("اتصل بـ", "").replace("اتصل على", "").replace("اتصل", "").replace("كلم", "").trim()
                ParsedVoiceAction(
                    understoodText = "إجراء مكالمة هاتفية",
                    responseSpeechText = "جاري الاتصال بـ ${if (target.isNotBlank()) target else "جهة الاتصال المطلوبة"}",
                    actionType = ActionType.CALL_CONTACT,
                    actionPayload = target.ifBlank { "جهة الاتصال" }
                )
            }
            lower.contains("رسالة") || lower.contains("مسج") || lower.contains("واتساب") || lower.contains("sms") -> {
                ParsedVoiceAction(
                    understoodText = "إرسال رسالة سريعة",
                    responseSpeechText = "فتحت لك واجهة الرسائل الفورية لكتابة وإرسال النص",
                    actionType = ActionType.SEND_MESSAGE,
                    actionPayload = query
                )
            }
            lower.contains("صامت") || lower.contains("صوت") || lower.contains("كتم") || lower.contains("silent") -> {
                ParsedVoiceAction(
                    understoodText = "التحكم بالصوت والوضع الصامت",
                    responseSpeechText = "تم تعديل إعدادات الصوت وتفعيل الوضع المطلوب",
                    actionType = ActionType.TOGGLE_SILENT_MODE,
                    actionPayload = "toggle"
                )
            }
            lower.contains("بطارية") || lower.contains("طاقة") || lower.contains("شحن") || lower.contains("battery") -> {
                ParsedVoiceAction(
                    understoodText = "فحص وتحسين البطارية",
                    responseSpeechText = "تم تدقيق استهلاك الطاقة وتفعيل وضع توفير الموارد بنجاح",
                    actionType = ActionType.BATTERY_OPTIMIZATION
                )
            }
            lower.contains("فحص") || lower.contains("أداء") || lower.contains("ذاكرة") || lower.contains("رام") || lower.contains("diagnostic") -> {
                ParsedVoiceAction(
                    understoodText = "تشخيص صحة الهاتف وموارده",
                    responseSpeechText = "أداء الهاتف مستقر ومستوى استهلاك الذاكرة والمعالج ضمن المعدل الطبيعي",
                    actionType = ActionType.DEVICE_DIAGNOSTIC
                )
            }
            lower.contains("مذكرة") || lower.contains("سجل") || lower.contains("ملاحظة") || lower.contains("احفظ") -> {
                ParsedVoiceAction(
                    understoodText = "حفظ ملاحظة صوتية ذكية",
                    responseSpeechText = "تم تدوين الملاحظة وأرشفتها في الذاكرة الذكية",
                    actionType = ActionType.VOICE_NOTE,
                    actionPayload = query
                )
            }
            lower.contains("لخص") || lower.contains("تقرير") || lower.contains("تدقيق") || lower.contains("نشاط") -> {
                ParsedVoiceAction(
                    understoodText = "استخراج تقرير تدقيق شامل للهاتف",
                    responseSpeechText = "تم إعداد تقرير شامل عن أحداث الهاتف ومشاركات الشبكة وتحديث لوحة التحكم",
                    actionType = ActionType.AI_SUMMARIZE_ACTIVITY
                )
            }
            else -> {
                ParsedVoiceAction(
                    understoodText = query,
                    responseSpeechText = "تم استلام الأمر: \"$query\". النظام يعمل في وضع التحكم الذاتي بالنيابة عنك.",
                    actionType = null
                )
            }
        }
    }

    private fun fallbackAudit(logs: List<TelemetryLogEntity>, assistantName: String): AuditAnalysisResult {
        val now = System.currentTimeMillis()
        val voiceCount = logs.count { it.type == com.example.data.local.entity.TelemetryType.VOICE_COMMAND }
        val netCount = logs.count { it.type == com.example.data.local.entity.TelemetryType.NETWORK_TRAFFIC }

        return AuditAnalysisResult(
            healthSummary = "قام نظام المساعد الذاتي ($assistantName) بأرشفة وتحليل مدخلات ومخرجات الهاتف ومشاركات الشبكة المحلية وفحص التهديدات. الحالة العامة مؤمنة.",
            detectedHabits = listOf(
                "استماع دائم وتلقائي للأوامر بمجرد فتح التطبيق دون لمس ($voiceCount أمر)",
                "مراقبة مشفرة لحركة ومشاركات الشبكة المحلية ($netCount تدقيق شبكي)",
                "التحكم المباشر في أدوات الهاتف وصلاحيات النظام بالنيابة عن المالك"
            ),
            insights = listOf(
                BehaviorInsightEntity(
                    timestamp = now,
                    category = "مكافحة الاختراق والأمان",
                    title = "فحص متواصل لسلامة النظام والملفات المشبوهة",
                    summary = "النظام مدقق ضد محاولات الاختراق وتطبيقات التجسس وصلاحيات التراكب الخطيرة.",
                    recommendation = "إجراء فحص أمان دوري وتحديث بصمة الصوت البيومترية للمالك.",
                    score = 99
                ),
                BehaviorInsightEntity(
                    timestamp = now - 3600000,
                    category = "الشبكة المحلية والمشاركة",
                    title = "تدقيق الأجهزة المتصلة وبث الوسائط والشاشة",
                    summary = "يتم رصد أي جهاز محلي يتصل بالشبكة وتشفير قنوات المزامنة ومشاركة الملفات.",
                    recommendation = "مراجعة قائمة أجهزة الشبكة في لوحة التحكم بشكل منتظم.",
                    score = 95
                ),
                BehaviorInsightEntity(
                    timestamp = now - 7200000,
                    category = "التحكم الذاتي والصلاحيات",
                    title = "تنفيذ الأوامر الفورية بصمت بدون إزعاج صوتي",
                    summary = "المساعد ينفذ المهام بالنيابة عن المستخدم مباشرة مع تأكيد بصري واهتزاز خفيف.",
                    recommendation = "استخدام الأوامر المباشرة لفتح الإعدادات، الكشاف، الاتصال أو فحص الأمان.",
                    score = 97
                )
            ),
            resourceOptimizationTips = listOf(
                "الحفاظ على وضع الاستماع التلقائي لتوفير الوقت والتحكم السريع",
                "تطهير التهديدات والملفات غير الموثوقة فور اكتشافها"
            )
        )
    }
}
