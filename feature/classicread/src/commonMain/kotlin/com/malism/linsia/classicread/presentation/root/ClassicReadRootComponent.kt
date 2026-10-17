package com.malism.linsia.classicread.presentation.root

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

interface ClassicReadRootComponent {
    val stack: Value<ChildStack<*, Child>>
    val backHandler: BackHandler

    fun openReader(bookId: String)
    fun back()

    sealed interface Child {
        data object Catalog : Child
        data class Reader(val bookId: String) : Child
    }
}

class DefaultClassicReadRootComponent(
    componentContext: ComponentContext,
) : ClassicReadRootComponent, ComponentContext by componentContext {
    private val navigation = StackNavigation<Config>()

    override val stack: Value<ChildStack<*, ClassicReadRootComponent.Child>> = childStack(
        source = navigation,
        serializer = Config.serializer(),
        initialConfiguration = Config.Catalog,
        handleBackButton = false,
        childFactory = { config, _ -> child(config) },
    )

    private val backCallback = BackCallback(isEnabled = false) {
        navigation.pop()
    }

    init {
        backHandler.register(backCallback)
        stack.subscribe { childStack ->
            backCallback.isEnabled = childStack.active.configuration !is Config.Catalog
        }
    }

    private fun child(config: Config): ClassicReadRootComponent.Child =
        when (config) {
            Config.Catalog -> ClassicReadRootComponent.Child.Catalog
            is Config.Reader -> ClassicReadRootComponent.Child.Reader(config.bookId)
        }

    override fun openReader(bookId: String) = navigation.pushNew(Config.Reader(bookId))
    override fun back() = navigation.pop()

    @Serializable
    private sealed interface Config {
        @Serializable
        data object Catalog : Config

        @Serializable
        data class Reader(val bookId: String) : Config
    }
}
