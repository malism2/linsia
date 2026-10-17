package com.malism.linsia.dailylisten.data.db

import androidx.room3.Room
import androidx.room3.RoomDatabase
import java.io.File

actual fun dailyListenDatabaseBuilder(): RoomDatabase.Builder<DailyListenDatabase> {
    val dir = File(System.getProperty("user.home"), ".linsia")
    dir.mkdirs()
    val dbFile = File(dir, "dailylisten.db")
    return Room.databaseBuilder<DailyListenDatabase>(name = dbFile.absolutePath)
}
