package com.malism.linsia.typeword.di

import com.malism.linsia.typeword.data.DictRepository
import com.malism.linsia.typeword.data.TypewordSettings
import com.malism.linsia.typeword.data.audio.WordAudioPlayer
import com.malism.linsia.typeword.presentation.TypewordViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val typewordModule = module {
    single { DictRepository() }
    single { TypewordSettings(get()) }
    single { WordAudioPlayer() }
    viewModelOf(::TypewordViewModel)
}
