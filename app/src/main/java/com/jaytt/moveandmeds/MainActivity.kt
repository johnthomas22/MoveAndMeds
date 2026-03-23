package com.jaytt.moveandmeds

import android.Manifest
import android.app.AlarmManager
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.jaytt.moveandmeds.ui.navigation.NavGraph
import com.jaytt.moveandmeds.ui.navigation.Screen
import com.jaytt.moveandmeds.ui.theme.MoveAndMedsTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* handled silently */ }

    private val activityRecognitionPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* handled silently */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestNotificationPermission()
        requestActivityRecognitionPermission()

        val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
        val onboardingDone = prefs.getBoolean("onboarding_done", false)

        // Check exact alarm and store flag
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(AlarmManager::class.java)
            val denied = !alarmManager.canScheduleExactAlarms()
            prefs.edit().putBoolean("exact_alarm_denied", denied).apply()
        }

        val startDestination = if (onboardingDone) Screen.Medicines.route else Screen.Onboarding.route

        setContent {
            MoveAndMedsTheme {
                NavGraph(
                    startDestination = startDestination,
                    onOnboardingFinished = {
                        prefs.edit().putBoolean("onboarding_done", true).apply()
                    }
                )
            }
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun requestActivityRecognitionPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACTIVITY_RECOGNITION)
                != PackageManager.PERMISSION_GRANTED) {
                activityRecognitionPermissionLauncher.launch(Manifest.permission.ACTIVITY_RECOGNITION)
            }
        }
    }
}
