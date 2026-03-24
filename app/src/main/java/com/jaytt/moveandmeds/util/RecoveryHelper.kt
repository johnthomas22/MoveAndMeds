package com.jaytt.moveandmeds.util

import android.content.Context
import java.time.LocalDate
import java.time.temporal.ChronoUnit

data class RecoveryMilestone(val day: Int, val description: String)

object RecoveryHelper {

    const val PREFS_NAME = "app_prefs"
    private const val KEY_RECOVERY_START = "recovery_start_date"

    private val milestones = listOf(
        RecoveryMilestone(3, "your first major steps"),
        RecoveryMilestone(7, "your one-week milestone"),
        RecoveryMilestone(14, "your two-week check-up"),
        RecoveryMilestone(21, "three weeks of healing"),
        RecoveryMilestone(30, "your one-month milestone"),
        RecoveryMilestone(42, "your six-week check-up"),
        RecoveryMilestone(60, "two months of recovery"),
        RecoveryMilestone(90, "your three-month milestone"),
        RecoveryMilestone(180, "six months of recovery"),
        RecoveryMilestone(365, "your one-year anniversary")
    )

    fun getRecoveryStartEpochDay(context: Context): Long {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getLong(KEY_RECOVERY_START, -1L)
    }

    fun getRecoveryStartDate(context: Context): LocalDate? {
        val epochDay = getRecoveryStartEpochDay(context)
        return if (epochDay >= 0) LocalDate.ofEpochDay(epochDay) else null
    }

    fun setRecoveryStartDate(context: Context, date: LocalDate?) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (date == null) {
            prefs.edit().remove(KEY_RECOVERY_START).apply()
        } else {
            prefs.edit().putLong(KEY_RECOVERY_START, date.toEpochDay()).apply()
        }
    }

    fun getMotivationalMessage(startDate: LocalDate): String {
        val today = LocalDate.now()
        val dayOfRecovery = ChronoUnit.DAYS.between(startDate, today).toInt() + 1
        if (dayOfRecovery < 1) return ""

        val next = milestones.firstOrNull { it.day > dayOfRecovery }
        return if (next != null) {
            val daysUntil = next.day - dayOfRecovery
            when (daysUntil) {
                1 -> "Day $dayOfRecovery of recovery — tomorrow is ${next.description}!"
                else -> "Day $dayOfRecovery of recovery — only $daysUntil days until ${next.description}!"
            }
        } else {
            "Day $dayOfRecovery of recovery — incredible progress! You've reached all major milestones."
        }
    }
}
