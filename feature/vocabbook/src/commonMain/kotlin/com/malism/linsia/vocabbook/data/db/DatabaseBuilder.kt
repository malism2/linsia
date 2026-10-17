package com.malism.linsia.vocabbook.data.db

import androidx.room3.RoomDatabase
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO

expect fun vocabBookDatabaseBuilder(): RoomDatabase.Builder<VocabBookDatabase>

fun createVocabBookDatabase(): VocabBookDatabase =
    vocabBookDatabaseBuilder()
        .fallbackToDestructiveMigration(dropAllTables = true)
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.IO)
        .build()
