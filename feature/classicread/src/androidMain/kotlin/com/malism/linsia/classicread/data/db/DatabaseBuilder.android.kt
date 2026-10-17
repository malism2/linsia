package com.malism.linsia.classicread.data.db

import androidx.room3.Room
import androidx.room3.RoomDatabase
import com.malism.linsia.core.platform.AndroidAppContext

actual fun classicReadDatabaseBuilder(): RoomDatabase.Builder<ClassicReadDatabase> {
    val context = AndroidAppContext.require()
    val dbFile = context.getDatabasePath("classicread.db")
    return Room.databaseBuilder<ClassicReadDatabase>(
        context = context,
        name = dbFile.absolutePath,
    )
}
