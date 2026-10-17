package com.malism.linsia.newsread.presentation.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewsReadTitleBar(
    title: String,
    onBack: () -> Unit,
    onRefresh: (() -> Unit)? = null,
    refreshEnabled: Boolean = true,
    onToggleFavorite: (() -> Unit)? = null,
    favorited: Boolean = false,
    favoriteEnabled: Boolean = true,
) {
    TopAppBar(
        title = { Text(title) },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "返回",
                )
            }
        },
        actions = {
            if (onToggleFavorite != null) {
                IconButton(onClick = onToggleFavorite, enabled = favoriteEnabled) {
                    Icon(
                        imageVector = Icons.Filled.Star,
                        contentDescription = if (favorited) "取消收藏" else "收藏",
                        tint = if (favorited) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                }
            }
            if (onRefresh != null) {
                IconButton(onClick = onRefresh, enabled = refreshEnabled) {
                    Icon(
                        imageVector = Icons.Filled.Refresh,
                        contentDescription = "刷新",
                    )
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
        ),
    )
}
