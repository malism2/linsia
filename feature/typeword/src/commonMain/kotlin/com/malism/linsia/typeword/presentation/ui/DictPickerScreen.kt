package com.malism.linsia.typeword.presentation.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.malism.linsia.typeword.presentation.TypewordViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DictPickerScreen(
    onConfirm: () -> Unit,
    viewModel: TypewordViewModel,
) {
    val state by viewModel.picker.collectAsState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("选择词库", style = MaterialTheme.typography.titleMedium)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            state.dicts.forEach { dict ->
                FilterChip(
                    selected = dict.id == state.selectedId,
                    onClick = { viewModel.selectDict(dict.id) },
                    label = { Text("${dict.title} · ${dict.words.size} 词") },
                )
            }
        }
        val selected = state.dicts.firstOrNull { it.id == state.selectedId }
        if (selected != null && selected.chapterCount > 0) {
            Text("选择章节（每章 20 词）", style = MaterialTheme.typography.titleMedium)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                repeat(selected.chapterCount) { index ->
                    FilterChip(
                        selected = index == state.chapterIndex,
                        onClick = { viewModel.selectChapter(index) },
                        label = { Text("${index + 1}") },
                    )
                }
            }
        }
        Button(onClick = onConfirm, modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
            Text("开始练习")
        }
    }
}
