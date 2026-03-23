package com.jaytt.moveandmeds.data.repository

import com.jaytt.moveandmeds.data.db.MovementSettingsDao
import com.jaytt.moveandmeds.data.model.MovementSettings
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MovementRepository @Inject constructor(private val dao: MovementSettingsDao) {
    fun getSettings(): Flow<MovementSettings?> = dao.getSettings()
    suspend fun saveSettings(settings: MovementSettings) = dao.upsertSettings(settings)
}
