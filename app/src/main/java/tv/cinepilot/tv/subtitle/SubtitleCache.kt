package tv.cinepilot.tv.subtitle

import android.content.Context
import tv.cinepilot.plugin.spi.SubtitleSearchResult
import java.io.File

/**
 * Local cache for subtitle files downloaded from [SubtitleSearchPlugin]s.
 *
 * Subtitles are stored as files in the app's cache directory, keyed by
 * (serverId, itemId, providerId, subtitleId). Callers are responsible for
 * managing the lifecycle of selected subtitles — the cache never evicts on
 * its own. Use [clearForItem] or [clearAll] to free space.
 */
class SubtitleCache(context: Context) {

    private val cacheDir: File = File(context.cacheDir, "subtitles").apply { mkdirs() }

    data class CachedSubtitle(
        val result: SubtitleSearchResult,
        val file: File,
    )

    /** Returns the cached file for a subtitle, or null if not downloaded. */
    fun get(
        serverId: String,
        itemId: String,
        result: SubtitleSearchResult,
    ): CachedSubtitle? {
        val file = fileFor(serverId, itemId, result)
        return if (file.exists() && file.length() > 0) {
            CachedSubtitle(result, file)
        } else {
            null
        }
    }

    /** Saves subtitle bytes to the cache and returns the cached file. */
    fun put(
        serverId: String,
        itemId: String,
        result: SubtitleSearchResult,
        bytes: ByteArray,
    ): CachedSubtitle {
        val file = fileFor(serverId, itemId, result)
        file.parentFile?.mkdirs()
        file.writeBytes(bytes)
        return CachedSubtitle(result, file)
    }

    /** Returns true if this subtitle has already been downloaded. */
    fun has(serverId: String, itemId: String, result: SubtitleSearchResult): Boolean =
        fileFor(serverId, itemId, result).let { it.exists() && it.length() > 0 }

    /** Removes all cached subtitles for a given item. */
    fun clearForItem(serverId: String, itemId: String) {
        itemDir(serverId, itemId)?.deleteRecursively()
    }

    /** Clears the entire subtitle cache. */
    fun clearAll() {
        cacheDir.deleteRecursively()
        cacheDir.mkdirs()
    }

    private fun fileFor(
        serverId: String,
        itemId: String,
        result: SubtitleSearchResult,
    ): File {
        val safeServer = serverId.sanitize()
        val safeItem = itemId.sanitize()
        val safeProvider = result.providerId().sanitize()
        val safeSubId = result.id().sanitize()
        val ext = extensionFor(result.format())
        return File(cacheDir, "$safeServer/$safeItem/$safeProvider/$safeSubId.$ext")
    }

    private fun itemDir(serverId: String, itemId: String): File? {
        val dir = File(cacheDir, "${serverId.sanitize()}/${itemId.sanitize()}")
        return if (dir.exists()) dir else null
    }

    private fun String.sanitize(): String =
        replace(Regex("[^a-zA-Z0-9._-]"), "_").ifBlank { "_" }

    private fun extensionFor(format: SubtitleSearchResult.Format): String =
        when (format) {
            SubtitleSearchResult.Format.SRT -> "srt"
            SubtitleSearchResult.Format.ASS -> "ass"
            SubtitleSearchResult.Format.SSA -> "ssa"
            SubtitleSearchResult.Format.VTT -> "vtt"
            SubtitleSearchResult.Format.PGS -> "sup"
            SubtitleSearchResult.Format.UNKNOWN -> "sub"
        }
}
