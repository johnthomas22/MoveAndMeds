package com.jaytt.moveandmeds.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "movement_settings")
data class MovementSettings(
    @PrimaryKey val id: Int = 1,
    val intervalMinutes: Int = 60,
    val isEnabled: Boolean = false,
    val startHour: Int = 8,
    val endHour: Int = 22,
    val stepThreshold: Int = 50
)
