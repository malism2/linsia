package com.malism.linsia.dailylisten.data.db

import androidx.room3.RoomDatabase
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO

expect fun dailyListenDatabaseBuilder(): RoomDatabase.Builder<DailyListenDatabase>

fun createDailyListenDatabase(): DailyListenDatabase =
    dailyListenDatabaseBuilder()
        .fallbackToDestructiveMigration(dropAllTables = true)
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.IO)
        .build()
