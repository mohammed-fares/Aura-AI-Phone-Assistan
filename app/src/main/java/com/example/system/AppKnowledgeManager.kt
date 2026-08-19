package com.example.system

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import com.example.data.local.dao.InstalledAppDao
import com.example.data.local.entity.InstalledAppEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AppKnowledgeManager(
    private val context: Context,
    private val installedAppDao: InstalledAppDao
) {
    private val scope = CoroutineScope(Dispatchers.IO)
    private val _isIndexing = MutableStateFlow(false)
    val isIndexing: StateFlow<Boolean> = _isIndexing.asStateFlow()

    private val _indexedCount = MutableStateFlow(0)
    val indexedCount: StateFlow<Int> = _indexedCount.asStateFlow()

    val allIndexedApps: Flow<List<InstalledAppEntity>> = installedAppDao.getAllApps()

    init {
        // Automatically scan and index all installed apps on startup
        scope.launch {
            scanAndIndexAllInstalledApps()
        }
    }

    suspend fun scanAndIndexAllInstalledApps(): List<InstalledAppEntity> = withContext(Dispatchers.IO) {
        _isIndexing.value = true
        val pm = context.packageManager
        val appsList = mutableListOf<InstalledAppEntity>()

        try {
            val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }
            val resolveInfos = pm.queryIntentActivities(mainIntent, 0)
            val seenPackages = mutableSetOf<String>()

            for (info in resolveInfos) {
                val pkgName = info.activityInfo.packageName
                if (seenPackages.contains(pkgName)) continue
                seenPackages.add(pkgName)

                val appName = try {
                    info.loadLabel(pm).toString()
                } catch (e: Exception) {
                    pkgName
                }

                val appInfo = try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        pm.getApplicationInfo(pkgName, PackageManager.ApplicationInfoFlags.of(0))
                    } else {
                        @Suppress("DEPRECATION")
                        pm.getApplicationInfo(pkgName, 0)
                    }
                } catch (e: Exception) {
                    null
                }

                val isSystem = (appInfo?.flags ?: 0) and ApplicationInfo.FLAG_SYSTEM != 0
                val (arabicName, category, keywords, mechanics, capabilities) = analyzeAppMechanics(pkgName, appName)

                val entity = InstalledAppEntity(
                    packageName = pkgName,
                    appName = appName,
                    appNameArabic = arabicName,
                    category = category,
                    capabilities = capabilities,
                    keywords = keywords,
                    isSystemApp = isSystem,
                    launchIntentAvailable = true,
                    aiMechanicsDescription = mechanics,
                    lastScannedTimestamp = System.currentTimeMillis()
                )
                appsList.add(entity)
            }

            // Also check all installed applications to include background & system tools
            val allInstalled = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getInstalledApplications(PackageManager.ApplicationInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                pm.getInstalledApplications(0)
            }

            for (app in allInstalled) {
                if (seenPackages.contains(app.packageName)) continue
                val launchIntent = pm.getLaunchIntentForPackage(app.packageName)
                if (launchIntent == null && !isKeySystemPackage(app.packageName)) continue

                seenPackages.add(app.packageName)
                val label = try {
                    pm.getApplicationLabel(app).toString()
                } catch (e: Exception) {
                    app.packageName
                }
                val isSystem = (app.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                val (arabicName, category, keywords, mechanics, capabilities) = analyzeAppMechanics(app.packageName, label)

                appsList.add(
                    InstalledAppEntity(
                        packageName = app.packageName,
                        appName = label,
                        appNameArabic = arabicName,
                        category = category,
                        capabilities = capabilities,
                        keywords = keywords,
                        isSystemApp = isSystem,
                        launchIntentAvailable = launchIntent != null,
                        aiMechanicsDescription = mechanics,
                        lastScannedTimestamp = System.currentTimeMillis()
                    )
                )
            }

            // Save indexed knowledge into Room
            installedAppDao.insertApps(appsList)
            _indexedCount.value = appsList.size
            Log.d("AppKnowledgeManager", "Indexed ${appsList.size} installed applications into AI knowledge base.")
        } catch (e: Exception) {
            Log.e("AppKnowledgeManager", "Failed to index apps", e)
        } finally {
            _isIndexing.value = false
        }

        appsList
    }

    private fun isKeySystemPackage(pkg: String): Boolean {
        val lower = pkg.lowercase()
        return lower.contains("camera") || lower.contains("dialer") || lower.contains("telecom") ||
                lower.contains("messaging") || lower.contains("settings") || lower.contains("calculator") ||
                lower.contains("clock") || lower.contains("gallery") || lower.contains("contacts")
    }

    /**
     * AI Knowledge rules: understands the operational mechanics, keywords, and capabilities
     * for every app installed on the device.
     */
    private fun analyzeAppMechanics(
        packageName: String,
        appName: String
    ): Quintuple<String, String, String, String, String> {
        val pkg = packageName.lowercase()
        val name = appName.lowercase()

        return when {
            pkg.contains("whatsapp") || name.contains("whatsapp") || name.contains("واتساب") || name.contains("واتس") -> {
                Quintuple(
                    "واتساب",
                    "MESSAGING",
                    "واتساب, واتس, الواتساب, whatsapp, whats, رسائل واتس, دردشة, chat",
                    "تطبيق مراسلة فورية. يدعم الذكاء الاصطناعي: فتح المحادثات، كتابة وإرسال الرسائل النصية ذاتياً، وإرسال الوسائط والعودة للشاشة الرئيسية.",
                    "SEND_MESSAGE,VIEW_CHATS,CALL_VOICE,CALL_VIDEO,CLOSE_APP"
                )
            }
            pkg.contains("telegram") || name.contains("telegram") || name.contains("تليجرام") || name.contains("تيليجرام") -> {
                Quintuple(
                    "تيليجرام",
                    "MESSAGING",
                    "تليجرام, تيليجرام, التليجرام, telegram, tg, قنوات, محادثات",
                    "تطبيق مراسلة وتواصل. يدعم: فتح القنوات، إرسال الرسائل النصية، والبحث عن جهات الاتصال.",
                    "SEND_MESSAGE,VIEW_CHATS,OPEN_CHANNEL"
                )
            }
            pkg.contains("youtube") || name.contains("youtube") || name.contains("يوتيوب") -> {
                Quintuple(
                    "يوتيوب",
                    "MEDIA_VIDEO",
                    "يوتيوب, اليوتيوب, youtube, yt, فيديو, مقاطع, بث, اغاني",
                    "منصة بث الفيديو والموسيقى. يدعم: تشغيل المقاطع، البحث عن الفيديوهات، والتحكم بالتشغيل ومستوى الصوت.",
                    "SEARCH_VIDEO,PLAY_MEDIA,OPEN_CHANNEL"
                )
            }
            pkg.contains("chrome") || name.contains("chrome") || name.contains("كروم") || pkg.contains("browser") || name.contains("browser") || name.contains("متصفح") -> {
                Quintuple(
                    "جوجل كروم / متصفح الويب",
                    "BROWSER",
                    "كروم, الكروم, chrome, متصفح, المتصفح, browser, جوجل, ويب, بحث نت",
                    "متصفح الإنترنت. يدعم: فتح الروابط والمواقع، البحث المباشر، وفتح التبويبات وتصفح الأخبار.",
                    "OPEN_URL,WEB_SEARCH,NAVIGATE_PAGE"
                )
            }
            pkg.contains("messaging") || pkg.contains("mms") || pkg.contains("sms") || name.contains("messages") || name.contains("رسائل") -> {
                Quintuple(
                    "الرسائل النصية القصيرة (SMS)",
                    "MESSAGING_SMS",
                    "رسائل, الرسائل, مسجات, messages, sms, text, رسالة نصية",
                    "تطبيق الرسائل النصية الافتراضي. يدعم الذكاء الاصطناعي: كتابة الرسائل تلقائياً وإرسالها للأرقام والعودة للشاشة الرئيسية.",
                    "SEND_SMS,VIEW_SMS,AUTO_SEND_AND_CLOSE"
                )
            }
            pkg.contains("dialer") || pkg.contains("phone") || pkg.contains("telecom") || name.contains("phone") || name.contains("هاتف") || name.contains("اتصال") -> {
                Quintuple(
                    "لوحة الاتصال والهاتف",
                    "COMMUNICATION",
                    "اتصال, هاتف, الهاتف, phone, call, dialer, مكالمة, سجل المكالمات",
                    "تطبيق إدارة المكالمات والاتصال. يدعم: الاتصال المباشر بالأسماء والأرقام، إنهاء المكالمة، وفحص سجل الاتصالات.",
                    "MAKE_CALL,END_CALL,VIEW_CALL_LOG"
                )
            }
            pkg.contains("camera") || name.contains("camera") || name.contains("كاميرا") || name.contains("تصوير") -> {
                Quintuple(
                    "كاميرا الهاتف",
                    "CAMERA_PHOTO",
                    "كاميرا, الكاميرا, camera, تصوير, صورة, فيديو, سيلفي, التقاط",
                    "تطبيق الكاميرا المدمج. يدعم: التقاط الصور الثابتة، تسجيل الفيديو، والتحكم بالفلاش والأوضاع.",
                    "TAKE_PHOTO,RECORD_VIDEO,SWITCH_CAMERA"
                )
            }
            pkg.contains("photos") || pkg.contains("gallery") || name.contains("gallery") || name.contains("معرض") || name.contains("استوديو") || name.contains("صور") -> {
                Quintuple(
                    "معرض الصور والاستوديو",
                    "MEDIA_GALLERY",
                    "معرض, استوديو, الاستوديو, صور, الصور, gallery, photos, البومات",
                    "معرض الوسائط. يدعم: استعراض الصور والفيديوهات، تنظيم الألبومات، ومشاركة الملفات.",
                    "VIEW_PHOTOS,OPEN_ALBUM,SHARE_IMAGE"
                )
            }
            pkg.contains("calculator") || name.contains("calculator") || name.contains("حاسبة") || name.contains("حساب") -> {
                Quintuple(
                    "الآلة الحاسبة",
                    "UTILITY_CALC",
                    "حاسبة, الحاسبة, calculator, calc, حساب, رياضيات, ارقام",
                    "أداة الحساب. يدعم: إجراء العمليات الحسابية المباشرة وحساب النسب والمعادلات.",
                    "CALCULATE,CONVERT_UNITS"
                )
            }
            pkg.contains("deskclock") || pkg.contains("alarm") || name.contains("clock") || name.contains("ساعة") || name.contains("منبه") -> {
                Quintuple(
                    "الساعة والمنبه",
                    "UTILITY_TIME",
                    "ساعة, الساعة, منبه, المنبه, clock, alarm, مؤقت, وقت, تنبيه",
                    "إدارة الوقت والمنبهات. يدعم: ضبط المنبهات في أوقات محددة، تشغيل المؤقت الزمني، وساعة الإيقاف.",
                    "SET_ALARM,START_TIMER,VIEW_TIME"
                )
            }
            pkg.contains("maps") || name.contains("maps") || name.contains("خرائط") || name.contains("موقع") -> {
                Quintuple(
                    "خرائط جوجل والملاحة",
                    "NAVIGATION",
                    "خرائط, الخرائط, maps, موقع, خريطة, navigation, gps, اتجاهات, طريق",
                    "الملاحة والخرائط الجغرافية. يدعم: البحث عن المواقع والأماكن، الملاحة المباشرة، وتحديد مسارات القيادة.",
                    "NAVIGATE_TO,SEARCH_LOCATION,VIEW_TRAFFIC"
                )
            }
            pkg.contains("instagram") || name.contains("instagram") || name.contains("انستقرام") || name.contains("انستغرام") -> {
                Quintuple(
                    "إنستغرام",
                    "SOCIAL",
                    "انستغرام, انستقرام, انستا, instagram, insta, ريلز, ستوري",
                    "شبكة التواصل المرئي. يدعم: فتح المنشورات، استعراض الريلز، والبحث عن الحسابات والمراسلة الخاصة.",
                    "OPEN_REELS,VIEW_POSTS,DIRECT_MSG"
                )
            }
            pkg.contains("facebook") || name.contains("facebook") || name.contains("فيسبوك") -> {
                Quintuple(
                    "فيسبوك",
                    "SOCIAL",
                    "فيسبوك, الفيس, facebook, fb, منشورات, بوستات",
                    "شبكة التواصل الاجتماعي. يدعم: استعراض الأخبار والمنشورات والتفاعل والبحث.",
                    "VIEW_FEED,POST_STATUS,SEARCH_FRIENDS"
                )
            }
            pkg.contains("tiktok") || name.contains("tiktok") || name.contains("تيك توك") -> {
                Quintuple(
                    "تيك توك",
                    "SOCIAL_VIDEO",
                    "تيك توك, التيك توك, tiktok, مقاطع قصيرة, تريند",
                    "منصة الفيديوهات القصيرة. يدعم: استعراض الفيديوهات والتريندات والبحث.",
                    "PLAY_FEED,SEARCH_TRENDS"
                )
            }
            pkg.contains("settings") || name.contains("settings") || name.contains("إعدادات") || name.contains("ضبط") -> {
                Quintuple(
                    "إعدادات النظام والضبط",
                    "SYSTEM_SETTINGS",
                    "إعدادات, الاعدادات, ضبط, settings, واي فاي, بلوتوث, شاشة, صوت",
                    "لوحة تحكم إعدادات الهاتف. يدعم الذكاء الاصطناعي: ضبط الواي فاي، البلوتوث، وضع الطيران، السطوع، وحالة الصوت.",
                    "TOGGLE_WIFI,TOGGLE_BLUETOOTH,ADJUST_SETTINGS"
                )
            }
            pkg.contains("vending") || name.contains("play store") || name.contains("متجر") -> {
                Quintuple(
                    "متجر التطبيقات (Google Play)",
                    "STORE",
                    "متجر, المتجر, بلاي, play store, store, تحميل تطبيقات, العاب",
                    "متجر تنزيل التطبيقات والألعاب وتحديثها.",
                    "SEARCH_APP,UPDATE_APPS"
                )
            }
            pkg.contains("gm") || pkg.contains("email") || name.contains("gmail") || name.contains("بريد") -> {
                Quintuple(
                    "البريد الإلكتروني (Gmail)",
                    "EMAIL",
                    "جيميل, الجيميل, gmail, بريد, ايميل, رسائل بريد, mail",
                    "خدمة البريد الإلكتروني. يدعم: إنشاء وإرسال الرسائل الإلكترونية، قراءة الوارد، والبحث.",
                    "COMPOSE_EMAIL,READ_INBOX"
                )
            }
            else -> {
                Quintuple(
                    appName,
                    "APPLICATION",
                    "$appName, $name, $pkg",
                    "تطبيق مثبت على الهاتف ($appName). يدعم الذكاء الاصطناعي فتحه وتشغيله وتنفيذ الأوامر داخله وإغلاقه.",
                    "LAUNCH,CLOSE,RUN_ACTIONS"
                )
            }
        }
    }

    suspend fun findMatchingApp(voiceQuery: String): InstalledAppEntity? = withContext(Dispatchers.IO) {
        val clean = voiceQuery.lowercase().trim()
        val direct = installedAppDao.findMatchingApps(clean)
        if (direct.isNotEmpty()) return@withContext direct.first()

        // Match against word tokens
        val tokens = clean.split(" ")
        for (token in tokens) {
            if (token.length >= 3) {
                val matches = installedAppDao.findMatchingApps(token)
                if (matches.isNotEmpty()) return@withContext matches.first()
            }
        }
        null
    }
}

data class Quintuple<A, B, C, D, E>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D,
    val fifth: E
)
