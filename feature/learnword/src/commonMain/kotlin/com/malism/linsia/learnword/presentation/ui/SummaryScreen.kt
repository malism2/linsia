package com.malism.linsia.learnword.presentation.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.malism.linsia.learnword.domain.SummaryStats
import kotlin.math.roundToInt

@Composable
fun SummaryScreen(
    stats: SummaryStats,
    onHome: () -> Unit,
) {
    val minutes = stats.elapsedSec / 60
    val seconds = stats.elapsedSec % 60
    val accuracy = (stats.accuracy * 100).roundToInt()
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("今日斩获 ${stats.graduated} 词", style = MaterialTheme.typography.titleLarge)
        Text("用时 ${minutes} 分 ${seconds} 秒")
        Text("正确率 $accuracy%")
        Text("连续打卡 ${stats.streak} 天")
        Button(onClick = onHome, modifier = Modifier.fillMaxWidth().padding(top = 24.dp)) {
            Text("回到首页")
        }
    }
}
