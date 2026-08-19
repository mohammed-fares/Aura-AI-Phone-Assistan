package com.example

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.MainViewModel
import com.example.ui.screens.ActivityArchiveScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.PermissionsScreen
import com.example.ui.screens.SecurityScanScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.VoiceAssistantScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.PolishBackground
import com.example.ui.theme.PolishOnSecondaryContainer
import com.example.ui.theme.PolishSecondaryContainer
import com.example.ui.theme.PolishSurface
import com.example.ui.theme.PolishSurfaceBorder
import com.example.ui.theme.PolishTextSecondary
import kotlin.math.sqrt

enum class AppTab(val title: String, val testTag: String) {
    VOICE("التحكم الصوتي", "tab_voice"),
    SECURITY("فحص الأمان", "tab_security"),
    DASHBOARD("المراقبة و LAN", "tab_dashboard"),
    ARCHIVE("سجل الأرشفة", "tab_archive"),
    SETTINGS("بصمة الصوت", "tab_settings")
}

class MainActivity : ComponentActivity(), SensorEventListener {

    private val viewModel: MainViewModel by viewModels()
    private var sensorManager: SensorManager? = null
    private var accelerometer: Sensor? = null
    private var lastShakeTime = 0L

    private val requiredPermissions = buildList {
        add(Manifest.permission.RECORD_AUDIO)
        add(Manifest.permission.READ_PHONE_STATE)
        add(Manifest.permission.READ_CONTACTS)
        add(Manifest.permission.ACCESS_FINE_LOCATION)
        add(Manifest.permission.ACCESS_COARSE_LOCATION)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        checkAndUpdatePermissions()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Auto request all permissions immediately on launch
        requestAllPermissions()

        // Init Shake Sensor to support gesture to voice command
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        setContent {
            MyApplicationTheme {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    MainAppLayout(
                        viewModel = viewModel,
                        onRequestAllPermissions = { requestAllPermissions() }
                    )
                }
            }
        }
    }

    private fun requestAllPermissions() {
        val permissionsToRequest = requiredPermissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (permissionsToRequest.isNotEmpty()) {
            permissionLauncher.launch(permissionsToRequest.toTypedArray())
        } else {
            checkAndUpdatePermissions()
        }
    }

    private fun checkAndUpdatePermissions() {
        val map = requiredPermissions.associateWith {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
        viewModel.updatePermissionsState(map)
    }

    override fun onResume() {
        super.onResume()
        checkAndUpdatePermissions()
        accelerometer?.let {
            sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
    }

    override fun onPause() {
        super.onPause()
        sensorManager?.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_ACCELEROMETER) {
            val x = event.values[0]
            val y = event.values[1]
            val z = event.values[2]
            val gForce = sqrt((x * x + y * y + z * z).toDouble()) / SensorManager.GRAVITY_EARTH

            if (gForce > 2.7f) {
                val now = System.currentTimeMillis()
                if (now - lastShakeTime > 2000L) {
                    lastShakeTime = now
                    if (viewModel.assistantConfig.value.shakeGestureActionEnabled) {
                        viewModel.toggleVoiceListening()
                    }
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}

@Composable
fun MainAppLayout(
    viewModel: MainViewModel,
    onRequestAllPermissions: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(AppTab.VOICE) }
    val snackbarHostState = remember { SnackbarHostState() }
    val systemNotice by viewModel.systemStatusNotice.collectAsStateWithLifecycle()

    LaunchedEffect(systemNotice) {
        systemNotice?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearStatusNotice()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = PolishBackground,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            Surface(
                color = PolishSurface,
                border = androidx.compose.foundation.BorderStroke(1.dp, PolishSurfaceBorder)
            ) {
                NavigationBar(
                    containerColor = PolishSurface,
                    tonalElevation = 0.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("main_bottom_nav")
                ) {
                    AppTab.values().forEach { tab ->
                        val isSelected = selectedTab == tab
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = { selectedTab = tab },
                            icon = {
                                Icon(
                                    imageVector = when (tab) {
                                        AppTab.VOICE -> Icons.Default.Mic
                                        AppTab.SECURITY -> Icons.Default.Shield
                                        AppTab.DASHBOARD -> Icons.Default.Dashboard
                                        AppTab.ARCHIVE -> Icons.Default.Storage
                                        AppTab.SETTINGS -> Icons.Default.Settings
                                    },
                                    contentDescription = tab.title,
                                    modifier = Modifier.size(22.dp)
                                )
                            },
                            label = {
                                Text(
                                    text = tab.title,
                                    fontSize = 10.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = PolishOnSecondaryContainer,
                                selectedTextColor = PolishOnSecondaryContainer,
                                indicatorColor = PolishSecondaryContainer,
                                unselectedIconColor = PolishTextSecondary,
                                unselectedTextColor = PolishTextSecondary
                            ),
                            modifier = Modifier.testTag(tab.testTag)
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                AppTab.VOICE -> VoiceAssistantScreen(viewModel = viewModel)
                AppTab.SECURITY -> SecurityScanScreen(viewModel = viewModel)
                AppTab.DASHBOARD -> DashboardScreen(viewModel = viewModel)
                AppTab.ARCHIVE -> ActivityArchiveScreen(viewModel = viewModel)
                AppTab.SETTINGS -> SettingsScreen(viewModel = viewModel)
            }
        }
    }
}
