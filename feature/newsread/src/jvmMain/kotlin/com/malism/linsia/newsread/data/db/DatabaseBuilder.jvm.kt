package com.malism.linsia.newsread.data.db

import androidx.room3.Room
import androidx.room3.RoomDatabase
import java.io.File

actual fun newsReadDatabaseBuilder(): RoomDatabase.Builder<NewsReadDatabase> {
    val dir = File(System.getProperty("user.home"), ".linsia")
    dir.mkdirs()
    val dbFile = File(dir, "newsread.db")
    return Room.databaseBuilder<NewsReadDatabase>(name = dbFile.absolutePath)
}
