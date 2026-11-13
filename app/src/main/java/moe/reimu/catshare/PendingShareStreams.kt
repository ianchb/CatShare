package moe.reimu.catshare

import android.content.Context
import moe.reimu.catshare.models.FileInfo
import java.io.InputStream

/**
 * Keeps externally shared content streams alive between ShareActivity and P2pSenderService.
 *
 * Both components live in the same process. Passing a stream or ParcelFileDescriptor through the
 * service Intent is unnecessary and can fail for external document providers.
 */
object PendingShareStreams {
    private val pending = mutableMapOf<Int, MutableList<InputStream?>>()
    private val lock = Any()

    fun prepare(context: Context, taskId: Int, files: List<FileInfo>): Boolean {
        val streams = mutableListOf<InputStream?>()
        try {
            for (file in files) {
                streams += if (file.textContent == null) {
                    context.contentResolver.openInputStream(file.uri)
                        ?: throw IllegalStateException("Unable to open ${file.uri}")
                } else {
                    null
                }
            }
        } catch (_: Throwable) {
            close(streams)
            return false
        }

        synchronized(lock) {
            close(pending.remove(taskId))
            pending[taskId] = streams
        }
        return true
    }

    fun open(taskId: Int, index: Int): InputStream? = synchronized(lock) {
        pending[taskId]?.getOrNull(index)
    }

    fun discard(taskId: Int) {
        val streams = synchronized(lock) {
            pending.remove(taskId)
        }
        close(streams)
    }

    private fun close(streams: List<InputStream?>?) {
        streams?.forEach { stream ->
            runCatching { stream?.close() }
        }
    }
}
