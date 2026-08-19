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
        if (intent.action == Intent.ACTION_BOOT_COMPLETED || intent.action == "android.intent.action.QUICKBOOT_POWERON") {
            val app = context.applicationContext as? AuraApplication ?: return
            CoroutineScope(Dispatchers.IO).launch {
                val config = app.repository.config.firstOrNull()
                if (config?.backgroundServiceEnabled == true) {
                    AssistantForegroundService.startService(context)
                }
            }
        }
    }
}
