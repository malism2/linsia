package com.malism.linsia.dailylisten.data.db

import androidx.room3.Room
import androidx.room3.RoomDatabase
import com.malism.linsia.core.platform.AndroidAppContext

actual fun dailyListenDatabaseBuilder(): RoomDatabase.Builder<DailyListenDatabase> {
    val context = AndroidAppContext.require()
    val dbFile = context.getDatabasePath("dailylisten.db")
    return Room.databaseBuilder<DailyListenDatabase>(
        context = context,
        name = dbFile.absolutePath,
    )
}
