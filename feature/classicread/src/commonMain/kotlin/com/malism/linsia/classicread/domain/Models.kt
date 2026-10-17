package com.malism.linsia.classicread.domain

import kotlinx.serialization.Serializable

object ClassicReadDefaults {
    const val RemoteCatalogUrl =
        "https://objectstorage.uk-london-1.oraclecloud.com/n/lr3rsi02p2te/b/bucket-linsia/o/se_books.json"
    const val FetchFailedMessage = "本书暂时无法获取"
    const val UnavailableMessage = "本书暂未提供下载"
    const val UserAgent =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36"
}

@Serializable
data class SeBooksCatalogFile(
    val version: Int,
    val updatedAt: String = "",
    val source: String = "standardebooks.org/ebooks",
    val books: List<SeBookDto> = emptyList(),
)

@Serializable
data class SeBookDto(
    val id: String,
    val rank: Int,
    val title: String,
    val author: String,
    val coverUrl: String = "",
    val epubUrl: String = "",
)

data class ClassicBook(
    val id: String,
    val rank: Int,
    val title: String,
    val author: String,
    val coverUrl: String,
    val epubUrl: String,
    val downloaded: Boolean,
    val chapterCount: Int,
    val progressChapterIndex: Int?,
)

data class ClassicChapterMeta(
    val chapterIndex: Int,
    val title: String,
)

data class ClassicChapter(
    val bookId: String,
    val chapterIndex: Int,
    val title: String,
    val body: String,
)

data class ParsedChapter(
    val title: String,
    val body: String,
)

/** SE download interstitial requires `?source=download` to return the real EPUB bytes. */
fun String.ensureSeEpubDownloadUrl(): String {
    if (isBlank() || !contains("standardebooks.org")) return this
    if (contains("source=download")) return this
    val base = substringBefore('?')
    return "$base?source=download"
}
