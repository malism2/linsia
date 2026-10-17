package com.malism.linsia.dailylisten.presentation.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.malism.linsia.dailylisten.presentation.LessonArgs
import com.malism.linsia.dailylisten.presentation.LessonViewModel
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun LessonScreen(
    lessonId: String,
    viewModel: LessonViewModel = koinViewModel(
        key = "lesson-$lessonId",
        parameters = { parametersOf(LessonArgs(lessonId)) },
    ),
) {
    val state by viewModel.state.collectAsState()
    when {
        state.loading -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        state.lesson == null -> {
            Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                Text("课程不存在", color = MaterialTheme.colorScheme.error)
            }
        }
        else -> {
            LessonBody(
                lesson = state.lesson!!,
                streak = null,
                completed = state.progress?.completed == true,
                showTranscript = state.showTranscript,
                playbackSpeed = state.playbackSpeed,
                initialPositionMs = state.progress?.positionMs ?: 0L,
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
