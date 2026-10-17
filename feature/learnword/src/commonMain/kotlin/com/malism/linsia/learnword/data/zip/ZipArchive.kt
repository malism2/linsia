package com.malism.linsia.learnword.data.zip

internal expect fun inflateRawDeflate(input: ByteArray): ByteArray

object ZipArchive {
    fun extract(zipBytes: ByteArray): Map<String, ByteArray> {
        val eocd = findEocd(zipBytes) ?: error("Invalid zip: EOCD not found")
        val cdOffset = eocd.cdOffset
        val cdSize = eocd.cdSize
        val result = LinkedHashMap<String, ByteArray>()
        var pos = cdOffset
        val cdEnd = cdOffset + cdSize
        while (pos + 46 <= cdEnd && pos + 4 <= zipBytes.size) {
            if (readInt(zipBytes, pos) != 0x02014b50) break
            val method = readShort(zipBytes, pos + 10)
            val compSize = readInt(zipBytes, pos + 20)
            val nameLen = readShort(zipBytes, pos + 28)
            val extraLen = readShort(zipBytes, pos + 30)
            val commentLen = readShort(zipBytes, pos + 32)
            val localOffset = readInt(zipBytes, pos + 42)
            val name = zipBytes.decodeToString(pos + 46, pos + 46 + nameLen)
            val data = readLocalFile(zipBytes, localOffset, method, compSize)
            if (!name.endsWith("/")) {
                result[name] = data
            }
            pos += 46 + nameLen + extraLen + commentLen
        }
        return result
    }

    private fun readLocalFile(
        bytes: ByteArray,
        localOffset: Int,
        method: Int,
        compSize: Int,
    ): ByteArray {
        require(readInt(bytes, localOffset) == 0x04034b50) { "Bad local header" }
        val nameLen = readShort(bytes, localOffset + 26)
        val extraLen = readShort(bytes, localOffset + 28)
        val dataStart = localOffset + 30 + nameLen + extraLen
        val compressed = bytes.copyOfRange(dataStart, dataStart + compSize)
        return when (method) {
            0 -> compressed
            8 -> inflateRawDeflate(compressed)
            else -> error("Unsupported zip method $method")
        }
    }

    private data class Eocd(val cdOffset: Int, val cdSize: Int)

    private fun findEocd(bytes: ByteArray): Eocd? {
        val min = (bytes.size - 22 - 0xFFFF).coerceAtLeast(0)
        for (i in bytes.size - 22 downTo min) {
            if (readInt(bytes, i) == 0x06054b50) {
                val cdSize = readInt(bytes, i + 12)
                val cdOffset = readInt(bytes, i + 16)
                return Eocd(cdOffset, cdSize)
            }
        }
        return null
    }

    private fun readInt(bytes: ByteArray, offset: Int): Int {
        val b0 = bytes[offset].toInt() and 0xFF
        val b1 = bytes[offset + 1].toInt() and 0xFF
        val b2 = bytes[offset + 2].toInt() and 0xFF
        val b3 = bytes[offset + 3].toInt() and 0xFF
        return b0 or (b1 shl 8) or (b2 shl 16) or (b3 shl 24)
    }

    private fun readShort(bytes: ByteArray, offset: Int): Int {
        val b0 = bytes[offset].toInt() and 0xFF
        val b1 = bytes[offset + 1].toInt() and 0xFF
        return b0 or (b1 shl 8)
    }
}
