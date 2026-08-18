package com.example.system

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.os.Environment
import android.os.StatFs
import java.io.File

data class DeviceMetrics(
    val batteryPercent: Int,
    val isCharging: Boolean,
    val batteryTemperatureCelsius: Float,
    val ramUsedMb: Long,
    val ramTotalMb: Long,
    val ramUsagePercent: Int,
    val freeStorageGb: Float,
    val totalStorageGb: Float,
    val networkType: String, // "Wi-Fi", "بيانات الجوال", "غير متصل"
    val isInternetAvailable: Boolean,
    val audioRingerMode: String, // "عادي", "صامت", "اهتزاز"
    val healthScore: Int // 0-100
)

class DeviceTelemetryManager(private val context: Context) {

    fun getLiveMetrics(): DeviceMetrics {
        val batteryIntent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: 50
        val scale = batteryIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: 100
        val batteryPercent = if (level >= 0 && scale > 0) (level * 100) / scale else 85
        val status = batteryIntent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
        val rawTemp = batteryIntent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 280
        val tempCelsius = rawTemp / 10.0f

        // RAM Info
        val actManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        actManager?.getMemoryInfo(memInfo)
        val totalRamMb = (memInfo.totalMem / (1024 * 1024))
        val availRamMb = (memInfo.availMem / (1024 * 1024))
        val usedRamMb = (totalRamMb - availRamMb).coerceAtLeast(0)
        val ramPercent = if (totalRamMb > 0) ((usedRamMb * 100) / totalRamMb).toInt() else 45

        // Storage Info
        val stat = StatFs(Environment.getDataDirectory().path)
        val totalBytes = stat.blockCountLong * stat.blockSizeLong
        val freeBytes = stat.availableBlocksLong * stat.blockSizeLong
        val totalGb = totalBytes / (1024f * 1024f * 1024f)
        val freeGb = freeBytes / (1024f * 1024f * 1024f)

        // Network Info
        val connManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        val activeNet = connManager?.activeNetwork
        val caps = connManager?.getNetworkCapabilities(activeNet)
        val isConnected = caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
        val netType = when {
            caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true -> "شبكة Wi-Fi"
            caps?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true -> "بيانات الهاتف 5G/LTE"
            else -> "غير متصل"
        }

        // Audio Ringer
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        val ringerText = when (audioManager?.ringerMode) {
            AudioManager.RINGER_MODE_SILENT -> "صامت"
            AudioManager.RINGER_MODE_VIBRATE -> "اهتزاز"
            else -> "عادي"
        }

        // Calculate aggregate health score
        var score = 100
        if (batteryPercent < 20) score -= 15
        if (ramPercent > 80) score -= 15
        if (!isConnected) score -= 10
        if (tempCelsius > 40) score -= 10

        return DeviceMetrics(
            batteryPercent = batteryPercent.coerceIn(1, 100),
            isCharging = isCharging,
            batteryTemperatureCelsius = tempCelsius,
            ramUsedMb = usedRamMb,
            ramTotalMb = totalRamMb,
            ramUsagePercent = ramPercent.coerceIn(0, 100),
            freeStorageGb = freeGb,
            totalStorageGb = totalGb,
            networkType = netType,
            isInternetAvailable = isConnected,
            audioRingerMode = ringerText,
            healthScore = score.coerceIn(40, 100)
        )
    }
}
