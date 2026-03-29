package com.jaytt.moveandmeds.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.jaytt.moveandmeds.data.model.ContactInfo
import com.jaytt.moveandmeds.data.model.Exercise
import com.jaytt.moveandmeds.data.model.ExerciseSettings
import com.jaytt.moveandmeds.data.model.ExerciseTime
import com.jaytt.moveandmeds.data.model.Medicine
import com.jaytt.moveandmeds.data.model.MedicineTime
import com.jaytt.moveandmeds.data.model.MovementSettings
import com.jaytt.moveandmeds.data.model.ReminderHistory

@Database(
    entities = [
        Medicine::class,
        MedicineTime::class,
        MovementSettings::class,
        Exercise::class,
        ExerciseTime::class,
        ExerciseSettings::class,
        ReminderHistory::class,
        ContactInfo::class
    ],
    version = 9,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun medicineDao(): MedicineDao
    abstract fun movementSettingsDao(): MovementSettingsDao
    abstract fun exerciseDao(): ExerciseDao
    abstract fun exerciseSettingsDao(): ExerciseSettingsDao
    abstract fun historyDao(): HistoryDao
    abstract fun contactDao(): ContactDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """CREATE TABLE IF NOT EXISTS exercises (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        sets TEXT NOT NULL,
                        reps TEXT NOT NULL,
                        notes TEXT NOT NULL DEFAULT '',
                        isEnabled INTEGER NOT NULL DEFAULT 1
                    )"""
                )
                database.execSQL(
                    """CREATE TABLE IF NOT EXISTS exercise_times (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        exerciseId INTEGER NOT NULL,
                        hour INTEGER NOT NULL,
                        minute INTEGER NOT NULL,
                        FOREIGN KEY (exerciseId) REFERENCES exercises(id) ON DELETE CASCADE
                    )"""
                )
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_exercise_times_exerciseId ON exercise_times(exerciseId)"
                )
                database.execSQL(
                    """CREATE TABLE IF NOT EXISTS exercise_settings (
                        id INTEGER PRIMARY KEY NOT NULL DEFAULT 1,
                        intervalMinutes INTEGER NOT NULL DEFAULT 120,
                        isEnabled INTEGER NOT NULL DEFAULT 0,
                        startHour INTEGER NOT NULL DEFAULT 8,
                        endHour INTEGER NOT NULL DEFAULT 22
                    )"""
                )
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "ALTER TABLE medicines ADD COLUMN daysOfWeek TEXT NOT NULL DEFAULT '1,2,3,4,5,6,7'"
                )
                database.execSQL(
                    "ALTER TABLE exercises ADD COLUMN daysOfWeek TEXT NOT NULL DEFAULT '1,2,3,4,5,6,7'"
                )
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """CREATE TABLE IF NOT EXISTS reminder_history (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        itemType TEXT NOT NULL,
                        itemId INTEGER NOT NULL,
                        itemName TEXT NOT NULL,
                        scheduledTime INTEGER NOT NULL,
                        firedTime INTEGER NOT NULL,
                        status TEXT NOT NULL
                    )"""
                )
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS contacts (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, description TEXT NOT NULL, phoneNumber TEXT NOT NULL)"
                )
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("DROP TABLE IF EXISTS contacts")
                database.execSQL(
                    "CREATE TABLE contacts (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, description TEXT NOT NULL, value TEXT NOT NULL, type TEXT NOT NULL DEFAULT 'phone')"
                )
            }
        }

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "ALTER TABLE movement_settings ADD COLUMN stepThreshold INTEGER NOT NULL DEFAULT 50"
                )
            }
        }

        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE exercises ADD COLUMN reminderType TEXT NOT NULL DEFAULT 'timed'")
                database.execSQL("ALTER TABLE exercises ADD COLUMN intervalMinutes INTEGER NOT NULL DEFAULT 60")
                database.execSQL("ALTER TABLE exercises ADD COLUMN intervalStartHour INTEGER NOT NULL DEFAULT 8")
                database.execSQL("ALTER TABLE exercises ADD COLUMN intervalEndHour INTEGER NOT NULL DEFAULT 22")
            }
        }

        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE exercises ADD COLUMN imagePath TEXT")
            }
        }
    }
}
