package com.jaytt.moveandmeds.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jaytt.moveandmeds.data.model.ReminderHistory
import com.jaytt.moveandmeds.data.repository.HistoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val repository: HistoryRepository
) : ViewModel() {

    val allHistory: StateFlow<List<ReminderHistory>> = repository.getAllHistory()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun getHistoryForItem(itemType: String, itemId: Int): StateFlow<List<ReminderHistory>> =
        repository.getHistoryForItem(itemType, itemId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun pruneOldHistory() {
        viewModelScope.launch {
            val cutoff = System.currentTimeMillis() - 90L * 24 * 60 * 60 * 1000
            repository.deleteOldHistory(cutoff)
        }
    }
}
