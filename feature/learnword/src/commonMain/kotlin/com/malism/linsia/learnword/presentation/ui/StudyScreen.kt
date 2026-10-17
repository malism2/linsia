package com.malism.linsia.learnword.presentation.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.malism.linsia.learnword.domain.StudyCardContent
import com.malism.linsia.learnword.domain.StudyExample
import com.malism.linsia.learnword.domain.SummaryStats
import com.malism.linsia.learnword.presentation.StudyViewModel
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun StudyScreen(
    onFinished: (SummaryStats) -> Unit,
    vm: StudyViewModel = koinViewModel(),
) {
    val card by vm.card.collectAsState()
    val back by vm.isBack.collectAsState()
    val busy by vm.busy.collectAsState()

    DisposableEffect(Unit) {
        vm.startTicker()
        onDispose { vm.stopTicker() }
    }
    LaunchedEffect(card?.wordId, back) {
        if (!back) vm.playCurrent(card)
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Card(modifier = Modifier.fillMaxWidth().weight(1f)) {
            Box(Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(24.dp),
                    verticalArrangement = if (back) Arrangement.Top else Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    val content = card
                    if (content == null) {
                        Text("今日无任务")
                    } else if (!back) {
                        Text(content.headWord, fontSize = 40.sp, style = MaterialTheme.typography.displaySmall)
                        PhoneticRow(content, onPlay = { vm.playCurrent(content) })
                        content.examples.firstOrNull()?.let { ex ->
                            Text(
                                quoteEnglish(ex.english),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 16.dp),
                            )
                        }
                    } else {
                        Text(content.headWord, style = MaterialTheme.typography.headlineMedium)
                        PhoneticRow(content, onPlay = { vm.playCurrent(content) })
                        content.transLines.forEach {
                            Text(it, style = MaterialTheme.typography.titleMedium)
                        }
                        ExampleBlock(content.examples)
                    }
                }
                val total = card?.total ?: 0
                if (total > 0) {
                    Text(
                        "${card!!.index}/$total",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.align(Alignment.BottomEnd).padding(12.dp),
                    )
                }
            }
        }
        if (card == null) {
            Text("可返回首页或更换词书后再试")
        } else if (!back) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = vm::onUnknown,
                    enabled = !busy,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) { Text("不认识") }
                Button(
                    onClick = { vm.onKnow(onFinished) },
                    enabled = !busy,
                    modifier = Modifier.weight(1f),
                ) { Text("认识") }
            }
        } else {
            Button(
                onClick = { vm.onRemembered(onFinished) },
                enabled = !busy,
                modifier = Modifier.fillMaxWidth(),
            ) { Text("记住了，下一个") }
        }
    }
}

@Composable
private fun PhoneticRow(
    content: StudyCardContent,
    onPlay: () -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (content.phonetic.isNotBlank()) {
            Text(content.phonetic, style = MaterialTheme.typography.titleMedium)
        }
        IconButton(onClick = onPlay) {
            Icon(
                imageVector = VolumeUpIcon,
                contentDescription = "播放发音",
            )
        }
    }
}

@Composable
private fun ExampleBlock(examples: List<StudyExample>) {
    if (examples.isEmpty()) return
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.Start,
    ) {
        Text(
            "例句",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.secondary,
        )
        examples.forEach { ex ->
            Text(quoteEnglish(ex.english), style = MaterialTheme.typography.bodyLarge)
            if (ex.chinese.isNotBlank()) {
                Text(
                    ex.chinese,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private fun quoteEnglish(text: String): String {
    val inner = text.trim().trim('“', '”', '"', '\'')
    return "“$inner”"
}
