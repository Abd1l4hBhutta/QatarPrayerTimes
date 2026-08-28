package com.example.qatarprayertimes

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import com.example.qatarprayertimes.azan.AzanScheduler
import com.example.qatarprayertimes.azan.ExactAlarmAccess
import com.example.qatarprayertimes.data.PrayerTimeCache
import com.example.qatarprayertimes.ui.PrayerDashboard
import com.example.qatarprayertimes.ui.WelcomeScreen
import com.example.qatarprayertimes.ui.theme.Background
import com.example.qatarprayertimes.ui.theme.QatarPrayerTimesTheme

class MainActivity : ComponentActivity() {

    private val preferences by lazy {
        getSharedPreferences(PREFERENCES_NAME, MODE_PRIVATE)
    }

    private var initialPermissionFlowComplete by mutableStateOf(false)
    private var hasExactAlarmAccess by mutableStateOf(false)
    private var exactAlarmPromptDismissed by mutableStateOf(false)

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        askLocationPermission()
    }

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        initialPermissionFlowComplete = true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars = false
        
        setContent {
            QatarPrayerTimesTheme {
                var hasCompletedWelcome by remember {
                    mutableStateOf(preferences.getBoolean(KEY_WELCOME_COMPLETED, false))
                }

                if (hasCompletedWelcome) {
                    LaunchedEffect(Unit) {
                        askNotificationPermission()
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Background)
                            .systemBarsPadding(),
                    ) {
                        PrayerDashboard()
                    }

                    if (initialPermissionFlowComplete &&
                        !hasExactAlarmAccess &&
                        !exactAlarmPromptDismissed
                    ) {
                        AlertDialog(
                            onDismissRequest = { exactAlarmPromptDismissed = true },
                            title = { Text("Allow prayer alarms") },
                            text = {
                                Text(
                                    "Allow Alarms & reminders so the Adhaan can play at the exact prayer time, even when the app is closed.",
                                )
                            },
                            confirmButton = {
                                TextButton(
                                    onClick = {
                                        exactAlarmPromptDismissed = true
                                        requestExactAlarmPermission()
                                    },
                                ) {
                                    Text("Allow")
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { exactAlarmPromptDismissed = true }) {
                                    Text("Not now")
                                }
                            },
                        )
                    }
                } else {
                    WelcomeScreen(
                        onContinue = {
                            preferences.edit().putBoolean(KEY_WELCOME_COMPLETED, true).apply()
                            hasCompletedWelcome = true
                        },
                    )
                }
            }
        }
    }

    private fun askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                return
            }
        }
        askLocationPermission()
    }

    private fun askLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        } else {
            initialPermissionFlowComplete = true
        }
    }

    override fun onResume() {
        super.onResume()
        val wasGranted = hasExactAlarmAccess
        hasExactAlarmAccess = ExactAlarmAccess.hasAccess(this)
        if (!wasGranted && hasExactAlarmAccess) {
            PrayerTimeCache(this).readLatest()?.prayers?.let { prayers ->
                AzanScheduler.reschedule(this, prayers)
            }
        }
    }

    private fun requestExactAlarmPermission() {
        runCatching { ExactAlarmAccess.request(this) }
    }

    private companion object {
        const val PREFERENCES_NAME = "app_preferences"
        const val KEY_WELCOME_COMPLETED = "welcome_completed"
    }
}
