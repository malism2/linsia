package com.malism.linsia.newsread.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.malism.linsia.newsread.data.NewsReadRepository
import com.malism.linsia.newsread.domain.NewsArticle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ArticleReaderUiState(
    val article: NewsArticle? = null,
    val loading: Boolean = true,
    val error: String? = null,
)

class ArticleReaderViewModel(
    private val articleId: String,
    private val repository: NewsReadRepository,
) : ViewModel() {
    private val loading = MutableStateFlow(true)
    private val error = MutableStateFlow<String?>(null)

    val state: StateFlow<ArticleReaderUiState> = combine(
        repository.observeArticle(articleId),
        loading,
        error,
    ) { article, isLoading, err ->
        ArticleReaderUiState(article = article, loading = isLoading && article?.body.isNullOrBlank(), error = err)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ArticleReaderUiState())

    init {
        viewModelScope.launch {
            loading.value = true
            runCatching { repository.ensureBody(articleId) }
                .onFailure { e -> error.value = e.message ?: "正文加载失败" }
            loading.value = false
        }
    }

    fun toggleFavorite() {
        viewModelScope.launch {
            repository.toggleFavorite(articleId)
        }
    }
}
