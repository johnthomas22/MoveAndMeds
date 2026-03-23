package com.jaytt.moveandmeds.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "exercises")
data class Exercise(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val sets: String,
    val reps: String,
    val notes: String = "",
    val isEnabled: Boolean = true,
    val daysOfWeek: String = "1,2,3,4,5,6,7",
    val reminderType: String = "timed",
    val intervalMinutes: Int = 60,
    val intervalStartHour: Int = 8,
    val intervalEndHour: Int = 22
)
