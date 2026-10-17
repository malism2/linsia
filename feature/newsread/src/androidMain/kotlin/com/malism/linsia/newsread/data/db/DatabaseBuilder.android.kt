package com.malism.linsia.newsread.data.db

import androidx.room3.Room
import androidx.room3.RoomDatabase
import com.malism.linsia.core.platform.AndroidAppContext

actual fun newsReadDatabaseBuilder(): RoomDatabase.Builder<NewsReadDatabase> {
    val context = AndroidAppContext.require()
    val dbFile = context.getDatabasePath("newsread.db")
    return Room.databaseBuilder<NewsReadDatabase>(
        context = context,
        name = dbFile.absolutePath,
    )
}
