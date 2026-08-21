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
    val canonicalMeaning: String = "",
    val actionType: ActionType,
    val payload: String? = null,
    val addedTimestamp: Long = System.currentTimeMillis()
)

typealias LearnedSynonym = CustomSynonymEntry

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
                val canonical = obj.optString("canonical", "")
                val actionTypeStr = obj.optString("actionType", ActionType.CLOSE_APP.name)
                val payload = obj.optString("payload").takeIf { it.isNotBlank() && it != "null" }
                val time = obj.optLong("timestamp", System.currentTimeMillis())
                try {
                    list.add(
                        CustomSynonymEntry(
                            triggerPhrase = phrase,
                            canonicalMeaning = canonical,
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
                put("canonical", item.canonicalMeaning)
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
     * When user teaches: "اطفي" -> "اغلق" or ActionType.CLOSE_APP
     */
    fun learnSynonym(
        phrase: String,
        canonical: String = "",
        actionType: ActionType? = null,
        payload: String? = null
    ) {
        val cleanPhrase = phrase.trim().lowercase()
        if (cleanPhrase.isBlank()) return

        val resolvedAction = actionType ?: inferActionFromCanonical(canonical)

        val current = _learnedSynonyms.value.toMutableList()
        current.removeAll { it.triggerPhrase.equals(cleanPhrase, ignoreCase = true) }
        current.add(
            CustomSynonymEntry(
                triggerPhrase = cleanPhrase,
                canonicalMeaning = canonical.trim(),
                actionType = resolvedAction,
                payload = payload?.trim()?.takeIf { it.isNotBlank() },
                addedTimestamp = System.currentTimeMillis()
            )
        )
        persistSynonyms(current)
    }

    fun learnSynonym(phrase: String, actionType: ActionType, payload: String? = null) {
        learnSynonym(phrase = phrase, canonical = actionType.name, actionType = actionType, payload = payload)
    }

    fun removeLearnedSynonym(phrase: String) {
        val current = _learnedSynonyms.value.toMutableList()
        current.removeAll { it.triggerPhrase.equals(phrase.trim(), ignoreCase = true) }
        persistSynonyms(current)
    }

    private fun inferActionFromCanonical(canonical: String): ActionType {
        val lower = canonical.lowercase().trim()
        return when {
            lower.containsAny("اغلق", "إغلاق", "اطفي", "طفي", "قفل", "سكر", "بند", "close", "shut", "stop") -> ActionType.CLOSE_APP
            lower.containsAny("افتح", "فتح", "شغل", "تشغيل", "ولع", "open", "launch", "start") -> ActionType.OPEN_APP
            lower.containsAny("اتصل", "اتصال", "كلم", "رن", "خابر", "call", "dial") -> ActionType.CALL_CONTACT
            lower.containsAny("اقرأ الشاشة", "اقرا الشاشة", "شوف الشاشة", "قراءة الشاشة", "read screen") -> ActionType.READ_SCREEN_TEXT
            lower.containsAny("لخص", "تلخيص", "summarize") -> ActionType.SUMMARIZE_SCREEN
            lower.containsAny("كشاف", "فلاش", "flashlight") -> ActionType.TOGGLE_FLASHLIGHT
            lower.containsAny("ارجع", "رجوع", "للخلف", "back") -> ActionType.GLOBAL_BACK
            lower.containsAny("الرئيسية", "home") -> ActionType.RETURN_HOME
            lower.containsAny("الغي", "إنهاء", "انهي", "end") -> ActionType.END_CALL
            else -> ActionType.CLOSE_APP
        }
    }

    /**
     * Resolves dialect words within a sentence to their canonical standard form.
     */
    fun resolve(rawInput: String): String {
        var resolved = rawInput.trim()
        val lower = resolved.lowercase()

        // 1. Check custom learned synonyms
        for (entry in _learnedSynonyms.value) {
            if (entry.canonicalMeaning.isNotBlank() && lower.contains(entry.triggerPhrase)) {
                resolved = resolved.replace(Regex("(?i)${Regex.escape(entry.triggerPhrase)}"), entry.canonicalMeaning)
            }
        }

        // 2. Built-in dialect standardizations
        val dialectReplacements = mapOf(
            "اطفي" to "اغلق",
            "طفي" to "اغلق",
            "طفيها" to "اغلق",
            "سكر" to "اغلق",
            "سكره" to "اغلق",
            "بند" to "اغلق",
            "بندها" to "اغلق",
            "فركش" to "اغلق",
            "ولع" to "شغل",
            "ضوّي" to "شغل",
            "شعل" to "شغل",
            "رن على" to "اتصل ب",
            "دق على" to "اتصل ب",
            "خابر" to "اتصل ب",
            "شوف الشاشة" to "اقرأ الشاشة",
            "ايش في بالشاشة" to "اقرأ الشاشة",
            "شو في بالشاشة" to "اقرأ الشاشة"
        )

        for ((dialect, standard) in dialectReplacements) {
            if (resolved.contains(dialect, ignoreCase = true)) {
                resolved = resolved.replace(Regex("(?i)${Regex.escape(dialect)}"), standard)
            }
        }

        return resolved
    }

    /**
     * Vector / N-gram Similarity Match Result
     */
    data class SynonymMatchResult(
        val matchedCluster: SynonymCluster?,
        val matchedSynonymWord: String,
        val targetAction: ActionType,
        val similarityScore: Float,
        val isLearnedCustom: Boolean = false
    )

    /**
     * Finds the closest synonym cluster using vector-like character n-gram cosine similarity.
     * Groups synonymous commands (e.g. 'close', 'off', 'exit', 'shut', 'سكر', 'اطفي', 'بند')
     * into a unified ActionType trigger.
     */
    fun findVectorSynonymMatch(rawQuery: String): SynonymMatchResult? {
        val clean = rawQuery.trim().lowercase()
        if (clean.isBlank()) return null

        // 1. Check custom learned synonyms first (Priority)
        for (learned in _learnedSynonyms.value) {
            val score = calculateVectorSimilarity(clean, learned.triggerPhrase)
            if (score >= 0.70f || clean.contains(learned.triggerPhrase)) {
                val matchedClust = builtInClusters.firstOrNull { it.representativeAction == learned.actionType }
                return SynonymMatchResult(
                    matchedCluster = matchedClust,
                    matchedSynonymWord = learned.triggerPhrase,
                    targetAction = learned.actionType,
                    similarityScore = if (clean.contains(learned.triggerPhrase)) 1.0f else score,
                    isLearnedCustom = true
                )
            }
        }

        // 2. Vector-based cluster matching across all built-in clusters
        var bestMatch: SynonymMatchResult? = null
        var highestScore = 0f

        for (cluster in builtInClusters) {
            for (synonym in cluster.synonymWords) {
                val score = calculateVectorSimilarity(clean, synonym.lowercase())
                val isExactSubstring = clean.contains(synonym.lowercase())
                val effectiveScore = if (isExactSubstring) 0.95f else score

                if (effectiveScore > highestScore && effectiveScore >= 0.60f) {
                    highestScore = effectiveScore
                    bestMatch = SynonymMatchResult(
                        matchedCluster = cluster,
                        matchedSynonymWord = synonym,
                        targetAction = cluster.representativeAction,
                        similarityScore = effectiveScore,
                        isLearnedCustom = false
                    )
                }
            }
        }

        return bestMatch
    }

    /**
     * Vector similarity calculation using 2-gram and 3-gram character frequency vectors (Cosine / Dice Coefficient).
     */
    fun calculateVectorSimilarity(s1: String, s2: String): Float {
        if (s1.equals(s2, ignoreCase = true)) return 1.0f
        if (s1.contains(s2, ignoreCase = true) || s2.contains(s1, ignoreCase = true)) return 0.90f

        val grams1 = extractNGrams(s1, 2) + extractNGrams(s1, 3)
        val grams2 = extractNGrams(s2, 2) + extractNGrams(s2, 3)

        if (grams1.isEmpty() || grams2.isEmpty()) return 0f

        val intersection = grams1.count { grams2.contains(it) }
        val total = grams1.size + grams2.size
        return (2.0f * intersection) / total
    }

    private fun extractNGrams(str: String, n: Int): List<String> {
        val list = mutableListOf<String>()
        val clean = str.replace("\\s+".toRegex(), "")
        if (clean.length < n) return list
        for (i in 0..clean.length - n) {
            list.add(clean.substring(i, i + n))
        }
        return list
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
