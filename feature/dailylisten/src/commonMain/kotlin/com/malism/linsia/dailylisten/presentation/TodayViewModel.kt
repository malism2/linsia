package com.malism.linsia.dailylisten.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.malism.linsia.dailylisten.data.DailyListenRepository
import com.malism.linsia.dailylisten.data.DailyListenSettings
import com.malism.linsia.dailylisten.domain.ListenLesson
import com.malism.linsia.dailylisten.domain.ListenProgress
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class TodayUiState(
    val lesson: ListenLesson? = null,
    val progress: ListenProgress? = null,
    val streak: Int = 0,
    val loading: Boolean = true,
    val error: String? = null,
    val showTranscript: Boolean = false,
    val playbackSpeed: Float = 1.0f,
    val completedToday: Boolean = false,
)

@OptIn(ExperimentalCoroutinesApi::class)
class TodayViewModel(
    private val repository: DailyListenRepository,
    private val settings: DailyListenSettings,
) : ViewModel() {
    private val loading = MutableStateFlow(true)
    private val error = MutableStateFlow<String?>(null)
    private val streak = MutableStateFlow(0)
    private val showTranscript = MutableStateFlow(settings.showTranscript)
    private val playbackSpeed = MutableStateFlow(settings.playbackSpeed)

    private val lessonAndProgress = repository.todayLessonId.flatMapLatest { id ->
        if (id == null) {
            flowOf(null to null)
        } else {
            combine(
                repository.observeLesson(id),
                repository.observeProgress(id),
            ) { lesson, progress -> lesson to progress }
        }
    }

    val state: StateFlow<TodayUiState> = combine(
        lessonAndProgress,
        combine(loading, error, streak) { isLoading, err, streakDays ->
            Triple(isLoading, err, streakDays)
        },
        combine(showTranscript, playbackSpeed) { transcript, speed -> transcript to speed },
    ) { lessonProgress, status, prefs ->
        val (lesson, progress) = lessonProgress
        val (isLoading, err, streakDays) = status
        val (transcript, speed) = prefs
        TodayUiState(
            lesson = lesson,
            progress = progress,
            streak = streakDays,
            loading = isLoading,
            error = err,
            showTranscript = transcript,
            playbackSpeed = speed,
            completedToday = progress?.completed == true,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TodayUiState())

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            loading.value = true
            error.value = null
            runCatching { repository.refreshAndResolveToday() }
                .onFailure { e ->
                    error.value = e.message ?: "加载失败"
                }
            streak.value = runCatching { repository.streakDays() }.getOrDefault(0)
            loading.value = false
        }
    }

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
        val id = state.value.lesson?.id ?: return
        viewModelScope.launch {
            repository.saveProgress(id, positionMs)
        }
    }

    fun onPlaybackProgress(positionMs: Long, durationMs: Long) {
        val id = state.value.lesson?.id ?: return
        viewModelScope.launch {
            repository.maybeAutoComplete(id, positionMs, durationMs)
            streak.value = runCatching { repository.streakDays() }.getOrDefault(streak.value)
        }
    }

    fun markCompleted() {
        val id = state.value.lesson?.id ?: return
        viewModelScope.launch {
            repository.markCompleted(id)
            streak.value = runCatching { repository.streakDays() }.getOrDefault(streak.value)
        }
    }

    suspend fun ensureLocalAudio(lessonId: String, remoteUrl: String): String =
        repository.ensureLocalAudio(lessonId, remoteUrl)
}
