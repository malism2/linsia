package com.malism.linsia.newsread.di

import com.malism.linsia.newsread.data.NewsReadRepository
import com.malism.linsia.newsread.data.db.NewsReadDatabase
import com.malism.linsia.newsread.data.db.createNewsReadDatabase
import com.malism.linsia.newsread.presentation.ArticleListViewModel
import com.malism.linsia.newsread.presentation.ArticleReaderViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val newsReadModule = module {
    single { createNewsReadDatabase() }
    single { get<NewsReadDatabase>().articleDao() }
    single { NewsReadRepository(get(), get()) }
    viewModelOf(::ArticleListViewModel)
    viewModel { params -> ArticleReaderViewModel(params.get(), get()) }
}
