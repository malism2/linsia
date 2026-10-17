package com.malism.linsia.vocabbook.presentation.ui

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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.malism.linsia.vocabbook.domain.VocabWord
import com.malism.linsia.vocabbook.presentation.VocabBookViewModel
import com.malism.linsia.vocabbook.presentation.root.VocabBookRootComponent
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun VocabBookRootContent(
    component: VocabBookRootComponent,
    onClose: () -> Unit = {},
    viewModel: VocabBookViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()
    Scaffold(
        topBar = {
            VocabBookTitleBar(title = "生词本", onBack = onClose)
        },
    ) { padding ->
        when {
            state.loading -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("加载中…", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            state.words.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "暂无生词\n在阅读中点击英文单词即可加入",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentPadding = PaddingValues(vertical = 8.dp),
                ) {
                    items(state.words, key = { it.wordLower }) { word ->
                        VocabWordRow(
                            word = word,
                            onRemove = { viewModel.remove(word.wordLower) },
                        )
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

@Composable
private fun VocabWordRow(
    word: VocabWord,
    onRemove: () -> Unit,
) {
    val firstLine = word.translation.lineSequence().firstOrNull().orEmpty()
    ListItem(
        headlineContent = {
            Text(word.word, fontWeight = FontWeight.SemiBold)
        },
        supportingContent = {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                word.phonetic?.takeIf { it.isNotBlank() }?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (firstLine.isNotBlank()) {
                    Text(
                        text = firstLine,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                word.sourceTitle?.takeIf { it.isNotBlank() }?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        },
        trailingContent = {
            IconButton(onClick = onRemove) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = "删除",
                )
            }
        },
        modifier = Modifier.fillMaxWidth(),
    )
}
