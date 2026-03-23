package com.jaytt.moveandmeds.data.db

import androidx.room.*
import com.jaytt.moveandmeds.data.model.ExerciseSettings
import kotlinx.coroutines.flow.Flow

@Dao
interface ExerciseSettingsDao {
    @Query("SELECT * FROM exercise_settings WHERE id = 1")
    fun getSettings(): Flow<ExerciseSettings?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSettings(settings: ExerciseSettings)
}
