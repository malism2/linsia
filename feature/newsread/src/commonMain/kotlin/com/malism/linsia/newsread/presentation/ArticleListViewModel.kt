package com.malism.linsia.newsread.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.malism.linsia.newsread.data.NewsReadRepository
import com.malism.linsia.newsread.domain.NewsArticle
import com.malism.linsia.newsread.domain.NewsCategory
import com.malism.linsia.newsread.domain.NewsReadDefaults
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ArticleListUiState(
    val categories: List<NewsCategory> = NewsReadDefaults.tabs,
    val selectedId: String = NewsReadDefaults.categories.first().id,
    val articles: List<NewsArticle> = emptyList(),
    val loading: Boolean = false,
    val error: String? = null,
    val isFavorites: Boolean = false,
)

class ArticleListViewModel(
    private val repository: NewsReadRepository,
) : ViewModel() {
    private val selectedId = MutableStateFlow(NewsReadDefaults.categories.first().id)
    private val loading = MutableStateFlow(false)
    private val error = MutableStateFlow<String?>(null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val state: StateFlow<ArticleListUiState> = combine(
        selectedId,
        selectedId.flatMapLatest { repository.observeArticles(it) },
        loading,
        error,
    ) { id, articles, isLoading, err ->
        ArticleListUiState(
            selectedId = id,
            articles = articles,
            loading = isLoading,
            error = err,
            isFavorites = NewsReadDefaults.isFavorites(id),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ArticleListUiState())

    init {
        refresh()
    }

    fun selectCategory(id: String) {
        selectedId.value = id
        if (!NewsReadDefaults.isFavorites(id)) {
            refresh()
        }
    }

    fun toggleFavorite(id: String) {
        viewModelScope.launch {
            repository.toggleFavorite(id)
        }
    }

    fun refresh() {
        val category = NewsReadRepository.categoryById(selectedId.value) ?: return
        viewModelScope.launch {
            loading.value = true
            error.value = null
            runCatching { repository.refresh(category) }
                .onFailure { e -> error.update { e.message ?: "加载失败" } }
            loading.value = false
        }
    }
}
