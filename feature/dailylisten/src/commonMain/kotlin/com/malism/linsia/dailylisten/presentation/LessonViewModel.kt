package com.malism.linsia.dailylisten.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.malism.linsia.dailylisten.data.DailyListenRepository
import com.malism.linsia.dailylisten.data.DailyListenSettings
import com.malism.linsia.dailylisten.domain.ListenLesson
import com.malism.linsia.dailylisten.domain.ListenProgress
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class LessonUiState(
    val lesson: ListenLesson? = null,
    val progress: ListenProgress? = null,
    val showTranscript: Boolean = false,
    val playbackSpeed: Float = 1.0f,
    val loading: Boolean = true,
)

data class LessonArgs(val lessonId: String)

class LessonViewModel(
    private val lessonId: String,
    private val repository: DailyListenRepository,
    private val settings: DailyListenSettings,
) : ViewModel() {
    private val showTranscript = MutableStateFlow(settings.showTranscript)
    private val playbackSpeed = MutableStateFlow(settings.playbackSpeed)

    val state: StateFlow<LessonUiState> = combine(
        repository.observeLesson(lessonId),
        repository.observeProgress(lessonId),
        showTranscript,
        playbackSpeed,
    ) { lesson, progress, transcript, speed ->
        LessonUiState(
            lesson = lesson,
            progress = progress,
            showTranscript = transcript,
            playbackSpeed = speed,
            loading = lesson == null,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LessonUiState())

    fun toggleTranscript() {
        val next = !showTranscript.value
        showTranscript.value = next
        settings.showTranscript = next
    }

    fun setSpeed(speed: Float) {
        playbackSpeed.value = speed
        settings.playbackSpeed = speed
    }

    fun persistPosition(positionMs: Long) {
        viewModelScope.launch {
            repository.saveProgress(lessonId, positionMs)
        }
    }

    fun onPlaybackProgress(positionMs: Long, durationMs: Long) {
        viewModelScope.launch {
            repository.maybeAutoComplete(lessonId, positionMs, durationMs)
        }
    }

    fun markCompleted() {
        viewModelScope.launch {
            repository.markCompleted(lessonId)
        }
    }

    suspend fun ensureLocalAudio(lessonId: String, remoteUrl: String): String =
        repository.ensureLocalAudio(lessonId, remoteUrl)
}
