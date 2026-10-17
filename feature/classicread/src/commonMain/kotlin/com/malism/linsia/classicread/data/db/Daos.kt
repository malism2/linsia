package com.malism.linsia.classicread.data.db

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ClassicReadDao {
    @Query("SELECT * FROM catalog_state WHERE id = 1")
    suspend fun getCatalogState(): CatalogStateEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCatalogState(state: CatalogStateEntity)

    @Query("SELECT * FROM books WHERE listed = 1 ORDER BY rank ASC")
    fun observeListedBooks(): Flow<List<BookEntity>>

    @Query("SELECT * FROM books")
    suspend fun getBooks(): List<BookEntity>

    @Query("SELECT * FROM books WHERE id = :id")
    fun observeBook(id: String): Flow<BookEntity?>

    @Query("SELECT * FROM books WHERE id = :id")
    suspend fun getBook(id: String): BookEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertBooks(books: List<BookEntity>)

    @Query("UPDATE books SET listed = 0 WHERE id NOT IN (:ids)")
    suspend fun unlistMissing(ids: List<String>)

    @Query(
        """
        UPDATE books
        SET downloaded = :downloaded, downloadedAt = :downloadedAt
        WHERE id = :id
        """,
    )
    suspend fun setDownloaded(id: String, downloaded: Boolean, downloadedAt: Long?)

    @Query("SELECT bookId, COUNT(*) AS count FROM chapters GROUP BY bookId")
    fun observeChapterCounts(): Flow<List<ChapterCountRow>>

    @Query("SELECT chapterIndex, title FROM chapters WHERE bookId = :bookId ORDER BY chapterIndex ASC")
    fun observeChapterTitles(bookId: String): Flow<List<ChapterTitleRow>>

    @Query("SELECT * FROM chapters WHERE bookId = :bookId AND chapterIndex = :chapterIndex")
    fun observeChapter(bookId: String, chapterIndex: Int): Flow<ChapterEntity?>

    @Query("SELECT body FROM chapters WHERE bookId = :bookId ORDER BY chapterIndex ASC")
    suspend fun getChapterBodies(bookId: String): List<String>

    @Query("SELECT COUNT(*) FROM chapters WHERE bookId = :bookId")
    suspend fun chapterCount(bookId: String): Int

    @Query("DELETE FROM chapters WHERE bookId = :bookId")
    suspend fun deleteChapters(bookId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChapters(chapters: List<ChapterEntity>)

    @Query("SELECT * FROM progress")
    fun observeProgress(): Flow<List<ProgressEntity>>

    @Query("SELECT * FROM progress WHERE bookId = :bookId")
    fun observeProgress(bookId: String): Flow<ProgressEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProgress(progress: ProgressEntity)
}
