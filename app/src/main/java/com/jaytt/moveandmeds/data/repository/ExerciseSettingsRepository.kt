package com.jaytt.moveandmeds.data.repository

import com.jaytt.moveandmeds.data.db.ExerciseSettingsDao
import com.jaytt.moveandmeds.data.model.ExerciseSettings
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExerciseSettingsRepository @Inject constructor(private val dao: ExerciseSettingsDao) {
    fun getSettings(): Flow<ExerciseSettings?> = dao.getSettings()
    suspend fun saveSettings(settings: ExerciseSettings) = dao.upsertSettings(settings)
}
