package com.malism.linsia.learnword.data.db

import androidx.room3.Entity
import androidx.room3.Index
import androidx.room3.PrimaryKey

@Entity(tableName = "word_book_meta")
data class WordBookMetaEntity(
    @PrimaryKey val name: String,
    val title: String,
    val url: String,
    val bundled: Boolean,
    val imported: Boolean,
    val importedAt: Long?,
)

@Entity(tableName = "catalog_state")
data class CatalogStateEntity(
    @PrimaryKey val id: Int = 1,
    val version: Int,
    val updatedAt: Long,
)

@Entity(
    tableName = "words",
    indices = [Index(value = ["bookId", "ordinal"])],
)
data class WordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val bookId: String,
    val headWord: String,
    val ukPhone: String?,
    val usPhone: String?,
    val transJson: String,
    val sentenceJson: String?,
    val extraJson: String?,
    val ordinal: Int,
)

@Entity(tableName = "word_progress", primaryKeys = ["bookId", "wordId"])
data class WordProgressEntity(
    val bookId: String,
    val wordId: Long,
    val easeFactor: Double,
    val intervalDays: Int,
    val repetitions: Int,
    val nextReviewEpochDay: Int?,
    val lastReviewEpochDay: Int?,
    val seen: Boolean,
)

@Entity(tableName = "user_study_settings")
data class UserStudySettingsEntity(
    @PrimaryKey val id: Int = 1,
    val dailyNewWords: Int,
    val activeBookId: String,
    val accentUk: Boolean,
)

@Entity(tableName = "daily_study_log", primaryKeys = ["date", "bookId"])
data class DailyStudyLogEntity(
    val date: String,
    val bookId: String,
    val newGraduated: Int,
    val reviewGraduated: Int,
    val durationSec: Long,
    val firstPassCorrect: Int,
    val totalEncounters: Int,
    val taskCompleted: Boolean,
)

@Entity(tableName = "active_session")
data class ActiveSessionEntity(
    @PrimaryKey val id: Int = 1,
    val date: String,
    val bookId: String,
    val queueJson: String,
    val graduatedJson: String,
    val firstPassCorrect: Int,
    val totalEncounters: Int,
    val elapsedSec: Long,
    val completed: Boolean,
    val leftoverJson: String,
)
