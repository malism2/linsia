package com.malism.linsia.newsread.data.db

import androidx.room3.ConstructedBy
import androidx.room3.Database
import androidx.room3.RoomDatabase
import androidx.room3.RoomDatabaseConstructor

@Database(
    entities = [ArticleEntity::class],
    version = 2,
    exportSchema = true,
)
@ConstructedBy(NewsReadDatabaseConstructor::class)
abstract class NewsReadDatabase : RoomDatabase() {
    abstract fun articleDao(): ArticleDao
}

@Suppress("NO_ACTUAL_FOR_EXPECT")
expect object NewsReadDatabaseConstructor : RoomDatabaseConstructor<NewsReadDatabase> {
    override fun initialize(): NewsReadDatabase
}
