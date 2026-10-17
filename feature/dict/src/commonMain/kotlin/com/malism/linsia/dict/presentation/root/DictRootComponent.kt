package com.malism.linsia.dict.presentation.root

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.backhandler.BackHandler

interface DictRootComponent {
    val backHandler: BackHandler
}

class DefaultDictRootComponent(
    componentContext: ComponentContext,
) : DictRootComponent, ComponentContext by componentContext
