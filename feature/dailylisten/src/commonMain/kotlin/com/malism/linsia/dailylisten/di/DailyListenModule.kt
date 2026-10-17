package com.malism.linsia.dailylisten.di

import com.malism.linsia.dailylisten.data.DailyListenRepository
import com.malism.linsia.dailylisten.data.DailyListenSettings
import com.malism.linsia.dailylisten.data.db.DailyListenDatabase
import com.malism.linsia.dailylisten.data.db.createDailyListenDatabase
import com.malism.linsia.dailylisten.presentation.HistoryViewModel
import com.malism.linsia.dailylisten.presentation.LessonArgs
import com.malism.linsia.dailylisten.presentation.LessonViewModel
import com.malism.linsia.dailylisten.presentation.TodayViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val dailyListenModule = module {
    single { createDailyListenDatabase() }
    single { get<DailyListenDatabase>().dao() }
    single { DailyListenRepository(get(), get()) }
    single { DailyListenSettings(get()) }
    viewModelOf(::TodayViewModel)
    viewModelOf(::HistoryViewModel)
    viewModel { params ->
        val args = params.get<LessonArgs>()
        LessonViewModel(args.lessonId, get(), get())
    }
}
