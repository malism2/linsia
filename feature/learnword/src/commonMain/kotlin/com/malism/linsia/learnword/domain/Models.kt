package com.malism.linsia.learnword.domain

import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable

@Serializable
enum class WordSource { New, Review, Leftover }

data class TaskWord(
    val wordId: Long,
    val ordinal: Int,
    val source: WordSource,
)

data class TaskInput(
    val newWords: List<TaskWord>,
    val dueReview: List<TaskWord>,
    val leftovers: List<TaskWord>,
    val dailyNewLimit: Int,
)

data class SessionCard(
    val wordId: Long,
    val life: Int,
    val source: WordSource,
    val firstPassKnown: Boolean? = null,
)

sealed interface QueueEvent {
    data class Show(val card: SessionCard) : QueueEvent
    data object Empty : QueueEvent
}

data class KnowResult(
    val graduated: SessionCard?,
    val next: QueueEvent,
)

data class Sm2State(
    val easeFactor: Double = 2.5,
    val intervalDays: Int = 0,
    val repetitions: Int = 0,
)

@Serializable
data class SummaryStats(
    val graduated: Int,
    val elapsedSec: Long,
    val firstPassCorrect: Int,
    val totalEncounters: Int,
    val streak: Int,
) {
    val accuracy: Float
        get() = if (totalEncounters == 0) 0f else firstPassCorrect.toFloat() / totalEncounters
}

data class StudyExample(
    val english: String,
    val chinese: String,
)

data class StudyCardContent(
    val wordId: Long,
    val headWord: String,
    val phonetic: String,
    val transLines: List<String>,
    val examples: List<StudyExample>,
    val source: WordSource,
    val life: Int,
    val index: Int,
    val total: Int,
)

fun formatIpa(raw: String?): String {
    val inner = raw.orEmpty().trim().trim('/', '[', ']', '／', '［', '］')
    if (inner.isEmpty()) return ""
    return "/$inner/"
}

object LearnWordDefaults {
    const val BundledBookId = "CET4luan_1"
    const val RemoteCatalogUrl =
        "https://objectstorage.uk-london-1.oraclecloud.com/n/lr3rsi02p2te/b/linsia/o/learn_dist.json"
    const val DefaultNewWords = 20
    const val MinNewWords = 5
    const val MaxNewWords = 100
    const val InitialLife = 2
    const val ReinsertMin = 5
    const val ReinsertMax = 8
}

fun LocalDate.iso(): String = toString()
