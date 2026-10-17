package com.malism.linsia.vocabbook.data.db

import androidx.room3.Room
import androidx.room3.RoomDatabase
import java.io.File

actual fun vocabBookDatabaseBuilder(): RoomDatabase.Builder<VocabBookDatabase> {
    val dir = File(System.getProperty("user.home"), ".linsia")
    dir.mkdirs()
    val dbFile = File(dir, "vocabbook.db")
    return Room.databaseBuilder<VocabBookDatabase>(name = dbFile.absolutePath)
}
