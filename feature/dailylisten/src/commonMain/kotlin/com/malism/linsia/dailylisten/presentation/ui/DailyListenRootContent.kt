package com.malism.linsia.dailylisten.presentation.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.arkivanov.decompose.extensions.compose.stack.Children
import com.arkivanov.decompose.extensions.compose.stack.animation.slide
import com.arkivanov.decompose.extensions.compose.stack.animation.stackAnimation
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.malism.linsia.dailylisten.presentation.TodayViewModel
import com.malism.linsia.dailylisten.presentation.root.DailyListenRootComponent
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun DailyListenRootContent(
    component: DailyListenRootComponent,
    onClose: () -> Unit = {},
) {
    val stack by component.stack.subscribeAsState()
    val active = stack.active.instance
    val todayViewModel: TodayViewModel = koinViewModel()
    val todayState by todayViewModel.state.collectAsState()
    val title = when (active) {
        DailyListenRootComponent.Child.Today -> "每日听力"
        DailyListenRootComponent.Child.History -> "听力历史"
        is DailyListenRootComponent.Child.Lesson -> "再听一遍"
    }

    Scaffold(
        topBar = {
            DailyListenTitleBar(
                title = title,
                onBack = {
                    when (active) {
                        DailyListenRootComponent.Child.Today -> onClose()
                        else -> component.back()
                    }
                },
                onHistory = if (active is DailyListenRootComponent.Child.Today) {
                    component::openHistory
                } else {
                    null
                },
                onRefresh = if (active is DailyListenRootComponent.Child.Today) {
                    todayViewModel::refresh
                } else {
                    null
                },
                refreshEnabled = !todayState.loading,
            )
        },
    ) { padding ->
        Children(
            stack = component.stack,
            modifier = Modifier.padding(padding),
            animation = stackAnimation(slide()),
        ) { child ->
            when (val instance = child.instance) {
                DailyListenRootComponent.Child.Today -> TodayScreen(viewModel = todayViewModel)
                DailyListenRootComponent.Child.History -> HistoryScreen(
                    onOpenLesson = component::openLesson,
                )
                is DailyListenRootComponent.Child.Lesson -> LessonScreen(lessonId = instance.id)
            }
        }
    }
}
