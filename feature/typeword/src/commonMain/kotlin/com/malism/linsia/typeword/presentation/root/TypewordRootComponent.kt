package com.malism.linsia.typeword.presentation.root

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
import com.malism.linsia.typeword.domain.ChapterStats
import kotlinx.serialization.Serializable

interface TypewordRootComponent {
    val stack: Value<ChildStack<*, Child>>
    val backHandler: BackHandler

    fun openDictPicker()
    fun openSummary(stats: ChapterStats)
    fun back()
    fun backToPractice()

    sealed interface Child {
        data object Practice : Child
        data object DictPicker : Child
        data class Summary(val stats: ChapterStats) : Child
    }
}

class DefaultTypewordRootComponent(
    componentContext: ComponentContext,
) : TypewordRootComponent, ComponentContext by componentContext {
    private val navigation = StackNavigation<Config>()

    override val stack: Value<ChildStack<*, TypewordRootComponent.Child>> = childStack(
        source = navigation,
        serializer = Config.serializer(),
        initialConfiguration = Config.Practice,
        handleBackButton = false,
        childFactory = { config, _ -> child(config) },
    )

    private val backCallback = BackCallback(isEnabled = false) {
        when (stack.value.active.configuration) {
            is Config.Summary -> navigation.popTo(0)
            Config.DictPicker -> navigation.pop()
            else -> Unit
        }
    }

    init {
        backHandler.register(backCallback)
        stack.subscribe { childStack ->
            backCallback.isEnabled = childStack.active.configuration !is Config.Practice
        }
    }

    private fun child(config: Config): TypewordRootComponent.Child =
        when (config) {
            Config.Practice -> TypewordRootComponent.Child.Practice
            Config.DictPicker -> TypewordRootComponent.Child.DictPicker
            is Config.Summary -> TypewordRootComponent.Child.Summary(config.stats)
        }

    override fun openDictPicker() = navigation.pushNew(Config.DictPicker)
    override fun openSummary(stats: ChapterStats) = navigation.pushNew(Config.Summary(stats))
    override fun back() = navigation.pop()
    override fun backToPractice() = navigation.popTo(0)

    @Serializable
    private sealed interface Config {
        @Serializable
        data object Practice : Config

        @Serializable
        data object DictPicker : Config

        @Serializable
        data class Summary(val stats: ChapterStats) : Config
    }
}
