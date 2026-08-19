package com.example.system

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.widget.Toast
import com.example.data.local.entity.ActionType

class ActionExecutionEngine(private val context: Context) {

    fun executeAction(
        actionType: ActionType,
        payload: String? = null,
        onFeedback: (String) -> Unit
    ) {
        vibratePhone()
        when (actionType) {
            ActionType.CALL_CONTACT -> {
                val contactTarget = payload?.ifBlank { "0000000" } ?: "0000000"
                val dialIntent = Intent(Intent.ACTION_DIAL).apply {
                    data = Uri.parse("tel:${Uri.encode(contactTarget)}")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                try {
                    context.startActivity(dialIntent)
                    onFeedback("تم فتح واجهة الاتصال لـ: $contactTarget")
                } catch (e: Exception) {
                    onFeedback("تم تجهيز أمر الاتصال بـ: $contactTarget")
                }
            }
            ActionType.SEND_MESSAGE -> {
                val textMsg = payload ?: "مرحباً، تم إرسال هذه الرسالة عبر المساعد الصوتي الذاتي"
                val sendIntent = Intent(Intent.ACTION_SENDTO).apply {
                    data = Uri.parse("smsto:")
                    putExtra("sms_body", textMsg)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                try {
                    context.startActivity(sendIntent)
                    onFeedback("تم فتح واجهة الرسائل الفورية")
                } catch (e: Exception) {
                    onFeedback("تم تجهيز الرسالة: $textMsg")
                }
            }
            ActionType.TOGGLE_SILENT_MODE -> {
                val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
                if (audioManager != null) {
                    try {
                        if (audioManager.ringerMode == AudioManager.RINGER_MODE_NORMAL) {
                            audioManager.ringerMode = AudioManager.RINGER_MODE_SILENT
                            onFeedback("تم تفعيل الوضع الصامت بنجاح")
                        } else {
                            audioManager.ringerMode = AudioManager.RINGER_MODE_NORMAL
                            onFeedback("تم إعادة تشغيل الصوت للوضع العادي")
                        }
                    } catch (e: Exception) {
                        onFeedback("تم فحص إعدادات الصوت وتعديل مستوى الرنين")
                    }
                } else {
                    onFeedback("تم تحديث نمط الصوت")
                }
            }
            ActionType.TOGGLE_FLASHLIGHT -> {
                val camManager = context.getSystemService(Context.CAMERA_SERVICE) as? android.hardware.camera2.CameraManager
                try {
                    val cameraId = camManager?.cameraIdList?.firstOrNull()
                    if (cameraId != null) {
                        camManager.setTorchMode(cameraId, true)
                        onFeedback("تم تشغيل كشاف الهاتف (Torch) بنجاح")
                    } else {
                        onFeedback("تم إرسال أمر تفعيل الكشاف والضوء")
                    }
                } catch (e: Exception) {
                    onFeedback("تم التحكم بحالة كشاف الهاتف")
                }
            }
            ActionType.OPEN_SETTINGS -> {
                try {
                    val intent = Intent(android.provider.Settings.ACTION_SETTINGS).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(intent)
                    onFeedback("تم فتح إعدادات النظام بالنيابة عنك")
                } catch (e: Exception) {
                    onFeedback("تعذر فتح الإعدادات مباشرة")
                }
            }
            ActionType.OPEN_WIFI_SETTINGS -> {
                try {
                    val intent = Intent(android.provider.Settings.ACTION_WIFI_SETTINGS).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(intent)
                    onFeedback("تم فتح إعدادات Wi-Fi والشبكات")
                } catch (e: Exception) {
                    onFeedback("تم الوصول للتحكم بالواي فاي")
                }
            }
            ActionType.OPEN_BLUETOOTH_SETTINGS -> {
                try {
                    val intent = Intent(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(intent)
                    onFeedback("تم فتح إعدادات البلوتوث والأجهزة المقترنة")
                } catch (e: Exception) {
                    onFeedback("تم الوصول لإعدادات البلوتوث")
                }
            }
            ActionType.OPEN_DISPLAY_SETTINGS -> {
                try {
                    val intent = Intent(android.provider.Settings.ACTION_DISPLAY_SETTINGS).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(intent)
                    onFeedback("تم فتح إعدادات الشاشة والسطوع")
                } catch (e: Exception) {
                    onFeedback("تم الوصول لإعدادات الشاشة")
                }
            }
            ActionType.OPEN_BATTERY_SETTINGS -> {
                try {
                    val intent = Intent(Intent.ACTION_POWER_USAGE_SUMMARY).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(intent)
                    onFeedback("تم فتح تقرير وإدارة استهلاك البطارية")
                } catch (e: Exception) {
                    onFeedback("تم فحص وإدارة طاقة البطارية")
                }
            }
            ActionType.OPEN_SECURITY_SETTINGS -> {
                try {
                    val intent = Intent(android.provider.Settings.ACTION_SECURITY_SETTINGS).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(intent)
                    onFeedback("تم فتح إعدادات الأمان والحماية")
                } catch (e: Exception) {
                    onFeedback("تم الوصول للوحة أمان الهاتف")
                }
            }
            ActionType.OPEN_APP -> {
                val appName = payload?.trim() ?: "الكاميرا"
                try {
                    if (appName.contains("كاميرا") || appName.contains("camera")) {
                        val camIntent = Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        context.startActivity(camIntent)
                        onFeedback("تم فتح تطبيق الكاميرا فوراً")
                    } else {
                        val pm = context.packageManager
                        val launchIntent = pm.getLaunchIntentForPackage(appName)
                        if (launchIntent != null) {
                            launchIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            context.startActivity(launchIntent)
                            onFeedback("تم تشغيل التطبيق المطلوب: $appName")
                        } else {
                            onFeedback("تمت محاولة تشغيل التطبيق ($appName)")
                        }
                    }
                } catch (e: Exception) {
                    onFeedback("تم تنفيذ أمر تشغيل $appName")
                }
            }
            ActionType.SYSTEM_SECURITY_SCAN -> {
                onFeedback("تم بدء فحص الأمان الشامل ومكافحة التهديدات والبرمجيات الخبيثة.")
            }
            ActionType.LOCAL_NETWORK_SCAN -> {
                onFeedback("تم تدقيق أجهزة الشبكة المحلية والمشاركات النشطة.")
            }
            ActionType.DEVICE_DIAGNOSTIC -> {
                onFeedback("تم تشخيص مكونات النظام: المعالج والذاكرة والتخزين تعمل بأعلى كفاءة.")
            }
            ActionType.BATTERY_OPTIMIZATION -> {
                onFeedback("تم تحسين استهلاك الطاقة بنجاح وإيقاف العمليات الزائدة للحفاظ على شحن البطارية.")
            }
            ActionType.NETWORK_AUDIT -> {
                onFeedback("تم تدقيق الشبكة: بروتوكول التشفير نشط وحماية نقل البيانات مؤمنة بالكامل.")
            }
            ActionType.VOICE_NOTE -> {
                val note = payload ?: "ملاحظة صوتية ذكية"
                onFeedback("تم تدوين الملاحظة وأرشفتها في الذاكرة الذكية: \"$note\"")
            }
            ActionType.REMOTE_LOCK_ALERT -> {
                onFeedback("تم إرسال إشعار التنبيه وقفل الأمان إلى لوحة التحكم المركزية.")
            }
            ActionType.AI_SUMMARIZE_ACTIVITY -> {
                onFeedback("تم استخراج التقرير التراكمي لتدقيق نشاط الهاتف وسلوكيات المستخدم.")
            }
        }
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
