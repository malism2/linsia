package com.malism.linsia.home.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.malism.linsia.core.theme.DopamineColors
import com.malism.linsia.home.AppFeature
import com.malism.linsia.home.HomeViewModel
import com.malism.linsia.home.quote.DailyQuote
import com.malism.linsia.home.quote.DailyQuoteDefaults
import io.github.kdroidfilter.composemediaplayer.InitialPlayerState
import io.github.kdroidfilter.composemediaplayer.VideoPlayerSurface
import io.github.kdroidfilter.composemediaplayer.rememberVideoPlayerState
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppHomeScreen(
    onOpenManage: () -> Unit,
    onOpenFeature: (String) -> Unit,
    viewModel: HomeViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Linsia") },
                actions = {
                    TextButton(onClick = onOpenManage) { Text("管理") }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                ),
            )
        },
    ) { padding ->
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 96.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(20.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                DailyQuoteCard(quote = state.quote, loading = state.quoteLoading)
            }
            if (state.visibleFeatures.isEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text("首页还没有功能", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "去管理页把功能重新显示出来",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            } else {
                itemsIndexed(state.visibleFeatures, key = { _, feature -> feature.id }) { index, feature ->
                    FeatureTile(
                        feature = feature,
                        accent = tileAccent(index),
                        onClick = { onOpenFeature(feature.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun DailyQuoteCard(
    quote: DailyQuote?,
    loading: Boolean,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = DopamineColors.CoralContainer.copy(alpha = 0.55f),
        ),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("每天美句", style = MaterialTheme.typography.titleSmall)
                if (quote != null && !quote.ttsUrl.isNullOrBlank()) {
                    QuotePlayButton(url = quote.ttsUrl)
                }
            }
            when {
                quote != null -> {
                    Text(quote.english, style = MaterialTheme.typography.titleMedium)
                    Text(
                        quote.chinese,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        if (quote.fromNetwork) DailyQuoteDefaults.AttributionRemote else DailyQuoteDefaults.AttributionLocal,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline,
                    )
                }
                loading -> Text(
                    "正在获取今日一句…",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                else -> Text(
                    "暂时没有句子",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun QuotePlayButton(url: String) {
    val playerState = rememberVideoPlayerState()
    LaunchedEffect(url) {
        playerState.openUri(url, InitialPlayerState.PAUSE)
    }
    Box {
        VideoPlayerSurface(
            playerState = playerState,
            modifier = Modifier.size(1.dp),
        )
        IconButton(onClick = {
            if (playerState.isPlaying) playerState.pause() else playerState.play()
        }) {
            Icon(
                imageVector = Icons.Filled.PlayArrow,
                contentDescription = if (playerState.isPlaying) "暂停" else "朗读",
                tint = if (playerState.isPlaying) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
        }
    }
}

@Composable
private fun FeatureTile(
    feature: AppFeature,
    accent: Color,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier.clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Surface(
            modifier = Modifier.size(64.dp),
            shape = CircleShape,
            color = accent,
        ) {
            Icon(
                imageVector = feature.icon.asImageVector(),
                contentDescription = feature.title,
                tint = Color.White,
                modifier = Modifier.padding(16.dp),
            )
        }
        Text(feature.title, style = MaterialTheme.typography.labelLarge)
    }
}

private fun tileAccent(index: Int): Color = when (index % 3) {
    0 -> DopamineColors.Coral
    1 -> DopamineColors.Orange
    else -> DopamineColors.Sunshine
}
