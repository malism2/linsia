package com.malism.linsia.newsread.presentation.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.arkivanov.decompose.extensions.compose.stack.Children
import com.arkivanov.decompose.extensions.compose.stack.animation.slide
import com.arkivanov.decompose.extensions.compose.stack.animation.stackAnimation
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.malism.linsia.newsread.presentation.ArticleListViewModel
import com.malism.linsia.newsread.presentation.ArticleReaderUiState
import com.malism.linsia.newsread.presentation.ArticleReaderViewModel
import com.malism.linsia.newsread.presentation.root.NewsReadRootComponent
import kotlinx.coroutines.flow.MutableStateFlow
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun NewsReadRootContent(
    component: NewsReadRootComponent,
    onClose: () -> Unit = {},
) {
    val stack by component.stack.subscribeAsState()
    val active = stack.active.instance
    val listViewModel: ArticleListViewModel = koinViewModel()
    val listState by listViewModel.state.collectAsState()
    val articleId = (active as? NewsReadRootComponent.Child.Article)?.id
    val readerViewModel = rememberReaderViewModel(articleId)
    val readerState by rememberReaderState(readerViewModel)
    val isList = active is NewsReadRootComponent.Child.List
    val title = when (active) {
        NewsReadRootComponent.Child.List -> "时文阅读"
        is NewsReadRootComponent.Child.Article -> "阅读"
    }

    Scaffold(
        topBar = {
            NewsReadTitleBar(
                title = title,
                onBack = {
                    when (active) {
                        NewsReadRootComponent.Child.List -> onClose()
                        is NewsReadRootComponent.Child.Article -> component.back()
                    }
                },
                onRefresh = if (isList && !listState.isFavorites) listViewModel::refresh else null,
                refreshEnabled = !listState.loading,
                onToggleFavorite = readerViewModel?.let { it::toggleFavorite },
                favorited = readerState.article?.favorited == true,
                favoriteEnabled = readerState.article != null,
            )
        },
    ) { padding ->
        Children(
            stack = component.stack,
            modifier = Modifier.padding(padding),
            animation = stackAnimation(slide()),
        ) { child ->
            when (val instance = child.instance) {
                NewsReadRootComponent.Child.List -> ArticleListScreen(
                    onOpenArticle = component::openArticle,
                    viewModel = listViewModel,
                )
                is NewsReadRootComponent.Child.Article -> ArticleReaderScreen(
                    articleId = instance.id,
                    viewModel = checkNotNull(rememberReaderViewModel(instance.id)),
                )
            }
        }
    }
}

@Composable
private fun rememberReaderViewModel(articleId: String?): ArticleReaderViewModel? {
    if (articleId == null) return null
    return koinViewModel(key = articleId, parameters = { parametersOf(articleId) })
}

@Composable
private fun rememberReaderState(viewModel: ArticleReaderViewModel?): State<ArticleReaderUiState> {
    val empty = remember { MutableStateFlow(ArticleReaderUiState(loading = false)) }
    return (viewModel?.state ?: empty).collectAsState()
}
