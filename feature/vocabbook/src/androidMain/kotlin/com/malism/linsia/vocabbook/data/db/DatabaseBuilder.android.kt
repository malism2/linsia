package com.malism.linsia.vocabbook.data.db

import androidx.room3.Room
import androidx.room3.RoomDatabase
import com.malism.linsia.core.platform.AndroidAppContext

actual fun vocabBookDatabaseBuilder(): RoomDatabase.Builder<VocabBookDatabase> {
    val context = AndroidAppContext.require()
    val dbFile = context.getDatabasePath("vocabbook.db")
    return Room.databaseBuilder<VocabBookDatabase>(
        context = context,
        name = dbFile.absolutePath,
    )
}
