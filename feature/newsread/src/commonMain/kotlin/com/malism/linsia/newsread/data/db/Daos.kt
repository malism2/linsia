package com.malism.linsia.newsread.data.db

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ArticleDao {
    @Query("SELECT * FROM articles WHERE categoryId = :categoryId ORDER BY publishedEpoch DESC")
    fun observeByCategory(categoryId: String): Flow<List<ArticleEntity>>

    @Query("SELECT * FROM articles WHERE id = :id")
    fun observeById(id: String): Flow<ArticleEntity?>

    @Query("SELECT * FROM articles WHERE id = :id")
    suspend fun getById(id: String): ArticleEntity?

    @Query("SELECT * FROM articles WHERE favorited = 1 ORDER BY favoritedAt DESC")
    fun observeFavorites(): Flow<List<ArticleEntity>>

    @Query("UPDATE articles SET favorited = :favorited, favoritedAt = :favoritedAt WHERE id = :id")
    suspend fun setFavorite(id: String, favorited: Boolean, favoritedAt: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(articles: List<ArticleEntity>)
}
