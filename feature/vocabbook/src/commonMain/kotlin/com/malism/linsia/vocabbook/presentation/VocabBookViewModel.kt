package com.malism.linsia.vocabbook.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.malism.linsia.vocabbook.data.VocabBookRepository
import com.malism.linsia.vocabbook.domain.VocabWord
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class VocabBookUiState(
    val words: List<VocabWord> = emptyList(),
    val loading: Boolean = true,
)

class VocabBookViewModel(
    private val repository: VocabBookRepository,
) : ViewModel() {
    val state: StateFlow<VocabBookUiState> = repository.observeAll()
        .map { VocabBookUiState(words = it, loading = false) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = VocabBookUiState(),
        )

    fun remove(wordLower: String) {
        viewModelScope.launch {
            repository.remove(wordLower)
        }
    }
}
