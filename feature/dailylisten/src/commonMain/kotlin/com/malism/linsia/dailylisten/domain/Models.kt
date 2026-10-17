package com.malism.linsia.dailylisten.domain

object DailyListenDefaults {
    const val SourceName = "VOA Learning English"
    const val SourceHost = "www.21voa.com"
    const val Attribution = "VOA Learning English / 21voa.com"
    const val ListingPath = "as_it_is.html"
    const val UserAgent =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36"
    const val CompleteThreshold = 0.8f
    const val CandidateLimit = 12
    val Speeds = listOf(0.75f, 1.0f, 1.25f, 1.5f)
}

data class ListenLesson(
    val id: String,
    val title: String,
    val publishedAt: String,
    val publishedDate: String,
    val publishedEpoch: Long,
    val sourceUrl: String,
    val transcript: String,
    val audioUrl: String,
)

data class ListenProgress(
    val lessonId: String,
    val positionMs: Long,
    val completed: Boolean,
    val completedDate: String?,
)

data class HistoryItem(
    val lesson: ListenLesson,
    val progress: ListenProgress?,
)

data class ListingCandidate(
    val id: String,
    val title: String,
    val publishedAt: String,
    val publishedEpoch: Long,
    val sourceUrl: String,
)

/** Pick listing candidates whose publish day is not after [todayIso] (yyyy-MM-dd). */
fun filterCandidatesNotAfterToday(
    candidates: List<ListingCandidate>,
    todayIso: String,
    epochToDateIso: (Long) -> String,
): List<ListingCandidate> =
    candidates
        .filter { epochToDateIso(it.publishedEpoch) <= todayIso }
        .sortedByDescending { it.publishedEpoch }
