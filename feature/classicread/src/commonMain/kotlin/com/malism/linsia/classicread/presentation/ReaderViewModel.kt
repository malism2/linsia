package com.malism.linsia.classicread.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.malism.linsia.classicread.data.ClassicReadRepository
import com.malism.linsia.classicread.domain.BookPagination
import com.malism.linsia.classicread.domain.ClassicBook
import com.malism.linsia.classicread.domain.ClassicReadDefaults
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ReaderUiState(
    val book: ClassicBook? = null,
    val paragraphs: List<String> = emptyList(),
    val pageIndex: Int = 0,
    val pageCount: Int = 0,
    val initialScrollIndex: Int = 0,
    val loading: Boolean = true,
    val downloadProgress: Float? = null,
    val error: String? = null,
)

data class ReaderArgs(
    val bookId: String,
)

class ReaderViewModel(
    private val bookId: String,
    private val repository: ClassicReadRepository,
) : ViewModel() {
    private val pageIndex = MutableStateFlow(0)
    private val paragraphs = MutableStateFlow<List<String>>(emptyList())
    private val paragraphPageIndex = MutableStateFlow<List<Int>>(emptyList())
    private val pageCount = MutableStateFlow(0)
    private val initialScrollIndex = MutableStateFlow(0)
    private val loading = MutableStateFlow(true)
    private val error = MutableStateFlow<String?>(null)
    private val downloadProgress = MutableStateFlow<Float?>(null)

    val state: StateFlow<ReaderUiState> = combine(
        combine(
            repository.observeBook(bookId),
            pageIndex,
            paragraphs,
            pageCount,
            initialScrollIndex,
        ) { book, index, text, count, scrollIndex ->
            ReaderUiState(
                book = book,
                paragraphs = text,
                pageIndex = index,
                pageCount = count,
                initialScrollIndex = scrollIndex,
                loading = text.isEmpty(),
            )
        },
        combine(loading, error, downloadProgress) { isLoading, err, progress ->
            Triple(isLoading, err, progress)
        },
    ) { ui, (isLoading, err, progress) ->
        ui.copy(
            loading = isLoading && ui.paragraphs.isEmpty(),
            error = err,
            downloadProgress = progress,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ReaderUiState())

    init {
        viewModelScope.launch {
            val book = repository.observeBook(bookId).first { it != null }
            if (book?.epubUrl.isNullOrBlank()) {
                error.value = ClassicReadDefaults.UnavailableMessage
                loading.value = false
                return@launch
            }
            loading.value = true
            downloadProgress.value = null
            runCatching {
                repository.ensureDownloaded(bookId) { received, total ->
                    downloadProgress.value = if (total != null && total > 0) {
                        received.toFloat() / total.toFloat()
                    } else {
                        null
                    }
                }
            }.onFailure { e ->
                error.value = e.message ?: ClassicReadDefaults.FetchFailedMessage
                loading.value = false
                downloadProgress.value = null
                return@launch
            }
            downloadProgress.value = null

            val bodies = repository.getChapterBodies(bookId)
            val content = BookPagination.build(bodies)
            if (content.paragraphs.isEmpty()) {
                error.value = ClassicReadDefaults.FetchFailedMessage
                loading.value = false
                return@launch
            }
            paragraphs.value = content.paragraphs
            paragraphPageIndex.value = content.paragraphPageIndex
            pageCount.value = content.pageCount
            val savedPage = repository.observeProgress(bookId).first()?.chapterIndex ?: 0
            val page = savedPage.coerceIn(0, content.pageCount.coerceAtLeast(1) - 1)
            pageIndex.value = page
            initialScrollIndex.value = content.paragraphPageIndex.indexOfFirst { it >= page }
                .coerceAtLeast(0)
            loading.value = false
        }
    }

    fun onVisibleParagraph(paragraphIndex: Int) {
        val page = paragraphPageIndex.value.getOrNull(paragraphIndex) ?: return
        if (page != pageIndex.value) {
            pageIndex.value = page
            persist(page)
        }
    }

    fun persistCurrent() {
        persist(pageIndex.value)
    }

    private fun persist(index: Int) {
        viewModelScope.launch {
            repository.saveProgress(bookId, index)
        }
    }
}
