package com.malism.linsia.dict.presentation.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import com.malism.linsia.dict.domain.DictEntry
import com.malism.linsia.dict.presentation.DictPhase
import com.malism.linsia.dict.presentation.DictUiState
import com.malism.linsia.dict.presentation.DictViewModel
import com.malism.linsia.dict.presentation.root.DictRootComponent
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun DictRootContent(
    component: DictRootComponent,
    onClose: () -> Unit = {},
    viewModel: DictViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val title = when {
        state.selected != null -> state.selected!!.word
        else -> "英汉词典"
    }

    Scaffold(
        topBar = {
            DictTitleBar(
                title = title,
                onBack = {
                    when {
                        state.selected != null -> viewModel.clearSelection()
                        else -> onClose()
                    }
                },
            )
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when {
                state.selected != null -> DetailPane(entry = state.selected!!)
                state.phase == DictPhase.Ready -> SearchPane(
                    state = state,
                    onQueryChange = viewModel::onQueryChange,
                    onSelect = viewModel::selectEntry,
                )
                else -> DownloadPane(
                    state = state,
                    onDownload = viewModel::startDownload,
                    onRetry = viewModel::retryDownload,
                )
            }
        }
    }
}

@Composable
private fun DownloadPane(
    state: DictUiState,
    onDownload: () -> Unit,
    onRetry: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "英汉词典",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "首次使用需下载 ECDICT 词库，下载后可离线查询。",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(24.dp))

        when (state.phase) {
            DictPhase.Checking -> CircularProgressIndicator()
            DictPhase.Downloading -> {
                val total = state.downloadTotal
                if (total != null && total > 0L) {
                    LinearProgressIndicator(
                        progress = { (state.downloadReceived.toFloat() / total.toFloat()).coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = formatBytes(state.downloadReceived) + " / " + formatBytes(total),
                        style = MaterialTheme.typography.bodySmall,
                    )
                } else {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = formatBytes(state.downloadReceived),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
            DictPhase.Error -> {
                Text(
                    text = state.errorMessage ?: "下载失败",
                    color = MaterialTheme.colorScheme.error,
                )
                Spacer(Modifier.height(12.dp))
                Button(onClick = onRetry) { Text("重试") }
            }
            DictPhase.NeedDownload -> {
                Button(onClick = onDownload) { Text("下载词典") }
            }
            DictPhase.Ready -> Unit
        }
    }
}

@Composable
private fun SearchPane(
    state: DictUiState,
    onQueryChange: (String) -> Unit,
    onSelect: (DictEntry) -> Unit,
) {
    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        OutlinedTextField(
            value = state.query,
            onValueChange = onQueryChange,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 8.dp),
            singleLine = true,
            placeholder = { Text("输入英文单词") },
        )
        if (state.query.isNotBlank() && state.results.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    "无匹配结果",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 24.dp),
            ) {
                items(state.results, key = { it.wordLower }) { entry ->
                    ResultRow(entry = entry, onClick = { onSelect(entry) })
                }
            }
        }
    }
}

@Composable
private fun ResultRow(
    entry: DictEntry,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
    ) {
        Text(
            text = entry.word,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium,
        )
        val phonetic = entry.phonetic?.takeIf { it.isNotBlank() }
        if (phonetic != null) {
            Text(
                text = phonetic,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = entry.translation.lineSequence().firstOrNull().orEmpty(),
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun DetailPane(entry: DictEntry) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = entry.word,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
        )
        entry.phonetic?.takeIf { it.isNotBlank() }?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        entry.tag?.takeIf { it.isNotBlank() }?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Text(
            text = "释义",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
        )
        entry.translation.lineSequence().forEach { line ->
            if (line.isNotBlank()) {
                Text(text = line, style = MaterialTheme.typography.bodyLarge)
            }
        }
        entry.definition?.takeIf { it.isNotBlank() }?.let { def ->
            Spacer(Modifier.height(8.dp))
            Text(
                text = "英英",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Text(text = def, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

private fun formatBytes(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val kb = bytes / 1024.0
    if (kb < 1024) return "${(kb * 10).toInt() / 10.0} KB"
    val mb = kb / 1024.0
    return "${(mb * 10).toInt() / 10.0} MB"
}
