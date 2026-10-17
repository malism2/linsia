package com.malism.linsia.learnword.data.db

import androidx.room3.RoomDatabase
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO

expect fun learnWordDatabaseBuilder(): RoomDatabase.Builder<LearnWordDatabase>

fun createLearnWordDatabase(): LearnWordDatabase =
    learnWordDatabaseBuilder()
        .fallbackToDestructiveMigration(dropAllTables = true)
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.IO)
        .build()
