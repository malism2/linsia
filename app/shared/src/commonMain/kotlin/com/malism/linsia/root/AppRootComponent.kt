package com.malism.linsia.root

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.router.stack.pop
import com.arkivanov.decompose.router.stack.pushNew
import com.arkivanov.decompose.value.Value
import com.malism.linsia.classicread.presentation.root.ClassicReadRootComponent
import com.malism.linsia.classicread.presentation.root.DefaultClassicReadRootComponent
import com.malism.linsia.dailylisten.presentation.root.DailyListenRootComponent
import com.malism.linsia.dailylisten.presentation.root.DefaultDailyListenRootComponent
import com.malism.linsia.dict.presentation.root.DefaultDictRootComponent
import com.malism.linsia.dict.presentation.root.DictRootComponent
import com.malism.linsia.home.AppFeatureCatalog
import com.malism.linsia.learnword.presentation.root.DefaultLearnWordRootComponent
import com.malism.linsia.learnword.presentation.root.LearnWordRootComponent
import com.malism.linsia.newsread.presentation.root.DefaultNewsReadRootComponent
import com.malism.linsia.newsread.presentation.root.NewsReadRootComponent
import com.malism.linsia.typeword.presentation.root.DefaultTypewordRootComponent
import com.malism.linsia.typeword.presentation.root.TypewordRootComponent
import com.malism.linsia.vocabbook.presentation.root.DefaultVocabBookRootComponent
import com.malism.linsia.vocabbook.presentation.root.VocabBookRootComponent
import kotlinx.serialization.Serializable

interface AppRootComponent {
    val stack: Value<ChildStack<*, Child>>

    fun openManage()
    fun openFeature(id: String)
    fun back()

    sealed interface Child {
        data object Home : Child
        data object Manage : Child
        data class LearnWord(val component: LearnWordRootComponent) : Child
        data class NewsRead(val component: NewsReadRootComponent) : Child
        data class DailyListen(val component: DailyListenRootComponent) : Child
        data class TypeWord(val component: TypewordRootComponent) : Child
        data class ClassicRead(val component: ClassicReadRootComponent) : Child
        data class Dict(val component: DictRootComponent) : Child
        data class VocabBook(val component: VocabBookRootComponent) : Child
    }
}

class DefaultAppRootComponent(
    componentContext: ComponentContext,
) : AppRootComponent, ComponentContext by componentContext {
    private val navigation = StackNavigation<Config>()

    override val stack: Value<ChildStack<*, AppRootComponent.Child>> = childStack(
        source = navigation,
        serializer = Config.serializer(),
        initialConfiguration = Config.Home,
        handleBackButton = true,
        childFactory = ::child,
    )

    private fun child(config: Config, componentContext: ComponentContext): AppRootComponent.Child =
        when (config) {
            Config.Home -> AppRootComponent.Child.Home
            Config.Manage -> AppRootComponent.Child.Manage
            Config.LearnWord -> AppRootComponent.Child.LearnWord(
                DefaultLearnWordRootComponent(componentContext),
            )
            Config.NewsRead -> AppRootComponent.Child.NewsRead(
                DefaultNewsReadRootComponent(componentContext),
            )
            Config.DailyListen -> AppRootComponent.Child.DailyListen(
                DefaultDailyListenRootComponent(componentContext),
            )
            Config.TypeWord -> AppRootComponent.Child.TypeWord(
                DefaultTypewordRootComponent(componentContext),
            )
            Config.ClassicRead -> AppRootComponent.Child.ClassicRead(
                DefaultClassicReadRootComponent(componentContext),
            )
            Config.Dict -> AppRootComponent.Child.Dict(
                DefaultDictRootComponent(componentContext),
            )
            Config.VocabBook -> AppRootComponent.Child.VocabBook(
                DefaultVocabBookRootComponent(componentContext),
            )
        }

    override fun openManage() = navigation.pushNew(Config.Manage)

    override fun openFeature(id: String) {
        when (id) {
            AppFeatureCatalog.LEARN_WORD -> navigation.pushNew(Config.LearnWord)
            AppFeatureCatalog.NEWS_READ -> navigation.pushNew(Config.NewsRead)
            AppFeatureCatalog.DAILY_LISTEN -> navigation.pushNew(Config.DailyListen)
            AppFeatureCatalog.TYPE_WORD -> navigation.pushNew(Config.TypeWord)
            AppFeatureCatalog.CLASSIC_READ -> navigation.pushNew(Config.ClassicRead)
            AppFeatureCatalog.DICT -> navigation.pushNew(Config.Dict)
            AppFeatureCatalog.VOCAB_BOOK -> navigation.pushNew(Config.VocabBook)
        }
    }

    override fun back() {
        navigation.pop()
    }

    @Serializable
    private sealed interface Config {
        @Serializable
        data object Home : Config

        @Serializable
        data object Manage : Config

        @Serializable
        data object LearnWord : Config

        @Serializable
        data object NewsRead : Config

        @Serializable
        data object DailyListen : Config

        @Serializable
        data object TypeWord : Config

        @Serializable
        data object ClassicRead : Config

        @Serializable
        data object Dict : Config

        @Serializable
        data object VocabBook : Config
    }
}
