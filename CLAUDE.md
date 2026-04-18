# MoveAndMeds

Android health reminder app — medicine reminders, exercise reminders, contact info storage, and activity logging.

## Project Info

- **Package ID:** com.jaytt.moveandmeds
- **Min SDK:** 26 | **Target/Compile SDK:** 35
- **Language:** Kotlin
- **UI:** Jetpack Compose + Material 3

## Current Version

- **versionName:** 2.6
- **versionCode:** 20 (next release must use 21 or higher)

## Build & Release

```bash
# Debug APK (for device testing)
./gradlew assembleDebug

# Release AAB (for Play Store)
./gradlew bundleRelease
# Output: app/build/outputs/bundle/release/app-release.aab

# Install debug build on connected device
adb install app/build/outputs/apk/debug/app-debug.apk
```

## Signing

- **Keystore:** `moveandmeds-release.jks` (project root)
- **Alias:** moveandmeds
- Signing config is in `app/build.gradle.kts`
- **Keep the keystore backed up** — losing it means you can never update the app on Play Store

## Architecture

- **Room** — local database (medicines, medicine times, exercises, exercise times, movement settings, contacts, reminder history)
- **AlarmManager** (`setExactAndAllowWhileIdle`) — reliable reminders in Doze mode
- **Hilt** — dependency injection
- **Navigation Compose** — single Activity with bottom nav (Medicines, Exercises, Contacts, Log)
- **BootReceiver** — reschedules all alarms after device reboot
- **ML Kit Text Recognition + CameraX** — OCR scanner for pre-filling medicines, exercises, and contacts
- **PDFBox Android** (`com.tom-roush:pdfbox-android:2.0.27.0`) — direct PDF text + image extraction for exercise physio sheets
- **DND-aware alarms** — vibrates instead of ringing when Do Not Disturb is active (`CHANNEL_MOVEMENT_VIBRATE`)

## Screens

Bottom nav tabs:
- **Medicines** — list of medicines with reminders; tap to edit/view detail
- **Exercises** — list of exercises with interval or daily reminders; tap to edit/view detail
- **Contacts (Info)** — store emergency/care contacts
- **Log** — activity log of all reminder events

Detail/modal screens:
- **MedicineDetail** — add/edit medicine, set times-per-day + auto-spaced reminder times
- **ExerciseDetail** — add/edit exercise, reminder type (interval or daily times); sets/reps fields removed — free-text Notes only
- **History** — per-item reminder history (medicines or exercises)
- **Settings** — app settings + link to Privacy Policy
- **PrivacyPolicy** — static privacy policy screen
- **Scanner** — shared CameraX OCR scanner; routes results to medicines, exercises, or contacts
- **MedicineScanResult / ExerciseScanResult / ScanResult** — review OCR output before confirming
- **Onboarding** — first-run walkthrough
- **Disclaimer** — first-run medical disclaimer (must accept to proceed)

## Key Source Locations

```
app/src/main/java/com/jaytt/moveandmeds/
├── alarm/          # AlarmScheduler, AlarmReceiver, BootReceiver
├── data/           # Room DB, DAOs, models, repositories
├── di/             # Hilt AppModule, AlarmModule
├── notification/   # NotificationHelper (channels + builders)
├── util/           # RecoveryHelper, StepCountHelper, CsvExporter
└── ui/
    ├── exercise/       # ExerciseDetailScreen, ExerciseViewModel
    ├── exercises/      # ExercisesScreen, ExercisesViewModel, ExerciseScanResultScreen
    ├── history/        # HistoryScreen, HistoryViewModel, LogScreen
    ├── info/           # InfoScreen, InfoViewModel, ScannerScreen, ScanResultScreen
    ├── medicine/       # MedicineDetailScreen, MedicineViewModel
    ├── medicines/      # MedicinesScreen, MedicinesViewModel, MedicineScanResultScreen
    ├── movement/       # MovementViewModel
    ├── navigation/     # NavGraph (Screen sealed class + NavHost)
    ├── onboarding/     # OnboardingScreen, DisclaimerScreen
    ├── privacy/        # PrivacyPolicyScreen
    ├── settings/       # SettingsScreen
    └── theme/          # Theme.kt
```

## Play Store

- Developer: John Thomas
- App signing: Play App Signing enabled (upload key = moveandmeds-release.jks)
