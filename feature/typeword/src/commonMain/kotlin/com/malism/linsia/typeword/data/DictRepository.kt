package com.malism.linsia.typeword.data

import com.malism.linsia.typeword.domain.TypewordDefaults
import com.malism.linsia.typeword.domain.WordDict
import com.malism.linsia.typeword.domain.WordDictFile
import com.malism.linsia.typeword.generated.Res
import kotlinx.serialization.json.Json
import org.jetbrains.compose.resources.ExperimentalResourceApi

class DictRepository {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    private var cache: List<WordDict> = emptyList()

    suspend fun all(): List<WordDict> {
        if (cache.isEmpty()) cache = TypewordDefaults.DictFiles.map { loadFile(it) }
        return cache
    }

    suspend fun byId(id: String): WordDict {
        val dicts = all()
        return dicts.firstOrNull { it.id == id } ?: dicts.first()
    }

    @OptIn(ExperimentalResourceApi::class)
    private suspend fun loadFile(name: String): WordDict {
        val bytes = Res.readBytes("files/$name")
        val file = json.decodeFromString(WordDictFile.serializer(), bytes.decodeToString())
        return WordDict(
            id = file.id,
            title = file.title,
            words = file.words.filter { it.name.isNotBlank() },
        )
    }
}
