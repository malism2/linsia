package com.malism.linsia.vocabbook.data.db

import androidx.room3.ConstructedBy
import androidx.room3.Database
import androidx.room3.RoomDatabase
import androidx.room3.RoomDatabaseConstructor

@Database(
    entities = [VocabWordEntity::class],
    version = 1,
    exportSchema = true,
)
@ConstructedBy(VocabBookDatabaseConstructor::class)
abstract class VocabBookDatabase : RoomDatabase() {
    abstract fun vocabWordDao(): VocabWordDao
}

@Suppress("NO_ACTUAL_FOR_EXPECT")
expect object VocabBookDatabaseConstructor : RoomDatabaseConstructor<VocabBookDatabase> {
    override fun initialize(): VocabBookDatabase
}
