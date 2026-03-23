package com.jaytt.moveandmeds.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.jaytt.moveandmeds.MainActivity
import com.jaytt.moveandmeds.R
import com.jaytt.moveandmeds.alarm.AlarmReceiver
import com.jaytt.moveandmeds.alarm.AlarmScheduler

object NotificationHelper {
    const val CHANNEL_MOVEMENT = "channel_movement"
    const val CHANNEL_MEDICINE = "channel_medicine"
    const val CHANNEL_EXERCISE = "channel_exercise"

    fun createChannels(context: Context) {
        val nm = context.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_MOVEMENT, "Movement Reminders",
                NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Reminders to get up and move"
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

    fun showMovementNotification(context: Context) {
        val intent = Intent(context, MainActivity::class.java)
        val pi = PendingIntent.getActivity(context, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val notification = NotificationCompat.Builder(context, CHANNEL_MOVEMENT)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Time to move!")
            .setContentText("Stand up and stretch for a few minutes.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pi)
            .build()
        context.getSystemService(NotificationManager::class.java)
            .notify(1000, notification)
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
        daysOfWeek: String
    ) {
        val intent = Intent(context, MainActivity::class.java)
        val pi = PendingIntent.getActivity(context, notifId, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

        // Snooze action
        val snoozeIntent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra(AlarmScheduler.EXTRA_TYPE, AlarmScheduler.TYPE_SNOOZE)
            putExtra(AlarmScheduler.EXTRA_ALARM_ID, alarmId)
            putExtra(AlarmScheduler.EXTRA_MEDICINE_NAME, medicineName)
            putExtra(AlarmScheduler.EXTRA_MEDICINE_DOSAGE, dosage)
            putExtra(AlarmScheduler.EXTRA_HOUR, hour)
            putExtra(AlarmScheduler.EXTRA_MINUTE, minute)
            putExtra(AlarmScheduler.EXTRA_ITEM_ID, itemId)
            putExtra(AlarmScheduler.EXTRA_DAYS_OF_WEEK, daysOfWeek)
            putExtra(AlarmScheduler.EXTRA_SNOOZE_ORIGINAL_TYPE, AlarmScheduler.TYPE_MEDICINE)
        }
        val snoozePi = PendingIntent.getBroadcast(
            context,
            alarmId + 50000,
            snoozeIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_MEDICINE)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Medicine reminder")
            .setContentText("Time to take: $medicineName${if (dosage.isNotBlank()) " ($dosage)" else ""}")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pi)
            .addAction(R.drawable.ic_notification, "Snooze 10 min", snoozePi)
            .build()
        context.getSystemService(NotificationManager::class.java)
            .notify(notifId, notification)
    }

    fun showExerciseNotification(
        context: Context,
        exerciseName: String,
        sets: String,
        reps: String,
        notifId: Int,
        alarmId: Int,
        hour: Int,
        minute: Int,
        itemId: Int,
        daysOfWeek: String
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

        // Snooze action
        val snoozeIntent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra(AlarmScheduler.EXTRA_TYPE, AlarmScheduler.TYPE_SNOOZE)
            putExtra(AlarmScheduler.EXTRA_ALARM_ID, alarmId)
            putExtra(AlarmScheduler.EXTRA_EXERCISE_NAME, exerciseName)
            putExtra(AlarmScheduler.EXTRA_EXERCISE_SETS, sets)
            putExtra(AlarmScheduler.EXTRA_EXERCISE_REPS, reps)
            putExtra(AlarmScheduler.EXTRA_HOUR, hour)
            putExtra(AlarmScheduler.EXTRA_MINUTE, minute)
            putExtra(AlarmScheduler.EXTRA_ITEM_ID, itemId)
            putExtra(AlarmScheduler.EXTRA_DAYS_OF_WEEK, daysOfWeek)
            putExtra(AlarmScheduler.EXTRA_SNOOZE_ORIGINAL_TYPE, AlarmScheduler.TYPE_EXERCISE)
        }
        val snoozePi = PendingIntent.getBroadcast(
            context,
            alarmId + 50000,
            snoozeIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_EXERCISE)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Exercise reminder")
            .setContentText(detail)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pi)
            .addAction(R.drawable.ic_notification, "Snooze 10 min", snoozePi)
            .build()
        context.getSystemService(NotificationManager::class.java)
            .notify(notifId, notification)
    }
}
