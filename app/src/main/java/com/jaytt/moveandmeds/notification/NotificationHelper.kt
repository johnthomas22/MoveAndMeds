package com.jaytt.moveandmeds.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.core.app.NotificationCompat
import com.jaytt.moveandmeds.ExerciseInfoActivity
import com.jaytt.moveandmeds.MainActivity
import com.jaytt.moveandmeds.R
import com.jaytt.moveandmeds.alarm.AlarmReceiver
import com.jaytt.moveandmeds.alarm.AlarmScheduler

object NotificationHelper {
    const val CHANNEL_MOVEMENT = "channel_movement"
    // Separate channel so we can set alarm-volume sound (channel settings are immutable once created)
    const val CHANNEL_MOVEMENT_ALARM = "channel_movement_alarm"
    // DND-respecting movement channel: IMPORTANCE_HIGH + vibration, no USAGE_ALARM bypass
    const val CHANNEL_MOVEMENT_VIBRATE = "channel_movement_vibrate"
    const val CHANNEL_MEDICINE = "channel_medicine"
    const val CHANNEL_EXERCISE = "channel_exercise"

    fun createChannels(context: Context) {
        val nm = context.getSystemService(NotificationManager::class.java)

        // Legacy channel — kept so old notifications still resolve; not used for new alarms
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_MOVEMENT, "Movement Reminders",
                NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "Reminders to get up and move"
            }
        )

        // Alarm-volume movement channel — plays at alarm volume, ignores silent mode
        val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_MOVEMENT_ALARM, "Movement Alarm",
                NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Loud alarm to get up and move"
                setSound(
                    alarmUri,
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 300, 150, 600)
            }
        )

        // DND-respecting movement channel — vibration only, no alarm-volume bypass
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_MOVEMENT_VIBRATE, "Movement Reminder (Silent)",
                NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Vibration-only movement reminder used when Do Not Disturb is active"
                setSound(null, null)
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 300, 150, 600)
            }
        )

        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_MEDICINE, "Medicine Reminders",
                NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Reminders to take your medicine"
            }
        )
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_EXERCISE, "Exercise Reminders",
                NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Reminders to do your physiotherapy exercises"
            }
        )
    }

    /**
     * Play the phone's alarm ringtone at alarm volume for [durationMs] milliseconds.
     * Uses USAGE_ALARM so it plays even in silent mode, just like a real alarm clock.
     * To use Ride of the Valkyries: set it as your phone's default alarm ringtone
     * (Settings → Sounds → Alarm sound).
     */
    fun playAlarmSound(context: Context, durationMs: Long = 8_000L) {
        val nm = context.getSystemService(NotificationManager::class.java)
        val isDnd = nm.currentInterruptionFilter != NotificationManager.INTERRUPTION_FILTER_ALL
        if (isDnd) {
            val vibrator = context.getSystemService(Vibrator::class.java)
            vibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 300, 150, 600, 150, 300), -1))
            return
        }
        val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        val ringtone = RingtoneManager.getRingtone(context, uri) ?: return
        ringtone.audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build()
        ringtone.play()
        Handler(Looper.getMainLooper()).postDelayed({
            if (ringtone.isPlaying) ringtone.stop()
        }, durationMs)
    }

    fun showMovementNotification(context: Context) {
        val nm = context.getSystemService(NotificationManager::class.java)
        val isDnd = nm.currentInterruptionFilter != NotificationManager.INTERRUPTION_FILTER_ALL
        val channel = if (isDnd) CHANNEL_MOVEMENT_VIBRATE else CHANNEL_MOVEMENT_ALARM

        val openAppIntent = Intent(context, MainActivity::class.java)
        val openAppPi = PendingIntent.getActivity(context, 0, openAppIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

        val dismissIntent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra(AlarmScheduler.EXTRA_TYPE, AlarmScheduler.TYPE_DISMISSED)
            putExtra(AlarmScheduler.EXTRA_SNOOZE_ORIGINAL_TYPE, AlarmScheduler.TYPE_MOVEMENT)
            putExtra(AlarmScheduler.EXTRA_SCHEDULED_TIME, System.currentTimeMillis())
        }
        val dismissPi = PendingIntent.getBroadcast(
            context,
            AlarmScheduler.MOVEMENT_ALARM_ID + 70000,
            dismissIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Time to move!")
            .setContentText("Get up and move for a few minutes.")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(openAppPi)
            .setDeleteIntent(dismissPi)
            // Full-screen intent: pops up even on locked screen
            .build()
        context.getSystemService(NotificationManager::class.java)
            .notify(AlarmScheduler.MOVEMENT_ALARM_ID, notification)
    }

    fun showMedicineNotification(
        context: Context,
        medicineName: String,
        dosage: String,
        notifId: Int,
        alarmId: Int,
        hour: Int,
        minute: Int,
        itemId: Int,
        daysOfWeek: String,
        scheduledTime: Long = System.currentTimeMillis()
    ) {
        val intent = Intent(context, MainActivity::class.java)
        val pi = PendingIntent.getActivity(context, notifId, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

        // Taken action
        val takenIntent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra(AlarmScheduler.EXTRA_TYPE, AlarmScheduler.TYPE_TAKEN)
            putExtra(AlarmScheduler.EXTRA_ALARM_ID, alarmId)
            putExtra(AlarmScheduler.EXTRA_SNOOZE_ORIGINAL_TYPE, AlarmScheduler.TYPE_MEDICINE)
            putExtra(AlarmScheduler.EXTRA_ITEM_ID, itemId)
            putExtra(AlarmScheduler.EXTRA_MEDICINE_NAME, medicineName)
            putExtra(AlarmScheduler.EXTRA_SCHEDULED_TIME, scheduledTime)
        }
        val takenPi = PendingIntent.getBroadcast(
            context,
            alarmId + 50000,
            takenIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Skip action
        val skipIntent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra(AlarmScheduler.EXTRA_TYPE, AlarmScheduler.TYPE_SKIPPED)
            putExtra(AlarmScheduler.EXTRA_ALARM_ID, alarmId)
            putExtra(AlarmScheduler.EXTRA_SNOOZE_ORIGINAL_TYPE, AlarmScheduler.TYPE_MEDICINE)
            putExtra(AlarmScheduler.EXTRA_ITEM_ID, itemId)
            putExtra(AlarmScheduler.EXTRA_MEDICINE_NAME, medicineName)
            putExtra(AlarmScheduler.EXTRA_SCHEDULED_TIME, scheduledTime)
        }
        val skipPi = PendingIntent.getBroadcast(
            context,
            alarmId + 80000,
            skipIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Dismiss intent — fires when user swipes the notification away
        val dismissIntent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra(AlarmScheduler.EXTRA_TYPE, AlarmScheduler.TYPE_DISMISSED)
            putExtra(AlarmScheduler.EXTRA_SNOOZE_ORIGINAL_TYPE, AlarmScheduler.TYPE_MEDICINE)
            putExtra(AlarmScheduler.EXTRA_ITEM_ID, itemId)
            putExtra(AlarmScheduler.EXTRA_MEDICINE_NAME, medicineName)
            putExtra(AlarmScheduler.EXTRA_SCHEDULED_TIME, scheduledTime)
        }
        val dismissPi = PendingIntent.getBroadcast(
            context,
            alarmId + 70000,
            dismissIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_MEDICINE)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Medicine reminder")
            .setContentText("Time to take: $medicineName${if (dosage.isNotBlank()) " ($dosage)" else ""}")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pi)
            .setDeleteIntent(dismissPi)
            .addAction(R.drawable.ic_notification, "✓ Taken", takenPi)
            .addAction(R.drawable.ic_notification, "✗ Skip", skipPi)
            .build()
        context.getSystemService(NotificationManager::class.java)
            .notify(notifId, notification)
    }

    fun showExerciseNotification(
        context: Context,
        exerciseName: String,
        sets: String,
        reps: String,
        notes: String,
        imagePath: String?,
        notifId: Int,
        alarmId: Int,
        hour: Int,
        minute: Int,
        itemId: Int,
        daysOfWeek: String,
        scheduledTime: Long = System.currentTimeMillis()
    ) {
        val intent = Intent(context, MainActivity::class.java)
        val pi = PendingIntent.getActivity(context, notifId, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

        val detail = buildString {
            append(exerciseName)
            if (sets.isNotBlank() || reps.isNotBlank()) {
                append(" — ")
                if (sets.isNotBlank()) append("$sets sets")
                if (sets.isNotBlank() && reps.isNotBlank()) append(" x ")
                if (reps.isNotBlank()) append("$reps reps")
            }
        }

        // Done action
        val takenIntent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra(AlarmScheduler.EXTRA_TYPE, AlarmScheduler.TYPE_TAKEN)
            putExtra(AlarmScheduler.EXTRA_ALARM_ID, alarmId)
            putExtra(AlarmScheduler.EXTRA_SNOOZE_ORIGINAL_TYPE, AlarmScheduler.TYPE_EXERCISE)
            putExtra(AlarmScheduler.EXTRA_ITEM_ID, itemId)
            putExtra(AlarmScheduler.EXTRA_EXERCISE_NAME, exerciseName)
            putExtra(AlarmScheduler.EXTRA_SCHEDULED_TIME, scheduledTime)
        }
        val takenPi = PendingIntent.getBroadcast(
            context,
            alarmId + 50000,
            takenIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Skip action
        val skipIntent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra(AlarmScheduler.EXTRA_TYPE, AlarmScheduler.TYPE_SKIPPED)
            putExtra(AlarmScheduler.EXTRA_ALARM_ID, alarmId)
            putExtra(AlarmScheduler.EXTRA_SNOOZE_ORIGINAL_TYPE, AlarmScheduler.TYPE_EXERCISE)
            putExtra(AlarmScheduler.EXTRA_ITEM_ID, itemId)
            putExtra(AlarmScheduler.EXTRA_EXERCISE_NAME, exerciseName)
            putExtra(AlarmScheduler.EXTRA_SCHEDULED_TIME, scheduledTime)
        }
        val skipPi = PendingIntent.getBroadcast(
            context,
            alarmId + 80000,
            skipIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Dismiss intent — fires when user swipes the notification away
        val dismissIntent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra(AlarmScheduler.EXTRA_TYPE, AlarmScheduler.TYPE_DISMISSED)
            putExtra(AlarmScheduler.EXTRA_SNOOZE_ORIGINAL_TYPE, AlarmScheduler.TYPE_EXERCISE)
            putExtra(AlarmScheduler.EXTRA_ITEM_ID, itemId)
            putExtra(AlarmScheduler.EXTRA_EXERCISE_NAME, exerciseName)
            putExtra(AlarmScheduler.EXTRA_SCHEDULED_TIME, scheduledTime)
        }
        val dismissPi = PendingIntent.getBroadcast(
            context,
            alarmId + 70000,
            dismissIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val viewIntent = Intent(context, ExerciseInfoActivity::class.java).apply {
            putExtra("exercise_name", exerciseName)
            putExtra("exercise_notes", notes)
            putExtra("exercise_image_path", imagePath)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        val viewPi = PendingIntent.getActivity(
            context,
            notifId + 90000,
            viewIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_EXERCISE)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Exercise reminder")
            .setContentText(detail)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pi)
            .setDeleteIntent(dismissPi)
            .addAction(R.drawable.ic_notification, "View Details", viewPi)
            .addAction(R.drawable.ic_notification, "✓ Done", takenPi)
            .addAction(R.drawable.ic_notification, "✗ Skip", skipPi)
            .build()
        context.getSystemService(NotificationManager::class.java)
            .notify(notifId, notification)
    }
}
