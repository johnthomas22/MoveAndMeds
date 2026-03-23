package com.jaytt.moveandmeds.data.db

import androidx.room.*
import com.jaytt.moveandmeds.data.model.MovementSettings
import kotlinx.coroutines.flow.Flow

@Dao
interface MovementSettingsDao {
    @Query("SELECT * FROM movement_settings WHERE id = 1")
    fun getSettings(): Flow<MovementSettings?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSettings(settings: MovementSettings)
}
