package com.jaytt.moveandmeds.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.jaytt.moveandmeds.data.model.ReminderHistory
import kotlinx.coroutines.flow.Flow

@Dao
interface HistoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(history: ReminderHistory)

    @Query("SELECT * FROM reminder_history WHERE itemType = :itemType AND itemId = :itemId ORDER BY scheduledTime DESC")
    fun getHistoryForItem(itemType: String, itemId: Int): Flow<List<ReminderHistory>>

    @Query("SELECT * FROM reminder_history ORDER BY scheduledTime DESC")
    fun getAllHistory(): Flow<List<ReminderHistory>>

    @Query("DELETE FROM reminder_history WHERE scheduledTime < :beforeTime")
    suspend fun deleteOldHistory(beforeTime: Long)

    @Query("SELECT * FROM reminder_history WHERE itemType = :itemType AND itemId = :itemId AND scheduledTime >= :windowStart AND scheduledTime <= :windowEnd AND status = 'fired'")
    suspend fun getFiredInWindow(itemType: String, itemId: Int, windowStart: Long, windowEnd: Long): List<ReminderHistory>
}
