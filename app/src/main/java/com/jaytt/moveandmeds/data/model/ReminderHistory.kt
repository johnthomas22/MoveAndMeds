package com.jaytt.moveandmeds.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reminder_history")
data class ReminderHistory(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val itemType: String,    // "medicine" or "exercise"
    val itemId: Int,
    val itemName: String,
    val scheduledTime: Long, // epoch ms of when it was scheduled
    val firedTime: Long,     // epoch ms of when notification actually fired
    val status: String       // "fired" or "missed"
)
