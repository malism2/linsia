package com.malism.linsia.typeword.presentation.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TypewordTitleBar(
    title: String,
    onBack: () -> Unit,
    ukAccent: Boolean = false,
    showPhonetic: Boolean = true,
    dictation: Boolean = false,
    showToggles: Boolean = false,
    onToggleAccent: (() -> Unit)? = null,
    onTogglePhonetic: (() -> Unit)? = null,
    onToggleDictation: (() -> Unit)? = null,
) {
    TopAppBar(
        title = { Text(title) },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "返回",
                )
            }
        },
        actions = {
            if (showToggles) {
                if (onToggleAccent != null) {
                    TextButton(onClick = onToggleAccent) {
                        Text(if (ukAccent) "英音" else "美音")
                    }
                }
                if (onTogglePhonetic != null) {
                    TextButton(onClick = onTogglePhonetic) {
                        Text(if (showPhonetic) "音标" else "无标")
                    }
                }
                if (onToggleDictation != null) {
                    TextButton(onClick = onToggleDictation) {
                        Text(if (dictation) "默写" else "看词")
                    }
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
        ),
    )
}
