package com.jaytt.moveandmeds.data.repository

import com.jaytt.moveandmeds.data.db.ContactDao
import com.jaytt.moveandmeds.data.model.ContactInfo
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ContactRepository @Inject constructor(private val dao: ContactDao) {
    fun getAllContacts(): Flow<List<ContactInfo>> = dao.getAllContacts()

    suspend fun insert(contact: ContactInfo) = dao.insert(contact)

    suspend fun update(contact: ContactInfo) = dao.update(contact)

    suspend fun delete(contact: ContactInfo) = dao.delete(contact)
}
