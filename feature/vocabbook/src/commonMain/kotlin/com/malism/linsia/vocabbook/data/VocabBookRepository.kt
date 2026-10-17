package com.malism.linsia.vocabbook.data

import com.malism.linsia.vocabbook.data.db.VocabWordDao
import com.malism.linsia.vocabbook.data.db.VocabWordEntity
import com.malism.linsia.vocabbook.domain.VocabWord
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.time.Clock

class VocabBookRepository(
    private val dao: VocabWordDao,
) {
    fun observeAll(): Flow<List<VocabWord>> =
        dao.observeAll().map { rows -> rows.map { it.toDomain() } }

    fun observeContains(wordLower: String): Flow<Boolean> =
        dao.observeContains(wordLower)

    suspend fun contains(wordLower: String): Boolean =
        dao.contains(wordLower)

    suspend fun add(
        word: String,
        wordLower: String,
        phonetic: String?,
        translation: String,
        sourceType: String,
        sourceTitle: String?,
    ) {
        dao.upsert(
            VocabWordEntity(
                wordLower = wordLower,
                word = word,
                phonetic = phonetic,
                translation = translation,
                sourceType = sourceType,
                sourceTitle = sourceTitle,
                createdAt = Clock.System.now().toEpochMilliseconds(),
            ),
        )
    }

    suspend fun remove(wordLower: String) {
        dao.delete(wordLower)
    }
}

private fun VocabWordEntity.toDomain() = VocabWord(
    word = word,
    wordLower = wordLower,
    phonetic = phonetic,
    translation = translation,
    sourceType = sourceType,
    sourceTitle = sourceTitle,
    createdAt = createdAt,
)
