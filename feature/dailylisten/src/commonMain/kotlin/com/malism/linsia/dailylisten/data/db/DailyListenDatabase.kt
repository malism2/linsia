package com.malism.linsia.dailylisten.data.db

import androidx.room3.ConstructedBy
import androidx.room3.Database
import androidx.room3.RoomDatabase
import androidx.room3.RoomDatabaseConstructor

@Database(
    entities = [LessonEntity::class, ProgressEntity::class],
    version = 1,
    exportSchema = true,
)
@ConstructedBy(DailyListenDatabaseConstructor::class)
abstract class DailyListenDatabase : RoomDatabase() {
    abstract fun dao(): DailyListenDao
}

@Suppress("NO_ACTUAL_FOR_EXPECT")
expect object DailyListenDatabaseConstructor : RoomDatabaseConstructor<DailyListenDatabase> {
    override fun initialize(): DailyListenDatabase
}
