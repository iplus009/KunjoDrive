package com.novo.drive.core.media

class BufferedMediaByteSource(
    private val source: MediaByteSource,
    private val chunkSize: Int = DEFAULT_CHUNK_SIZE
) : MediaByteSource {

    private var buffer = ByteArray(0)
    private var bufferStart = -1L
    private var knownSize: Long? = null

    override suspend fun size(): Long = source.size().also { knownSize = it }

    override suspend fun read(position: Long, count: Int): ByteArray {
        if (count <= 0 || position < 0L) return ByteArray(0)
        if (holds(position)) {
            val from = (position - bufferStart).toInt()
            val available = buffer.size - from
            if (available >= count) {
                return buffer.copyOfRange(from, from + count)
            }
            // Tail of cached chunk doesn't cover the full count: copy tail and stream forward up to EOF
            val out = java.io.ByteArrayOutputStream(count)
            out.write(buffer, from, available)
            var cursor = position + available
            var remaining = count - available
            val totalSize = knownSize ?: runCatching { source.size() }.getOrNull()?.also { knownSize = it }
            while (remaining > 0) {
                if (totalSize != null && cursor >= totalSize) break
                val nextChunk = source.read(cursor, maxOf(remaining, chunkSize))
                if (nextChunk.isEmpty()) break
                buffer = nextChunk
                bufferStart = cursor
                val take = minOf(remaining, nextChunk.size)
                out.write(nextChunk, 0, take)
                cursor += take
                remaining -= take
            }
            return out.toByteArray()
        }
        val chunk = source.read(position, maxOf(count, chunkSize))
        if (chunk.isEmpty()) return chunk
        buffer = chunk
        bufferStart = position
        return buffer.copyOfRange(0, minOf(count, buffer.size))
    }

    private fun holds(position: Long): Boolean =
        bufferStart >= 0 && position >= bufferStart && position < bufferStart + buffer.size

    override fun close() {
        buffer = ByteArray(0)
        bufferStart = -1L
        source.close()
    }

    private companion object {
        const val DEFAULT_CHUNK_SIZE = 4 * 1024 * 1024
    }
}
