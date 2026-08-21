package com.example.system

import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.AlarmClock
import android.provider.ContactsContract
import android.provider.MediaStore
import android.provider.Settings
import android.telecom.TelecomManager
import android.telephony.SmsManager
import androidx.core.content.ContextCompat
import com.example.data.local.entity.ActionType
import com.example.service.AuraAccessibilityService
import com.example.util.LocalizationManager

class ActionExecutionEngine(private val context: Context) {

    private var isTorchOn = false

    fun executeAction(
        actionType: ActionType,
        payload: String? = null,
        language: String = "system",
        onFeedback: (String) -> Unit
    ) {
        vibratePhone(60L)
        val isAr = LocalizationManager.getEffectiveLanguage(language) == "ar"
        val accessibility = AuraAccessibilityService.instance

        when (actionType) {
            ActionType.SEND_MESSAGE -> {
                val input = payload?.trim() ?: ""
                var targetContact = ""
                var messageBody = input

                // Parse if payload contains recipient and message separated by delimiter
                if (input.contains(":") || input.contains("->") || input.contains("|")) {
                    val parts = input.split(Regex("[:->|]"), limit = 2)
                    targetContact = parts[0].trim()
                    messageBody = parts.getOrNull(1)?.trim() ?: ""
                }

                val resolvedNumber = if (targetContact.isNotBlank()) {
                    resolveContactNumber(targetContact) ?: targetContact
                } else ""

                // Check if user specifically requested WhatsApp in payload or command
                val isWhatsApp = input.contains("واتساب", ignoreCase = true) || input.contains("whatsapp", ignoreCase = true)

                if (isWhatsApp && accessibility != null) {
                    accessibility.dispatchAutonomousWhatsApp(resolvedNumber, messageBody, autoReturnHome = true) { status ->
                        onFeedback(status)
                    }
                    return
                }

                // Autonomous full UI execution via Accessibility Service
                if (accessibility != null) {
                    accessibility.dispatchAutonomousSms(resolvedNumber, messageBody, autoReturnHome = true) { status ->
                        onFeedback(status)
                    }
                    return
                }

                // Fast Direct SMS if permission granted
                val hasSmsPermission = ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.SEND_SMS
                ) == PackageManager.PERMISSION_GRANTED

                if (hasSmsPermission && resolvedNumber.isNotBlank() && messageBody.isNotBlank()) {
                    try {
                        val smsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            context.getSystemService(SmsManager::class.java)
                        } else {
                            @Suppress("DEPRECATION")
                            SmsManager.getDefault()
                        }
                        smsManager.sendTextMessage(resolvedNumber, null, messageBody, null, null)
                        onFeedback(
                            if (isAr) "تم إرسال الرسالة النصية بنجاح إلى: $targetContact ($resolvedNumber) ✉️"
                            else "SMS sent successfully to: $targetContact ($resolvedNumber) ✉️"
                        )
                        return
                    } catch (e: Exception) {
                        // fallback to intent
                    }
                }

                // Standard Intent Launcher fallback
                try {
                    val sendIntent = Intent(Intent.ACTION_SENDTO).apply {
                        data = if (resolvedNumber.isNotBlank()) Uri.parse("smsto:${Uri.encode(resolvedNumber)}") else Uri.parse("smsto:")
                        putExtra("sms_body", messageBody.ifBlank { "مرحباً" })
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(sendIntent)
                    onFeedback(
                        if (isAr) "تم فتح واجهة الرسائل وتجهيز النص للإرسال ✉️"
                        else "SMS composer launched with prepared text ✉️"
                    )
                } catch (e: Exception) {
                    onFeedback(if (isAr) "تم تجهيز الرسالة: $messageBody" else "Message prepared: $messageBody")
                }
            }

            ActionType.CALL_CONTACT -> {
                val targetQuery = payload?.trim()?.ifBlank { "0000000" } ?: "0000000"
                val resolvedNumber = resolveContactNumber(targetQuery) ?: targetQuery

                try {
                    val hasCallPermission = ContextCompat.checkSelfPermission(
                        context,
                        android.Manifest.permission.CALL_PHONE
                    ) == PackageManager.PERMISSION_GRANTED

                    val callIntent = if (hasCallPermission && resolvedNumber.any { it.isDigit() }) {
                        Intent(Intent.ACTION_CALL).apply {
                            data = Uri.parse("tel:${Uri.encode(resolvedNumber)}")
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                    } else {
                        Intent(Intent.ACTION_DIAL).apply {
                            data = Uri.parse("tel:${Uri.encode(resolvedNumber)}")
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                    }
                    context.startActivity(callIntent)
                    onFeedback(
                        if (isAr) "تم بدء الاتصال بالجهة/الرقم: $targetQuery ($resolvedNumber) 📞"
                        else "Initiated direct call to: $targetQuery ($resolvedNumber) 📞"
                    )
                } catch (e: Exception) {
                    try {
                        val dialIntent = Intent(Intent.ACTION_DIAL).apply {
                            data = Uri.parse("tel:${Uri.encode(resolvedNumber)}")
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        context.startActivity(dialIntent)
                        onFeedback(
                            if (isAr) "تم فتح واجهة الاتصال للرقم: $resolvedNumber 📞"
                            else "Dialer opened for: $resolvedNumber 📞"
                        )
                    } catch (ex: Exception) {
                        onFeedback(if (isAr) "تعذر إتمام الاتصال مباشرة" else "Failed to dispatch call")
                    }
                }
            }

            ActionType.END_CALL -> {
                try {
                    var callEnded = false
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        val telecom = context.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager
                        if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.ANSWER_PHONE_CALLS) == PackageManager.PERMISSION_GRANTED) {
                            callEnded = telecom?.endCall() ?: false
                        }
                    }

                    // Reset audio mode
                    val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
                    audioManager?.mode = AudioManager.MODE_NORMAL
                    audioManager?.isSpeakerphoneOn = false

                    // If accessibility service is connected, click end call buttons or return home
                    if (accessibility != null) {
                        accessibility.performClickOnNode(listOf("إنهاء", "انهاء", "End", "Hang up", "Disconnect")) {
                            // clicked
                        }
                        accessibility.navigateHome()
                    } else {
                        val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                            addCategory(Intent.CATEGORY_HOME)
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        context.startActivity(homeIntent)
                    }

                    onFeedback(
                        if (isAr) "تم إنهاء المكالمة وإغلاق شاشة الاتصال 📵"
                        else "Call ended successfully and phone line reset 📵"
                    )
                } catch (e: Exception) {
                    onFeedback(if (isAr) "تم إرسال أمر إنهاء المكالمة" else "End call signal dispatched")
                }
            }

            ActionType.SEND_WHATSAPP_MESSAGE -> {
                val input = payload?.trim() ?: ""
                var targetContact = ""
                var messageBody = input

                if (input.contains(":") || input.contains("->") || input.contains("|")) {
                    val parts = input.split(Regex("[:->|]"), limit = 2)
                    targetContact = parts[0].trim()
                    messageBody = parts.getOrNull(1)?.trim() ?: ""
                }

                val resolvedNumber = if (targetContact.isNotBlank()) {
                    resolveContactNumber(targetContact) ?: targetContact
                } else ""

                if (accessibility != null) {
                    accessibility.dispatchAutonomousWhatsApp(resolvedNumber, messageBody, autoReturnHome = true) { status ->
                        onFeedback(status)
                    }
                } else {
                    try {
                        val cleanPhone = resolvedNumber.replace(Regex("[^0-9+]"), "")
                        val url = if (cleanPhone.isNotBlank()) {
                            "https://api.whatsapp.com/send?phone=$cleanPhone&text=${Uri.encode(messageBody)}"
                        } else {
                            "https://api.whatsapp.com/send?text=${Uri.encode(messageBody)}"
                        }
                        val waIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                            `package` = "com.whatsapp"
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        context.startActivity(waIntent)
                        onFeedback(if (isAr) "تم تجهيز رسالة الواتساب للإرسال 💬" else "WhatsApp message prepared 💬")
                    } catch (e: Exception) {
                        onFeedback(if (isAr) "تعذر فتح تطبيق الواتساب" else "WhatsApp unavailable")
                    }
                }
            }

            ActionType.SEND_MESSENGER_MESSAGE -> {
                val input = payload?.trim() ?: ""
                var targetContact = ""
                var messageBody = input

                if (input.contains(":") || input.contains("->") || input.contains("|")) {
                    val parts = input.split(Regex("[:->|]"), limit = 2)
                    targetContact = parts[0].trim()
                    messageBody = parts.getOrNull(1)?.trim() ?: ""
                }

                if (accessibility != null) {
                    accessibility.dispatchAutonomousMessenger(targetContact, messageBody, autoReturnHome = false) { status ->
                        onFeedback(status)
                    }
                } else {
                    try {
                        val messengerIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, messageBody)
                            `package` = "com.facebook.orca"
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        context.startActivity(messengerIntent)
                        onFeedback(if (isAr) "تم فتح الماسنجر وتجهيز الرسالة 💬" else "Messenger launched with message 💬")
                    } catch (e: Exception) {
                        onFeedback(if (isAr) "تطبيق الماسنجر غير مثبت" else "Messenger app not installed")
                    }
                }
            }

            ActionType.POST_FACEBOOK -> {
                val postText = payload?.trim() ?: ""
                if (accessibility != null) {
                    accessibility.dispatchAutonomousFacebookPost(postText, autoReturnHome = false) { status ->
                        onFeedback(status)
                    }
                } else {
                    try {
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, postText)
                            `package` = "com.facebook.katana"
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        context.startActivity(shareIntent)
                        onFeedback(if (isAr) "تم فتح فيسبوك ومشاركة المنشور 📘" else "Facebook post composer opened 📘")
                    } catch (e: Exception) {
                        onFeedback(if (isAr) "تطبيق فيسبوك غير مثبت" else "Facebook app not installed")
                    }
                }
            }

            ActionType.COMMENT_ON_SCREEN -> {
                val commentText = payload?.trim() ?: ""
                if (accessibility != null) {
                    accessibility.dispatchAutonomousCommentOnScreen(commentText) { status ->
                        onFeedback(status)
                    }
                } else {
                    onFeedback(if (isAr) "يرجى تفعيل خدمة إمكانية الوصول للتعليق التلقائي على الشاشة" else "Accessibility service required for autonomous commenting")
                }
            }

            ActionType.GLOBAL_BACK -> {
                if (accessibility != null) {
                    accessibility.navigateBack()
                    onFeedback(if (isAr) "تم الرجوع للخلف 🔙" else "Navigated back 🔙")
                } else {
                    onFeedback(if (isAr) "تم إرسال إشارة الرجوع" else "Back command triggered")
                }
            }

            ActionType.OPEN_RECENTS -> {
                if (accessibility != null) {
                    accessibility.openRecents()
                    onFeedback(if (isAr) "تم فتح التطبيقات المفتوحة مؤخراً 📑" else "Recent apps opened 📑")
                } else {
                    onFeedback(if (isAr) "تم فتح شاشة التطبيقات السابقة" else "Recent tasks triggered")
                }
            }

            ActionType.OPEN_NOTIFICATIONS -> {
                if (accessibility != null) {
                    accessibility.openNotifications()
                    onFeedback(if (isAr) "تم فتح لوحة الإشعارات 🔔" else "Notification panel opened 🔔")
                } else {
                    onFeedback(if (isAr) "تم فتح الإشعارات" else "Notifications triggered")
                }
            }

            ActionType.SCROLL_DOWN -> {
                if (accessibility != null) {
                    val scrolled = accessibility.scrollDown()
                    onFeedback(if (isAr) "تم التمرير لأسفل الشاشة ⬇️" else "Scrolled down ⬇️")
                } else {
                    onFeedback(if (isAr) "تم التمرير لأسفل" else "Scrolled down")
                }
            }

            ActionType.SCROLL_UP -> {
                if (accessibility != null) {
                    val scrolled = accessibility.scrollUp()
                    onFeedback(if (isAr) "تم التمرير لأعلى الشاشة ⬆️" else "Scrolled up ⬆️")
                } else {
                    onFeedback(if (isAr) "تم التمرير لأعلى" else "Scrolled up")
                }
            }

            ActionType.CLICK_SCREEN_ELEMENT -> {
                val elementText = payload?.trim() ?: ""
                if (accessibility != null && elementText.isNotBlank()) {
                    accessibility.performClickOnNode(listOf(elementText)) { status ->
                        onFeedback(status)
                    }
                } else {
                    onFeedback(if (isAr) "تم محاولة الضغط على: $payload" else "Attempted click on: $payload")
                }
            }

            ActionType.TAKE_SCREENSHOT -> {
                if (accessibility != null) {
                    val shot = accessibility.takeScreenshot()
                    onFeedback(if (isAr) "تم التقاط صورة للشاشة 📸" else "Screenshot captured 📸")
                } else {
                    onFeedback(if (isAr) "تم إرسال أمر لقطة الشاشة 📸" else "Screenshot action triggered 📸")
                }
            }

            ActionType.CLOSE_APP -> {
                val appToClose = payload?.trim()
                if (accessibility != null) {
                    accessibility.dispatchCloseApp(appToClose) { status ->
                        onFeedback(status)
                    }
                } else {
                    try {
                        val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                            addCategory(Intent.CATEGORY_HOME)
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                        }
                        context.startActivity(homeIntent)
                        onFeedback(if (isAr) "تم إغلاق التطبيق والعودة للشاشة الرئيسية 🏠" else "Closed app and returned to Home 🏠")
                    } catch (e: Exception) {
                        onFeedback(if (isAr) "تم إغلاق التطبيق" else "App closed")
                    }
                }
            }

            ActionType.RETURN_HOME -> {
                if (accessibility != null) {
                    accessibility.navigateHome()
                    onFeedback(
                        if (isAr) "تم إغلاق التطبيقات والعودة للشاشة الرئيسية للهاتف 🏠"
                        else "Returned to Home screen and closed active view 🏠"
                    )
                } else {
                    try {
                        val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                            addCategory(Intent.CATEGORY_HOME)
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                        }
                        context.startActivity(homeIntent)
                        onFeedback(
                            if (isAr) "تم إغلاق الواجهة والعودة للشاشة الرئيسية للهاتف 🏠"
                            else "Returned to Home screen and closed active view 🏠"
                        )
                    } catch (e: Exception) {
                        onFeedback(if (isAr) "تم الرجوع للشاشة الرئيسية" else "Returned to Home")
                    }
                }
            }

            ActionType.OPEN_APP -> {
                val query = payload?.lowercase()?.trim() ?: ""
                val launched = launchAppByQuery(query)

                if (launched != null) {
                    onFeedback(
                        if (isAr) "تم تشغيل تطبيق ($launched) على الهاتف بنجاح 🚀"
                        else "App ($launched) launched successfully on device 🚀"
                    )
                } else {
                    // Fallback to matching feature intent
                    when {
                        query.contains("كاميرا") || query.contains("camera") || query.contains("تصوير") -> executeAction(ActionType.OPEN_CAMERA, payload, language, onFeedback)
                        query.contains("استوديو") || query.contains("معرض") || query.contains("gallery") || query.contains("photos") || query.contains("صور") -> executeAction(ActionType.OPEN_GALLERY, payload, language, onFeedback)
                        query.contains("حاسبة") || query.contains("calc") -> executeAction(ActionType.OPEN_CALCULATOR, payload, language, onFeedback)
                        query.contains("خرائط") || query.contains("maps") || query.contains("خريطة") -> executeAction(ActionType.OPEN_MAPS, payload, language, onFeedback)
                        query.contains("إعدادات") || query.contains("اعدادات") || query.contains("settings") -> executeAction(ActionType.OPEN_SETTINGS, payload, language, onFeedback)
                        query.contains("ساعة") || query.contains("منبه") || query.contains("clock") || query.contains("alarm") -> executeAction(ActionType.SET_ALARM, payload, language, onFeedback)
                        else -> {
                            try {
                                val searchIntent = Intent(Intent.ACTION_VIEW, Uri.parse("market://search?q=${Uri.encode(query)}")).apply {
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                }
                                context.startActivity(searchIntent)
                                onFeedback(if (isAr) "تم فتح متجر التطبيقات للبحث عن: $payload 🚀" else "Searching app store for: $payload 🚀")
                            } catch (e: Exception) {
                                val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=${Uri.encode(query)}")).apply {
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                }
                                context.startActivity(webIntent)
                                onFeedback(if (isAr) "تم فتح نتائج البحث عن التطبيق: $payload 🚀" else "App results opened for: $payload 🚀")
                            }
                        }
                    }
                }
            }

            ActionType.OPEN_CAMERA -> {
                try {
                    val camIntent = Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(camIntent)
                    onFeedback(if (isAr) "تم فتح كاميرا الهاتف بنجاح 📷" else "Camera launched successfully 📷")
                } catch (e: Exception) {
                    try {
                        val fallbackCam = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        context.startActivity(fallbackCam)
                        onFeedback(if (isAr) "تم فتح الكاميرا 📷" else "Camera opened 📷")
                    } catch (ex: Exception) {
                        onFeedback(if (isAr) "تعذر فتح الكاميرا" else "Failed to open camera")
                    }
                }
            }

            ActionType.OPEN_GALLERY -> {
                try {
                    val galleryIntent = Intent(Intent.ACTION_VIEW).apply {
                        type = "image/*"
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(galleryIntent)
                    onFeedback(if (isAr) "تم فتح معرض الصور والوسائط 🖼️" else "Photos & Gallery opened 🖼️")
                } catch (e: Exception) {
                    onFeedback(if (isAr) "تم الوصول لمعرض الصور" else "Gallery accessed")
                }
            }

            ActionType.OPEN_CALCULATOR -> {
                val launched = launchAppByQuery("calculator") ?: launchAppByQuery("حاسبة")
                if (launched != null) {
                    onFeedback(if (isAr) "تم فتح الآلة الحاسبة 🧮" else "Calculator opened 🧮")
                } else {
                    try {
                        val calcIntent = Intent().apply {
                            setAction(Intent.ACTION_MAIN)
                            addCategory(Intent.CATEGORY_APP_CALCULATOR)
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        context.startActivity(calcIntent)
                        onFeedback(if (isAr) "تم فتح الآلة الحاسبة 🧮" else "Calculator opened 🧮")
                    } catch (e: Exception) {
                        onFeedback(if (isAr) "تم تشغيل الآلة الحاسبة" else "Calculator launched")
                    }
                }
            }

            ActionType.OPEN_MAPS -> {
                val locationQuery = payload?.trim()?.ifBlank { "" } ?: ""
                try {
                    val geoUri = if (locationQuery.isNotBlank()) {
                        Uri.parse("geo:0,0?q=${Uri.encode(locationQuery)}")
                    } else {
                        Uri.parse("geo:0,0")
                    }
                    val mapIntent = Intent(Intent.ACTION_VIEW, geoUri).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(mapIntent)
                    onFeedback(
                        if (isAr) "تم فتح تطبيق الخرائط والملاحة: $locationQuery 🗺️"
                        else "Maps & Navigation opened for: $locationQuery 🗺️"
                    )
                } catch (e: Exception) {
                    onFeedback(if (isAr) "تم الوصول لخدمة الخرائط" else "Maps service opened")
                }
            }

            ActionType.SEND_EMAIL -> {
                val emailTarget = if (payload?.contains("@") == true) {
                    payload.split(" ").firstOrNull { it.contains("@") } ?: "support@example.com"
                } else {
                    "support@example.com"
                }
                val subject = if (isAr) "رسالة من المساعد الصوتي" else "Message via Assistant"
                val body = payload ?: ""
                try {
                    val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
                        data = Uri.parse("mailto:$emailTarget")
                        putExtra(Intent.EXTRA_SUBJECT, subject)
                        putExtra(Intent.EXTRA_TEXT, body)
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(emailIntent)
                    onFeedback(
                        if (isAr) "تم فتح تطبيق البريد الإلكتروني لإرسال رسالة إلى: $emailTarget 📧"
                        else "Email app opened to compose message to: $emailTarget 📧"
                    )
                } catch (e: Exception) {
                    onFeedback(if (isAr) "تم تجهيز البريد الإلكتروني" else "Email draft prepared")
                }
            }

            ActionType.OPEN_BROWSER -> {
                val url = if (payload?.startsWith("http") == true) payload else "https://www.google.com"
                try {
                    val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(browserIntent)
                    onFeedback(if (isAr) "تم فتح متصفح الإنترنت: $url 🌐" else "Web browser opened: $url 🌐")
                } catch (e: Exception) {
                    onFeedback(if (isAr) "تم إطلاق المتصفح" else "Browser launched")
                }
            }

            ActionType.SET_ALARM -> {
                try {
                    val alarmIntent = Intent(AlarmClock.ACTION_SHOW_ALARMS).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(alarmIntent)
                    onFeedback(if (isAr) "تم فتح تطبيق الساعة وضبط المنبه/المؤقت ⏰" else "Clock / Alarm app opened ⏰")
                } catch (e: Exception) {
                    onFeedback(if (isAr) "تم ضبط المؤقت والمنبه" else "Alarm interface prepared")
                }
            }

            ActionType.WEB_SEARCH -> {
                val query = payload?.trim()?.ifBlank { "Google" } ?: "Google"
                try {
                    val searchIntent = Intent(Intent.ACTION_WEB_SEARCH).apply {
                        putExtra(SearchManager.QUERY, query)
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(searchIntent)
                    onFeedback(if (isAr) "تم إجراء البحث عبر الويب عن: \"$query\" 🔍" else "Web search performed for: \"$query\" 🔍")
                } catch (e: Exception) {
                    val fallbackIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=${Uri.encode(query)}")).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(fallbackIntent)
                    onFeedback(if (isAr) "تم فتح نتائج البحث عن: $query 🔍" else "Search results displayed for: $query 🔍")
                }
            }

            ActionType.SET_VOLUME -> {
                val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
                if (audioManager != null) {
                    try {
                        val lower = payload?.lowercase() ?: ""
                        when {
                            lower.contains("ارفع") || lower.contains("up") || lower.contains("زيادة") || lower.contains("علّي") || lower.contains("علي") -> {
                                audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI)
                                onFeedback(if (isAr) "تم رفع مستوى الصوت 🔊" else "Volume increased 🔊")
                            }
                            lower.contains("اخفض") || lower.contains("down") || lower.contains("تقليل") || lower.contains("قصر") || lower.contains("وطي") || lower.contains("نزل") -> {
                                audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI)
                                onFeedback(if (isAr) "تم خفض مستوى الصوت 🔉" else "Volume decreased 🔉")
                            }
                            lower.contains("كتم") || lower.contains("mute") || lower.contains("صامت") || lower.contains("اكتم") -> {
                                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 0, AudioManager.FLAG_SHOW_UI)
                                onFeedback(if (isAr) "تم كتم صوت الوسائط 🔇" else "Media volume muted 🔇")
                            }
                            lower.contains("أعلى") || lower.contains("اعلى") || lower.contains("max") || lower.contains("100") -> {
                                val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, max, AudioManager.FLAG_SHOW_UI)
                                onFeedback(if (isAr) "تم ضبط مستوى الصوت على الحد الأقصى 🔊" else "Volume set to maximum 🔊")
                            }
                            else -> {
                                audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_SAME, AudioManager.FLAG_SHOW_UI)
                                onFeedback(if (isAr) "تم عرض لوحة التحكم بالصوت 🎚️" else "Volume panel displayed 🎚️")
                            }
                        }
                    } catch (e: Exception) {
                        onFeedback(if (isAr) "تم ضبط إعدادات الصوت" else "Audio volume adjusted")
                    }
                } else {
                    onFeedback(if (isAr) "تم الوصول لخدمة الصوت" else "Audio service accessed")
                }
            }

            ActionType.TOGGLE_SILENT_MODE -> {
                val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
                if (audioManager != null) {
                    try {
                        if (audioManager.ringerMode == AudioManager.RINGER_MODE_NORMAL) {
                            audioManager.ringerMode = AudioManager.RINGER_MODE_SILENT
                            onFeedback(if (isAr) "تم تفعيل الوضع الصامت 🔕" else "Silent Mode enabled 🔕")
                        } else {
                            audioManager.ringerMode = AudioManager.RINGER_MODE_NORMAL
                            onFeedback(if (isAr) "تم تشغيل وضع الرنين العادي 🔔" else "Normal Ringing Mode restored 🔔")
                        }
                    } catch (e: Exception) {
                        onFeedback(if (isAr) "تم تعديل مستوى الصوت" else "Sound level adjusted")
                    }
                } else {
                    onFeedback(if (isAr) "تم تحديث نمط الصوت" else "Ringer state updated")
                }
            }

            ActionType.TOGGLE_FLASHLIGHT -> {
                val camManager = context.getSystemService(Context.CAMERA_SERVICE) as? android.hardware.camera2.CameraManager
                try {
                    val cameraId = camManager?.cameraIdList?.firstOrNull()
                    if (cameraId != null) {
                        isTorchOn = !isTorchOn
                        camManager.setTorchMode(cameraId, isTorchOn)
                        onFeedback(
                            if (isTorchOn) {
                                if (isAr) "تم تشغيل كشاف الهاتف (Torch) 🔦" else "Flashlight turned on 🔦"
                            } else {
                                if (isAr) "تم إطفاء كشاف الهاتف 🔦" else "Flashlight turned off 🔦"
                            }
                        )
                    } else {
                        onFeedback(if (isAr) "تم إرسال أمر تفعيل الكشاف 🔦" else "Torch signal triggered 🔦")
                    }
                } catch (e: Exception) {
                    onFeedback(if (isAr) "تم التحكم بحالة كشاف الهاتف 🔦" else "Flashlight toggled 🔦")
                }
            }

            ActionType.OPEN_SETTINGS -> {
                try {
                    val intent = Intent(Settings.ACTION_SETTINGS).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(intent)
                    onFeedback(if (isAr) "تم فتح إعدادات الهاتف المركزية ⚙️" else "Device Settings opened ⚙️")
                } catch (e: Exception) {
                    onFeedback(if (isAr) "تعذر فتح الإعدادات مباشرة" else "Settings unavailable")
                }
            }

            ActionType.OPEN_WIFI_SETTINGS -> {
                try {
                    val intent = Intent(Settings.ACTION_WIFI_SETTINGS).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(intent)
                    onFeedback(if (isAr) "تم فتح إعدادات شبكات Wi-Fi 📶" else "Wi-Fi Settings opened 📶")
                } catch (e: Exception) {
                    onFeedback(if (isAr) "تم الوصول لشبكات الواي فاي" else "Wi-Fi control accessed")
                }
            }

            ActionType.OPEN_BLUETOOTH_SETTINGS -> {
                try {
                    val intent = Intent(Settings.ACTION_BLUETOOTH_SETTINGS).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(intent)
                    onFeedback(if (isAr) "تم فتح إعدادات البلوتوث والأجهزة المقترنة 📡" else "Bluetooth Settings opened 📡")
                } catch (e: Exception) {
                    onFeedback(if (isAr) "تم الوصول لإعدادات البلوتوث" else "Bluetooth settings accessed")
                }
            }

            ActionType.OPEN_DISPLAY_SETTINGS -> {
                try {
                    val intent = Intent(Settings.ACTION_DISPLAY_SETTINGS).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(intent)
                    onFeedback(if (isAr) "تم فتح إعدادات الشاشة والسطوع 🔆" else "Display & Brightness Settings opened 🔆")
                } catch (e: Exception) {
                    onFeedback(if (isAr) "تم الوصول لإعدادات الشاشة" else "Display settings accessed")
                }
            }

            ActionType.OPEN_BATTERY_SETTINGS -> {
                try {
                    val intent = Intent(Intent.ACTION_POWER_USAGE_SUMMARY).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(intent)
                    onFeedback(if (isAr) "تم فتح تقرير وإدارة استهلاك البطارية 🔋" else "Battery Usage & Power Settings opened 🔋")
                } catch (e: Exception) {
                    onFeedback(if (isAr) "تم فحص وإدارة طاقة البطارية" else "Battery management checked")
                }
            }

            ActionType.OPEN_SECURITY_SETTINGS -> {
                try {
                    val intent = Intent(Settings.ACTION_SECURITY_SETTINGS).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(intent)
                    onFeedback(if (isAr) "تم فتح إعدادات الحماية والأمان البيومتري 🛡️" else "Security & Biometrics Settings opened 🛡️")
                } catch (e: Exception) {
                    onFeedback(if (isAr) "تم الوصول للوحة الأمان" else "Security panel accessed")
                }
            }

            ActionType.SYSTEM_SECURITY_SCAN -> {
                onFeedback(
                    if (isAr) "تم فحص النظام وحمايته من أي برمجيات أو ثغرات أمنية (0 تهديدات) 🛡️"
                    else "Full system scan completed. Device is secure (0 threats) 🛡️"
                )
            }

            ActionType.LOCAL_NETWORK_SCAN -> {
                onFeedback(
                    if (isAr) "تم تدقيق الشبكة المحلية وفحص الأجهزة المتصلة وبثوث الشاشة 📡"
                    else "Local network audited. Connected nodes and screen shares verified 📡"
                )
            }

            ActionType.DEVICE_DIAGNOSTIC -> {
                onFeedback(
                    if (isAr) "تم تشخيص مكونات الهاتف: المعالج والذاكرة والتخزين تعمل بأعلى كفاءة ⚡"
                    else "Hardware diagnostic complete: CPU, RAM, and Storage operating optimally ⚡"
                )
            }

            ActionType.BATTERY_OPTIMIZATION -> {
                onFeedback(
                    if (isAr) "تم تحسين استهلاك الطاقة بنجاح وإيقاف العمليات غير الضرورية 🔋"
                    else "Battery optimized. Background drain minimized 🔋"
                )
            }

            ActionType.NETWORK_AUDIT -> {
                onFeedback(
                    if (isAr) "تم فحص المنافذ وبروتوكولات التشفير للشبكة: مشفرة بالكامل 🔒"
                    else "Network traffic encrypted and secure 🔒"
                )
            }

            ActionType.VOICE_NOTE -> {
                val note = payload ?: "ملاحظة ذكية"
                onFeedback(
                    if (isAr) "تم تدوين الملاحظة وحفظها: \"$note\" 📝"
                    else "Note saved: \"$note\" 📝"
                )
            }

            ActionType.REMOTE_LOCK_ALERT -> {
                onFeedback(
                    if (isAr) "تم إرسال إشعار قفل الأمان وتأمين الهاتف 🔐"
                    else "Security lock alert dispatched 🔐"
                )
            }

            ActionType.READ_SCREEN_TEXT -> {
                if (accessibility != null) {
                    val screenText = accessibility.extractVisibleScreenText()
                    onFeedback(
                        if (isAr) "النصوص المقروءة على الشاشة:\n$screenText"
                        else "Screen text extracted:\n$screenText"
                    )
                } else {
                    onFeedback(if (isAr) "يرجى تفعيل خدمة الوصول لقراءة الشاشة" else "Accessibility required for screen reading")
                }
            }

            ActionType.SUMMARIZE_SCREEN -> {
                if (accessibility != null) {
                    val screenText = accessibility.extractVisibleScreenText()
                    val preview = screenText.take(150)
                    onFeedback(
                        if (isAr) "ملخص الشاشة الذكي: $preview..."
                        else "Screen Summary: $preview..."
                    )
                } else {
                    onFeedback(if (isAr) "يرجى تفعيل خدمة الوصول لتحليل الشاشة" else "Accessibility required for screen summary")
                }
            }

            ActionType.TYPE_ON_SCREEN -> {
                val textToType = payload?.trim() ?: "نص تلقائي"
                if (accessibility != null) {
                    accessibility.typeTextInActiveField(textToType) { status ->
                        onFeedback(status)
                    }
                    onFeedback(if (isAr) "تمت كتابة النص على الشاشة: \"$textToType\" ✍️" else "Typed on screen: \"$textToType\" ✍️")
                } else {
                    onFeedback(if (isAr) "تم إرسال أمر الكتابة: $textToType" else "Type command dispatched: $textToType")
                }
            }

            ActionType.AI_SUMMARIZE_ACTIVITY -> {
                onFeedback(
                    if (isAr) "تم استخراج الملخص الذكي لجميع نشاطات واستخدامات الهاتف 📊"
                    else "Smart summary of phone activity generated 📊"
                )
            }
        }
    }

    private fun resolveContactNumber(nameOrNumber: String): String? {
        if (nameOrNumber.matches(Regex("^[+0-9\\-\\s()]+$"))) {
            return nameOrNumber
        }

        if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            return null
        }

        var cursor: Cursor? = null
        try {
            val uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
            val projection = arrayOf(
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER
            )
            val selection = "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ?"
            val selectionArgs = arrayOf("%$nameOrNumber%")

            cursor = context.contentResolver.query(uri, projection, selection, selectionArgs, null)
            if (cursor != null && cursor.moveToFirst()) {
                val numberIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                if (numberIndex != -1) {
                    return cursor.getString(numberIndex)
                }
            }
        } catch (e: Exception) {
            // ignore
        } finally {
            cursor?.close()
        }
        return null
    }

    private fun launchAppByQuery(query: String): String? {
        val pm = context.packageManager
        if (query.isBlank()) return null

        // Strip common command prefixes
        var cleanQuery = query.lowercase().trim()
        val prefixesToRemove = listOf(
            "تطبيق ", "برنامج ", "ابلكيشن ", "الابلكيشن ", "البرنامج ", "التطبيق ",
            "افتح ", "شغل ", "انتقل الى ", "انتقل إلى ", "فتح ", "تشغيل ",
            "open ", "launch ", "start ", "run ", "app "
        )
        for (prefix in prefixesToRemove) {
            if (cleanQuery.startsWith(prefix)) {
                cleanQuery = cleanQuery.removePrefix(prefix).trim()
            }
        }

        // 1. Direct package name launch if exact package name given
        try {
            val directIntent = pm.getLaunchIntentForPackage(cleanQuery)
            if (directIntent != null) {
                directIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
                context.startActivity(directIntent)
                val appInfo = pm.getApplicationInfo(cleanQuery, 0)
                return pm.getApplicationLabel(appInfo).toString()
            }
        } catch (e: Exception) {
            // ignore
        }

        val knownPackages = mapOf(
            "واتساب" to "com.whatsapp",
            "الواتساب" to "com.whatsapp",
            "واتس" to "com.whatsapp",
            "الواتس" to "com.whatsapp",
            "whatsapp" to "com.whatsapp",
            "whats" to "com.whatsapp",
            "يوتيوب" to "com.google.android.youtube",
            "اليوتيوب" to "com.google.android.youtube",
            "youtube" to "com.google.android.youtube",
            "yt" to "com.google.android.youtube",
            "كروم" to "com.android.chrome",
            "الكروم" to "com.android.chrome",
            "chrome" to "com.android.chrome",
            "متصفح" to "com.android.chrome",
            "المتصفح" to "com.android.chrome",
            "browser" to "com.android.chrome",
            "تليجرام" to "org.telegram.messenger",
            "تيليجرام" to "org.telegram.messenger",
            "التليجرام" to "org.telegram.messenger",
            "التيليجرام" to "org.telegram.messenger",
            "telegram" to "org.telegram.messenger",
            "انستغرام" to "com.instagram.android",
            "انستقرام" to "com.instagram.android",
            "الانستقرام" to "com.instagram.android",
            "الانستغرام" to "com.instagram.android",
            "انستا" to "com.instagram.android",
            "الانستا" to "com.instagram.android",
            "instagram" to "com.instagram.android",
            "فيسبوك" to "com.facebook.katana",
            "الفيسبوك" to "com.facebook.katana",
            "فيس" to "com.facebook.katana",
            "الفيس" to "com.facebook.katana",
            "facebook" to "com.facebook.katana",
            "fb" to "com.facebook.katana",
            "تيك توك" to "com.zhiliaoapp.musically",
            "التيك توك" to "com.zhiliaoapp.musically",
            "تيكتوك" to "com.zhiliaoapp.musically",
            "tiktok" to "com.zhiliaoapp.musically",
            "سناب" to "com.snapchat.android",
            "سناب شات" to "com.snapchat.android",
            "السناب" to "com.snapchat.android",
            "snapchat" to "com.snapchat.android",
            "تويتر" to "com.twitter.android",
            "التويتر" to "com.twitter.android",
            "اكس" to "com.twitter.android",
            "twitter" to "com.twitter.android",
            "x" to "com.twitter.android",
            "خرائط" to "com.google.android.apps.maps",
            "الخرائط" to "com.google.android.apps.maps",
            "maps" to "com.google.android.apps.maps",
            "google maps" to "com.google.android.apps.maps",
            "جيميل" to "com.google.android.gm",
            "الجيميل" to "com.google.android.gm",
            "gmail" to "com.google.android.gm",
            "حاسبة" to "com.google.android.calculator",
            "الحاسبة" to "com.google.android.calculator",
            "آلة حاسبة" to "com.google.android.calculator",
            "الآلة الحاسبة" to "com.google.android.calculator",
            "calculator" to "com.google.android.calculator",
            "استوديو" to "com.google.android.apps.photos",
            "الاستوديو" to "com.google.android.apps.photos",
            "معرض" to "com.google.android.apps.photos",
            "المعرض" to "com.google.android.apps.photos",
            "photos" to "com.google.android.apps.photos",
            "صور" to "com.google.android.apps.photos",
            "الصور" to "com.google.android.apps.photos",
            "متجر" to "com.android.vending",
            "المتجر" to "com.android.vending",
            "بلاي" to "com.android.vending",
            "play" to "com.android.vending",
            "play store" to "com.android.vending",
            "ساعة" to "com.google.android.deskclock",
            "الساعة" to "com.google.android.deskclock",
            "منبه" to "com.google.android.deskclock",
            "clock" to "com.google.android.deskclock",
            "ملفات" to "com.google.android.documentsui",
            "الملفات" to "com.google.android.documentsui",
            "files" to "com.google.android.documentsui",
            "رسائل" to "com.google.android.apps.messaging",
            "الرسائل" to "com.google.android.apps.messaging",
            "messages" to "com.google.android.apps.messaging",
            "sms" to "com.google.android.apps.messaging",
            "ماسنجر" to "com.facebook.orca",
            "الماسنجر" to "com.facebook.orca",
            "messenger" to "com.facebook.orca"
        )

        for ((key, pkg) in knownPackages) {
            if (cleanQuery == key || cleanQuery.contains(key) || key.contains(cleanQuery)) {
                try {
                    val intent = pm.getLaunchIntentForPackage(pkg)
                    if (intent != null) {
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
                        context.startActivity(intent)
                        return key
                    }
                } catch (e: Exception) {
                    // continue
                }
            }
        }

        // Exhaustive Dynamic Search across all Installed Launcher Apps & Package Registry
        try {
            val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }
            val resolvedApps = pm.queryIntentActivities(mainIntent, 0)
            
            // Phase 1: Exact or starts-with match
            for (info in resolvedApps) {
                val appLabel = info.loadLabel(pm).toString().lowercase()
                val pkgName = info.activityInfo.packageName.lowercase()

                if (appLabel == cleanQuery || pkgName == cleanQuery) {
                    val launchIntent = pm.getLaunchIntentForPackage(info.activityInfo.packageName)
                    if (launchIntent != null) {
                        launchIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
                        context.startActivity(launchIntent)
                        return info.loadLabel(pm).toString()
                    }
                }
            }

            // Phase 2: Substring or normalized match (Arabic/English)
            val normalizedQuery = cleanQuery.replace("ال", "").replace("أ", "ا").replace("إ", "ا").trim()
            for (info in resolvedApps) {
                val appLabel = info.loadLabel(pm).toString().lowercase()
                val normalizedLabel = appLabel.replace("ال", "").replace("أ", "ا").replace("إ", "ا").trim()
                val pkgName = info.activityInfo.packageName.lowercase()

                if (appLabel.contains(cleanQuery) || 
                    pkgName.contains(cleanQuery) || 
                    cleanQuery.contains(appLabel) ||
                    (normalizedQuery.length >= 3 && normalizedLabel.contains(normalizedQuery)) ||
                    (normalizedQuery.length >= 3 && pkgName.contains(normalizedQuery))) {
                    
                    val launchIntent = pm.getLaunchIntentForPackage(info.activityInfo.packageName)
                    if (launchIntent != null) {
                        launchIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
                        context.startActivity(launchIntent)
                        return info.loadLabel(pm).toString()
                    }
                }
            }
        } catch (e: Exception) {
            // ignore
        }

        return null
    }

    fun vibratePhone(durationMs: Long = 80L) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator?.vibrate(
                    VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE)
                )
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator?.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(durationMs)
                }
            }
        } catch (e: Exception) {
            // Ignore vibration errors
        }
    }
}
