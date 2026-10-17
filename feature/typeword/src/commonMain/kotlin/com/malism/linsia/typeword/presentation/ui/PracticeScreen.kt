package com.malism.linsia.typeword.presentation.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.malism.linsia.typeword.domain.ChapterStats
import com.malism.linsia.typeword.domain.TypingSnapshot
import com.malism.linsia.typeword.presentation.TypewordViewModel
import kotlin.math.roundToInt

@Composable
fun PracticeScreen(
    onOpenDicts: () -> Unit,
    onChapterFinished: (ChapterStats) -> Unit,
    viewModel: TypewordViewModel,
) {
    val state by viewModel.practice.collectAsState()
    val snap = state.snapshot
    val focusRequester = remember { FocusRequester() }
    var field by remember { mutableStateOf(TextFieldValue()) }

    LaunchedEffect(state.pendingStats) {
        val stats = state.pendingStats ?: return@LaunchedEffect
        viewModel.consumePendingStats()
        onChapterFinished(stats)
    }

    LaunchedEffect(state.dictId, state.chapterIndex, state.dictation, snap?.wordIndex) {
        focusRequester.requestFocus()
        field = TextFieldValue()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
            ) { focusRequester.requestFocus() }
            .padding(horizontal = 24.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (state.loading || snap == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("加载词库…")
            }
            return@Column
        }

        StatsRow(snap)
        val progress = if (snap.wordCount == 0) 0f else {
            snap.wordIndex.toFloat() / snap.wordCount
        }
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 8.dp),
        )
        Box(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            BasicTextField(
                value = field,
                onValueChange = { next ->
                    val added = if (next.text.length >= field.text.length) {
                        next.text.substring(field.text.length)
                    } else {
                        ""
                    }
                    added.forEach { viewModel.onChar(it) }
                    field = TextFieldValue(text = "", selection = TextRange(0))
                },
                cursorBrush = SolidColor(Color.Transparent),
                modifier = Modifier
                    .matchParentSize()
                    .focusRequester(focusRequester)
                    .alpha(0f),
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                WordLetters(snap, onPlay = viewModel::playCurrent)
                val phone = if (state.ukAccent) snap.word?.ukphone else snap.word?.usphone
                if (state.showPhonetic && !phone.isNullOrBlank()) {
                    Text(
                        text = "/$phone/",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 16.dp),
                    )
                }
                Text(
                    text = snap.translation,
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(top = 12.dp),
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = onOpenDicts) {
                Text("${state.dictTitle} · 第 ${state.chapterIndex + 1}/${state.chapterCount} 章")
            }
            TextButton(onClick = viewModel::skip, enabled = !snap.finished) {
                Text("跳过")
            }
        }
    }
}

@Composable
private fun StatsRow(snap: TypingSnapshot) {
    val minutes = snap.elapsedMs / 1000 / 60
    val seconds = (snap.elapsedMs / 1000) % 60
    val accuracy = (snap.accuracy * 100).roundToInt()
    val wpm = snap.wpm.roundToInt()
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        StatCell("进度", "${snap.wordIndex.coerceAtMost(snap.wordCount)}/${snap.wordCount}")
        StatCell("用时", "$minutes:${seconds.toString().padStart(2, '0')}")
        StatCell("正确率", "$accuracy%")
        StatCell("WPM", "$wpm")
    }
}

@Composable
private fun StatCell(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun WordLetters(snap: TypingSnapshot, onPlay: () -> Unit) {
    val word = snap.word?.name.orEmpty()
    val typed = snap.typed
    val wrong = snap.wrongChar
    Row(
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        modifier = Modifier.clickable(
            indication = null,
            interactionSource = remember { MutableInteractionSource() },
            onClick = onPlay,
        ),
    ) {
        word.forEachIndexed { i, expected ->
            val typedHere = i < typed.length
            val current = i == typed.length
            val display = when {
                typedHere -> typed[i].toString()
                current && wrong != null -> wrong.toString()
                snap.dictation -> "_"
                else -> expected.toString()
            }
            val color = when {
                typedHere -> Color(0xFF2E7D32)
                current && wrong != null -> MaterialTheme.colorScheme.error
                current -> MaterialTheme.colorScheme.primary
                else -> MaterialTheme.colorScheme.outline
            }
            val weight = if (typedHere || current) FontWeight.Bold else FontWeight.Normal
            Text(
                text = display,
                fontSize = 36.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = weight,
                color = color,
            )
        }
        if (word.isEmpty()) {
            Text("本章没有单词", style = MaterialTheme.typography.bodyLarge)
        }
    }
}
