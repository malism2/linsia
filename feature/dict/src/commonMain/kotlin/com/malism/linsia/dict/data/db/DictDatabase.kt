package com.malism.linsia.dict.data.db

import androidx.sqlite.SQLiteConnection
import androidx.sqlite.SQLiteStatement
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.malism.linsia.dict.data.fs.dictDatabasePath
import com.malism.linsia.dict.domain.DictDefaults
import com.malism.linsia.dict.domain.DictEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Opens the prebuilt ECDICT SQLite file via BundledSQLiteDriver.
 * Room is not used so the shipped schema need not carry room_master_table.
 */
class DictDatabase {
    private val mutex = Mutex()
    private var connection: SQLiteConnection? = null

    val isOpen: Boolean get() = connection != null

    suspend fun openIfNeeded() = withContext(Dispatchers.IO) {
        mutex.withLock {
            if (connection != null) return@withLock
            val path = dictDatabasePath()
            connection = BundledSQLiteDriver().open(path)
        }
    }

    suspend fun close() = withContext(Dispatchers.IO) {
        mutex.withLock {
            connection?.close()
            connection = null
        }
    }

    suspend fun findExact(wordLower: String): DictEntry? = withContext(Dispatchers.IO) {
        mutex.withLock {
            val conn = connection ?: return@withLock null
            conn.prepare(
                """
                SELECT word, wordLower, phonetic, translation, definition, tag, frq
                FROM dict_entries
                WHERE wordLower = ?
                LIMIT 1
                """.trimIndent(),
            ).useStmt { stmt ->
                stmt.bindText(1, wordLower)
                if (stmt.step()) stmt.readEntry() else null
            }
        }
    }

    suspend fun searchPrefix(wordLower: String, limit: Int = DictDefaults.SearchLimit): List<DictEntry> =
        withContext(Dispatchers.IO) {
            mutex.withLock {
                val conn = connection ?: return@withLock emptyList()
                conn.prepare(
                    """
                    SELECT word, wordLower, phonetic, translation, definition, tag, frq
                    FROM dict_entries
                    WHERE wordLower LIKE ? || '%'
                    ORDER BY
                      CASE WHEN frq IS NULL THEN 1 ELSE 0 END,
                      frq ASC,
                      wordLower ASC
                    LIMIT ?
                    """.trimIndent(),
                ).useStmt { stmt ->
                    stmt.bindText(1, wordLower)
                    stmt.bindLong(2, limit.toLong())
                    buildList {
                        while (stmt.step()) {
                            add(stmt.readEntry())
                        }
                    }
                }
            }
        }
}

private inline fun <T> SQLiteStatement.useStmt(block: (SQLiteStatement) -> T): T {
    try {
        return block(this)
    } finally {
        close()
    }
}

private fun SQLiteStatement.readEntry(): DictEntry = DictEntry(
    word = getText(0),
    wordLower = getText(1),
    phonetic = getTextOrNull(2),
    translation = getText(3),
    definition = getTextOrNull(4),
    tag = getTextOrNull(5),
    frq = if (isNull(6)) null else getLong(6).toInt(),
)

private fun SQLiteStatement.getTextOrNull(index: Int): String? =
    if (isNull(index)) null else getText(index)
