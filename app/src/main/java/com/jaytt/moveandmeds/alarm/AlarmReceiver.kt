package com.jaytt.moveandmeds.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import androidx.room.Room
import com.jaytt.moveandmeds.data.db.AppDatabase
import com.jaytt.moveandmeds.data.model.ReminderHistory
import com.jaytt.moveandmeds.notification.NotificationHelper
import com.jaytt.moveandmeds.util.StepCountHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Calendar
import kotlin.coroutines.resume

class AlarmReceiver : BroadcastReceiver() {

    private fun isTodayInDays(daysOfWeek: String): Boolean {
        if (daysOfWeek.isBlank()) return true
        val cal = Calendar.getInstance()
        // Calendar.DAY_OF_WEEK: Sunday=1, Monday=2, ..., Saturday=7
        // Our format: Monday=1 ... Sunday=7
        val calDay = cal.get(Calendar.DAY_OF_WEEK) // 1=Sun, 2=Mon, ..., 7=Sat
        val ourDay = when (calDay) {
            Calendar.MONDAY -> 1
            Calendar.TUESDAY -> 2
            Calendar.WEDNESDAY -> 3
            Calendar.THURSDAY -> 4
            Calendar.FRIDAY -> 5
            Calendar.SATURDAY -> 6
            Calendar.SUNDAY -> 7
            else -> 1
        }
        return daysOfWeek.split(",").map { it.trim().toIntOrNull() ?: 0 }.contains(ourDay)
    }

    private fun scheduleExactOrInexact(alarmManager: AlarmManager, triggerAt: Long, pi: PendingIntent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
            } else {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
            }
        } else {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        val type = intent.getStringExtra(AlarmScheduler.EXTRA_TYPE) ?: return
        val alarmId = intent.getIntExtra(AlarmScheduler.EXTRA_ALARM_ID, -1)

        when (type) {
            AlarmScheduler.TYPE_MOVEMENT -> {
                val intervalMinutes = intent.getIntExtra("interval_minutes", 60)
                val startHour = intent.getIntExtra("start_hour", 8)
                val endHour = intent.getIntExtra("end_hour", 22)
                val stepThreshold = intent.getIntExtra("step_threshold", 50)
                val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
                val inActiveHours = currentHour in startHour until endHour

                val pendingResult = goAsync()
                CoroutineScope(Dispatchers.Main).launch {
                    val currentSteps = withTimeoutOrNull(2000L) {
                        suspendCancellableCoroutine<Long> { cont ->
                            val sensorManager = context.getSystemService(SensorManager::class.java)
                            val stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
                            if (stepSensor == null) {
                                cont.resume(-1L)
                                return@suspendCancellableCoroutine
                            }
                            val listener = object : SensorEventListener {
                                override fun onSensorChanged(event: SensorEvent) {
                                    sensorManager.unregisterListener(this)
                                    cont.resume(event.values[0].toLong())
                                }
                                override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}
                            }
                            sensorManager.registerListener(listener, stepSensor, SensorManager.SENSOR_DELAY_FASTEST)
                            cont.invokeOnCancellation { sensorManager.unregisterListener(listener) }
                        }
                    } ?: -1L

                    if (inActiveHours) {
                        val baseline = StepCountHelper.getBaseline(context)
                        val stepsSinceBaseline = if (currentSteps >= 0L && baseline >= 0L) currentSteps - baseline else -1L
                        val userAlreadyMoved = stepsSinceBaseline >= stepThreshold
                        if (!userAlreadyMoved) {
                            NotificationHelper.showMovementNotification(context)
                            NotificationHelper.playAlarmSound(context)
                        }
                    }

                    if (currentSteps >= 0L) {
                        StepCountHelper.saveBaseline(context, currentSteps)
                    }

                    val triggerAt = System.currentTimeMillis() + intervalMinutes * 60_000L
                    val nextIntent = Intent(context, AlarmReceiver::class.java).apply {
                        putExtra(AlarmScheduler.EXTRA_TYPE, AlarmScheduler.TYPE_MOVEMENT)
                        putExtra(AlarmScheduler.EXTRA_ALARM_ID, AlarmScheduler.MOVEMENT_ALARM_ID)
                        putExtra("interval_minutes", intervalMinutes)
                        putExtra("start_hour", startHour)
                        putExtra("end_hour", endHour)
                        putExtra("step_threshold", stepThreshold)
                    }
                    val pi = PendingIntent.getBroadcast(
                        context, AlarmScheduler.MOVEMENT_ALARM_ID, nextIntent,
                        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                    )
                    val am = context.getSystemService(AlarmManager::class.java)
                    scheduleExactOrInexact(am, triggerAt, pi)
                    pendingResult.finish()
                }
            }

            AlarmScheduler.TYPE_MEDICINE -> {
                val name = intent.getStringExtra(AlarmScheduler.EXTRA_MEDICINE_NAME) ?: ""
                val dosage = intent.getStringExtra(AlarmScheduler.EXTRA_MEDICINE_DOSAGE) ?: ""
                val hour = intent.getIntExtra(AlarmScheduler.EXTRA_HOUR, 8)
                val minute = intent.getIntExtra(AlarmScheduler.EXTRA_MINUTE, 0)
                val itemId = intent.getIntExtra(AlarmScheduler.EXTRA_ITEM_ID, 0)
                val daysOfWeek = intent.getStringExtra(AlarmScheduler.EXTRA_DAYS_OF_WEEK) ?: "1,2,3,4,5,6,7"
                val scheduledTime = intent.getLongExtra(AlarmScheduler.EXTRA_SCHEDULED_TIME, System.currentTimeMillis())

                val shouldShow = isTodayInDays(daysOfWeek)
                if (shouldShow) {
                    NotificationHelper.showMedicineNotification(
                        context, name, dosage, alarmId, alarmId, hour, minute, itemId, daysOfWeek, scheduledTime
                    )
                    // Record history
                    val pendingResult = goAsync()
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            val db = openDb(context)
                            db.historyDao().insert(
                                ReminderHistory(
                                    itemType = "medicine",
                                    itemId = itemId,
                                    itemName = name,
                                    scheduledTime = scheduledTime,
                                    firedTime = System.currentTimeMillis(),
                                    status = "fired"
                                )
                            )
                            db.close()
                        } finally {
                            pendingResult.finish()
                        }
                    }
                }

                // Reschedule for same time tomorrow
                val cal = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, hour)
                    set(Calendar.MINUTE, minute)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                    add(Calendar.DAY_OF_YEAR, 1)
                }
                val nextTrigger = cal.timeInMillis
                val nextIntent = Intent(context, AlarmReceiver::class.java).apply {
                    putExtra(AlarmScheduler.EXTRA_TYPE, AlarmScheduler.TYPE_MEDICINE)
                    putExtra(AlarmScheduler.EXTRA_ALARM_ID, alarmId)
                    putExtra(AlarmScheduler.EXTRA_MEDICINE_NAME, name)
                    putExtra(AlarmScheduler.EXTRA_MEDICINE_DOSAGE, dosage)
                    putExtra(AlarmScheduler.EXTRA_HOUR, hour)
                    putExtra(AlarmScheduler.EXTRA_MINUTE, minute)
                    putExtra(AlarmScheduler.EXTRA_ITEM_ID, itemId)
                    putExtra(AlarmScheduler.EXTRA_DAYS_OF_WEEK, daysOfWeek)
                    putExtra(AlarmScheduler.EXTRA_SCHEDULED_TIME, nextTrigger)
                }
                val pi = PendingIntent.getBroadcast(context, alarmId, nextIntent,
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
                val am = context.getSystemService(AlarmManager::class.java)
                scheduleExactOrInexact(am, nextTrigger, pi)

                // Schedule missed check for next occurrence
                if (shouldShow) {
                    val scheduler = AlarmScheduler(context)
                    scheduler.scheduleMissedCheck(alarmId, scheduledTime, AlarmScheduler.TYPE_MEDICINE, itemId, name, daysOfWeek)
                }
            }

            AlarmScheduler.TYPE_EXERCISE -> {
                val name = intent.getStringExtra(AlarmScheduler.EXTRA_EXERCISE_NAME) ?: ""
                val sets = intent.getStringExtra(AlarmScheduler.EXTRA_EXERCISE_SETS) ?: ""
                val reps = intent.getStringExtra(AlarmScheduler.EXTRA_EXERCISE_REPS) ?: ""
                val notes = intent.getStringExtra(AlarmScheduler.EXTRA_EXERCISE_NOTES) ?: ""
                val imagePath = intent.getStringExtra(AlarmScheduler.EXTRA_EXERCISE_IMAGE_PATH)
                val hour = intent.getIntExtra(AlarmScheduler.EXTRA_HOUR, 8)
                val minute = intent.getIntExtra(AlarmScheduler.EXTRA_MINUTE, 0)
                val itemId = intent.getIntExtra(AlarmScheduler.EXTRA_ITEM_ID, 0)
                val daysOfWeek = intent.getStringExtra(AlarmScheduler.EXTRA_DAYS_OF_WEEK) ?: "1,2,3,4,5,6,7"
                val scheduledTime = intent.getLongExtra(AlarmScheduler.EXTRA_SCHEDULED_TIME, System.currentTimeMillis())

                val shouldShow = isTodayInDays(daysOfWeek)
                if (shouldShow) {
                    NotificationHelper.showExerciseNotification(
                        context, name, sets, reps, notes, imagePath, alarmId, alarmId, hour, minute, itemId, daysOfWeek, scheduledTime
                    )
                    val pendingResult = goAsync()
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            val db = openDb(context)
                            db.historyDao().insert(
                                ReminderHistory(
                                    itemType = "exercise",
                                    itemId = itemId,
                                    itemName = name,
                                    scheduledTime = scheduledTime,
                                    firedTime = System.currentTimeMillis(),
                                    status = "fired"
                                )
                            )
                            db.close()
                        } finally {
                            pendingResult.finish()
                        }
                    }
                }

                val cal = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, hour)
                    set(Calendar.MINUTE, minute)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                    add(Calendar.DAY_OF_YEAR, 1)
                }
                val nextTrigger = cal.timeInMillis
                val nextIntent = Intent(context, AlarmReceiver::class.java).apply {
                    putExtra(AlarmScheduler.EXTRA_TYPE, AlarmScheduler.TYPE_EXERCISE)
                    putExtra(AlarmScheduler.EXTRA_ALARM_ID, alarmId)
                    putExtra(AlarmScheduler.EXTRA_EXERCISE_NAME, name)
                    putExtra(AlarmScheduler.EXTRA_EXERCISE_SETS, sets)
                    putExtra(AlarmScheduler.EXTRA_EXERCISE_REPS, reps)
                    putExtra(AlarmScheduler.EXTRA_EXERCISE_NOTES, notes)
                    putExtra(AlarmScheduler.EXTRA_EXERCISE_IMAGE_PATH, imagePath)
                    putExtra(AlarmScheduler.EXTRA_HOUR, hour)
                    putExtra(AlarmScheduler.EXTRA_MINUTE, minute)
                    putExtra(AlarmScheduler.EXTRA_ITEM_ID, itemId)
                    putExtra(AlarmScheduler.EXTRA_DAYS_OF_WEEK, daysOfWeek)
                    putExtra(AlarmScheduler.EXTRA_SCHEDULED_TIME, nextTrigger)
                }
                val pi = PendingIntent.getBroadcast(context, alarmId, nextIntent,
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
                val am = context.getSystemService(AlarmManager::class.java)
                scheduleExactOrInexact(am, nextTrigger, pi)

                if (shouldShow) {
                    val scheduler = AlarmScheduler(context)
                    scheduler.scheduleMissedCheck(alarmId, scheduledTime, AlarmScheduler.TYPE_EXERCISE, itemId, name, daysOfWeek)
                }
            }

            AlarmScheduler.TYPE_EXERCISE_INTERVAL_SINGLE -> {
                val name = intent.getStringExtra(AlarmScheduler.EXTRA_EXERCISE_NAME) ?: ""
                val sets = intent.getStringExtra(AlarmScheduler.EXTRA_EXERCISE_SETS) ?: ""
                val reps = intent.getStringExtra(AlarmScheduler.EXTRA_EXERCISE_REPS) ?: ""
                val notes = intent.getStringExtra(AlarmScheduler.EXTRA_EXERCISE_NOTES) ?: ""
                val imagePath = intent.getStringExtra(AlarmScheduler.EXTRA_EXERCISE_IMAGE_PATH)
                val itemId = intent.getIntExtra(AlarmScheduler.EXTRA_ITEM_ID, 0)
                val intervalMinutes = intent.getIntExtra(AlarmScheduler.EXTRA_INTERVAL_MINUTES, 60)
                val startHour = intent.getIntExtra(AlarmScheduler.EXTRA_INTERVAL_START_HOUR, 8)
                val endHour = intent.getIntExtra(AlarmScheduler.EXTRA_INTERVAL_END_HOUR, 22)

                NotificationHelper.showExerciseNotification(
                    context, name, sets, reps, notes, imagePath, alarmId, alarmId, 0, 0, itemId, "1,2,3,4,5,6,7"
                )

                val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
                if (currentHour in startHour until endHour) {
                    val triggerAt = System.currentTimeMillis() + intervalMinutes * 60_000L
                    val nextIntent = Intent(context, AlarmReceiver::class.java).apply {
                        putExtra(AlarmScheduler.EXTRA_TYPE, AlarmScheduler.TYPE_EXERCISE_INTERVAL_SINGLE)
                        putExtra(AlarmScheduler.EXTRA_ALARM_ID, alarmId)
                        putExtra(AlarmScheduler.EXTRA_EXERCISE_NAME, name)
                        putExtra(AlarmScheduler.EXTRA_EXERCISE_SETS, sets)
                        putExtra(AlarmScheduler.EXTRA_EXERCISE_REPS, reps)
                        putExtra(AlarmScheduler.EXTRA_EXERCISE_NOTES, notes)
                        putExtra(AlarmScheduler.EXTRA_EXERCISE_IMAGE_PATH, imagePath)
                        putExtra(AlarmScheduler.EXTRA_ITEM_ID, itemId)
                        putExtra(AlarmScheduler.EXTRA_INTERVAL_MINUTES, intervalMinutes)
                        putExtra(AlarmScheduler.EXTRA_INTERVAL_START_HOUR, startHour)
                        putExtra(AlarmScheduler.EXTRA_INTERVAL_END_HOUR, endHour)
                    }
                    val pi = PendingIntent.getBroadcast(context, alarmId, nextIntent,
                        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
                    val am = context.getSystemService(AlarmManager::class.java)
                    scheduleExactOrInexact(am, triggerAt, pi)
                }
            }

            AlarmScheduler.TYPE_SNOOZE -> {
                val originalType = intent.getStringExtra(AlarmScheduler.EXTRA_SNOOZE_ORIGINAL_TYPE) ?: return
                val snoozeAlarmId = alarmId + 60000 // unique ID for snooze alarm
                val triggerAt = System.currentTimeMillis() + 10 * 60_000L

                when (originalType) {
                    AlarmScheduler.TYPE_MEDICINE -> {
                        val name = intent.getStringExtra(AlarmScheduler.EXTRA_MEDICINE_NAME) ?: ""
                        val dosage = intent.getStringExtra(AlarmScheduler.EXTRA_MEDICINE_DOSAGE) ?: ""
                        val hour = intent.getIntExtra(AlarmScheduler.EXTRA_HOUR, 8)
                        val minute = intent.getIntExtra(AlarmScheduler.EXTRA_MINUTE, 0)
                        val itemId = intent.getIntExtra(AlarmScheduler.EXTRA_ITEM_ID, 0)
                        val daysOfWeek = intent.getStringExtra(AlarmScheduler.EXTRA_DAYS_OF_WEEK) ?: "1,2,3,4,5,6,7"
                        val snoozeIntent = Intent(context, AlarmReceiver::class.java).apply {
                            putExtra(AlarmScheduler.EXTRA_TYPE, AlarmScheduler.TYPE_MEDICINE)
                            putExtra(AlarmScheduler.EXTRA_ALARM_ID, snoozeAlarmId)
                            putExtra(AlarmScheduler.EXTRA_MEDICINE_NAME, name)
                            putExtra(AlarmScheduler.EXTRA_MEDICINE_DOSAGE, dosage)
                            putExtra(AlarmScheduler.EXTRA_HOUR, hour)
                            putExtra(AlarmScheduler.EXTRA_MINUTE, minute)
                            putExtra(AlarmScheduler.EXTRA_ITEM_ID, itemId)
                            putExtra(AlarmScheduler.EXTRA_DAYS_OF_WEEK, daysOfWeek)
                            putExtra(AlarmScheduler.EXTRA_SCHEDULED_TIME, triggerAt)
                        }
                        val pi = PendingIntent.getBroadcast(context, snoozeAlarmId, snoozeIntent,
                            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
                        val am = context.getSystemService(AlarmManager::class.java)
                        scheduleExactOrInexact(am, triggerAt, pi)
                    }
                    AlarmScheduler.TYPE_EXERCISE -> {
                        val name = intent.getStringExtra(AlarmScheduler.EXTRA_EXERCISE_NAME) ?: ""
                        val sets = intent.getStringExtra(AlarmScheduler.EXTRA_EXERCISE_SETS) ?: ""
                        val reps = intent.getStringExtra(AlarmScheduler.EXTRA_EXERCISE_REPS) ?: ""
                        val notes = intent.getStringExtra(AlarmScheduler.EXTRA_EXERCISE_NOTES) ?: ""
                        val imagePath = intent.getStringExtra(AlarmScheduler.EXTRA_EXERCISE_IMAGE_PATH)
                        val hour = intent.getIntExtra(AlarmScheduler.EXTRA_HOUR, 8)
                        val minute = intent.getIntExtra(AlarmScheduler.EXTRA_MINUTE, 0)
                        val itemId = intent.getIntExtra(AlarmScheduler.EXTRA_ITEM_ID, 0)
                        val daysOfWeek = intent.getStringExtra(AlarmScheduler.EXTRA_DAYS_OF_WEEK) ?: "1,2,3,4,5,6,7"
                        val snoozeIntent = Intent(context, AlarmReceiver::class.java).apply {
                            putExtra(AlarmScheduler.EXTRA_TYPE, AlarmScheduler.TYPE_EXERCISE)
                            putExtra(AlarmScheduler.EXTRA_ALARM_ID, snoozeAlarmId)
                            putExtra(AlarmScheduler.EXTRA_EXERCISE_NAME, name)
                            putExtra(AlarmScheduler.EXTRA_EXERCISE_SETS, sets)
                            putExtra(AlarmScheduler.EXTRA_EXERCISE_REPS, reps)
                            putExtra(AlarmScheduler.EXTRA_EXERCISE_NOTES, notes)
                            putExtra(AlarmScheduler.EXTRA_EXERCISE_IMAGE_PATH, imagePath)
                            putExtra(AlarmScheduler.EXTRA_HOUR, hour)
                            putExtra(AlarmScheduler.EXTRA_MINUTE, minute)
                            putExtra(AlarmScheduler.EXTRA_ITEM_ID, itemId)
                            putExtra(AlarmScheduler.EXTRA_DAYS_OF_WEEK, daysOfWeek)
                            putExtra(AlarmScheduler.EXTRA_SCHEDULED_TIME, triggerAt)
                        }
                        val pi = PendingIntent.getBroadcast(context, snoozeAlarmId, snoozeIntent,
                            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
                        val am = context.getSystemService(AlarmManager::class.java)
                        scheduleExactOrInexact(am, triggerAt, pi)
                    }
                }
            }

            AlarmScheduler.TYPE_TAKEN -> {
                val originalType = intent.getStringExtra(AlarmScheduler.EXTRA_SNOOZE_ORIGINAL_TYPE) ?: return
                val itemId = intent.getIntExtra(AlarmScheduler.EXTRA_ITEM_ID, 0)
                val itemName = when (originalType) {
                    AlarmScheduler.TYPE_MEDICINE -> intent.getStringExtra(AlarmScheduler.EXTRA_MEDICINE_NAME) ?: ""
                    AlarmScheduler.TYPE_EXERCISE -> intent.getStringExtra(AlarmScheduler.EXTRA_EXERCISE_NAME) ?: ""
                    else -> ""
                }
                val scheduledTime = intent.getLongExtra(AlarmScheduler.EXTRA_SCHEDULED_TIME, System.currentTimeMillis())

                context.getSystemService(android.app.NotificationManager::class.java).cancel(alarmId)

                val pendingResult = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val db = openDb(context)
                        db.historyDao().insert(
                            ReminderHistory(
                                itemType = originalType,
                                itemId = itemId,
                                itemName = itemName,
                                scheduledTime = scheduledTime,
                                firedTime = System.currentTimeMillis(),
                                status = "taken_$originalType"
                            )
                        )
                        db.close()
                    } finally {
                        pendingResult.finish()
                    }
                }
            }

            AlarmScheduler.TYPE_SKIPPED -> {
                val originalType = intent.getStringExtra(AlarmScheduler.EXTRA_SNOOZE_ORIGINAL_TYPE) ?: return
                val itemId = intent.getIntExtra(AlarmScheduler.EXTRA_ITEM_ID, 0)
                val itemName = when (originalType) {
                    AlarmScheduler.TYPE_MEDICINE -> intent.getStringExtra(AlarmScheduler.EXTRA_MEDICINE_NAME) ?: ""
                    AlarmScheduler.TYPE_EXERCISE -> intent.getStringExtra(AlarmScheduler.EXTRA_EXERCISE_NAME) ?: ""
                    else -> ""
                }
                val scheduledTime = intent.getLongExtra(AlarmScheduler.EXTRA_SCHEDULED_TIME, System.currentTimeMillis())

                context.getSystemService(android.app.NotificationManager::class.java).cancel(alarmId)

                val pendingResult = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val db = openDb(context)
                        db.historyDao().insert(
                            ReminderHistory(
                                itemType = originalType,
                                itemId = itemId,
                                itemName = itemName,
                                scheduledTime = scheduledTime,
                                firedTime = System.currentTimeMillis(),
                                status = "skipped_$originalType"
                            )
                        )
                        db.close()
                    } finally {
                        pendingResult.finish()
                    }
                }
            }

            AlarmScheduler.TYPE_DISMISSED -> {
                val originalType = intent.getStringExtra(AlarmScheduler.EXTRA_SNOOZE_ORIGINAL_TYPE) ?: return
                val itemId = intent.getIntExtra(AlarmScheduler.EXTRA_ITEM_ID, 0)
                val itemName = when (originalType) {
                    AlarmScheduler.TYPE_MEDICINE -> intent.getStringExtra(AlarmScheduler.EXTRA_MEDICINE_NAME) ?: ""
                    AlarmScheduler.TYPE_EXERCISE -> intent.getStringExtra(AlarmScheduler.EXTRA_EXERCISE_NAME) ?: ""
                    else -> "Movement"
                }
                val scheduledTime = intent.getLongExtra(AlarmScheduler.EXTRA_SCHEDULED_TIME, System.currentTimeMillis())

                val pendingResult = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val db = openDb(context)
                        db.historyDao().insert(
                            ReminderHistory(
                                itemType = originalType,
                                itemId = itemId,
                                itemName = itemName,
                                scheduledTime = scheduledTime,
                                firedTime = System.currentTimeMillis(),
                                status = "dismissed"
                            )
                        )
                        db.close()
                    } finally {
                        pendingResult.finish()
                    }
                }
            }

            AlarmScheduler.TYPE_MISSED_CHECK -> {
                val originalType = intent.getStringExtra(AlarmScheduler.EXTRA_SNOOZE_ORIGINAL_TYPE) ?: return
                val itemId = intent.getIntExtra(AlarmScheduler.EXTRA_ITEM_ID, 0)
                val itemName = intent.getStringExtra(AlarmScheduler.EXTRA_MEDICINE_NAME) ?: ""
                val scheduledTime = intent.getLongExtra(AlarmScheduler.EXTRA_SCHEDULED_TIME, 0L)

                val pendingResult = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val db = openDb(context)
                        val windowStart = scheduledTime - 5 * 60_000L
                        val windowEnd = scheduledTime + 60 * 60_000L
                        val fired = db.historyDao().getFiredInWindow(originalType, itemId, windowStart, windowEnd)
                        if (fired.isEmpty()) {
                            db.historyDao().insert(
                                ReminderHistory(
                                    itemType = originalType,
                                    itemId = itemId,
                                    itemName = itemName,
                                    scheduledTime = scheduledTime,
                                    firedTime = System.currentTimeMillis(),
                                    status = "missed"
                                )
                            )
                        }
                        db.close()
                    } finally {
                        pendingResult.finish()
                    }
                }
            }
        }
    }

    private fun openDb(context: Context): AppDatabase {
        return Room.databaseBuilder(context, AppDatabase::class.java, "moveandmeds.db")
            .addMigrations(
                AppDatabase.MIGRATION_1_2,
                AppDatabase.MIGRATION_2_3,
                AppDatabase.MIGRATION_3_4,
                AppDatabase.MIGRATION_4_5,
                AppDatabase.MIGRATION_5_6,
                AppDatabase.MIGRATION_6_7,
                AppDatabase.MIGRATION_7_8,
                AppDatabase.MIGRATION_8_9
            )
            .build()
    }
}
