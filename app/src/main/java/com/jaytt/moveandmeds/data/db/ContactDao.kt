package com.jaytt.moveandmeds.data.db

import androidx.room.*
import com.jaytt.moveandmeds.data.model.ContactInfo
import kotlinx.coroutines.flow.Flow

@Dao
interface ContactDao {
    @Query("SELECT * FROM contacts ORDER BY description ASC")
    fun getAllContacts(): Flow<List<ContactInfo>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(contact: ContactInfo)

    @Update
    suspend fun update(contact: ContactInfo)

    @Delete
    suspend fun delete(contact: ContactInfo)
}
