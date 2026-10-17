package com.malism.linsia.classicread.data.db

import androidx.room3.Entity
import androidx.room3.Index
import androidx.room3.PrimaryKey

@Entity(tableName = "catalog_state")
data class CatalogStateEntity(
    @PrimaryKey val id: Int = 1,
    val version: Int,
    val updatedAt: Long,
)

@Entity(tableName = "books")
data class BookEntity(
    @PrimaryKey val id: String,
    val title: String,
    val author: String,
    val rank: Int,
    val coverUrl: String,
    val epubUrl: String,
    val listed: Boolean,
    val downloaded: Boolean,
    val downloadedAt: Long?,
)

@Entity(
    tableName = "chapters",
    primaryKeys = ["bookId", "chapterIndex"],
    indices = [Index(value = ["bookId"])],
)
data class ChapterEntity(
    val bookId: String,
    val chapterIndex: Int,
    val title: String,
    val body: String,
)

@Entity(tableName = "progress")
data class ProgressEntity(
    @PrimaryKey val bookId: String,
    val chapterIndex: Int,
    val updatedAt: Long,
)

data class ChapterTitleRow(
    val chapterIndex: Int,
    val title: String,
)

data class ChapterCountRow(
    val bookId: String,
    val count: Int,
)
