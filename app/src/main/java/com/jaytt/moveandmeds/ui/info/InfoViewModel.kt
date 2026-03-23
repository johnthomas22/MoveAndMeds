package com.jaytt.moveandmeds.ui.info

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jaytt.moveandmeds.data.model.ContactInfo
import com.jaytt.moveandmeds.data.repository.ContactRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class InfoViewModel @Inject constructor(
    private val repository: ContactRepository
) : ViewModel() {

    val contacts: StateFlow<List<ContactInfo>> = repository.getAllContacts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun saveContact(id: Int?, description: String, value: String, type: String) {
        viewModelScope.launch {
            if (id == null) {
                repository.insert(ContactInfo(description = description, value = value, type = type))
            } else {
                repository.update(ContactInfo(id = id, description = description, value = value, type = type))
            }
        }
    }

    fun deleteContact(contact: ContactInfo) {
        viewModelScope.launch {
            repository.delete(contact)
        }
    }
}
