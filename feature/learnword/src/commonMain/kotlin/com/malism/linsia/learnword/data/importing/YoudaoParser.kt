package com.malism.linsia.learnword.data.importing

import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

private val json = Json { ignoreUnknownKeys = true; isLenient = true }

@Serializable
data class YoudaoWord(
    val wordRank: Int? = null,
    val headWord: String = "",
    val content: YoudaoOuterContent? = null,
    val bookId: String? = null,
)

@Serializable
data class YoudaoOuterContent(val word: YoudaoInnerWord? = null)

@Serializable
data class YoudaoInnerWord(val content: YoudaoDetail? = null)

@Serializable
data class YoudaoDetail(
    val ukphone: String? = null,
    val usphone: String? = null,
    val trans: List<YoudaoTrans> = emptyList(),
    val sentence: YoudaoSentenceBlock? = null,
)

@Serializable
data class YoudaoTrans(
    val tranCn: String? = null,
    val pos: String? = null,
)

@Serializable
data class YoudaoSentenceBlock(
    val sentences: List<YoudaoSentence> = emptyList(),
)

@Serializable
data class YoudaoSentence(
    val sContent: String? = null,
    val sCn: String? = null,
)

@Serializable
data class StoredSentence(
    val en: String,
    val cn: String = "",
)

data class ParsedWord(
    val headWord: String,
    val ordinal: Int,
    val ukPhone: String?,
    val usPhone: String?,
    val transJson: String,
    val sentenceJson: String?,
)

object YoudaoParser {
    fun parseConcatenated(text: String): List<ParsedWord> {
        return splitObjects(text).mapNotNull { raw ->
            runCatching { json.decodeFromString<YoudaoWord>(raw) }.getOrNull()
        }.mapIndexed { index, word ->
            val detail = word.content?.word?.content
            val trans = detail?.trans.orEmpty().mapNotNull { t ->
                val cn = t.tranCn?.trim().orEmpty()
                if (cn.isEmpty()) null
                else listOfNotNull(t.pos?.trim(), cn).joinToString(" ")
            }
            val sentences = detail?.sentence?.sentences.orEmpty().mapNotNull { s ->
                val en = s.sContent?.trim().orEmpty()
                if (en.isEmpty()) null else StoredSentence(en, s.sCn?.trim().orEmpty())
            }.take(3)
            val sentenceJson = sentences.takeIf { it.isNotEmpty() }?.let {
                json.encodeToString(it)
            }
            ParsedWord(
                headWord = word.headWord.ifBlank { return@mapIndexed null },
                ordinal = word.wordRank ?: (index + 1),
                ukPhone = detail?.ukphone,
                usPhone = detail?.usphone,
                transJson = json.encodeToString(ListSerializer(String.serializer()), trans),
                sentenceJson = sentenceJson,
            )
        }.filterNotNull()
    }

    internal fun splitObjects(text: String): List<String> {
        val result = ArrayList<String>()
        var depth = 0
        var start = -1
        var inString = false
        var escape = false
        for (i in text.indices) {
            val c = text[i]
            if (inString) {
                when {
                    escape -> escape = false
                    c == '\\' -> escape = true
                    c == '"' -> inString = false
                }
                continue
            }
            when (c) {
                '"' -> inString = true
                '{' -> {
                    if (depth == 0) start = i
                    depth++
                }
                '}' -> {
                    depth--
                    if (depth == 0 && start >= 0) {
                        result += text.substring(start, i + 1)
                        start = -1
                    }
                }
            }
        }
        return result
    }
}
