package com.malism.linsia.dailylisten.presentation.ui

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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.malism.linsia.dailylisten.domain.DailyListenDefaults
import com.malism.linsia.dailylisten.domain.ListenLesson
import com.malism.linsia.dailylisten.presentation.TodayViewModel
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun TodayScreen(
    viewModel: TodayViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()
    when {
        state.loading && state.lesson == null -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        state.lesson == null -> {
            Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        state.error ?: "暂无今日听力",
                        color = MaterialTheme.colorScheme.error,
                    )
                    TextButton(onClick = viewModel::refresh) {
                        Text("重试")
                    }
                }
            }
        }
        else -> {
            val lesson = state.lesson!!
            LessonBody(
                lesson = lesson,
                streak = state.streak,
                completed = state.completedToday,
                showTranscript = state.showTranscript,
                playbackSpeed = state.playbackSpeed,
                initialPositionMs = state.progress?.positionMs ?: 0L,
                error = state.error,
                ensureLocalAudio = viewModel::ensureLocalAudio,
                onToggleTranscript = viewModel::toggleTranscript,
                onSpeedChange = viewModel::setSpeed,
                onPositionPersist = viewModel::persistPosition,
                onProgress = viewModel::onPlaybackProgress,
                onMarkCompleted = viewModel::markCompleted,
            )
        }
    }
}

@Composable
fun LessonBody(
    lesson: ListenLesson,
    streak: Int? = null,
    completed: Boolean,
    showTranscript: Boolean,
    playbackSpeed: Float,
    initialPositionMs: Long,
    error: String? = null,
    ensureLocalAudio: suspend (lessonId: String, remoteUrl: String) -> String,
    onToggleTranscript: () -> Unit,
    onSpeedChange: (Float) -> Unit,
    onPositionPersist: (Long) -> Unit,
    onProgress: (Long, Long) -> Unit,
    onMarkCompleted: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (streak != null) {
                Text(
                    if (completed) "连续 $streak 天 · 今日已完成" else "连续 $streak 天 · 今日未完成",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            } else if (completed) {
                Text(
                    "已完成",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Text(lesson.title, style = MaterialTheme.typography.headlineSmall)
            Text(
                lesson.publishedAt,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                DailyListenDefaults.Attribution,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary,
            )
            error?.let {
                Text(it, color = MaterialTheme.colorScheme.error)
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = onToggleTranscript) {
                    Text(if (showTranscript) "隐藏原文" else "显示原文")
                }
                if (!completed) {
                    Button(onClick = onMarkCompleted) {
                        Text("完成今日")
                    }
                } else {
                    Text(
                        "已完成",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            if (showTranscript) {
                if (lesson.transcript.isBlank()) {
                    Text(
                        "暂无原文",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    Text(lesson.transcript, style = MaterialTheme.typography.bodyLarge)
                }
            } else {
                Text(
                    "先听一遍，再打开原文核对。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        ListenPlayerBar(
            lessonId = lesson.id,
            audioUrl = lesson.audioUrl,
            ensureLocalAudio = ensureLocalAudio,
            playbackSpeed = playbackSpeed,
            initialPositionMs = initialPositionMs,
            onSpeedChange = onSpeedChange,
            onPositionPersist = onPositionPersist,
            onProgress = onProgress,
        )
    }
}
