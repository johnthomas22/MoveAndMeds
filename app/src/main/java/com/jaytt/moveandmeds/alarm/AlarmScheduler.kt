package com.jaytt.moveandmeds.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.jaytt.moveandmeds.data.model.Exercise
import com.jaytt.moveandmeds.data.model.ExerciseWithTimes
import com.jaytt.moveandmeds.data.model.MedicineWithTimes
import com.jaytt.moveandmeds.data.model.MovementSettings
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AlarmScheduler @Inject constructor(private val context: Context) {

    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    companion object {
        const val MOVEMENT_ALARM_ID = 1000
        const val EXTRA_TYPE = "alarm_type"
        const val EXTRA_MEDICINE_NAME = "medicine_name"
        const val EXTRA_MEDICINE_DOSAGE = "medicine_dosage"
        const val EXTRA_EXERCISE_NAME = "exercise_name"
        const val EXTRA_EXERCISE_SETS = "exercise_sets"
        const val EXTRA_EXERCISE_REPS = "exercise_reps"
        const val EXTRA_ALARM_ID = "alarm_id"
        const val EXTRA_HOUR = "hour"
        const val EXTRA_MINUTE = "minute"
        const val EXTRA_ITEM_ID = "item_id"
        const val EXTRA_DAYS_OF_WEEK = "days_of_week"
        const val EXTRA_SNOOZE_ORIGINAL_TYPE = "snooze_original_type"
        const val TYPE_MOVEMENT = "movement"
        const val TYPE_MEDICINE = "medicine"
        const val TYPE_EXERCISE = "exercise"
        const val TYPE_EXERCISE_INTERVAL_SINGLE = "exercise_interval_single"
        const val TYPE_SNOOZE = "snooze"
        const val TYPE_MISSED_CHECK = "missed_check"
        const val TYPE_DISMISSED = "dismissed"
        const val TYPE_TAKEN = "taken"
        const val TYPE_SKIPPED = "skipped"
        const val EXTRA_SCHEDULED_TIME = "scheduled_time"
        const val EXTRA_EXERCISE_NOTES = "exercise_notes"
        const val EXTRA_EXERCISE_IMAGE_PATH = "exercise_image_path"
        const val EXTRA_INTERVAL_MINUTES = "interval_minutes_ex"
        const val EXTRA_INTERVAL_START_HOUR = "interval_start_hour"
        const val EXTRA_INTERVAL_END_HOUR = "interval_end_hour"

        fun medicineAlarmId(medicineId: Int, timeIndex: Int) = medicineId * 100 + timeIndex
        fun exerciseAlarmId(exerciseId: Int, timeIndex: Int) = 20000 + exerciseId * 100 + timeIndex
        fun exerciseIntervalAlarmId(exerciseId: Int) = 30000 + exerciseId
        fun missedCheckAlarmId(alarmId: Int) = alarmId + 100000
    }

    private fun canScheduleExact(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }
    }

    private fun setAlarm(triggerAt: Long, pi: PendingIntent) {
        if (canScheduleExact()) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
        } else {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
        }
    }

    fun scheduleMovementAlarm(settings: MovementSettings) {
        cancelMovementAlarm()
        if (!settings.isEnabled) return

        val triggerAt = System.currentTimeMillis() + settings.intervalMinutes * 60_000L
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra(EXTRA_TYPE, TYPE_MOVEMENT)
            putExtra(EXTRA_ALARM_ID, MOVEMENT_ALARM_ID)
            putExtra("interval_minutes", settings.intervalMinutes)
            putExtra("start_hour", settings.startHour)
            putExtra("end_hour", settings.endHour)
            putExtra("step_threshold", settings.stepThreshold)
        }
        val pi = PendingIntent.getBroadcast(context, MOVEMENT_ALARM_ID, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        setAlarm(triggerAt, pi)
    }

    fun cancelMovementAlarm() {
        val intent = Intent(context, AlarmReceiver::class.java)
        val pi = PendingIntent.getBroadcast(context, MOVEMENT_ALARM_ID, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_NO_CREATE)
        pi?.let { alarmManager.cancel(it) }
    }

    fun scheduleMedicineAlarms(mwt: MedicineWithTimes) {
        cancelMedicineAlarms(mwt.medicine.id)
        if (!mwt.medicine.isEnabled) return
        mwt.times.forEachIndexed { index, time ->
            val alarmId = medicineAlarmId(mwt.medicine.id, index)
            val triggerAt = nextOccurrence(time.hour, time.minute)
            val intent = Intent(context, AlarmReceiver::class.java).apply {
                putExtra(EXTRA_TYPE, TYPE_MEDICINE)
                putExtra(EXTRA_ALARM_ID, alarmId)
                putExtra(EXTRA_MEDICINE_NAME, mwt.medicine.name)
                putExtra(EXTRA_MEDICINE_DOSAGE, mwt.medicine.dosage)
                putExtra(EXTRA_HOUR, time.hour)
                putExtra(EXTRA_MINUTE, time.minute)
                putExtra(EXTRA_ITEM_ID, mwt.medicine.id)
                putExtra(EXTRA_DAYS_OF_WEEK, mwt.medicine.daysOfWeek)
                putExtra(EXTRA_SCHEDULED_TIME, triggerAt)
            }
            val pi = PendingIntent.getBroadcast(context, alarmId, intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
            setAlarm(triggerAt, pi)
        }
    }

    fun cancelMedicineAlarms(medicineId: Int) {
        for (i in 0 until 20) {
            val alarmId = medicineAlarmId(medicineId, i)
            val intent = Intent(context, AlarmReceiver::class.java)
            val pi = PendingIntent.getBroadcast(context, alarmId, intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_NO_CREATE)
            pi?.let { alarmManager.cancel(it) }
            // Also cancel missed check
            val missedPi = PendingIntent.getBroadcast(context, missedCheckAlarmId(alarmId), intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_NO_CREATE)
            missedPi?.let { alarmManager.cancel(it) }
        }
    }

    fun scheduleExerciseAlarms(ewt: ExerciseWithTimes) {
        cancelExerciseAlarms(ewt.exercise.id)
        if (!ewt.exercise.isEnabled) return
        if (ewt.exercise.reminderType == "interval") {
            scheduleExerciseIntervalAlarm(ewt.exercise)
        } else {
            ewt.times.forEachIndexed { index, time ->
                val alarmId = exerciseAlarmId(ewt.exercise.id, index)
                val triggerAt = nextOccurrence(time.hour, time.minute)
                val intent = Intent(context, AlarmReceiver::class.java).apply {
                    putExtra(EXTRA_TYPE, TYPE_EXERCISE)
                    putExtra(EXTRA_ALARM_ID, alarmId)
                    putExtra(EXTRA_EXERCISE_NAME, ewt.exercise.name)
                    putExtra(EXTRA_EXERCISE_SETS, ewt.exercise.sets)
                    putExtra(EXTRA_EXERCISE_REPS, ewt.exercise.reps)
                    putExtra(EXTRA_EXERCISE_NOTES, ewt.exercise.notes)
                    putExtra(EXTRA_EXERCISE_IMAGE_PATH, ewt.exercise.imagePath)
                    putExtra(EXTRA_HOUR, time.hour)
                    putExtra(EXTRA_MINUTE, time.minute)
                    putExtra(EXTRA_ITEM_ID, ewt.exercise.id)
                    putExtra(EXTRA_DAYS_OF_WEEK, ewt.exercise.daysOfWeek)
                    putExtra(EXTRA_SCHEDULED_TIME, triggerAt)
                }
                val pi = PendingIntent.getBroadcast(context, alarmId, intent,
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
                setAlarm(triggerAt, pi)
            }
        }
    }

    fun scheduleExerciseIntervalAlarm(exercise: Exercise) {
        val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        if (currentHour !in exercise.intervalStartHour until exercise.intervalEndHour) return
        val alarmId = exerciseIntervalAlarmId(exercise.id)
        val triggerAt = System.currentTimeMillis() + exercise.intervalMinutes * 60_000L
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra(EXTRA_TYPE, TYPE_EXERCISE_INTERVAL_SINGLE)
            putExtra(EXTRA_ALARM_ID, alarmId)
            putExtra(EXTRA_EXERCISE_NAME, exercise.name)
            putExtra(EXTRA_EXERCISE_SETS, exercise.sets)
            putExtra(EXTRA_EXERCISE_REPS, exercise.reps)
            putExtra(EXTRA_EXERCISE_NOTES, exercise.notes)
            putExtra(EXTRA_EXERCISE_IMAGE_PATH, exercise.imagePath)
            putExtra(EXTRA_ITEM_ID, exercise.id)
            putExtra(EXTRA_INTERVAL_MINUTES, exercise.intervalMinutes)
            putExtra(EXTRA_INTERVAL_START_HOUR, exercise.intervalStartHour)
            putExtra(EXTRA_INTERVAL_END_HOUR, exercise.intervalEndHour)
        }
        val pi = PendingIntent.getBroadcast(context, alarmId, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        setAlarm(triggerAt, pi)
    }

    fun cancelExerciseIntervalAlarm(exerciseId: Int) {
        val alarmId = exerciseIntervalAlarmId(exerciseId)
        val intent = Intent(context, AlarmReceiver::class.java)
        val pi = PendingIntent.getBroadcast(context, alarmId, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_NO_CREATE)
        pi?.let { alarmManager.cancel(it) }
    }

    fun cancelExerciseAlarms(exerciseId: Int) {
        for (i in 0 until 20) {
            val alarmId = exerciseAlarmId(exerciseId, i)
            val intent = Intent(context, AlarmReceiver::class.java)
            val pi = PendingIntent.getBroadcast(context, alarmId, intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_NO_CREATE)
            pi?.let { alarmManager.cancel(it) }
            // Also cancel missed check
            val missedPi = PendingIntent.getBroadcast(context, missedCheckAlarmId(alarmId), intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_NO_CREATE)
            missedPi?.let { alarmManager.cancel(it) }
        }
        // Also cancel any interval alarm for this exercise
        cancelExerciseIntervalAlarm(exerciseId)
    }

    fun scheduleAllAlarms(
        medicines: List<MedicineWithTimes>,
        movementSettings: MovementSettings?,
        exercises: List<ExerciseWithTimes> = emptyList()
    ) {
        medicines.forEach { scheduleMedicineAlarms(it) }
        movementSettings?.let { scheduleMovementAlarm(it) }
        exercises.forEach { scheduleExerciseAlarms(it) }
    }

    fun scheduleMissedCheck(alarmId: Int, scheduledTime: Long, type: String, itemId: Int, itemName: String, daysOfWeek: String) {
        val checkAt = scheduledTime + 60 * 60_000L // 1 hour after scheduled
        val missedCheckId = missedCheckAlarmId(alarmId)
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra(EXTRA_TYPE, TYPE_MISSED_CHECK)
            putExtra(EXTRA_ALARM_ID, alarmId)
            putExtra(EXTRA_SCHEDULED_TIME, scheduledTime)
            putExtra(EXTRA_SNOOZE_ORIGINAL_TYPE, type)
            putExtra(EXTRA_ITEM_ID, itemId)
            putExtra(EXTRA_MEDICINE_NAME, itemName)
            putExtra(EXTRA_DAYS_OF_WEEK, daysOfWeek)
        }
        val pi = PendingIntent.getBroadcast(context, missedCheckId, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        setAlarm(checkAt, pi)
    }

    fun nextOccurrence(hour: Int, minute: Int): Long {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (cal.timeInMillis <= System.currentTimeMillis()) {
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
        return cal.timeInMillis
    }
}
