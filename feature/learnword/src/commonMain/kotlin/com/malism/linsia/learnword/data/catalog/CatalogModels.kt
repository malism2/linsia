package com.malism.linsia.learnword.data.catalog

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class WordCatalogFile(
    val version: Int,
    val reops: List<WordBookMetaDto> = emptyList(),
)

@Serializable
data class WordBookMetaDto(
    val title: String,
    val name: String,
    val url: String? = null,
    @SerialName("file") val file: String? = null,
) {
    val downloadUrl: String get() = url?.takeIf { it.isNotBlank() } ?: file.orEmpty()
}
