package com.malism.linsia.newsread.data.db

import androidx.room3.Entity
import androidx.room3.Index
import androidx.room3.PrimaryKey

@Entity(
    tableName = "articles",
    indices = [
        Index(value = ["categoryId", "publishedEpoch"]),
        Index(value = ["favorited", "favoritedAt"]),
    ],
)
data class ArticleEntity(
    @PrimaryKey val id: String,
    val categoryId: String,
    val title: String,
    val summary: String,
    val publishedAt: String,
    val publishedEpoch: Long,
    val sourceUrl: String,
    val body: String,
    val audioUrl: String?,
    val cachedAt: Long,
    val favorited: Boolean = false,
    val favoritedAt: Long = 0L,
)
