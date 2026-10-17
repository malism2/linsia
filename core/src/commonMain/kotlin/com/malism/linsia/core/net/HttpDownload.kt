package com.malism.linsia.core.net

import com.malism.linsia.core.io.ensureParentDir
import io.ktor.client.HttpClient
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.prepareGet
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.isSuccess
import io.ktor.utils.io.readAvailable
import okio.FileSystem
import okio.Path.Companion.toPath
import okio.buffer
import okio.use

/**
 * Streams an HTTP body to [path] in 64KiB chunks — never buffers the whole response.
 */
suspend fun HttpClient.downloadToFile(
    url: String,
    path: String,
    onProgress: (received: Long, total: Long?) -> Unit = { _, _ -> },
    block: HttpRequestBuilder.() -> Unit = {},
) {
    ensureParentDir(path)
    val dest = path.toPath()
    if (FileSystem.SYSTEM.exists(dest)) {
        FileSystem.SYSTEM.delete(dest)
    }

    prepareGet(url) {
        block()
    }.execute { response ->
        if (!response.status.isSuccess()) {
            error("下载失败 HTTP ${response.status.value}")
        }
        val total = response.headers["Content-Length"]?.toLongOrNull()
        val channel = response.bodyAsChannel()
        val buffer = ByteArray(64 * 1024)
        var received = 0L
        FileSystem.SYSTEM.sink(dest).buffer().use { sink ->
            while (!channel.isClosedForRead) {
                val n = channel.readAvailable(buffer, 0, buffer.size)
                if (n < 0) break
                if (n == 0) continue
                sink.write(buffer, 0, n)
                received += n
                onProgress(received, total)
            }
            sink.flush()
        }
        if (received == 0L) {
            error("下载内容为空")
        }
    }
}
