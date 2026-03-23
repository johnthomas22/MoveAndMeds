# MoveAndMeds

Android health reminder app with movement interval reminders and daily medicine reminders.

## Project Info

- **Package ID:** com.jaytt.moveandmeds
- **Min SDK:** 26 | **Target/Compile SDK:** 35
- **Language:** Kotlin
- **UI:** Jetpack Compose + Material 3

## Current Version

- **versionName:** 1.2
- **versionCode:** 4 (next release must use 5 or higher)

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

- **Room** — local database (medicines, medicine times, movement settings)
- **AlarmManager** (`setExactAndAllowWhileIdle`) — reliable reminders in Doze mode
- **Hilt** — dependency injection
- **Navigation Compose** — single Activity, three screens (Home, MovementSettings, MedicineDetail)
- **BootReceiver** — reschedules all alarms after device reboot

## Key Source Locations

```
app/src/main/java/com/jaytt/moveandmeds/
├── alarm/          # AlarmScheduler, AlarmReceiver, BootReceiver
├── data/           # Room DB, DAOs, models, repositories
├── di/             # Hilt AppModule
├── notification/   # NotificationHelper (channels + builders)
└── ui/             # Compose screens and ViewModels
```

## Play Store

- Developer: John Thomas
- App signing: Play App Signing enabled (upload key = moveandmeds-release.jks)
