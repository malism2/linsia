package com.malism.linsia.typeword.presentation.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.malism.linsia.typeword.domain.ChapterStats
import kotlin.math.roundToInt

@Composable
fun SummaryScreen(
    stats: ChapterStats,
    onRetry: () -> Unit,
    onDictation: () -> Unit,
) {
    val minutes = stats.elapsedMs / 1000 / 60
    val seconds = (stats.elapsedMs / 1000) % 60
    val accuracy = (stats.accuracy * 100).roundToInt()
    val wpm = stats.wpm.roundToInt()
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("本章完成", style = MaterialTheme.typography.headlineSmall)
        Text("单词 ${stats.wordCount} · 跳过 ${stats.skipped}")
        Text("用时 ${minutes} 分 ${seconds} 秒")
        Text("正确率 $accuracy%")
        Text("速度 $wpm WPM")
        Button(onClick = onRetry, modifier = Modifier.fillMaxWidth().padding(top = 24.dp)) {
            Text("再练一遍")
        }
        OutlinedButton(onClick = onDictation, modifier = Modifier.fillMaxWidth()) {
            Text("默写本章")
        }
    }
}
