package com.malism.linsia.learnword.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.malism.linsia.learnword.data.catalog.CatalogRepository
import com.malism.linsia.learnword.data.db.WordBookMetaEntity
import com.malism.linsia.learnword.data.study.StudyRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class BookPickerViewModel(
    catalog: CatalogRepository,
    private val study: StudyRepository,
) : ViewModel() {
    private val busyId = MutableStateFlow<String?>(null)
    private val error = MutableStateFlow<String?>(null)

    val books: StateFlow<List<WordBookMetaEntity>> = catalog.observeBooks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val ui: StateFlow<Pair<String?, String?>> = combine(busyId, error) { a, b -> a to b }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null to null)

    fun select(bookId: String, onDone: () -> Unit) {
        viewModelScope.launch {
            busyId.value = bookId
            error.value = null
            val result = study.importBook(bookId)
            if (result.isSuccess) {
                study.updateSettings(activeBookId = bookId)
                busyId.value = null
                onDone()
            } else {
                error.value = result.exceptionOrNull()?.message ?: "下载失败"
                busyId.value = null
            }
        }
    }
}
