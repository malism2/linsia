package com.malism.linsia.newsread.presentation.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.malism.linsia.newsread.domain.NewsArticle
import com.malism.linsia.newsread.presentation.ArticleListViewModel
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArticleListScreen(
    onOpenArticle: (String) -> Unit,
    viewModel: ArticleListViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val selectedIndex = state.categories.indexOfFirst { it.id == state.selectedId }.coerceAtLeast(0)

    Column(modifier = Modifier.fillMaxSize()) {
        PrimaryScrollableTabRow(
            selectedTabIndex = selectedIndex,
            divider = {},
        ) {
            state.categories.forEach { category ->
                Tab(
                    selected = category.id == state.selectedId,
                    onClick = { viewModel.selectCategory(category.id) },
                    text = { Text(category.title) },
                )
            }
        }
        HorizontalDivider()
        when {
            state.articles.isEmpty() && state.loading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            state.articles.isEmpty() -> {
                Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                    Text(
                        state.error ?: if (state.isFavorites) {
                            "还没有收藏，阅读时点标题栏星标即可"
                        } else {
                            "暂无文章，点击刷新"
                        },
                        color = if (state.error != null) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 16.dp),
                ) {
                    if (state.error != null) {
                        item {
                            Text(
                                state.error.orEmpty(),
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(16.dp),
                            )
                        }
                    }
                    items(state.articles, key = { it.id }) { article ->
                        ArticleRow(
                            article = article,
                            onClick = { onOpenArticle(article.id) },
                            onToggleFavorite = { viewModel.toggleFavorite(article.id) },
                        )
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

@Composable
private fun ArticleRow(
    article: NewsArticle,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
) {
    ListItem(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        headlineContent = {
            Text(article.title, maxLines = 2, overflow = TextOverflow.Ellipsis)
        },
        supportingContent = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                if (article.summary.isNotBlank()) {
                    Text(article.summary, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
                Text(
                    article.publishedAt,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        trailingContent = {
            IconButton(onClick = onToggleFavorite) {
                Icon(
                    imageVector = Icons.Filled.Star,
                    contentDescription = if (article.favorited) "取消收藏" else "收藏",
                    tint = if (article.favorited) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }
        },
    )
}
