package com.malism.linsia.home.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.malism.linsia.home.HomeViewModel
import com.malism.linsia.home.ManagedFeature
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppHomeManageScreen(
    onBack: () -> Unit,
    viewModel: HomeViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("首页管理") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回",
                        )
                    }
                },
                actions = {
                    TextButton(onClick = viewModel::reset) { Text("恢复默认") }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                ),
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            items(state.managedFeatures, key = { it.feature.id }) { item ->
                ManageRow(
                    item = item,
                    onToggle = { viewModel.toggleVisible(item.feature.id) },
                    onMoveUp = { viewModel.moveUp(item.feature.id) },
                    onMoveDown = { viewModel.moveDown(item.feature.id) },
                )
                HorizontalDivider()
            }
        }
    }
}

@Composable
private fun ManageRow(
    item: ManagedFeature,
    onToggle: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
) {
    ListItem(
        headlineContent = { Text(item.feature.title) },
        supportingContent = { Text(item.feature.subtitle) },
        leadingContent = {
            Icon(
                imageVector = item.feature.icon.asImageVector(),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
        },
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onMoveUp, enabled = item.canMoveUp) {
                    Icon(Icons.Filled.KeyboardArrowUp, contentDescription = "上移")
                }
                IconButton(onClick = onMoveDown, enabled = item.canMoveDown) {
                    Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "下移")
                }
                Switch(checked = item.visible, onCheckedChange = { onToggle() })
            }
        },
    )
}
