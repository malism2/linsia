package com.malism.linsia.vocabbook.presentation.lookup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.dp
import com.malism.linsia.dict.data.DictRepository
import com.malism.linsia.dict.domain.DictEntry
import com.malism.linsia.dict.domain.normalizeQuery
import com.malism.linsia.vocabbook.data.VocabBookRepository
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@Composable
fun SelectableLookupText(
    text: String,
    sourceType: String,
    sourceTitle: String?,
    style: TextStyle,
    modifier: Modifier = Modifier,
) {
    var lookupQuery by remember { mutableStateOf<String?>(null) }
    val annotated = remember(text) { buildLookupAnnotatedString(text) { word -> lookupQuery = word } }

    Text(
        text = annotated,
        style = style,
        modifier = modifier,
    )

    lookupQuery?.let { query ->
        WordLookupDialog(
            query = query,
            sourceType = sourceType,
            sourceTitle = sourceTitle,
            onDismiss = { lookupQuery = null },
        )
    }
}

@Composable
fun WordLookupDialog(
    query: String,
    sourceType: String,
    sourceTitle: String?,
    onDismiss: () -> Unit,
    dictRepository: DictRepository = koinInject(),
    vocabBookRepository: VocabBookRepository = koinInject(),
) {
    val scope = rememberCoroutineScope()
    var loading by remember(query) { mutableStateOf(true) }
    var dictReady by remember(query) { mutableStateOf(true) }
    var entry by remember(query) { mutableStateOf<DictEntry?>(null) }
    val wordLower = remember(query) { normalizeQuery(query) }
    val inBook by vocabBookRepository.observeContains(wordLower)
        .collectAsState(initial = false)

    LaunchedEffect(query) {
        loading = true
        entry = null
        if (!dictRepository.isReady()) {
            dictReady = false
            loading = false
            return@LaunchedEffect
        }
        dictReady = true
        val exact = dictRepository.getEntry(wordLower)
        entry = exact ?: dictRepository.search(query).firstOrNull()
        loading = false
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(query, fontWeight = FontWeight.Bold)
        },
        text = {
            when {
                loading -> {
                    Column(
                        modifier = Modifier.fillMaxWidth().heightIn(min = 80.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                }
                !dictReady -> {
                    Text("请先在「英汉词典」中下载词库后再查词。")
                }
                entry == null -> {
                    Text("未找到释义")
                }
                else -> {
                    val e = entry!!
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 360.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        e.phonetic?.takeIf { it.isNotBlank() }?.let {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        e.tag?.takeIf { it.isNotBlank() }?.let {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                        Text(
                            text = "释义",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                        )
                        e.translation.lineSequence().forEach { line ->
                            if (line.isNotBlank()) {
                                Text(text = line, style = MaterialTheme.typography.bodyLarge)
                            }
                        }
                        e.definition?.takeIf { it.isNotBlank() }?.let { def ->
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "英英",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                            )
                            def.lineSequence().forEach { line ->
                                if (line.isNotBlank()) {
                                    Text(text = line, style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            val e = entry
            when {
                loading || !dictReady || e == null -> {
                    TextButton(onClick = onDismiss) { Text("关闭") }
                }
                inBook -> {
                    TextButton(onClick = onDismiss) { Text("已在生词本") }
                }
                else -> {
                    TextButton(
                        onClick = {
                            scope.launch {
                                vocabBookRepository.add(
                                    word = e.word,
                                    wordLower = e.wordLower,
                                    phonetic = e.phonetic,
                                    translation = e.translation,
                                    sourceType = sourceType,
                                    sourceTitle = sourceTitle,
                                )
                                onDismiss()
                            }
                        },
                    ) { Text("加入生词本") }
                }
            }
        },
        dismissButton = {
            if (!loading && dictReady && entry != null) {
                TextButton(onClick = onDismiss) { Text("关闭") }
            }
        },
    )
}

internal fun buildLookupAnnotatedString(
    text: String,
    onWordClick: (String) -> Unit,
): AnnotatedString {
    val linkStyles = TextLinkStyles(
        style = SpanStyle(textDecoration = TextDecoration.None),
        pressedStyle = SpanStyle(
            textDecoration = TextDecoration.Underline,
            fontWeight = FontWeight.Medium,
        ),
    )
    return buildAnnotatedString {
        var i = 0
        while (i < text.length) {
            val ch = text[i]
            if (isWordChar(ch)) {
                val start = i
                while (i < text.length && isWordChar(text[i])) i++
                val raw = text.substring(start, i)
                val normalized = normalizeQuery(raw)
                if (normalized.any { it.isLetter() }) {
                    withLink(
                        LinkAnnotation.Clickable(
                            tag = "w-$start",
                            styles = linkStyles,
                            linkInteractionListener = { onWordClick(raw) },
                        ),
                    ) {
                        append(raw)
                    }
                } else {
                    append(raw)
                }
            } else {
                append(ch)
                i++
            }
        }
    }
}

private fun isWordChar(ch: Char): Boolean =
    ch.isLetter() || ch == '\'' || ch == '\u2019'
