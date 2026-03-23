package com.jaytt.moveandmeds.data.repository

import com.jaytt.moveandmeds.data.db.HistoryDao
import com.jaytt.moveandmeds.data.model.ReminderHistory
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HistoryRepository @Inject constructor(private val dao: HistoryDao) {
    suspend fun insert(history: ReminderHistory) = dao.insert(history)

    fun getHistoryForItem(itemType: String, itemId: Int): Flow<List<ReminderHistory>> =
        dao.getHistoryForItem(itemType, itemId)

    fun getAllHistory(): Flow<List<ReminderHistory>> = dao.getAllHistory()

    suspend fun deleteOldHistory(beforeTime: Long) = dao.deleteOldHistory(beforeTime)

    suspend fun getFiredInWindow(itemType: String, itemId: Int, windowStart: Long, windowEnd: Long): List<ReminderHistory> =
        dao.getFiredInWindow(itemType, itemId, windowStart, windowEnd)
}
