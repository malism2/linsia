package com.malism.linsia.dict.di

import com.malism.linsia.dict.data.DictRepository
import com.malism.linsia.dict.data.db.DictDatabase
import com.malism.linsia.dict.presentation.DictViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val dictModule = module {
    single { DictDatabase() }
    single { DictRepository(get(), get(), get()) }
    viewModelOf(::DictViewModel)
}
