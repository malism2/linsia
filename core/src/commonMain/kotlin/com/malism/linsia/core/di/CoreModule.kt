package com.malism.linsia.core.di

import com.malism.linsia.core.net.createHttpClient
import com.malism.linsia.core.settings.createSettings
import org.koin.core.module.Module
import org.koin.dsl.module

val coreModule: Module = module {
    single { createHttpClient() }
    single { createSettings() }
}
