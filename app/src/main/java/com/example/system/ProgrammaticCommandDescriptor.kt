package com.example.system

import com.example.data.local.entity.ActionType

data class ProgrammaticCommand(
    val actionType: ActionType,
    val payload: String?,
    val kotlinCodeSnippet: String,
    val intentAction: String,
    val targetComponent: String,
    val adbShellCommand: String,
    val executionSummaryAr: String,
    val executionSummaryEn: String
)

object ProgrammaticCommandDescriptor {

    fun describe(actionType: ActionType, payload: String?): ProgrammaticCommand {
        val safePayload = payload?.trim() ?: ""

        return when (actionType) {
            ActionType.CALL_CONTACT -> {
                val num = safePayload.ifBlank { "0590000000" }
                ProgrammaticCommand(
                    actionType = actionType,
                    payload = payload,
                    kotlinCodeSnippet = """
                        // 1. Android Telecom Direct Call
                        val callIntent = Intent(Intent.ACTION_CALL).apply {
                            data = Uri.parse("tel:$num")
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        context.startActivity(callIntent)
                    """.trimIndent(),
                    intentAction = "android.intent.action.CALL",
                    targetComponent = "com.android.server.telecom (TelecomManager)",
                    adbShellCommand = "adb shell am start -a android.intent.action.CALL -d tel:$num",
                    executionSummaryAr = "إجراء اتصال هاتفي مباشر للرقم: $num",
                    executionSummaryEn = "Direct phone call execution to: $num"
                )
            }

            ActionType.END_CALL -> {
                ProgrammaticCommand(
                    actionType = actionType,
                    payload = payload,
                    kotlinCodeSnippet = """
                        // End Call via TelecomManager & Accessibility
                        val telecom = context.getSystemService(TelecomManager::class.java)
                        telecom?.endCall()
                        AuraAccessibilityService.instance?.performClickOnNode(listOf("إنهاء", "End", "Hang up"))
                    """.trimIndent(),
                    intentAction = "android.telecom.TelecomManager#endCall",
                    targetComponent = "TelecomManager / CallController",
                    adbShellCommand = "adb shell input keyevent KEYCODE_ENDCALL",
                    executionSummaryAr = "إنهاء المكالمة الهاتفية النشطة وإغلاق الخط",
                    executionSummaryEn = "Terminate active phone call and release line"
                )
            }

            ActionType.TOGGLE_FLASHLIGHT -> {
                ProgrammaticCommand(
                    actionType = actionType,
                    payload = payload,
                    kotlinCodeSnippet = """
                        val cameraManager = context.getSystemService(CameraManager::class.java)
                        val cameraId = cameraManager.cameraIdList.firstOrNull()
                        cameraManager.setTorchMode(cameraId, !isTorchOn)
                    """.trimIndent(),
                    intentAction = "android.hardware.camera2.CameraManager#setTorchMode",
                    targetComponent = "CameraService / TorchManager",
                    adbShellCommand = "adb shell cmd camera set-torch 0 1",
                    executionSummaryAr = "تبديل تشغيل/إيقاف كشاف الكاميرا الخلفية (Torch)",
                    executionSummaryEn = "Toggle device rear camera flashlight"
                )
            }

            ActionType.SEND_MESSAGE -> {
                val (recipient, msg) = if (safePayload.contains(":")) {
                    safePayload.substringBefore(":").trim() to safePayload.substringAfter(":").trim()
                } else "جهة الاتصال" to safePayload.ifBlank { "مرحباً" }
                ProgrammaticCommand(
                    actionType = actionType,
                    payload = payload,
                    kotlinCodeSnippet = """
                        val smsManager = context.getSystemService(SmsManager::class.java)
                        smsManager.sendTextMessage("$recipient", null, "$msg", null, null)
                    """.trimIndent(),
                    intentAction = "android.telephony.SmsManager#sendTextMessage",
                    targetComponent = "SmsManager / Telecom Stack",
                    adbShellCommand = "adb shell service call isms 5 s16 \"$safePayload\"",
                    executionSummaryAr = "إرسال رسالة SMS نصية للهاتف: \"$msg\"",
                    executionSummaryEn = "Send standard SMS text message: \"$msg\""
                )
            }

            ActionType.SEND_WHATSAPP_MESSAGE -> {
                val (contact, msg) = if (safePayload.contains(":")) {
                    safePayload.substringBefore(":").trim() to safePayload.substringAfter(":").trim()
                } else safePayload to "مرحباً"
                ProgrammaticCommand(
                    actionType = actionType,
                    payload = payload,
                    kotlinCodeSnippet = """
                        val uri = Uri.parse("https://api.whatsapp.com/send?phone=$contact&text=" + Uri.encode("$msg"))
                        val waIntent = Intent(Intent.ACTION_VIEW, uri).apply {
                            `package` = "com.whatsapp"
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        context.startActivity(waIntent)
                        AuraAccessibilityService.instance?.dispatchAutonomousWhatsApp("$contact", "$msg")
                    """.trimIndent(),
                    intentAction = "android.intent.action.VIEW (com.whatsapp)",
                    targetComponent = "com.whatsapp / Autonomous Accessibility Dispatch",
                    adbShellCommand = "adb shell am start -a android.intent.action.VIEW -d \"https://api.whatsapp.com/send?phone=$contact&text=$msg\"",
                    executionSummaryAr = "فتح محادثة واتساب وإرسال النص \"$msg\" تلقائياً",
                    executionSummaryEn = "Autonomous WhatsApp message send to $contact"
                )
            }

            ActionType.SEND_MESSENGER_MESSAGE -> {
                ProgrammaticCommand(
                    actionType = actionType,
                    payload = payload,
                    kotlinCodeSnippet = """
                        val messengerIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, "$safePayload")
                            `package` = "com.facebook.orca"
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        context.startActivity(messengerIntent)
                        AuraAccessibilityService.instance?.dispatchAutonomousMessenger("", "$safePayload")
                    """.trimIndent(),
                    intentAction = "android.intent.action.SEND (com.facebook.orca)",
                    targetComponent = "com.facebook.orca / Messenger Dispatcher",
                    adbShellCommand = "adb shell am start -a android.intent.action.SEND -t text/plain -e android.intent.extra.TEXT \"$safePayload\" -p com.facebook.orca",
                    executionSummaryAr = "إرسال رسالة خاصة عبر فيسبوك ماسنجر",
                    executionSummaryEn = "Send direct message via Facebook Messenger"
                )
            }

            ActionType.POST_FACEBOOK -> {
                ProgrammaticCommand(
                    actionType = actionType,
                    payload = payload,
                    kotlinCodeSnippet = """
                        AuraAccessibilityService.instance?.dispatchAutonomousFacebookPost("$safePayload")
                    """.trimIndent(),
                    intentAction = "com.facebook.katana / Autonomous UI Composition",
                    targetComponent = "AuraAccessibilityService / Facebook Automation",
                    adbShellCommand = "adb shell am start -a android.intent.action.SEND -t text/plain -e android.intent.extra.TEXT \"$safePayload\" -p com.facebook.katana",
                    executionSummaryAr = "كتابة ونشر منشور فيسبوك تلقائياً",
                    executionSummaryEn = "Publish Facebook post autonomously"
                )
            }

            ActionType.READ_SCREEN_TEXT -> {
                ProgrammaticCommand(
                    actionType = actionType,
                    payload = payload,
                    kotlinCodeSnippet = """
                        val screenText = AuraAccessibilityService.instance?.extractVisibleScreenText()
                        voiceSpeechEngine.speak(screenText)
                    """.trimIndent(),
                    intentAction = "AccessibilityService#rootInActiveWindow -> TextToSpeech",
                    targetComponent = "AuraAccessibilityService / ScreenPerceptionEngine",
                    adbShellCommand = "adb shell uiautomator dump /dev/stdout",
                    executionSummaryAr = "استخراج وقراءة كافة النصوص الظاهرة على الشاشة صوتياً",
                    executionSummaryEn = "Extract and read screen text aloud via TTS"
                )
            }

            ActionType.SUMMARIZE_SCREEN -> {
                ProgrammaticCommand(
                    actionType = actionType,
                    payload = payload,
                    kotlinCodeSnippet = """
                        val screenContext = AuraAccessibilityService.instance?.extractScreenSummary()
                        val summary = geminiService.generateSummary(screenContext)
                        voiceSpeechEngine.speak(summary)
                    """.trimIndent(),
                    intentAction = "Accessibility Perception -> Gemini 2.5 Flash -> TTS",
                    targetComponent = "ScreenPerceptionEngine / GeminiService",
                    adbShellCommand = "adb shell am broadcast -a com.example.AURA_SUMMARIZE_SCREEN",
                    executionSummaryAr = "تحليل وفهم ما يظهر على الشاشة بواسطة الذكاء الاصطناعي",
                    executionSummaryEn = "AI real-time screen content understanding & summary"
                )
            }

            ActionType.CLOSE_APP -> {
                val app = safePayload.ifBlank { "Active Top App" }
                ProgrammaticCommand(
                    actionType = actionType,
                    payload = payload,
                    kotlinCodeSnippet = """
                        AuraAccessibilityService.instance?.performGlobalAction(GLOBAL_ACTION_HOME)
                    """.trimIndent(),
                    intentAction = "android.accessibilityservice.AccessibilityService#GLOBAL_ACTION_HOME",
                    targetComponent = "ActivityManager / Accessibility Global Action",
                    adbShellCommand = "adb shell input keyevent KEYCODE_HOME",
                    executionSummaryAr = "إغلاق التطبيق النشط ($app) والعودة للشاشة الرئيسية",
                    executionSummaryEn = "Close active app ($app) & return to Home screen"
                )
            }

            ActionType.RETURN_HOME -> {
                ProgrammaticCommand(
                    actionType = actionType,
                    payload = payload,
                    kotlinCodeSnippet = """
                        AuraAccessibilityService.instance?.performGlobalAction(GLOBAL_ACTION_HOME)
                    """.trimIndent(),
                    intentAction = "android.accessibilityservice.AccessibilityService#GLOBAL_ACTION_HOME",
                    targetComponent = "AccessibilityService#GLOBAL_ACTION_HOME",
                    adbShellCommand = "adb shell input keyevent KEYCODE_HOME",
                    executionSummaryAr = "العودة المباشرة للشاشة الرئيسية للهاتف",
                    executionSummaryEn = "Return to Android Home launcher"
                )
            }

            ActionType.GLOBAL_BACK -> {
                ProgrammaticCommand(
                    actionType = actionType,
                    payload = payload,
                    kotlinCodeSnippet = """
                        AuraAccessibilityService.instance?.performGlobalAction(GLOBAL_ACTION_BACK)
                    """.trimIndent(),
                    intentAction = "android.accessibilityservice.AccessibilityService#GLOBAL_ACTION_BACK",
                    targetComponent = "AccessibilityService#GLOBAL_ACTION_BACK",
                    adbShellCommand = "adb shell input keyevent KEYCODE_BACK",
                    executionSummaryAr = "الرجوع للخلف للشاشة السابقة",
                    executionSummaryEn = "Navigate Back in active application"
                )
            }

            ActionType.OPEN_APP -> {
                val appName = safePayload.ifBlank { "Application" }
                ProgrammaticCommand(
                    actionType = actionType,
                    payload = payload,
                    kotlinCodeSnippet = """
                        val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
                        context.startActivity(launchIntent)
                    """.trimIndent(),
                    intentAction = "android.intent.action.MAIN / CATEGORY_LAUNCHER",
                    targetComponent = "PackageManager / ActivityStarter ($appName)",
                    adbShellCommand = "adb shell monkey -p $safePayload -c android.intent.category.LAUNCHER 1",
                    executionSummaryAr = "تشغيل تطبيق ($appName) على الهاتف",
                    executionSummaryEn = "Launch application: $appName"
                )
            }

            ActionType.CLICK_SCREEN_ELEMENT -> {
                ProgrammaticCommand(
                    actionType = actionType,
                    payload = payload,
                    kotlinCodeSnippet = """
                        AuraAccessibilityService.instance?.performClickOnNode(listOf("$safePayload"))
                    """.trimIndent(),
                    intentAction = "AccessibilityNodeInfo#performAction(ACTION_CLICK)",
                    targetComponent = "AuraAccessibilityService / UI Automation",
                    adbShellCommand = "adb shell input tap x y",
                    executionSummaryAr = "النقر التلقائي على العنصر \"$safePayload\" الظاهر في الشاشة",
                    executionSummaryEn = "Click UI node matching: $safePayload"
                )
            }

            ActionType.TYPE_ON_SCREEN -> {
                ProgrammaticCommand(
                    actionType = actionType,
                    payload = payload,
                    kotlinCodeSnippet = """
                        AuraAccessibilityService.instance?.typeTextInActiveField("$safePayload")
                    """.trimIndent(),
                    intentAction = "AccessibilityNodeInfo#performAction(ACTION_SET_TEXT)",
                    targetComponent = "AuraAccessibilityService / Text Input",
                    adbShellCommand = "adb shell input text \"$safePayload\"",
                    executionSummaryAr = "كتابة النص \"$safePayload\" في الحقل النشط على الشاشة",
                    executionSummaryEn = "Type text in active field on screen"
                )
            }

            ActionType.OPEN_CAMERA -> {
                ProgrammaticCommand(
                    actionType = actionType,
                    payload = payload,
                    kotlinCodeSnippet = """
                        val camIntent = Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        context.startActivity(camIntent)
                    """.trimIndent(),
                    intentAction = "android.media.action.STILL_IMAGE_CAMERA",
                    targetComponent = "Camera App / MediaStore",
                    adbShellCommand = "adb shell am start -a android.media.action.STILL_IMAGE_CAMERA",
                    executionSummaryAr = "فتح كاميرا الهاتف لالتقاط صورة",
                    executionSummaryEn = "Launch camera for still image capture"
                )
            }

            ActionType.TAKE_SCREENSHOT -> {
                ProgrammaticCommand(
                    actionType = actionType,
                    payload = payload,
                    kotlinCodeSnippet = """
                        AuraAccessibilityService.instance?.performGlobalAction(GLOBAL_ACTION_TAKE_SCREENSHOT)
                    """.trimIndent(),
                    intentAction = "AccessibilityService#GLOBAL_ACTION_TAKE_SCREENSHOT",
                    targetComponent = "SystemUI ScreenshotController",
                    adbShellCommand = "adb shell screencap -p /sdcard/screenshot.png",
                    executionSummaryAr = "التقاط صورة لشاشة الهاتف وحفظها",
                    executionSummaryEn = "Capture screenshot to device storage"
                )
            }

            ActionType.OPEN_SETTINGS -> {
                ProgrammaticCommand(
                    actionType = actionType,
                    payload = payload,
                    kotlinCodeSnippet = """
                        val settingsIntent = Intent(Settings.ACTION_SETTINGS).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        context.startActivity(settingsIntent)
                    """.trimIndent(),
                    intentAction = "android.settings.SETTINGS",
                    targetComponent = "com.android.settings",
                    adbShellCommand = "adb shell am start -a android.settings.SETTINGS",
                    executionSummaryAr = "فتح لوحة إعدادات النظام الرئيسية",
                    executionSummaryEn = "Open main system settings"
                )
            }

            ActionType.WEB_SEARCH -> {
                ProgrammaticCommand(
                    actionType = actionType,
                    payload = payload,
                    kotlinCodeSnippet = """
                        val searchIntent = Intent(Intent.ACTION_WEB_SEARCH).apply {
                            putExtra(SearchManager.QUERY, "$safePayload")
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        context.startActivity(searchIntent)
                    """.trimIndent(),
                    intentAction = "android.intent.action.WEB_SEARCH",
                    targetComponent = "Default Browser / Google Search",
                    adbShellCommand = "adb shell am start -a android.intent.action.WEB_SEARCH --es query \"$safePayload\"",
                    executionSummaryAr = "إجراء بحث فوري في جوجل عن: \"$safePayload\"",
                    executionSummaryEn = "Perform web search for: \"$safePayload\""
                )
            }

            ActionType.READ_SCREEN_TEXT -> {
                ProgrammaticCommand(
                    actionType = actionType,
                    payload = payload,
                    kotlinCodeSnippet = """
                        // Accessibility Screen Text Extraction & TTS
                        val screenText = AuraAccessibilityService.instance?.extractVisibleScreenText()
                        voiceEngine.speak(screenText ?: "لا توجد نصوص مقروءة")
                    """.trimIndent(),
                    intentAction = "AuraAccessibilityService#extractVisibleScreenText",
                    targetComponent = "AccessibilityNodeInfo Hierarchy Walker",
                    adbShellCommand = "adb shell uiautomator dump /sdcard/window_dump.xml && adb shell cat /sdcard/window_dump.xml",
                    executionSummaryAr = "استخراج وقراءة كافة النصوص المعروضة على الشاشة الحالية بصوت ناطق",
                    executionSummaryEn = "Extract and read all visible on-screen texts aloud"
                )
            }

            ActionType.SUMMARIZE_SCREEN -> {
                ProgrammaticCommand(
                    actionType = actionType,
                    payload = payload,
                    kotlinCodeSnippet = """
                        // AI Screen Perception & Summarization
                        val rawText = AuraAccessibilityService.instance?.extractVisibleScreenText()
                        val summary = geminiService.summarizeScreenContent(rawText)
                        voiceEngine.speak(summary)
                    """.trimIndent(),
                    intentAction = "GeminiService#summarizeScreenContent",
                    targetComponent = "Gemini Multi-Modal Screen Perception Engine",
                    adbShellCommand = "adb shell uiautomator dump /sdcard/window_dump.xml",
                    executionSummaryAr = "تحليل المحتوى المعروض على الشاشة وتلخيصه بالذكاء الاصطناعي",
                    executionSummaryEn = "Perceive and summarize current screen content with AI"
                )
            }

            ActionType.TYPE_ON_SCREEN -> {
                val textToType = safePayload.ifBlank { "نص مخصص" }
                ProgrammaticCommand(
                    actionType = actionType,
                    payload = payload,
                    kotlinCodeSnippet = """
                        // Direct Typing into Active Input Field
                        AuraAccessibilityService.instance?.typeTextInActiveField("$textToType")
                    """.trimIndent(),
                    intentAction = "AccessibilityNodeInfo#ACTION_SET_TEXT",
                    targetComponent = "Active Focus View / EditText",
                    adbShellCommand = "adb shell input text \"${textToType.replace(" ", "%s")}\"",
                    executionSummaryAr = "كتابة النص \"$textToType\" تلقائياً في الحقل النشط على الشاشة",
                    executionSummaryEn = "Autonomously type text \"$textToType\" into active field"
                )
            }

            else -> {
                ProgrammaticCommand(
                    actionType = actionType,
                    payload = payload,
                    kotlinCodeSnippet = """
                        actionExecutionEngine.executeAction(ActionType.$actionType, ${if (payload != null) "\"$payload\"" else "null"})
                    """.trimIndent(),
                    intentAction = "com.example.system.ActionExecutionEngine#executeAction",
                    targetComponent = "Aura Engine ($actionType)",
                    adbShellCommand = "adb shell am broadcast -a com.example.AURA_ACTION -e type $actionType",
                    executionSummaryAr = "تنفيذ إجراء $actionType على الهاتف",
                    executionSummaryEn = "Execute $actionType on hardware"
                )
            }
        }
    }
}
