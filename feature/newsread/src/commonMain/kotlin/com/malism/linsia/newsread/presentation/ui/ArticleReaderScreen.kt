package com.malism.linsia.newsread.presentation.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.malism.linsia.newsread.domain.NewsReadDefaults
import com.malism.linsia.newsread.presentation.ArticleReaderViewModel
import com.malism.linsia.vocabbook.domain.VocabSourceType
import com.malism.linsia.vocabbook.presentation.lookup.SelectableLookupText
import io.github.kdroidfilter.composemediaplayer.InitialPlayerState
import io.github.kdroidfilter.composemediaplayer.VideoPlayerState
import io.github.kdroidfilter.composemediaplayer.VideoPlayerSurface
import io.github.kdroidfilter.composemediaplayer.rememberVideoPlayerState
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun ArticleReaderScreen(
    articleId: String,
    viewModel: ArticleReaderViewModel = koinViewModel(parameters = { parametersOf(articleId) }),
) {
    val state by viewModel.state.collectAsState()
    val article = state.article
    val playerState = rememberVideoPlayerState()

    LaunchedEffect(article?.audioUrl) {
        val url = article?.audioUrl
        if (!url.isNullOrBlank()) {
            playerState.openUri(url, InitialPlayerState.PAUSE)
        }
    }

    when {
        article == null && state.loading -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        article == null -> {
            Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                Text(state.error ?: "文章不存在", color = MaterialTheme.colorScheme.error)
            }
        }
        else -> {
            Column(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(article.title, style = MaterialTheme.typography.headlineSmall)
                    Text(
                        article.publishedAt,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        NewsReadDefaults.Attribution,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                    if (state.loading && article.body.isBlank()) {
                        Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                    state.error?.let {
                        Text(it, color = MaterialTheme.colorScheme.error)
                    }
                    if (article.body.isNotBlank()) {
                        SelectableLookupText(
                            text = article.body,
                            sourceType = VocabSourceType.NEWS,
                            sourceTitle = article.title,
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                }
                if (!article.audioUrl.isNullOrBlank()) {
                    AudioBar(playerState = playerState)
                }
            }
        }
    }
}

@Composable
private fun AudioBar(playerState: VideoPlayerState) {
    Surface(tonalElevation = 2.dp) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
            VideoPlayerSurface(
                playerState = playerState,
                modifier = Modifier.fillMaxWidth().height(1.dp),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                IconButton(
                    onClick = {
                        if (playerState.isPlaying) playerState.pause() else playerState.play()
                    },
                ) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = if (playerState.isPlaying) "暂停" else "播放",
                    )
                }
                Slider(
                    value = playerState.sliderPos,
                    onValueChange = { playerState.seekStart(it) },
                    onValueChangeFinished = { playerState.seekFinished() },
                    valueRange = 0f..1000f,
                    modifier = Modifier.weight(1f),
                )
            }
            playerState.error?.let {
                Text(
                    "音频无法播放，仍可阅读正文",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(bottom = 4.dp),
                )
            }
        }
    }
}
