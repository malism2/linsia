package com.malism.linsia.vocabbook.data.db

import androidx.room3.Entity
import androidx.room3.PrimaryKey

@Entity(tableName = "vocab_words")
data class VocabWordEntity(
    @PrimaryKey val wordLower: String,
    val word: String,
    val phonetic: String?,
    val translation: String,
    val sourceType: String,
    val sourceTitle: String?,
    val createdAt: Long,
)
