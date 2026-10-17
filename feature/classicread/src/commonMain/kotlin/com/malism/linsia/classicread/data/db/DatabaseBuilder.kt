package com.malism.linsia.classicread.data.db

import androidx.room3.RoomDatabase
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO

expect fun classicReadDatabaseBuilder(): RoomDatabase.Builder<ClassicReadDatabase>

fun createClassicReadDatabase(): ClassicReadDatabase =
    classicReadDatabaseBuilder()
        .fallbackToDestructiveMigration(dropAllTables = true)
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.IO)
        .build()
