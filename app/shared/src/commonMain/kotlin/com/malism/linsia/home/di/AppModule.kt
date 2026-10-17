package com.malism.linsia.home.di

import com.malism.linsia.home.HomeLayoutRepository
import com.malism.linsia.home.HomeViewModel
import com.malism.linsia.home.quote.DailyQuoteRepository
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val appModule = module {
    single { HomeLayoutRepository(get()) }
    single { DailyQuoteRepository(get(), get()) }
    viewModelOf(::HomeViewModel)
}
