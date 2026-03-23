package com.jaytt.moveandmeds.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "exercise_settings")
data class ExerciseSettings(
    @PrimaryKey val id: Int = 1,
    val intervalMinutes: Int = 120,
    val isEnabled: Boolean = false,
    val startHour: Int = 8,
    val endHour: Int = 22
)
