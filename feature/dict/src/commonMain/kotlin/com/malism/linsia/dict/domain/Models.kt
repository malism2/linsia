package com.malism.linsia.dict.domain

import kotlinx.serialization.Serializable

object DictDefaults {
    const val RemoteCatalogUrl =
        "https://objectstorage.uk-london-1.oraclecloud.com/n/lr3rsi02p2te/b/bucket-linsia/o/dict_dist.json"
    const val DbFileName = "ecdict.db"
    const val ZipEntryName = "ecdict.db"
    const val SettingsVersionKey = "dict.localVersion"
    const val SettingsReadyKey = "dict.ready"
    const val SearchLimit = 30
    const val SearchDebounceMs = 250L
}

@Serializable
data class DictCatalogFile(
    val version: Int,
    val url: String,
    val entryCount: Int = 0,
    val sizeBytes: Long = 0L,
)

data class DictEntry(
    val word: String,
    val wordLower: String,
    val phonetic: String?,
    val translation: String,
    val definition: String?,
    val tag: String?,
    val frq: Int?,
)

fun normalizeQuery(raw: String): String {
    val trimmed = raw.trim().lowercase()
    if (trimmed.isEmpty()) return ""
    return trimmed.trim { ch ->
        ch in ".,!?;:\"'()[]{}«»\u201C\u201D\u2018\u2019、。，！？"
    }
}
