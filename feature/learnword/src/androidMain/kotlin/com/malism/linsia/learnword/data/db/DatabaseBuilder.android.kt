package com.malism.linsia.learnword.data.db

import androidx.room3.Room
import androidx.room3.RoomDatabase

actual fun learnWordDatabaseBuilder(): RoomDatabase.Builder<LearnWordDatabase> {
    val context = AndroidAppHolder.require()
    val dbFile = context.getDatabasePath("learnword.db")
    return Room.databaseBuilder<LearnWordDatabase>(
        context = context,
        name = dbFile.absolutePath,
    )
}
