package com.malism.linsia.learnword.presentation.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.malism.linsia.learnword.presentation.BookPickerViewModel
import com.malism.linsia.learnword.presentation.SettingsUi
import com.malism.linsia.learnword.presentation.SettingsViewModel
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun SettingsDrawerContent(
    onBookSelected: () -> Unit,
    settingsVm: SettingsViewModel = koinViewModel(),
    booksVm: BookPickerViewModel = koinViewModel(),
) {
    val settings by settingsVm.state.collectAsState()
    val books by booksVm.books.collectAsState()
    val ui by booksVm.ui.collectAsState()
    val busyId = ui.first
    val bookError = ui.second

    LazyColumn(Modifier.fillMaxSize()) {
        item {
            SettingsControls(
                settings = settings,
                bookError = bookError,
                onDailyNew = settingsVm::setDailyNew,
                onAccentUk = settingsVm::setAccentUk,
                onCheckCatalog = settingsVm::checkCatalog,
            )
        }
        items(books, key = { it.name }) { book ->
            ListItem(
                headlineContent = { Text(book.title) },
                supportingContent = {
                    Text(
                        when {
                            book.bundled && book.imported -> "内置 · 已导入"
                            book.bundled -> "内置 · 待导入"
                            book.imported -> "已下载"
                            else -> "需下载"
                        },
                    )
                },
                trailingContent = {
                    when {
                        busyId == book.name -> CircularProgressIndicator()
                        settings.activeBookId == book.name -> Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = "当前词书",
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth().clickable(enabled = busyId == null) {
                    booksVm.select(book.name, onBookSelected)
                },
            )
        }
    }
}

@Composable
private fun SettingsControls(
    settings: SettingsUi,
    bookError: String?,
    onDailyNew: (Int) -> Unit,
    onAccentUk: (Boolean) -> Unit,
    onCheckCatalog: () -> Unit,
) {
    Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text("每日新词 ${settings.dailyNew}", style = MaterialTheme.typography.titleSmall)
        Slider(
            value = settings.dailyNew.toFloat(),
            onValueChange = { onDailyNew(it.toInt()) },
            valueRange = 5f..100f,
            steps = 18,
        )

        Text("发音", style = MaterialTheme.typography.titleSmall)
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(vertical = 8.dp),
        ) {
            FilterChip(selected = !settings.accentUk, onClick = { onAccentUk(false) }, label = { Text("美音") })
            FilterChip(selected = settings.accentUk, onClick = { onAccentUk(true) }, label = { Text("英音") })
        }

        Button(
            onClick = onCheckCatalog,
            enabled = !settings.checking,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(if (settings.checking) "检查中…" else "检查词书更新")
        }
        settings.message?.let {
            Text(it, modifier = Modifier.padding(top = 8.dp), style = MaterialTheme.typography.bodySmall)
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
        Text("选择词书", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 8.dp))
        bookError?.let {
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
    }
}
