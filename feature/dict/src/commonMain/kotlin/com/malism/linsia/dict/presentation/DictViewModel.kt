package com.malism.linsia.dict.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.malism.linsia.dict.data.DictRepository
import com.malism.linsia.dict.domain.DictDefaults
import com.malism.linsia.dict.domain.DictEntry
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class DictPhase {
    Checking,
    NeedDownload,
    Downloading,
    Ready,
    Error,
}

data class DictUiState(
    val phase: DictPhase = DictPhase.Checking,
    val query: String = "",
    val results: List<DictEntry> = emptyList(),
    val selected: DictEntry? = null,
    val downloadReceived: Long = 0L,
    val downloadTotal: Long? = null,
    val errorMessage: String? = null,
    val entryCountHint: Int? = null,
)

class DictViewModel(
    private val repository: DictRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(DictUiState())
    val state: StateFlow<DictUiState> = _state.asStateFlow()

    private var searchJob: Job? = null

    init {
        bootstrap()
    }

    fun bootstrap() {
        viewModelScope.launch {
            if (repository.isReady()) {
                _state.update { it.copy(phase = DictPhase.Ready, errorMessage = null) }
                repository.ensureReady().onFailure { e ->
                    // Already usable offline; ignore silent update failure
                    co.touchlab.kermit.Logger.withTag("Dict").w { "Update check: ${e.message}" }
                }
            } else {
                _state.update { it.copy(phase = DictPhase.NeedDownload, errorMessage = null) }
            }
        }
    }

    fun startDownload() {
        viewModelScope.launch {
            _state.update {
                it.copy(
                    phase = DictPhase.Downloading,
                    downloadReceived = 0L,
                    downloadTotal = null,
                    errorMessage = null,
                )
            }
            repository.ensureReady(
                onProgress = { received, total ->
                    _state.update { s ->
                        s.copy(downloadReceived = received, downloadTotal = total)
                    }
                },
                forceUpdate = false,
            ).onSuccess {
                _state.update { it.copy(phase = DictPhase.Ready, errorMessage = null) }
            }.onFailure { e ->
                _state.update {
                    it.copy(
                        phase = DictPhase.Error,
                        errorMessage = e.message ?: "下载失败",
                    )
                }
            }
        }
    }

    fun retryDownload() = startDownload()

    fun onQueryChange(value: String) {
        _state.update { it.copy(query = value, selected = null) }
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(DictDefaults.SearchDebounceMs)
            val results = repository.search(value)
            _state.update { it.copy(results = results) }
        }
    }

    fun selectEntry(entry: DictEntry) {
        _state.update { it.copy(selected = entry) }
    }

    fun clearSelection() {
        _state.update { it.copy(selected = null) }
    }
}
