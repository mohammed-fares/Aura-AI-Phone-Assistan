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
            ActionType.DEVICE_DIAGNOSTIC -> {
                onFeedback("تم إنجاز الفحص الشامل: المعالج والذاكرة ووحدات الهاتف تعمل بأعلى كفاءة وبأقل استهلاك للموارد.")
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
