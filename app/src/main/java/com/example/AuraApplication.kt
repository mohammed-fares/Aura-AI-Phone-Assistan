package com.example

import android.app.Application
import com.example.data.local.AppDatabase
import com.example.data.remote.GeminiService
import com.example.data.repository.AssistantRepository
import com.example.service.AssistantForegroundService
import com.example.system.ActionExecutionEngine
import com.example.system.AppKnowledgeManager
import com.example.system.DeviceTelemetryManager
import com.example.system.LocalNetworkMonitor
import com.example.system.SecurityScanEngine
import com.example.system.VoiceSpeechEngine
import com.example.system.VoiceprintManager
import com.example.system.WakeWordManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

class AuraApplication : Application() {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val database by lazy { AppDatabase.getDatabase(this) }
    val geminiService by lazy { GeminiService() }
    val repository by lazy { AssistantRepository(database, geminiService) }
    val telemetryManager by lazy { DeviceTelemetryManager(this) }
    val voiceSpeechEngine by lazy { VoiceSpeechEngine(this) }
    val actionExecutionEngine by lazy { ActionExecutionEngine(this) }
    val voiceprintManager by lazy { VoiceprintManager(this, database.voiceprintDao()) }
    val securityScanEngine by lazy { SecurityScanEngine(this) }
    val localNetworkMonitor by lazy { LocalNetworkMonitor(this) }
    val appKnowledgeManager by lazy { AppKnowledgeManager(this, database.installedAppDao()) }
    val wakeWordManager by lazy { WakeWordManager() }
    val semanticSynonymManager by lazy { com.example.system.SemanticSynonymManager(this) }

    override fun onCreate() {
        super.onCreate()
        applicationScope.launch {
            repository.seedInitialDataIfEmpty()
            appKnowledgeManager.scanAndIndexAllInstalledApps()

            // Auto-start permanent background service on application launch
            val config = repository.config.firstOrNull()
            if (config == null || config.backgroundServiceEnabled) {
                AssistantForegroundService.startService(this@AuraApplication)
            }
        }
    }
}

