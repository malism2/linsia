package com.malism.linsia.learnword.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.malism.linsia.learnword.data.catalog.CatalogRepository
import com.malism.linsia.learnword.data.study.StudyRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class HomeViewModel(
    private val catalog: CatalogRepository,
    private val study: StudyRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(StudyRepository.HomeState("记单词", "", false, false, false, 0, 20, true))
    val state: StateFlow<StudyRepository.HomeState> = _state

    init {
        viewModelScope.launch {
            runCatching {
                catalog.ensureSeeded()
                catalog.checkRemoteUpdate()
            }
            refresh()
        }
        viewModelScope.launch {
            catalog.observeBooks().collect { refresh(showLoading = false) }
        }
    }

    fun refresh(showLoading: Boolean = true) {
        viewModelScope.launch {
            if (showLoading) _state.update { it.copy(loading = true, error = null) }
            runCatching { study.homeSnapshot() }
                .onSuccess { _state.value = it }
                .onFailure { e -> _state.update { it.copy(loading = false, error = e.message) } }
        }
    }

    fun prepareStudy(onReady: (ok: Boolean, empty: Boolean) -> Unit) {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            val imported = study.ensureActiveBookImported()
            if (imported.isFailure) {
                _state.update { it.copy(loading = false, error = imported.exceptionOrNull()?.message ?: "导入失败") }
                onReady(false, false)
                return@launch
            }
            val card = study.startOrContinue()
            _state.update { it.copy(loading = false) }
            if (card.isFailure) {
                _state.update { it.copy(error = card.exceptionOrNull()?.message) }
                onReady(false, false)
            } else {
                onReady(true, card.getOrNull() == null)
            }
        }
    }
}
