package com.malism.linsia.vocabbook.data.db

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface VocabWordDao {
    @Query("SELECT * FROM vocab_words ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<VocabWordEntity>>

    @Query("SELECT * FROM vocab_words WHERE wordLower = :wordLower LIMIT 1")
    suspend fun getByWordLower(wordLower: String): VocabWordEntity?

    @Query("SELECT EXISTS(SELECT 1 FROM vocab_words WHERE wordLower = :wordLower)")
    fun observeContains(wordLower: String): Flow<Boolean>

    @Query("SELECT EXISTS(SELECT 1 FROM vocab_words WHERE wordLower = :wordLower)")
    suspend fun contains(wordLower: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(word: VocabWordEntity)

    @Query("DELETE FROM vocab_words WHERE wordLower = :wordLower")
    suspend fun delete(wordLower: String)
}
