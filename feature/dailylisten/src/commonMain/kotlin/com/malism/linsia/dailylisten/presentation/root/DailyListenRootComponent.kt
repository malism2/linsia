package com.malism.linsia.dailylisten.presentation.root

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.router.stack.pop
import com.arkivanov.decompose.router.stack.pushNew
import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.backhandler.BackCallback
import com.arkivanov.essenty.backhandler.BackHandler
import kotlinx.serialization.Serializable

interface DailyListenRootComponent {
    val stack: Value<ChildStack<*, Child>>
    val backHandler: BackHandler

    fun openHistory()
    fun openLesson(id: String)
    fun back()

    sealed interface Child {
        data object Today : Child
        data object History : Child
        data class Lesson(val id: String) : Child
    }
}

class DefaultDailyListenRootComponent(
    componentContext: ComponentContext,
) : DailyListenRootComponent, ComponentContext by componentContext {
    private val navigation = StackNavigation<Config>()

    override val stack: Value<ChildStack<*, DailyListenRootComponent.Child>> = childStack(
        source = navigation,
        serializer = Config.serializer(),
        initialConfiguration = Config.Today,
        handleBackButton = false,
        childFactory = { config, _ -> child(config) },
    )

    private val backCallback = BackCallback(isEnabled = false) {
        navigation.pop()
    }

    init {
        backHandler.register(backCallback)
        stack.subscribe { childStack ->
            backCallback.isEnabled = childStack.active.configuration !is Config.Today
        }
    }

    private fun child(config: Config): DailyListenRootComponent.Child =
        when (config) {
            Config.Today -> DailyListenRootComponent.Child.Today
            Config.History -> DailyListenRootComponent.Child.History
            is Config.Lesson -> DailyListenRootComponent.Child.Lesson(config.id)
        }

    override fun openHistory() = navigation.pushNew(Config.History)
    override fun openLesson(id: String) = navigation.pushNew(Config.Lesson(id))
    override fun back() = navigation.pop()

    @Serializable
    private sealed interface Config {
        @Serializable
        data object Today : Config

        @Serializable
        data object History : Config

        @Serializable
        data class Lesson(val id: String) : Config
    }
}
