package com.malism.linsia.learnword.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.malism.linsia.learnword.data.audio.VoiceUrl
import com.malism.linsia.learnword.data.audio.WordAudioPlayer
import com.malism.linsia.learnword.data.study.StudyOutcome
import com.malism.linsia.learnword.data.study.StudyRepository
import com.malism.linsia.learnword.domain.StudyCardContent
import com.malism.linsia.learnword.domain.SummaryStats
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class StudyViewModel(
    private val study: StudyRepository,
    private val audio: WordAudioPlayer,
) : ViewModel() {
    val card = study.currentCard
    val isBack = study.isBack
    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy
    private var ticker: Job? = null
    private var playJob: Job? = null

    fun startTicker() {
        ticker?.cancel()
        ticker = viewModelScope.launch {
            while (isActive) {
                delay(1000)
                study.addElapsed(1)
            }
        }
    }

    fun stopTicker() {
        ticker?.cancel()
        ticker = null
    }

    fun playCurrent(card: StudyCardContent?) {
        playJob?.cancel()
        if (card == null) return
        playJob = viewModelScope.launch {
            val uk = study.settingsOrDefault().accentUk
            runCatching { audio.play(VoiceUrl.youdao(card.headWord, uk)) }
        }
    }

    fun onKnow(onFinished: (SummaryStats) -> Unit) {
        if (_busy.value) return
        viewModelScope.launch {
            _busy.value = true
            when (val out = study.know()) {
                is StudyOutcome.Finished -> onFinished(out.stats)
                else -> Unit
            }
            _busy.value = false
        }
    }

    fun onUnknown() {
        if (_busy.value) return
        study.showBack()
    }

    fun onRemembered(onFinished: (SummaryStats) -> Unit) {
        if (_busy.value) return
        viewModelScope.launch {
            _busy.value = true
            when (val out = study.rememberedAfterUnknown()) {
                is StudyOutcome.Finished -> onFinished(out.stats)
                else -> Unit
            }
            _busy.value = false
        }
    }

    override fun onCleared() {
        playJob?.cancel()
        stopTicker()
        audio.release()
        super.onCleared()
    }
}
