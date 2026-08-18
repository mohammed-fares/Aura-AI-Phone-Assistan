package com.example

import android.app.Application
import com.example.data.local.AppDatabase
import com.example.data.remote.GeminiService
import com.example.data.repository.AssistantRepository
import com.example.system.ActionExecutionEngine
import com.example.system.DeviceTelemetryManager
import com.example.system.VoiceSpeechEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class AuraApplication : Application() {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val database by lazy { AppDatabase.getDatabase(this) }
    val geminiService by lazy { GeminiService() }
    val repository by lazy { AssistantRepository(database, geminiService) }
    val telemetryManager by lazy { DeviceTelemetryManager(this) }
    val voiceSpeechEngine by lazy { VoiceSpeechEngine(this) }
    val actionExecutionEngine by lazy { ActionExecutionEngine(this) }

    override fun onCreate() {
        super.onCreate()
        applicationScope.launch {
            repository.seedInitialDataIfEmpty()
        }
    }
}
