package com.malism.linsia.vocabbook.presentation.root

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.backhandler.BackHandler

interface VocabBookRootComponent {
    val backHandler: BackHandler
}

class DefaultVocabBookRootComponent(
    componentContext: ComponentContext,
) : VocabBookRootComponent, ComponentContext by componentContext
