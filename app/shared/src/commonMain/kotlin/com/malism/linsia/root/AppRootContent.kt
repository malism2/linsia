package com.malism.linsia.root

import androidx.compose.runtime.Composable
import com.arkivanov.decompose.extensions.compose.stack.Children
import com.arkivanov.decompose.extensions.compose.stack.animation.slide
import com.arkivanov.decompose.extensions.compose.stack.animation.stackAnimation
import com.malism.linsia.classicread.presentation.ui.ClassicReadRootContent
import com.malism.linsia.dailylisten.presentation.ui.DailyListenRootContent
import com.malism.linsia.dict.presentation.ui.DictRootContent
import com.malism.linsia.home.ui.AppHomeManageScreen
import com.malism.linsia.home.ui.AppHomeScreen
import com.malism.linsia.learnword.presentation.ui.LearnWordRootContent
import com.malism.linsia.newsread.presentation.ui.NewsReadRootContent
import com.malism.linsia.typeword.presentation.ui.TypewordRootContent
import com.malism.linsia.vocabbook.presentation.ui.VocabBookRootContent

@Composable
fun AppRootContent(component: AppRootComponent) {
    Children(
        stack = component.stack,
        animation = stackAnimation(slide()),
    ) { child ->
        when (val instance = child.instance) {
            AppRootComponent.Child.Home -> AppHomeScreen(
                onOpenManage = component::openManage,
                onOpenFeature = component::openFeature,
            )
            AppRootComponent.Child.Manage -> AppHomeManageScreen(
                onBack = component::back,
            )
            is AppRootComponent.Child.LearnWord -> LearnWordRootContent(
                component = instance.component,
                onClose = component::back,
            )
            is AppRootComponent.Child.NewsRead -> NewsReadRootContent(
                component = instance.component,
                onClose = component::back,
            )
            is AppRootComponent.Child.DailyListen -> DailyListenRootContent(
                component = instance.component,
                onClose = component::back,
            )
            is AppRootComponent.Child.TypeWord -> TypewordRootContent(
                component = instance.component,
                onClose = component::back,
            )
            is AppRootComponent.Child.ClassicRead -> ClassicReadRootContent(
                component = instance.component,
                onClose = component::back,
            )
            is AppRootComponent.Child.Dict -> DictRootContent(
                component = instance.component,
                onClose = component::back,
            )
            is AppRootComponent.Child.VocabBook -> VocabBookRootContent(
                component = instance.component,
                onClose = component::back,
            )
        }
    }
}
