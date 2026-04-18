# Add project specific ProGuard rules here.

# Room - keep entity and DAO classes
-keep class com.jaytt.moveandmeds.data.model.** { *; }
-keep interface com.jaytt.moveandmeds.data.db.** { *; }

# Hilt
-dontwarn dagger.hilt.**
-keep class dagger.hilt.** { *; }
-keep @dagger.hilt.android.HiltAndroidApp class * { *; }
-keep @dagger.hilt.android.lifecycle.HiltViewModel class * { *; }

# Keep BroadcastReceivers (AlarmReceiver, BootReceiver)
-keep class com.jaytt.moveandmeds.alarm.** { *; }

# Keep Application class
-keep class com.jaytt.moveandmeds.ReminderApplication { *; }

# PDFBox optional JP2 decoder (not included in pdfbox-android)
-dontwarn com.gemalto.jp2.**
