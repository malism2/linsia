package com.malism.linsia.classicread.di

import com.malism.linsia.classicread.data.ClassicReadRepository
import com.malism.linsia.classicread.data.db.ClassicReadDatabase
import com.malism.linsia.classicread.data.db.createClassicReadDatabase
import com.malism.linsia.classicread.presentation.CatalogViewModel
import com.malism.linsia.classicread.presentation.ReaderArgs
import com.malism.linsia.classicread.presentation.ReaderViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val classicReadModule = module {
    single { createClassicReadDatabase() }
    single { get<ClassicReadDatabase>().dao() }
    single { ClassicReadRepository(get(), get()) }
    viewModelOf(::CatalogViewModel)
    viewModel { params ->
        val args = params.get<ReaderArgs>()
        ReaderViewModel(args.bookId, get())
    }
}
