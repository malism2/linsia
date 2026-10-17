package com.malism.linsia.learnword.data.db

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import androidx.room3.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface WordBookDao {
    @Query("SELECT * FROM word_book_meta")
    fun observeBooks(): Flow<List<WordBookMetaEntity>>

    @Query("SELECT * FROM word_book_meta")
    suspend fun getBooks(): List<WordBookMetaEntity>

    @Query("SELECT * FROM word_book_meta WHERE name = :name")
    suspend fun getBook(name: String): WordBookMetaEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(books: List<WordBookMetaEntity>)

    @Update
    suspend fun update(book: WordBookMetaEntity)

    @Query("SELECT * FROM catalog_state WHERE id = 1")
    suspend fun getCatalogState(): CatalogStateEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCatalogState(state: CatalogStateEntity)
}

@Dao
interface WordDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(words: List<WordEntity>)

    @Query("SELECT COUNT(*) FROM words WHERE bookId = :bookId")
    suspend fun countByBook(bookId: String): Int

    @Query("SELECT * FROM words WHERE id = :id")
    suspend fun getById(id: Long): WordEntity?

    @Query("SELECT * FROM words WHERE bookId = :bookId ORDER BY ordinal ASC")
    suspend fun getByBook(bookId: String): List<WordEntity>
}

@Dao
interface ProgressDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(progress: WordProgressEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<WordProgressEntity>)

    @Query("SELECT * FROM word_progress WHERE bookId = :bookId AND wordId = :wordId")
    suspend fun get(bookId: String, wordId: Long): WordProgressEntity?

    @Query("SELECT * FROM word_progress WHERE bookId = :bookId")
    suspend fun getByBook(bookId: String): List<WordProgressEntity>
}

@Dao
interface SettingsDao {
    @Query("SELECT * FROM user_study_settings WHERE id = 1")
    fun observe(): Flow<UserStudySettingsEntity?>

    @Query("SELECT * FROM user_study_settings WHERE id = 1")
    suspend fun get(): UserStudySettingsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(settings: UserStudySettingsEntity)
}

@Dao
interface StudyLogDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(log: DailyStudyLogEntity)

    @Query("SELECT * FROM daily_study_log WHERE date = :date AND bookId = :bookId")
    suspend fun get(date: String, bookId: String): DailyStudyLogEntity?

    @Query("SELECT * FROM daily_study_log WHERE bookId = :bookId ORDER BY date DESC LIMIT 60")
    suspend fun recent(bookId: String): List<DailyStudyLogEntity>
}

@Dao
interface SessionDao {
    @Query("SELECT * FROM active_session WHERE id = 1")
    suspend fun get(): ActiveSessionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(session: ActiveSessionEntity)

    @Query("DELETE FROM active_session")
    suspend fun clear()
}
