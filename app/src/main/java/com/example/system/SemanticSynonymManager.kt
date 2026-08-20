package com.example.system

import android.content.Context
import android.content.SharedPreferences
import com.example.data.local.entity.ActionType
import com.example.data.remote.ParsedVoiceAction
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

data class CustomSynonymEntry(
    val triggerPhrase: String,
    val actionType: ActionType,
    val payload: String? = null,
    val addedTimestamp: Long = System.currentTimeMillis()
)

data class SynonymCluster(
    val categoryNameAr: String,
    val categoryNameEn: String,
    val representativeAction: ActionType,
    val synonymWords: List<String>
)

class SemanticSynonymManager(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("aura_semantic_synonyms", Context.MODE_PRIVATE)

    private val _learnedSynonyms = MutableStateFlow<List<CustomSynonymEntry>>(emptyList())
    val learnedSynonyms: StateFlow<List<CustomSynonymEntry>> = _learnedSynonyms.asStateFlow()

    // Core Built-In Synonym Clusters (Covering Egyptian, Gulf, Levantine, Maghrebi, Standard Arabic & English)
    val builtInClusters: List<SynonymCluster> = listOf(
        SynonymCluster(
            categoryNameAr = "إغلاق وإطفاء وإلغاء (اطفي / سكر / بند / الغي)",
            categoryNameEn = "Close / Turn Off / Shut Down / Cancel",
            representativeAction = ActionType.CLOSE_APP,
            synonymWords = listOf(
                "اطفي", "طفي", "طفيها", "اغلق", "إغلاق", "اقفل", "قفل", "سكر", "سكره",
                "بند", "بندها", "الغي", "إلغاء", "وقف", "توقف", "كتم", "خروج", "انهي",
                "بطل", "ارمي", "close", "turn off", "shut down", "stop", "exit", "cancel", "kill"
            )
        ),
        SynonymCluster(
            categoryNameAr = "تشغيل وفتح وإضاءة (شغل / افتح / ولع / ضوّي)",
            categoryNameEn = "Open / Launch / Turn On / Enable",
            representativeAction = ActionType.OPEN_APP,
            synonymWords = listOf(
                "شغل", "تشغيل", "افتح", "فتح", "ولع", "ضوّي", "شعل", "قوم", "دور",
                "ابدأ", "أطلق", "نور", "open", "launch", "turn on", "start", "enable"
            )
        ),
        SynonymCluster(
            categoryNameAr = "إدراك وقراءة الشاشة (اقرأ الشاشة / شو في / لخص)",
            categoryNameEn = "Screen Perception & Reading",
            representativeAction = ActionType.READ_SCREEN_TEXT,
            synonymWords = listOf(
                "اقرأ الشاشة", "اقرا الشاشة", "شو في بالشاشة", "ايش في بالشاشة", "لخص الشاشة",
                "ما المعروض", "ما في الشاشة", "اقرأ ما في الشاشة", "شوف الشاشة", "احكيلي شو معروض",
                "قراءة الشاشة", "read screen", "summarize screen", "what's on screen", "screen content"
            )
        ),
        SynonymCluster(
            categoryNameAr = "رجوع وعودة (ارجع / لورا / للخلف / السابق)",
            categoryNameEn = "Navigate Back / Previous",
            representativeAction = ActionType.GLOBAL_BACK,
            synonymWords = listOf(
                "ارجع", "رجوع", "ارجع لورا", "ارجع للخلف", "للخلف", "السابق", "عد للخلف", "go back", "back", "previous"
            )
        ),
        SynonymCluster(
            categoryNameAr = "الشاشة الرئيسية (الرئيسية / الهوم / برا)",
            categoryNameEn = "Home Screen / Launcher",
            representativeAction = ActionType.RETURN_HOME,
            synonymWords = listOf(
                "الرئيسية", "الشاشة الرئيسية", "شاشة الهاتف", "الهوم", "برا", "اطلع برا", "روح الرئيسية", "home", "home screen"
            )
        ),
        SynonymCluster(
            categoryNameAr = "نقر وضغط على العناصر (اضغط / انقر / دوس / اكبس)",
            categoryNameEn = "Click / Tap / Press UI Element",
            representativeAction = ActionType.CLICK_SCREEN_ELEMENT,
            synonymWords = listOf(
                "اضغط", "انقر", "دوس", "اكبس", "حدد", "click", "tap", "press"
            )
        ),
        SynonymCluster(
            categoryNameAr = "كتابة وتعبئة النصوص (اكتب / سجل / عبي / حط)",
            categoryNameEn = "Type / Write / Input Text",
            representativeAction = ActionType.TYPE_ON_SCREEN,
            synonymWords = listOf(
                "اكتب", "أكتب", "سجل", "عبي", "حط", "ادخل", "أدخل", "type", "write", "enter", "input"
            )
        ),
        SynonymCluster(
            categoryNameAr = "اتصال ومكالمات (اتصل / رن / دق / خابر / كلم)",
            categoryNameEn = "Call / Dial / Phone Contact",
            representativeAction = ActionType.CALL_CONTACT,
            synonymWords = listOf(
                "اتصل", "إتصال", "رن على", "دق على", "خابر", "كلم", "تلفن", "call", "dial", "phone", "ring"
            )
        )
    )

    init {
        loadPersistedSynonyms()
    }

    private fun loadPersistedSynonyms() {
        val raw = prefs.getString("custom_synonyms_json", "[]") ?: "[]"
        try {
            val arr = JSONArray(raw)
            val list = mutableListOf<CustomSynonymEntry>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val phrase = obj.getString("phrase")
                val actionTypeStr = obj.getString("actionType")
                val payload = obj.optString("payload").takeIf { it.isNotBlank() && it != "null" }
                val time = obj.optLong("timestamp", System.currentTimeMillis())
                try {
                    list.add(
                        CustomSynonymEntry(
                            triggerPhrase = phrase,
                            actionType = ActionType.valueOf(actionTypeStr),
                            payload = payload,
                            addedTimestamp = time
                        )
                    )
                } catch (e: Exception) {
                    // ignore unknown action
                }
            }
            _learnedSynonyms.value = list
        } catch (e: Exception) {
            _learnedSynonyms.value = emptyList()
        }
    }

    private fun persistSynonyms(list: List<CustomSynonymEntry>) {
        val arr = JSONArray()
        for (item in list) {
            val obj = JSONObject().apply {
                put("phrase", item.triggerPhrase)
                put("actionType", item.actionType.name)
                put("payload", item.payload ?: "")
                put("timestamp", item.addedTimestamp)
            }
            arr.put(obj)
        }
        prefs.edit().putString("custom_synonyms_json", arr.toString()).apply()
        _learnedSynonyms.value = list
    }

    /**
     * Learns or overrides a user concept / dialect expression.
     * When user teaches: "اطفي الكشاف" -> ActionType.TOGGLE_FLASHLIGHT
     */
    fun learnSynonym(phrase: String, actionType: ActionType, payload: String? = null) {
        val cleanPhrase = phrase.trim().lowercase()
        if (cleanPhrase.isBlank()) return

        val current = _learnedSynonyms.value.toMutableList()
        current.removeAll { it.triggerPhrase.equals(cleanPhrase, ignoreCase = true) }
        current.add(
            CustomSynonymEntry(
                triggerPhrase = cleanPhrase,
                actionType = actionType,
                payload = payload?.trim()?.takeIf { it.isNotBlank() },
                addedTimestamp = System.currentTimeMillis()
            )
        )
        persistSynonyms(current)
    }

    fun removeLearnedSynonym(phrase: String) {
        val current = _learnedSynonyms.value.toMutableList()
        current.removeAll { it.triggerPhrase.equals(phrase.trim(), ignoreCase = true) }
        persistSynonyms(current)
    }

    /**
     * Resolves voice input against learned synonyms with semantic fallback.
     */
    fun resolveSynonymMatch(rawInput: String): ParsedVoiceAction? {
        val lower = rawInput.lowercase().trim()

        // 1. Check user customized / learned synonyms
        for (entry in _learnedSynonyms.value) {
            if (lower == entry.triggerPhrase || lower.contains(entry.triggerPhrase)) {
                val extractedPayload = if (entry.payload != null) {
                    entry.payload
                } else {
                    lower.replace(entry.triggerPhrase, "").trim().takeIf { it.isNotBlank() }
                }
                return ParsedVoiceAction(
                    understoodText = "مفهوم مخصص: ${entry.triggerPhrase} 🧠",
                    responseSpeechText = "تم تنفيذ الأمر المخصص (${entry.triggerPhrase}) على الهاتف بنجاح ⚡",
                    actionType = entry.actionType,
                    actionPayload = extractedPayload,
                    detectedDialect = "تعبير تم تعلمه ذاتياً",
                    confidence = 0.99f
                )
            }
        }

        // 2. Multi-word synonym pattern matching
        // e.g. "اطفي الكشاف" / "طفي الفلاش" / "بند الضو" / "سكر النور" -> TOGGLE_FLASHLIGHT
        if (lower.containsAny("كشاف", "فلاش", "flashlight", "torch", "ضو", "نور") &&
            lower.containsAny("اطفي", "طفي", "طفيها", "اغلق", "اقفل", "سكر", "بند", "الغي", "وقف", "كتم")
        ) {
            return ParsedVoiceAction(
                understoodText = "إطفاء كشاف الهاتف",
                responseSpeechText = "تم إطفاء الكشاف بنجاح 🔦",
                actionType = ActionType.TOGGLE_FLASHLIGHT,
                confidence = 0.99f
            )
        }

        if (lower.containsAny("كشاف", "فلاش", "flashlight", "torch", "ضو", "نور") &&
            lower.containsAny("شغل", "افتح", "ولع", "ضوّي", "شعل", "نور")
        ) {
            return ParsedVoiceAction(
                understoodText = "تشغيل كشاف الهاتف",
                responseSpeechText = "تم تشغيل الكشاف بنجاح 🔦",
                actionType = ActionType.TOGGLE_FLASHLIGHT,
                confidence = 0.99f
            )
        }

        // Screen Perception queries
        if (lower.containsAny("اقرأ الشاشة", "اقرا الشاشة", "اقرأ ما في الشاشة", "اقرا اللي قدامي", "شو في بالشاشة", "ايش في بالشاشة", "read screen")) {
            return ParsedVoiceAction(
                understoodText = "قراءة محتوى الشاشة الحالي",
                responseSpeechText = "جاري قراءة النصوص المعروضة على الشاشة 👁️📖",
                actionType = ActionType.READ_SCREEN_TEXT,
                confidence = 0.99f
            )
        }

        if (lower.containsAny("لخص الشاشة", "لخص ما في الشاشة", "ما المعروض", "ماذا يوجد على الشاشة", "شوف الشاشة", "summarize screen")) {
            return ParsedVoiceAction(
                understoodText = "تلخيص محتوى الشاشة بالذكاء الاصطناعي",
                responseSpeechText = "جاري تحليل الشاشة وتلخيص محتواها الذكي 🧠📊",
                actionType = ActionType.SUMMARIZE_SCREEN,
                confidence = 0.99f
            )
        }

        return null
    }

    private fun String.containsAny(vararg candidates: String): Boolean {
        return candidates.any { this.contains(it, ignoreCase = true) }
    }
}
