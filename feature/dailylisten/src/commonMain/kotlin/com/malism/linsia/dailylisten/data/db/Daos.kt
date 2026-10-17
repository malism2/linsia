package com.malism.linsia.dailylisten.data.db

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface DailyListenDao {
    @Query("SELECT * FROM lessons WHERE id = :id")
    fun observeLesson(id: String): Flow<LessonEntity?>

    @Query("SELECT * FROM lessons WHERE id = :id")
    suspend fun getLesson(id: String): LessonEntity?

    @Query(
        """
        SELECT l.* FROM lessons l
        INNER JOIN progress p ON p.lessonId = l.id
        ORDER BY p.updatedAt DESC
        """,
    )
    fun observeHistoryLessons(): Flow<List<LessonEntity>>

    @Query("SELECT * FROM progress WHERE lessonId = :lessonId")
    fun observeProgress(lessonId: String): Flow<ProgressEntity?>

    @Query("SELECT * FROM progress WHERE lessonId = :lessonId")
    suspend fun getProgress(lessonId: String): ProgressEntity?

    @Query("SELECT * FROM progress")
    fun observeAllProgress(): Flow<List<ProgressEntity>>

    @Query("SELECT completedDate FROM progress WHERE completed = 1 AND completedDate IS NOT NULL")
    suspend fun completedDates(): List<String>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertLesson(lesson: LessonEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProgress(progress: ProgressEntity)
}
