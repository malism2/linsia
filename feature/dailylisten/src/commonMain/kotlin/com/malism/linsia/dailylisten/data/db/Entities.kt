package com.malism.linsia.dailylisten.data.db

import androidx.room3.Entity
import androidx.room3.Index
import androidx.room3.PrimaryKey

@Entity(
    tableName = "lessons",
    indices = [Index(value = ["publishedEpoch"]), Index(value = ["publishedDate"])],
)
data class LessonEntity(
    @PrimaryKey val id: String,
    val title: String,
    val publishedAt: String,
    val publishedDate: String,
    val publishedEpoch: Long,
    val sourceUrl: String,
    val transcript: String,
    val audioUrl: String,
    val cachedAt: Long,
)

@Entity(tableName = "progress")
data class ProgressEntity(
    @PrimaryKey val lessonId: String,
    val positionMs: Long,
    val completed: Boolean,
    val completedDate: String?,
    val updatedAt: Long,
)
