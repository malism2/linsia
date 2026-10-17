package com.malism.linsia.classicread.data.db

import androidx.room3.Room
import androidx.room3.RoomDatabase
import java.io.File

actual fun classicReadDatabaseBuilder(): RoomDatabase.Builder<ClassicReadDatabase> {
    val dir = File(System.getProperty("user.home"), ".linsia")
    dir.mkdirs()
    val dbFile = File(dir, "classicread.db")
    return Room.databaseBuilder<ClassicReadDatabase>(name = dbFile.absolutePath)
}
