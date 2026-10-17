package com.malism.linsia.vocabbook.di

import com.malism.linsia.vocabbook.data.VocabBookRepository
import com.malism.linsia.vocabbook.data.db.VocabBookDatabase
import com.malism.linsia.vocabbook.data.db.createVocabBookDatabase
import com.malism.linsia.vocabbook.presentation.VocabBookViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val vocabBookModule = module {
    single { createVocabBookDatabase() }
    single { get<VocabBookDatabase>().vocabWordDao() }
    single { VocabBookRepository(get()) }
    viewModelOf(::VocabBookViewModel)
}
