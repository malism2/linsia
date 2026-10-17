package com.malism.linsia.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.malism.linsia.home.quote.DailyQuote
import com.malism.linsia.home.quote.DailyQuoteRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HomeUiState(
    val visibleFeatures: List<AppFeature> = emptyList(),
    val managedFeatures: List<ManagedFeature> = emptyList(),
    val quote: DailyQuote? = null,
    val quoteLoading: Boolean = true,
)

class HomeViewModel(
    private val repository: HomeLayoutRepository,
    private val quotes: DailyQuoteRepository,
) : ViewModel() {
    private val catalog = AppFeatureCatalog.all
    private val _state = MutableStateFlow(repository.load(catalog).toUiState())
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val quote = runCatching { quotes.load() }.getOrNull()
            _state.update { it.copy(quote = quote, quoteLoading = false) }
        }
    }

    fun toggleVisible(id: String) {
        val hidden = currentLayout().hiddenIds.toMutableSet()
        if (!hidden.add(id)) hidden.remove(id)
        persist(currentLayout().copy(hiddenIds = hidden.toList()))
    }

    fun moveUp(id: String) {
        val ids = currentLayout().orderedIds.toMutableList()
        val index = ids.indexOf(id)
        if (index <= 0) return
        ids.swap(index, index - 1)
        persist(currentLayout().copy(orderedIds = ids))
    }

    fun moveDown(id: String) {
        val ids = currentLayout().orderedIds.toMutableList()
        val index = ids.indexOf(id)
        if (index < 0 || index >= ids.lastIndex) return
        ids.swap(index, index + 1)
        persist(currentLayout().copy(orderedIds = ids))
    }

    fun reset() {
        persist(resolveHomeLayout(catalog, null))
    }

    private fun persist(layout: HomeLayout) {
        val resolved = resolveHomeLayout(catalog, layout)
        repository.save(resolved)
        _state.update {
            it.copy(
                visibleFeatures = resolved.visibleFeatures(catalog),
                managedFeatures = resolved.managedFeatures(catalog),
            )
        }
    }

    private fun currentLayout(): HomeLayout {
        val managed = _state.value.managedFeatures
        return HomeLayout(
            orderedIds = managed.map { it.feature.id },
            hiddenIds = managed.filterNot { it.visible }.map { it.feature.id },
        )
    }

    private fun HomeLayout.toUiState() = HomeUiState(
        visibleFeatures = visibleFeatures(catalog),
        managedFeatures = managedFeatures(catalog),
    )
}

private fun MutableList<String>.swap(i: Int, j: Int) {
    val tmp = this[i]
    this[i] = this[j]
    this[j] = tmp
}
