package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.AuraApplication
import com.example.service.AssistantForegroundService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

class BootCompletedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action == Intent.ACTION_BOOT_COMPLETED ||
            action == Intent.ACTION_LOCKED_BOOT_COMPLETED ||
            action == Intent.ACTION_MY_PACKAGE_REPLACED ||
            action == "android.intent.action.QUICKBOOT_POWERON" ||
            action == "com.htc.intent.action.QUICKBOOT_POWERON"
        ) {
            val app = context.applicationContext as? AuraApplication
            CoroutineScope(Dispatchers.IO).launch {
                val config = app?.repository?.config?.firstOrNull()
                if (config == null || config.backgroundServiceEnabled) {
                    AssistantForegroundService.startService(context)
                    app?.repository?.logTelemetry(
                        type = com.example.data.local.entity.TelemetryType.SYSTEM_PERFORMANCE,
                        title = "تشغيل تلقائي عند إقلاع الهاتف (Auto-Boot)",
                        description = "تم إطلاق الخدمة الخلفية وبدء الاستماع الذاتي فور اكتمال إعادة تشغيل النظام بنجاح ⚡",
                        severity = com.example.data.local.entity.TelemetrySeverity.OPTIMAL,
                        aiAudited = true
                    )
                }
            }
        }
    }
}

