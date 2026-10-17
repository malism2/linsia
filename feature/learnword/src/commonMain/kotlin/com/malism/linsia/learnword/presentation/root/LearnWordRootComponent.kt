package com.malism.linsia.learnword.presentation.root

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.router.stack.pop
import com.arkivanov.decompose.router.stack.popTo
import com.arkivanov.decompose.router.stack.pushNew
import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.backhandler.BackCallback
import com.arkivanov.essenty.backhandler.BackHandler
import com.malism.linsia.learnword.domain.SummaryStats
import kotlinx.serialization.Serializable

interface LearnWordRootComponent {
    val stack: Value<ChildStack<*, Child>>
    val backHandler: BackHandler

    fun openStudy()
    fun openSummary(stats: SummaryStats)
    fun openSettings()
    fun back()
    fun backToHome()

    sealed interface Child {
        data object Home : Child
        data object Study : Child
        data class Summary(val stats: SummaryStats) : Child
        data object Settings : Child
    }
}

class DefaultLearnWordRootComponent(
    componentContext: ComponentContext,
) : LearnWordRootComponent, ComponentContext by componentContext {
    private val navigation = StackNavigation<Config>()

    override val stack: Value<ChildStack<*, LearnWordRootComponent.Child>> = childStack(
        source = navigation,
        serializer = Config.serializer(),
        initialConfiguration = Config.Home,
        handleBackButton = false,
        childFactory = { config, _ -> child(config) },
    )

    private val backCallback = BackCallback(isEnabled = false) {
        when (stack.value.active.configuration) {
            is Config.Summary -> navigation.popTo(0)
            Config.Settings, Config.Study -> navigation.pop()
            else -> Unit
        }
    }

    init {
        backHandler.register(backCallback)
        stack.subscribe { childStack ->
            backCallback.isEnabled = childStack.active.configuration !is Config.Home
        }
    }

    private fun child(config: Config): LearnWordRootComponent.Child =
        when (config) {
            Config.Home -> LearnWordRootComponent.Child.Home
            Config.Study -> LearnWordRootComponent.Child.Study
            is Config.Summary -> LearnWordRootComponent.Child.Summary(config.stats)
            Config.Settings -> LearnWordRootComponent.Child.Settings
        }

    override fun openStudy() = navigation.pushNew(Config.Study)
    override fun openSummary(stats: SummaryStats) = navigation.pushNew(Config.Summary(stats))
    override fun openSettings() = navigation.pushNew(Config.Settings)
    override fun back() = navigation.pop()
    override fun backToHome() = navigation.popTo(0)

    @Serializable
    private sealed interface Config {
        @Serializable data object Home : Config
        @Serializable data object Study : Config
        @Serializable data class Summary(val stats: SummaryStats) : Config
        @Serializable data object Settings : Config
    }
}
