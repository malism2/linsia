package com.malism.linsia

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.tooling.preview.Preview
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.backhandler.BackDispatcher
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.arkivanov.essenty.lifecycle.destroy
import com.arkivanov.essenty.lifecycle.resume
import com.malism.linsia.classicread.di.classicReadModule
import com.malism.linsia.core.di.coreModule
import com.malism.linsia.core.theme.LinsiaTheme
import com.malism.linsia.dailylisten.di.dailyListenModule
import com.malism.linsia.dict.di.dictModule
import com.malism.linsia.home.di.appModule
import com.malism.linsia.learnword.di.learnWordModule
import com.malism.linsia.newsread.di.newsReadModule
import com.malism.linsia.typeword.di.typewordModule
import com.malism.linsia.vocabbook.di.vocabBookModule
import com.malism.linsia.root.AppRootComponent
import com.malism.linsia.root.AppRootContent
import com.malism.linsia.root.DefaultAppRootComponent
import org.koin.compose.KoinApplication

@Composable
fun App(root: AppRootComponent? = null) {
    KoinApplication(application = {
        modules(
            coreModule,
            appModule,
            learnWordModule,
            newsReadModule,
            dailyListenModule,
            typewordModule,
            classicReadModule,
            dictModule,
            vocabBookModule,
        )
    }) {
        LinsiaTheme {
            val component = root ?: rememberOwnedRoot()
            AppRootContent(component)
        }
    }
}

@Composable
private fun rememberOwnedRoot(): AppRootComponent {
    val lifecycle = remember { LifecycleRegistry() }
    val backDispatcher = remember { BackDispatcher() }
    val component = remember {
        lifecycle.resume()
        DefaultAppRootComponent(
            DefaultComponentContext(
                lifecycle = lifecycle,
                backHandler = backDispatcher,
            ),
        )
    }
    DisposableEffect(Unit) {
        onDispose { lifecycle.destroy() }
    }
    return component
}

@Preview
@Composable
fun AppPreview() {
    App()
}
