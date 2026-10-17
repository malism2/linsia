package com.malism.linsia.classicread.data.db

import androidx.room3.ConstructedBy
import androidx.room3.Database
import androidx.room3.RoomDatabase
import androidx.room3.RoomDatabaseConstructor

@Database(
    entities = [
        CatalogStateEntity::class,
        BookEntity::class,
        ChapterEntity::class,
        ProgressEntity::class,
    ],
    version = 2,
    exportSchema = true,
)
@ConstructedBy(ClassicReadDatabaseConstructor::class)
abstract class ClassicReadDatabase : RoomDatabase() {
    abstract fun dao(): ClassicReadDao
}

@Suppress("NO_ACTUAL_FOR_EXPECT")
expect object ClassicReadDatabaseConstructor : RoomDatabaseConstructor<ClassicReadDatabase> {
    override fun initialize(): ClassicReadDatabase
}
