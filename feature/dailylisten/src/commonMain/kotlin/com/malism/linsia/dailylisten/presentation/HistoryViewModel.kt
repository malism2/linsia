package com.malism.linsia.dailylisten.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.malism.linsia.dailylisten.data.DailyListenRepository
import com.malism.linsia.dailylisten.domain.HistoryItem
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class HistoryUiState(
    val items: List<HistoryItem> = emptyList(),
)

class HistoryViewModel(
    repository: DailyListenRepository,
) : ViewModel() {
    val state: StateFlow<HistoryUiState> = repository.observeHistory()
        .map { HistoryUiState(items = it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HistoryUiState())
}
