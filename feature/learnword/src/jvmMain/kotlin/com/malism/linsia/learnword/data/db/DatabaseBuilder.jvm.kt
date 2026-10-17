package com.malism.linsia.learnword.data.db

import androidx.room3.Room
import androidx.room3.RoomDatabase
import java.io.File

actual fun learnWordDatabaseBuilder(): RoomDatabase.Builder<LearnWordDatabase> {
    val dir = File(System.getProperty("user.home"), ".linsia")
    dir.mkdirs()
    val dbFile = File(dir, "learnword.db")
    return Room.databaseBuilder<LearnWordDatabase>(name = dbFile.absolutePath)
}
