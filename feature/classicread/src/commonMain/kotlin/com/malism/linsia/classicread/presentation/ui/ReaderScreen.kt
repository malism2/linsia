package com.malism.linsia.classicread.presentation.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.malism.linsia.classicread.presentation.ReaderArgs
import com.malism.linsia.classicread.presentation.ReaderViewModel
import com.malism.linsia.vocabbook.domain.VocabSourceType
import com.malism.linsia.vocabbook.presentation.lookup.SelectableLookupText
import kotlinx.coroutines.flow.distinctUntilChanged
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun ReaderScreen(
    bookId: String,
    viewModel: ReaderViewModel = koinViewModel(
        key = "reader-$bookId",
        parameters = { parametersOf(ReaderArgs(bookId)) },
    ),
) {
    val state by viewModel.state.collectAsState()
    DisposableEffect(viewModel) {
        onDispose { viewModel.persistCurrent() }
    }
    when {
        state.loading -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    val progress = state.downloadProgress
                    if (progress != null) {
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier.padding(top = 16.dp).fillMaxWidth(0.6f),
                        )
                    }
                }
            }
        }
        state.error != null -> {
            Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                Text(state.error.orEmpty(), color = MaterialTheme.colorScheme.error)
            }
        }
        state.paragraphs.isEmpty() -> {
            Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                Text("暂无内容", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        else -> {
            val listState = rememberLazyListState()
            var restored by remember { mutableStateOf(false) }
            LaunchedEffect(state.paragraphs, state.initialScrollIndex) {
                if (!restored) {
                    listState.scrollToItem(
                        state.initialScrollIndex.coerceIn(0, state.paragraphs.lastIndex),
                    )
                    restored = true
                }
            }
            LaunchedEffect(listState) {
                snapshotFlow { listState.firstVisibleItemIndex }
                    .distinctUntilChanged()
                    .collect(viewModel::onVisibleParagraph)
            }
            Column(modifier = Modifier.fillMaxSize()) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 20.dp),
                ) {
                    itemsIndexed(state.paragraphs) { _, paragraph ->
                        SelectableLookupText(
                            text = paragraph,
                            sourceType = VocabSourceType.CLASSIC,
                            sourceTitle = state.book?.title,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(vertical = 8.dp),
                        )
                    }
                }
                if (state.pageCount > 0) {
                    Surface(tonalElevation = 2.dp) {
                        Text(
                            text = "第 ${state.pageIndex + 1} / ${state.pageCount} 页",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }
    }
}
