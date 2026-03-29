package com.jaytt.moveandmeds.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.jaytt.moveandmeds.ui.exercise.ExerciseDetailScreen
import com.jaytt.moveandmeds.ui.exercises.ExerciseScanResultScreen
import com.jaytt.moveandmeds.ui.exercises.ExercisesScreen
import com.jaytt.moveandmeds.ui.history.HistoryScreen
import com.jaytt.moveandmeds.ui.history.LogScreen
import com.jaytt.moveandmeds.ui.onboarding.DisclaimerScreen
import com.jaytt.moveandmeds.ui.info.InfoScreen
import com.jaytt.moveandmeds.ui.info.ScannerScreen
import com.jaytt.moveandmeds.ui.info.ScanResultScreen
import com.jaytt.moveandmeds.ui.medicine.MedicineDetailScreen
import com.jaytt.moveandmeds.ui.medicines.MedicineScanResultScreen
import com.jaytt.moveandmeds.ui.medicines.MedicinesScreen
import com.jaytt.moveandmeds.ui.onboarding.OnboardingScreen
import com.jaytt.moveandmeds.ui.privacy.PrivacyPolicyScreen
import com.jaytt.moveandmeds.ui.settings.SettingsScreen

sealed class Screen(val route: String) {
    object Disclaimer : Screen("disclaimer")
    object Onboarding : Screen("onboarding")
    object Medicines : Screen("medicines")
    object Exercises : Screen("exercises")
    object Info : Screen("info")
    object Settings : Screen("settings")
    object PrivacyPolicy : Screen("privacy_policy")
    object MedicineDetail : Screen("medicine_detail/{medicineId}") {
        fun createRoute(medicineId: Int = -1) = "medicine_detail/$medicineId"
    }
    object MedicineDetailPrefill : Screen(
        "medicine_detail/{medicineId}?prefillName={prefillName}&prefillDosage={prefillDosage}"
    ) {
        fun createRoute(prefillName: String, prefillDosage: String): String {
            val encName = java.net.URLEncoder.encode(prefillName, "UTF-8")
            val encDosage = java.net.URLEncoder.encode(prefillDosage, "UTF-8")
            return "medicine_detail/-1?prefillName=$encName&prefillDosage=$encDosage"
        }
    }
    object ExerciseDetail : Screen("exercise_detail/{exerciseId}") {
        fun createRoute(exerciseId: Int = -1) = "exercise_detail/$exerciseId"
    }
    object ExerciseDetailPrefill : Screen(
        "exercise_detail/{exerciseId}?prefillName={prefillName}&prefillSets={prefillSets}&prefillReps={prefillReps}"
    ) {
        fun createRoute(prefillName: String, prefillSets: String, prefillReps: String): String {
            val encName = java.net.URLEncoder.encode(prefillName, "UTF-8")
            val encSets = java.net.URLEncoder.encode(prefillSets, "UTF-8")
            val encReps = java.net.URLEncoder.encode(prefillReps, "UTF-8")
            return "exercise_detail/-1?prefillName=$encName&prefillSets=$encSets&prefillReps=$encReps"
        }
    }
    object ExerciseDetailPrefillInterval : Screen(
        "exercise_detail/{exerciseId}?prefillName={prefillName}&prefillSets={prefillSets}&prefillReps={prefillReps}&prefillReminderType={prefillReminderType}&prefillIntervalMinutes={prefillIntervalMinutes}"
    ) {
        fun createRoute(
            prefillName: String,
            prefillSets: String,
            prefillReps: String,
            prefillReminderType: String,
            prefillIntervalMinutes: Int
        ): String {
            val encName = java.net.URLEncoder.encode(prefillName, "UTF-8")
            val encSets = java.net.URLEncoder.encode(prefillSets, "UTF-8")
            val encReps = java.net.URLEncoder.encode(prefillReps, "UTF-8")
            return "exercise_detail/-1?prefillName=$encName&prefillSets=$encSets&prefillReps=$encReps&prefillReminderType=$prefillReminderType&prefillIntervalMinutes=$prefillIntervalMinutes"
        }
    }
    object Log : Screen("log")
    object History : Screen("history/{itemType}/{itemId}/{itemName}") {
        fun createRoute(itemType: String, itemId: Int, itemName: String) =
            "history/$itemType/$itemId/${itemName.replace("/", "_")}"
    }
    // Scanner with destination context: "contacts", "medicines", "exercises"
    object Scanner : Screen("scanner/{destination}") {
        fun createRoute(destination: String) = "scanner/$destination"
    }
    // Contact scan result
    object ScanResult : Screen("scan_result/{encodedText}") {
        fun createRoute(encodedText: String) = "scan_result/$encodedText"
    }
    object InfoWithPrefill : Screen("info?prefillValue={prefillValue}&prefillType={prefillType}") {
        fun createRoute(prefillValue: String, prefillType: String): String {
            val encodedValue = java.net.URLEncoder.encode(prefillValue, "UTF-8")
            return "info?prefillValue=$encodedValue&prefillType=$prefillType"
        }
    }
    // Medicine scan result
    object MedicineScanResult : Screen("medicine_scan_result/{encodedText}") {
        fun createRoute(encodedText: String) = "medicine_scan_result/$encodedText"
    }
    // Exercise scan result
    object ExerciseScanResult : Screen("exercise_scan_result/{encodedText}") {
        fun createRoute(encodedText: String) = "exercise_scan_result/$encodedText"
    }
}

private val bottomNavRoutes = setOf(
    Screen.Medicines.route,
    Screen.Exercises.route,
    Screen.Info.route,
    Screen.InfoWithPrefill.route,
    Screen.Log.route
)

@Composable
fun NavGraph(
    startDestination: String = Screen.Medicines.route,
    onDisclaimerAccepted: () -> Unit = {},
    onOnboardingFinished: () -> Unit = {},
    onDeclineDisclaimer: () -> Unit = {}
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val showBottomBar = currentRoute in bottomNavRoutes

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    NavigationBarItem(
                        selected = currentRoute == Screen.Medicines.route,
                        onClick = {
                            if (currentRoute != Screen.Medicines.route) {
                                navController.navigate(Screen.Medicines.route) {
                                    popUpTo(Screen.Medicines.route) { inclusive = false }
                                    launchSingleTop = true
                                }
                            }
                        },
                        icon = { Icon(Icons.Default.MedicalServices, contentDescription = "Medicines") },
                        label = { Text("Medicines") }
                    )
                    NavigationBarItem(
                        selected = currentRoute == Screen.Exercises.route,
                        onClick = {
                            if (currentRoute != Screen.Exercises.route) {
                                navController.navigate(Screen.Exercises.route) {
                                    popUpTo(Screen.Medicines.route) { inclusive = false }
                                    launchSingleTop = true
                                }
                            }
                        },
                        icon = { Icon(Icons.Default.FitnessCenter, contentDescription = "Exercises") },
                        label = { Text("Exercises") }
                    )
                    NavigationBarItem(
                        selected = currentRoute == Screen.Info.route || currentRoute == Screen.InfoWithPrefill.route,
                        onClick = {
                            if (currentRoute != Screen.Info.route) {
                                navController.navigate(Screen.Info.route) {
                                    popUpTo(Screen.Medicines.route) { inclusive = false }
                                    launchSingleTop = true
                                }
                            }
                        },
                        icon = { Icon(Icons.Default.Info, contentDescription = "Contact Information") },
                        label = { Text("Contacts") }
                    )
                    NavigationBarItem(
                        selected = currentRoute == Screen.Log.route,
                        onClick = {
                            if (currentRoute != Screen.Log.route) {
                                navController.navigate(Screen.Log.route) {
                                    popUpTo(Screen.Medicines.route) { inclusive = false }
                                    launchSingleTop = true
                                }
                            }
                        },
                        icon = { Icon(Icons.Default.History, contentDescription = "Activity Log") },
                        label = { Text("Log") }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(bottom = innerPadding.calculateBottomPadding())
        ) {
            composable(Screen.Disclaimer.route) {
                DisclaimerScreen(
                    onAccept = {
                        onDisclaimerAccepted()
                        navController.navigate(Screen.Onboarding.route) {
                            popUpTo(Screen.Disclaimer.route) { inclusive = true }
                        }
                    },
                    onDecline = onDeclineDisclaimer
                )
            }

            composable(Screen.Onboarding.route) {
                OnboardingScreen(onFinished = {
                    onOnboardingFinished()
                    navController.navigate(Screen.Medicines.route) {
                        popUpTo(Screen.Onboarding.route) { inclusive = true }
                    }
                })
            }

            composable(Screen.Medicines.route) {
                MedicinesScreen(
                    onAddMedicine = { navController.navigate(Screen.MedicineDetail.createRoute()) },
                    onEditMedicine = { id -> navController.navigate(Screen.MedicineDetail.createRoute(id)) },
                    onScanClick = { navController.navigate(Screen.Scanner.createRoute("medicines")) },
                    onSettingsClick = { navController.navigate(Screen.Settings.route) }
                )
            }

            composable(Screen.Exercises.route) {
                ExercisesScreen(
                    onAddExercise = { navController.navigate(Screen.ExerciseDetail.createRoute()) },
                    onEditExercise = { id -> navController.navigate(Screen.ExerciseDetail.createRoute(id)) },
                    onScanClick = { navController.navigate(Screen.Scanner.createRoute("exercises")) },
                    onSettingsClick = { navController.navigate(Screen.Settings.route) }
                )
            }

            composable(Screen.Info.route) {
                InfoScreen(
                    onScanClick = { navController.navigate(Screen.Scanner.createRoute("contacts")) }
                )
            }
            composable(
                Screen.InfoWithPrefill.route,
                arguments = listOf(
                    navArgument("prefillValue") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    },
                    navArgument("prefillType") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    }
                )
            ) { backStackEntry ->
                val prefillValue = backStackEntry.arguments?.getString("prefillValue")
                val prefillType = backStackEntry.arguments?.getString("prefillType")
                InfoScreen(
                    onScanClick = { navController.navigate(Screen.Scanner.createRoute("contacts")) },
                    prefillValue = prefillValue,
                    prefillType = prefillType
                )
            }

            // Shared scanner screen — destination tells it where to route results
            composable(
                Screen.Scanner.route,
                arguments = listOf(navArgument("destination") { type = NavType.StringType })
            ) { backStackEntry ->
                val destination = backStackEntry.arguments?.getString("destination") ?: "contacts"
                ScannerScreen(
                    onBack = { navController.popBackStack() },
                    onScanResult = { encodedText ->
                        when (destination) {
                            "medicines" -> navController.navigate(Screen.MedicineScanResult.createRoute(encodedText))
                            "exercises" -> navController.navigate(Screen.ExerciseScanResult.createRoute(encodedText))
                            else -> navController.navigate(Screen.ScanResult.createRoute(encodedText))
                        }
                    }
                )
            }

            composable(
                Screen.ScanResult.route,
                arguments = listOf(
                    navArgument("encodedText") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val encodedText = backStackEntry.arguments?.getString("encodedText") ?: ""
                ScanResultScreen(
                    encodedText = encodedText,
                    onBack = { navController.popBackStack() },
                    onItemSelected = { value, type ->
                        navController.navigate(Screen.InfoWithPrefill.createRoute(value, type)) {
                            popUpTo(Screen.Info.route) { inclusive = false }
                        }
                    }
                )
            }

            composable(
                Screen.MedicineScanResult.route,
                arguments = listOf(
                    navArgument("encodedText") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val encodedText = backStackEntry.arguments?.getString("encodedText") ?: ""
                MedicineScanResultScreen(
                    encodedText = encodedText,
                    onBack = { navController.popBackStack() },
                    onAddMedicine = { name, dosage, _ ->
                        val encName = java.net.URLEncoder.encode(name, "UTF-8")
                        val encDosage = java.net.URLEncoder.encode(dosage, "UTF-8")
                        navController.navigate("medicine_detail/-1?prefillName=$encName&prefillDosage=$encDosage") {
                            popUpTo(Screen.Medicines.route) { inclusive = false }
                        }
                    }
                )
            }

            composable(
                Screen.ExerciseScanResult.route,
                arguments = listOf(
                    navArgument("encodedText") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val encodedText = backStackEntry.arguments?.getString("encodedText") ?: ""
                ExerciseScanResultScreen(
                    encodedText = encodedText,
                    onBack = { navController.popBackStack() },
                    onDone = {
                        navController.navigate(Screen.Exercises.route) {
                            popUpTo(Screen.Exercises.route) { inclusive = false }
                        }
                    }
                )
            }

            composable(Screen.Log.route) {
                LogScreen()
            }

            composable(Screen.Settings.route) {
                SettingsScreen(
                    onBack = { navController.popBackStack() },
                    onPrivacyPolicy = { navController.navigate(Screen.PrivacyPolicy.route) }
                )
            }

            composable(Screen.PrivacyPolicy.route) {
                PrivacyPolicyScreen(onBack = { navController.popBackStack() })
            }

            composable(
                "medicine_detail/{medicineId}",
                arguments = listOf(navArgument("medicineId") { type = NavType.IntType })
            ) { backStackEntry ->
                val medicineId = backStackEntry.arguments?.getInt("medicineId") ?: -1
                MedicineDetailScreen(
                    medicineId = if (medicineId == -1) null else medicineId,
                    onBack = { navController.popBackStack() },
                    onHistory = { id ->
                        navController.navigate(Screen.History.createRoute("medicine", id, "Medicine"))
                    }
                )
            }

            composable(
                "medicine_detail/{medicineId}?prefillName={prefillName}&prefillDosage={prefillDosage}",
                arguments = listOf(
                    navArgument("medicineId") { type = NavType.IntType },
                    navArgument("prefillName") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    },
                    navArgument("prefillDosage") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    }
                )
            ) { backStackEntry ->
                val medicineId = backStackEntry.arguments?.getInt("medicineId") ?: -1
                val prefillName = backStackEntry.arguments?.getString("prefillName")
                    ?.let { if (it.isBlank()) null else java.net.URLDecoder.decode(it, "UTF-8") }
                val prefillDosage = backStackEntry.arguments?.getString("prefillDosage")
                    ?.let { if (it.isBlank()) null else java.net.URLDecoder.decode(it, "UTF-8") }
                MedicineDetailScreen(
                    medicineId = if (medicineId == -1) null else medicineId,
                    onBack = { navController.popBackStack() },
                    onHistory = { id ->
                        navController.navigate(Screen.History.createRoute("medicine", id, "Medicine"))
                    },
                    prefillName = prefillName,
                    prefillDosage = prefillDosage
                )
            }

            composable(
                "exercise_detail/{exerciseId}",
                arguments = listOf(navArgument("exerciseId") { type = NavType.IntType })
            ) { backStackEntry ->
                val exerciseId = backStackEntry.arguments?.getInt("exerciseId") ?: -1
                ExerciseDetailScreen(
                    exerciseId = if (exerciseId == -1) null else exerciseId,
                    onBack = { navController.popBackStack() },
                    onHistory = { id ->
                        navController.navigate(Screen.History.createRoute("exercise", id, "Exercise"))
                    }
                )
            }

            composable(
                "exercise_detail/{exerciseId}?prefillName={prefillName}&prefillSets={prefillSets}&prefillReps={prefillReps}",
                arguments = listOf(
                    navArgument("exerciseId") { type = NavType.IntType },
                    navArgument("prefillName") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    },
                    navArgument("prefillSets") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    },
                    navArgument("prefillReps") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    }
                )
            ) { backStackEntry ->
                val exerciseId = backStackEntry.arguments?.getInt("exerciseId") ?: -1
                val prefillName = backStackEntry.arguments?.getString("prefillName")
                    ?.let { if (it.isBlank()) null else java.net.URLDecoder.decode(it, "UTF-8") }
                val prefillSets = backStackEntry.arguments?.getString("prefillSets")
                    ?.let { if (it.isBlank()) null else java.net.URLDecoder.decode(it, "UTF-8") }
                val prefillReps = backStackEntry.arguments?.getString("prefillReps")
                    ?.let { if (it.isBlank()) null else java.net.URLDecoder.decode(it, "UTF-8") }
                ExerciseDetailScreen(
                    exerciseId = if (exerciseId == -1) null else exerciseId,
                    onBack = { navController.popBackStack() },
                    onHistory = { id ->
                        navController.navigate(Screen.History.createRoute("exercise", id, "Exercise"))
                    },
                    prefillName = prefillName,
                    prefillSets = prefillSets,
                    prefillReps = prefillReps
                )
            }

            composable(
                "exercise_detail/{exerciseId}?prefillName={prefillName}&prefillSets={prefillSets}&prefillReps={prefillReps}&prefillReminderType={prefillReminderType}&prefillIntervalMinutes={prefillIntervalMinutes}",
                arguments = listOf(
                    navArgument("exerciseId") { type = NavType.IntType },
                    navArgument("prefillName") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    },
                    navArgument("prefillSets") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    },
                    navArgument("prefillReps") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    },
                    navArgument("prefillReminderType") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    },
                    navArgument("prefillIntervalMinutes") {
                        type = NavType.IntType
                        defaultValue = 60
                    }
                )
            ) { backStackEntry ->
                val exerciseId = backStackEntry.arguments?.getInt("exerciseId") ?: -1
                val prefillName = backStackEntry.arguments?.getString("prefillName")
                    ?.let { if (it.isBlank()) null else java.net.URLDecoder.decode(it, "UTF-8") }
                val prefillSets = backStackEntry.arguments?.getString("prefillSets")
                    ?.let { if (it.isBlank()) null else java.net.URLDecoder.decode(it, "UTF-8") }
                val prefillReps = backStackEntry.arguments?.getString("prefillReps")
                    ?.let { if (it.isBlank()) null else java.net.URLDecoder.decode(it, "UTF-8") }
                val prefillReminderType = backStackEntry.arguments?.getString("prefillReminderType")
                val prefillIntervalMinutes = backStackEntry.arguments?.getInt("prefillIntervalMinutes") ?: 60
                ExerciseDetailScreen(
                    exerciseId = if (exerciseId == -1) null else exerciseId,
                    onBack = { navController.popBackStack() },
                    onHistory = { id ->
                        navController.navigate(Screen.History.createRoute("exercise", id, "Exercise"))
                    },
                    prefillName = prefillName,
                    prefillSets = prefillSets,
                    prefillReps = prefillReps,
                    prefillReminderType = prefillReminderType,
                    prefillIntervalMinutes = prefillIntervalMinutes
                )
            }

            composable(
                Screen.History.route,
                arguments = listOf(
                    navArgument("itemType") { type = NavType.StringType },
                    navArgument("itemId") { type = NavType.IntType },
                    navArgument("itemName") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val itemType = backStackEntry.arguments?.getString("itemType") ?: "medicine"
                val itemId = backStackEntry.arguments?.getInt("itemId") ?: 0
                val itemName = backStackEntry.arguments?.getString("itemName") ?: ""
                HistoryScreen(
                    itemType = itemType,
                    itemId = itemId,
                    itemName = itemName,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
