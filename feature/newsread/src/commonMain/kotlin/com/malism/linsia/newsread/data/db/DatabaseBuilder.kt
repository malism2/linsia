package com.malism.linsia.newsread.data.db

import androidx.room3.RoomDatabase
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO

expect fun newsReadDatabaseBuilder(): RoomDatabase.Builder<NewsReadDatabase>

fun createNewsReadDatabase(): NewsReadDatabase =
    newsReadDatabaseBuilder()
        .fallbackToDestructiveMigration(dropAllTables = true)
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.IO)
        .build()
