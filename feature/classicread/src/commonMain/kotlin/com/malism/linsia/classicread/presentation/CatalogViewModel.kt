package com.malism.linsia.classicread.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.malism.linsia.classicread.data.ClassicReadRepository
import com.malism.linsia.classicread.domain.ClassicBook
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class CatalogUiState(
    val books: List<ClassicBook> = emptyList(),
    val loading: Boolean = true,
    val error: String? = null,
)

class CatalogViewModel(
    private val repository: ClassicReadRepository,
) : ViewModel() {
    private val loading = MutableStateFlow(true)
    private val error = MutableStateFlow<String?>(null)

    val state: StateFlow<CatalogUiState> = combine(
        repository.observeCatalog(),
        loading,
        error,
    ) { books, isLoading, err ->
        CatalogUiState(
            books = books,
            loading = isLoading && books.isEmpty(),
            error = err,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CatalogUiState())

    init {
        viewModelScope.launch {
            loading.value = true
            runCatching {
                repository.ensureSeeded()
                repository.checkRemoteUpdate()
            }.onFailure { e -> error.value = e.message ?: "书目加载失败" }
            loading.value = false
        }
    }
}
