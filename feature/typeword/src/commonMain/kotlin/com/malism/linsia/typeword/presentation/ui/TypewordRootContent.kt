package com.malism.linsia.typeword.presentation.ui

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.arkivanov.decompose.extensions.compose.stack.Children
import com.arkivanov.decompose.extensions.compose.stack.animation.slide
import com.arkivanov.decompose.extensions.compose.stack.animation.stackAnimation
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.malism.linsia.typeword.presentation.TypewordViewModel
import com.malism.linsia.typeword.presentation.root.TypewordRootComponent
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun TypewordRootContent(
    component: TypewordRootComponent,
    onClose: () -> Unit = {},
    viewModel: TypewordViewModel = koinViewModel(),
) {
    val stack by component.stack.subscribeAsState()
    val active = stack.active.instance
    val practice by viewModel.practice.collectAsState()

    val title = when (active) {
        TypewordRootComponent.Child.Practice -> "打字记词"
        TypewordRootComponent.Child.DictPicker -> "词库"
        is TypewordRootComponent.Child.Summary -> "本章完成"
    }

    Scaffold(
        contentWindowInsets = WindowInsets.systemBars,
        topBar = {
            TypewordTitleBar(
                title = title,
                onBack = {
                    when (active) {
                        TypewordRootComponent.Child.Practice -> onClose()
                        is TypewordRootComponent.Child.Summary -> component.backToPractice()
                        TypewordRootComponent.Child.DictPicker -> component.back()
                    }
                },
                ukAccent = practice.ukAccent,
                showPhonetic = practice.showPhonetic,
                dictation = practice.dictation,
                showToggles = active is TypewordRootComponent.Child.Practice,
                onToggleAccent = viewModel::toggleAccent,
                onTogglePhonetic = viewModel::togglePhonetic,
                onToggleDictation = viewModel::toggleDictation,
            )
        },
    ) { padding ->
        Children(
            stack = component.stack,
            modifier = Modifier.padding(padding),
            animation = stackAnimation(slide()),
        ) { child ->
            when (val instance = child.instance) {
                TypewordRootComponent.Child.Practice -> PracticeScreen(
                    onOpenDicts = component::openDictPicker,
                    onChapterFinished = component::openSummary,
                    viewModel = viewModel,
                )
                TypewordRootComponent.Child.DictPicker -> DictPickerScreen(
                    onConfirm = {
                        viewModel.applyPicker()
                        component.back()
                    },
                    viewModel = viewModel,
                )
                is TypewordRootComponent.Child.Summary -> SummaryScreen(
                    stats = instance.stats,
                    onRetry = {
                        viewModel.restart(dictation = false)
                        component.backToPractice()
                    },
                    onDictation = {
                        viewModel.restart(dictation = true)
                        component.backToPractice()
                    },
                )
            }
        }
    }
}
