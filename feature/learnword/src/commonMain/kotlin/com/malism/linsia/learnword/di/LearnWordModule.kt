package com.malism.linsia.learnword.di

import com.malism.linsia.learnword.data.audio.WordAudioPlayer
import com.malism.linsia.learnword.data.catalog.CatalogRepository
import com.malism.linsia.learnword.data.db.createLearnWordDatabase
import com.malism.linsia.learnword.data.importing.BookImportPipeline
import com.malism.linsia.learnword.data.study.StudyRepository
import com.malism.linsia.learnword.presentation.BookPickerViewModel
import com.malism.linsia.learnword.presentation.HomeViewModel
import com.malism.linsia.learnword.presentation.SettingsViewModel
import com.malism.linsia.learnword.presentation.StudyViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val learnWordModule = module {
    single { createLearnWordDatabase() }
    single { CatalogRepository(get(), get()) }
    single { BookImportPipeline(get(), get()) }
    single { StudyRepository(get(), get()) }
    single { WordAudioPlayer() }
    viewModelOf(::HomeViewModel)
    viewModelOf(::BookPickerViewModel)
    viewModelOf(::StudyViewModel)
    viewModelOf(::SettingsViewModel)
}
