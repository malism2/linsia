package com.malism.linsia.dailylisten.presentation.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.malism.linsia.dailylisten.domain.DailyListenDefaults
import io.github.kdroidfilter.composemediaplayer.InitialPlayerState
import io.github.kdroidfilter.composemediaplayer.VideoPlayerState
import io.github.kdroidfilter.composemediaplayer.VideoPlayerSurface
import io.github.kdroidfilter.composemediaplayer.rememberVideoPlayerState
import io.github.vinceglb.filekit.PlatformFile
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.isActive
import kotlin.math.roundToLong

@Composable
fun ListenPlayerBar(
    lessonId: String,
    audioUrl: String,
    ensureLocalAudio: suspend (lessonId: String, remoteUrl: String) -> String,
    playbackSpeed: Float,
    initialPositionMs: Long = 0L,
    onSpeedChange: (Float) -> Unit,
    onPositionPersist: (Long) -> Unit,
    onProgress: (positionMs: Long, durationMs: Long) -> Unit,
) {
    val playerState = rememberVideoPlayerState()
    var restored by remember(lessonId, audioUrl) { mutableStateOf(false) }
    var preparing by remember(lessonId, audioUrl) { mutableStateOf(true) }
    var prepareError by remember(lessonId, audioUrl) { mutableStateOf<String?>(null) }
    val persist by rememberUpdatedState(onPositionPersist)
    val progressCb by rememberUpdatedState(onProgress)
    val resolveAudio by rememberUpdatedState(ensureLocalAudio)

    LaunchedEffect(lessonId, audioUrl) {
        restored = false
        preparing = true
        prepareError = null
        runCatching { resolveAudio(lessonId, audioUrl) }
            .onSuccess { localPath ->
                playerState.openFile(PlatformFile(localPath), InitialPlayerState.PAUSE)
                preparing = false
            }
            .onFailure { e ->
                prepareError = e.message ?: "音频下载失败"
                preparing = false
            }
    }
    LaunchedEffect(playbackSpeed) {
        playerState.playbackSpeed = playbackSpeed
    }
    LaunchedEffect(playerState, lessonId, audioUrl, initialPositionMs, preparing) {
        if (preparing || initialPositionMs <= 0L) {
            if (!preparing && initialPositionMs <= 0L) restored = true
            return@LaunchedEffect
        }
        snapshotFlow { playerState.duration }
            .filter { it > 0.0 }
            .distinctUntilChanged()
            .collect { duration ->
                if (!restored) {
                    val ratio = (initialPositionMs.toDouble() / duration).coerceIn(0.0, 1.0)
                    playerState.seekTo((ratio * 1000.0).toFloat())
                    restored = true
                }
            }
    }
    LaunchedEffect(playerState, lessonId, audioUrl, preparing) {
        if (preparing) return@LaunchedEffect
        while (isActive) {
            delay(2_000)
            val durationMs = playerState.duration.roundToLong()
            if (durationMs <= 0L) continue
            val positionMs = playerState.currentTime.roundToLong().coerceIn(0L, durationMs)
            persist(positionMs)
            progressCb(positionMs, durationMs)
        }
    }
    DisposableEffect(lessonId, audioUrl) {
        onDispose {
            val durationMs = playerState.duration.roundToLong()
            if (durationMs > 0L) {
                persist(playerState.currentTime.roundToLong().coerceIn(0L, durationMs))
            }
        }
    }

    Surface(tonalElevation = 2.dp) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
            VideoPlayerSurface(
                playerState = playerState,
                modifier = Modifier.fillMaxWidth().height(1.dp),
            )
            if (preparing) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp))
                    Text(
                        "正在准备音频…",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                Text(
                    "${playerState.positionText} / ${playerState.durationText}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    TextButton(onClick = { seekByMs(playerState, -10_000.0) }) {
                        Text("-10s")
                    }
                    IconButton(
                        onClick = {
                            if (playerState.isPlaying) playerState.pause() else playerState.play()
                        },
                    ) {
                        Icon(
                            imageVector = if (playerState.isPlaying) PauseIcon else Icons.Filled.PlayArrow,
                            contentDescription = if (playerState.isPlaying) "暂停" else "播放",
                        )
                    }
                    TextButton(onClick = { seekByMs(playerState, 10_000.0) }) {
                        Text("+10s")
                    }
                    Slider(
                        value = playerState.sliderPos,
                        onValueChange = { playerState.seekStart(it) },
                        onValueChangeFinished = { playerState.seekFinished() },
                        valueRange = 0f..1000f,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("倍速", style = MaterialTheme.typography.labelMedium)
                DailyListenDefaults.Speeds.forEach { speed ->
                    FilterChip(
                        selected = playbackSpeed == speed,
                        onClick = { onSpeedChange(speed) },
                        label = { Text("${speed}x") },
                    )
                }
            }
            (prepareError ?: playerState.error?.let { "音频无法播放，仍可查看原文" })?.let { message ->
                Text(
                    message,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(bottom = 4.dp),
                )
            }
        }
    }
}

private fun seekByMs(playerState: VideoPlayerState, deltaMs: Double) {
    val duration = playerState.duration
    if (duration <= 0.0) return
    val skipPos = ((playerState.currentTime + deltaMs) / duration * 1000.0)
        .toFloat()
        .coerceIn(0f, 1000f)
    playerState.seekTo(skipPos)
}

private val PauseIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "Filled.Pause",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply {
        path(fill = SolidColor(Color.Black)) {
            moveTo(6f, 19f)
            horizontalLineToRelative(4f)
            verticalLineTo(5f)
            horizontalLineTo(6f)
            verticalLineToRelative(14f)
            close()
            moveTo(14f, 5f)
            verticalLineToRelative(14f)
            horizontalLineToRelative(4f)
            verticalLineTo(5f)
            horizontalLineToRelative(-4f)
            close()
        }
    }.build()
}
