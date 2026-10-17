package com.malism.linsia.learnword.data.db

import androidx.room3.ConstructedBy
import androidx.room3.Database
import androidx.room3.RoomDatabase
import androidx.room3.RoomDatabaseConstructor

@Database(
    entities = [
        WordBookMetaEntity::class,
        CatalogStateEntity::class,
        WordEntity::class,
        WordProgressEntity::class,
        UserStudySettingsEntity::class,
        DailyStudyLogEntity::class,
        ActiveSessionEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
@ConstructedBy(LearnWordDatabaseConstructor::class)
abstract class LearnWordDatabase : RoomDatabase() {
    abstract fun wordBookDao(): WordBookDao
    abstract fun wordDao(): WordDao
    abstract fun progressDao(): ProgressDao
    abstract fun settingsDao(): SettingsDao
    abstract fun studyLogDao(): StudyLogDao
    abstract fun sessionDao(): SessionDao
}

@Suppress("NO_ACTUAL_FOR_EXPECT")
expect object LearnWordDatabaseConstructor : RoomDatabaseConstructor<LearnWordDatabase> {
    override fun initialize(): LearnWordDatabase
}
