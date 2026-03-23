package com.jaytt.moveandmeds.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.room.Room
import com.jaytt.moveandmeds.data.db.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = Room.databaseBuilder(context, AppDatabase::class.java, "moveandmeds.db")
                    .addMigrations(
                        AppDatabase.MIGRATION_1_2,
                        AppDatabase.MIGRATION_2_3,
                        AppDatabase.MIGRATION_3_4,
                        AppDatabase.MIGRATION_4_5,
                        AppDatabase.MIGRATION_5_6,
                        AppDatabase.MIGRATION_6_7,
                        AppDatabase.MIGRATION_7_8
                    )
                    .build()
                val medicines = db.medicineDao().getAllMedicinesWithTimes().first()
                val movementSettings = db.movementSettingsDao().getSettings().first()
                val exercises = db.exerciseDao().getAllExercisesWithTimes().first()
                val scheduler = AlarmScheduler(context)
                scheduler.scheduleAllAlarms(medicines, movementSettings, exercises)
                db.close()
            } finally {
                pendingResult.finish()
            }
        }
    }
}
