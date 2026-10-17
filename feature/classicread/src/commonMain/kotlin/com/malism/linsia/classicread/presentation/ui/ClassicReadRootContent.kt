package com.malism.linsia.classicread.presentation.ui

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
import com.malism.linsia.classicread.presentation.CatalogViewModel
import com.malism.linsia.classicread.presentation.ReaderArgs
import com.malism.linsia.classicread.presentation.ReaderViewModel
import com.malism.linsia.classicread.presentation.root.ClassicReadRootComponent
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun ClassicReadRootContent(
    component: ClassicReadRootComponent,
    onClose: () -> Unit = {},
) {
    val stack by component.stack.subscribeAsState()
    val active = stack.active.instance
    val title = when (val current = active) {
        ClassicReadRootComponent.Child.Catalog -> "经典名著"
        is ClassicReadRootComponent.Child.Reader -> readerTitle(current.bookId)
    }

    Scaffold(
        topBar = {
            ClassicReadTitleBar(
                title = title,
                onBack = {
                    when (active) {
                        ClassicReadRootComponent.Child.Catalog -> onClose()
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
                ClassicReadRootComponent.Child.Catalog -> CatalogScreen(
                    onOpenBook = component::openReader,
                    viewModel = koinViewModel(),
                )
                is ClassicReadRootComponent.Child.Reader -> ReaderScreen(bookId = instance.bookId)
            }
        }
    }
}

@Composable
private fun readerTitle(bookId: String): String {
    val viewModel: ReaderViewModel = koinViewModel(
        key = "reader-$bookId",
        parameters = { parametersOf(ReaderArgs(bookId)) },
    )
    val state by viewModel.state.collectAsState()
    return state.book?.title ?: "阅读"
}
