package com.malism.linsia.typeword.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.malism.linsia.typeword.data.DictRepository
import com.malism.linsia.typeword.data.TypewordSettings
import com.malism.linsia.typeword.data.audio.VoiceUrl
import com.malism.linsia.typeword.data.audio.WordAudioPlayer
import com.malism.linsia.typeword.domain.ChapterStats
import com.malism.linsia.typeword.domain.TypingResult
import com.malism.linsia.typeword.domain.TypingSession
import com.malism.linsia.typeword.domain.TypingSnapshot
import com.malism.linsia.typeword.domain.WordDict
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.time.Clock

data class PracticeUiState(
    val loading: Boolean = true,
    val dictId: String = "",
    val dictTitle: String = "",
    val chapterIndex: Int = 0,
    val chapterCount: Int = 1,
    val snapshot: TypingSnapshot? = null,
    val ukAccent: Boolean = false,
    val showPhonetic: Boolean = true,
    val dictation: Boolean = false,
    val pendingStats: ChapterStats? = null,
)

data class DictPickerUiState(
    val dicts: List<WordDict> = emptyList(),
    val selectedId: String = "",
    val chapterIndex: Int = 0,
)

class TypewordViewModel(
    private val dicts: DictRepository,
    private val settings: TypewordSettings,
    private val audio: WordAudioPlayer,
) : ViewModel() {
    private val _practice = MutableStateFlow(PracticeUiState())
    val practice: StateFlow<PracticeUiState> = _practice

    private val _picker = MutableStateFlow(DictPickerUiState())
    val picker: StateFlow<DictPickerUiState> = _picker

    private var session: TypingSession? = null
    private var playJob: Job? = null
    private var flashJob: Job? = null

    init {
        viewModelScope.launch { startFromSettings() }
        viewModelScope.launch {
            while (isActive) {
                delay(250)
                val current = session
                if (current != null && !current.snapshot().finished) {
                    publishSnapshot()
                }
            }
        }
    }

    fun onChar(raw: Char) {
        val current = session ?: return
        if (current.snapshot().finished) return
        val result = current.type(raw)
        publishSnapshot()
        if (current.snapshot().wrongChar != null) {
            flashJob?.cancel()
            flashJob = viewModelScope.launch {
                delay(180)
                current.clearFlash()
                publishSnapshot()
            }
        }
        handleResult(result)
    }

    fun skip() {
        val current = session ?: return
        handleResult(current.skip())
        publishSnapshot()
    }

    fun playCurrent() {
        val word = session?.snapshot()?.word?.name ?: return
        playWord(word)
    }

    fun toggleAccent() {
        settings.ukAccent = !settings.ukAccent
        _practice.update { it.copy(ukAccent = settings.ukAccent) }
    }

    fun togglePhonetic() {
        settings.showPhonetic = !settings.showPhonetic
        _practice.update { it.copy(showPhonetic = settings.showPhonetic) }
    }

    fun toggleDictation() {
        val next = !_practice.value.dictation
        settings.dictation = next
        startChapter(
            dictId = _practice.value.dictId,
            chapterIndex = _practice.value.chapterIndex,
            dictation = next,
        )
    }

    fun consumePendingStats() {
        _practice.update { it.copy(pendingStats = null) }
    }

    fun restart(dictation: Boolean) {
        settings.dictation = dictation
        startChapter(
            dictId = _practice.value.dictId.ifBlank { settings.dictId },
            chapterIndex = _practice.value.chapterIndex,
            dictation = dictation,
        )
    }

    fun selectDict(id: String) {
        val dict = _picker.value.dicts.firstOrNull { it.id == id } ?: return
        val chapter = settings.chapterIndex.coerceIn(0, (dict.chapterCount - 1).coerceAtLeast(0))
        if (settings.dictId != id) {
            settings.dictId = id
            settings.chapterIndex = 0
            _picker.update { it.copy(selectedId = id, chapterIndex = 0) }
        } else {
            _picker.update { it.copy(selectedId = id, chapterIndex = chapter) }
        }
    }

    fun selectChapter(index: Int) {
        val dict = _picker.value.dicts.firstOrNull { it.id == _picker.value.selectedId } ?: return
        val clamped = index.coerceIn(0, (dict.chapterCount - 1).coerceAtLeast(0))
        settings.chapterIndex = clamped
        _picker.update { it.copy(chapterIndex = clamped) }
    }

    fun applyPicker() {
        val picker = _picker.value
        startChapter(picker.selectedId, picker.chapterIndex, settings.dictation)
    }

    override fun onCleared() {
        playJob?.cancel()
        audio.release()
        super.onCleared()
    }

    private suspend fun startFromSettings() {
        val all = dicts.all()
        val dict = dicts.byId(settings.dictId)
        val chapter = settings.chapterIndex.coerceIn(0, (dict.chapterCount - 1).coerceAtLeast(0))
        settings.dictId = dict.id
        settings.chapterIndex = chapter
        _picker.value = DictPickerUiState(
            dicts = all,
            selectedId = dict.id,
            chapterIndex = chapter,
        )
        startChapter(dict.id, chapter, settings.dictation)
    }

    private fun startChapter(dictId: String, chapterIndex: Int, dictation: Boolean) {
        viewModelScope.launch {
            val dict = dicts.byId(dictId.ifBlank { settings.dictId })
            val chapter = chapterIndex.coerceIn(0, (dict.chapterCount - 1).coerceAtLeast(0))
            settings.dictId = dict.id
            settings.chapterIndex = chapter
            session = TypingSession(
                words = dict.chapter(chapter),
                dictation = dictation,
                nowMs = { Clock.System.now().toEpochMilliseconds() },
            )
            _picker.update { it.copy(selectedId = dict.id, chapterIndex = chapter) }
            _practice.value = PracticeUiState(
                loading = false,
                dictId = dict.id,
                dictTitle = dict.title,
                chapterIndex = chapter,
                chapterCount = dict.chapterCount.coerceAtLeast(1),
                snapshot = session?.snapshot(),
                ukAccent = settings.ukAccent,
                showPhonetic = settings.showPhonetic,
                dictation = dictation,
                pendingStats = null,
            )
        }
    }

    private fun handleResult(result: TypingResult) {
        when (result) {
            TypingResult.Continue -> Unit
            is TypingResult.WordFinished -> {
                playWord(result.word.name)
                if (result.chapterDone) {
                    val stats = session?.stats() ?: return
                    _practice.update { it.copy(pendingStats = stats, snapshot = session?.snapshot()) }
                }
            }
        }
    }

    private fun playWord(word: String) {
        playJob?.cancel()
        playJob = viewModelScope.launch {
            runCatching { audio.play(VoiceUrl.youdao(word, settings.ukAccent)) }
        }
    }

    private fun publishSnapshot() {
        val snap = session?.snapshot() ?: return
        _practice.update { it.copy(snapshot = snap) }
    }
}
