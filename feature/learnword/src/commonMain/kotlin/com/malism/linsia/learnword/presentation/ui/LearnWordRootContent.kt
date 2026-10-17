package com.malism.linsia.learnword.presentation.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.arkivanov.decompose.extensions.compose.stack.Children
import com.arkivanov.decompose.extensions.compose.stack.animation.slide
import com.arkivanov.decompose.extensions.compose.stack.animation.stackAnimation
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.malism.linsia.learnword.presentation.root.LearnWordRootComponent

@Composable
fun LearnWordRootContent(
    component: LearnWordRootComponent,
    onClose: () -> Unit = {},
) {
    val stack by component.stack.subscribeAsState()
    val active = stack.active.instance
    val title = when (active) {
        LearnWordRootComponent.Child.Home -> "记单词"
        LearnWordRootComponent.Child.Study -> "背单词"
        is LearnWordRootComponent.Child.Summary -> "今日战报"
        LearnWordRootComponent.Child.Settings -> "设置"
    }

    Scaffold(
        topBar = {
            LearnWordTitleBar(
                title = title,
                onOpenSettings = component::openSettings,
                showBack = true,
                showSettings = active !is LearnWordRootComponent.Child.Settings,
                onBack = {
                    when (active) {
                        LearnWordRootComponent.Child.Home -> onClose()
                        is LearnWordRootComponent.Child.Summary -> component.backToHome()
                        else -> component.back()
                    }
                },
            )
        },
    ) { padding ->
        Children(
            stack = component.stack,
            modifier = Modifier.padding(padding),
            animation = stackAnimation(slide()),
        ) { child ->
            when (val instance = child.instance) {
                LearnWordRootComponent.Child.Home -> HomeScreen(onStudy = component::openStudy)
                LearnWordRootComponent.Child.Study -> StudyScreen(
                    onFinished = component::openSummary,
                )
                is LearnWordRootComponent.Child.Summary -> SummaryScreen(
                    stats = instance.stats,
                    onHome = component::backToHome,
                )
                LearnWordRootComponent.Child.Settings -> SettingsDrawerContent(
                    onBookSelected = component::back,
                )
            }
        }
    }
}
