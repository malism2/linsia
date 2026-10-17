package com.malism.linsia.learnword.presentation.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.malism.linsia.learnword.presentation.HomeViewModel
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun HomeScreen(
    onStudy: () -> Unit,
    vm: HomeViewModel = koinViewModel(),
) {
    val state by vm.state.collectAsState()
    LaunchedEffect(Unit) {
        vm.refresh(showLoading = false)
    }
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("学如潮汐 · 积微成博", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.secondary)
        Text(state.bookTitle, style = MaterialTheme.typography.titleLarge)
        Text("连续打卡 ${state.streak} 天")
        HomeProgressSection(
            todayDone = state.todayDone,
            todayTotal = state.todayTotal,
            bookSeen = state.bookSeen,
            bookTotal = state.bookTotal,
            tomorrowReview = state.tomorrowReview,
        )
        if (state.loading) {
            CircularProgressIndicator()
        }
        state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        Button(
            onClick = {
                vm.prepareStudy { ok, empty ->
                    if (ok && !empty) onStudy()
                    else vm.refresh()
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !state.loading,
        ) {
            Text(if (state.canContinue) "继续今日学习" else "开始学习")
        }
    }
}

@Composable
private fun HomeProgressSection(
    todayDone: Int,
    todayTotal: Int,
    bookSeen: Int,
    bookTotal: Int,
    tomorrowReview: Int,
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        ProgressLine(label = "今日进度", done = todayDone, total = todayTotal)
        ProgressLine(label = "词书进度", done = bookSeen, total = bookTotal)
        Text(
            "明日预计复习 $tomorrowReview 词",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ProgressLine(label: String, done: Int, total: Int) {
    val fraction = if (total <= 0) 0f else (done.toFloat() / total).coerceIn(0f, 1f)
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.titleSmall)
            Text(
                if (total <= 0) "—" else "$done/$total",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        LinearProgressIndicator(
            progress = { fraction },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
