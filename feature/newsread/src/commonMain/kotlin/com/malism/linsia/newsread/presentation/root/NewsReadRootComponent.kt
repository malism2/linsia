package com.malism.linsia.newsread.presentation.root

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

interface NewsReadRootComponent {
    val stack: Value<ChildStack<*, Child>>
    val backHandler: BackHandler

    fun openArticle(id: String)
    fun back()

    sealed interface Child {
        data object List : Child
        data class Article(val id: String) : Child
    }
}

class DefaultNewsReadRootComponent(
    componentContext: ComponentContext,
) : NewsReadRootComponent, ComponentContext by componentContext {
    private val navigation = StackNavigation<Config>()

    override val stack: Value<ChildStack<*, NewsReadRootComponent.Child>> = childStack(
        source = navigation,
        serializer = Config.serializer(),
        initialConfiguration = Config.List,
        handleBackButton = false,
        childFactory = { config, _ -> child(config) },
    )

    private val backCallback = BackCallback(isEnabled = false) {
        navigation.pop()
    }

    init {
        backHandler.register(backCallback)
        stack.subscribe { childStack ->
            backCallback.isEnabled = childStack.active.configuration !is Config.List
        }
    }

    private fun child(config: Config): NewsReadRootComponent.Child =
        when (config) {
            Config.List -> NewsReadRootComponent.Child.List
            is Config.Article -> NewsReadRootComponent.Child.Article(config.id)
        }

    override fun openArticle(id: String) = navigation.pushNew(Config.Article(id))
    override fun back() = navigation.pop()

    @Serializable
    private sealed interface Config {
        @Serializable
        data object List : Config

        @Serializable
        data class Article(val id: String) : Config
    }
}
