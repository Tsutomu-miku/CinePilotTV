package tv.cinepilot.tv.runtime

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.security.MessageDigest

/**
 * Shared two-tier (memory + disk) bitmap cache used by every image loader in
 * the runtime. Keys are full image URLs (including width/height query params
 * so different target sizes never alias).
 *
 * The disk cache is intentionally small (100 MB, ~enough for a full TV home
 * wall plus a few detail pages) and uses a dirt-simple last-modified LRU
 * policy: eviction walks the directory and removes the oldest files until we
 * are below the cap. No journal, no multi-process safety, which is fine for
 * this single-process TV app.
 *
 * All mutating operations on the disk layer are internally synchronized so
 * ArtworkLoader and other image loaders can share one instance safely.
 */
class BitmapCache private constructor(
    private val diskDir: File,
    maxMemoryBytes: Int,
    private val maxDiskBytes: Long,
) {
    private val memoryCache = object : LruCache<String, Bitmap>(maxMemoryBytes) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount
    }
    private val diskLock = Any()

    fun getMemory(url: String): Bitmap? {
        if (url.isBlank()) return null
        return memoryCache.get(url)
    }

    fun get(url: String): Bitmap? {
        if (url.isBlank()) return null
        memoryCache.get(url)?.let { return it }
        val file = synchronized(diskLock) { fileFor(url) }
        if (!file.isFile || file.length() == 0L) return null
        val bitmap = runCatching {
            file.inputStream().buffered().use { BitmapFactory.decodeStream(it) }
        }.getOrNull() ?: return null
        // Update last-modified so we keep recently-read files longer.
        synchronized(diskLock) { file.setLastModified(System.currentTimeMillis()) }
        memoryCache.put(url, bitmap)
        return bitmap
    }

    fun put(url: String, bitmap: Bitmap) {
        if (url.isBlank()) return
        memoryCache.put(url, bitmap)
        synchronized(diskLock) {
            val file = fileFor(url)
            val parent = file.parentFile ?: return@synchronized
            if (!parent.exists()) parent.mkdirs()
            val written = runCatching {
                FileOutputStream(file).use { out ->
                    // Artwork is photo-like and usually opaque; JPEG avoids
                    // the heavy PNG compression cost while scrolling a TV wall.
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 88, out)
                }
                file.length()
            }.getOrDefault(0L)
            if (written <= 0L) {
                runCatching { file.delete() }
                return@synchronized
            }
            pruneDisk()
        }
    }

    fun clearMemory() {
        memoryCache.evictAll()
    }

    fun clearDisk() {
        synchronized(diskLock) {
            diskDir.listFiles()?.forEach { it.delete() }
        }
    }

    /**
     * Returns true if a non-empty on-disk entry already exists for [url].
     * Useful for short-circuiting redundant network fetches from multiple
     * callers that race on the same URL.
     */
    fun hasOnDisk(url: String): Boolean {
        if (url.isBlank()) return false
        synchronized(diskLock) {
            val f = fileFor(url)
            return f.isFile && f.length() > 0L
        }
    }

    private fun pruneDisk() {
        val files = diskDir.listFiles() ?: return
        val total = files.sumOf { it.length() }
        if (total <= maxDiskBytes) return
        // oldest first
        files.sortBy { it.lastModified() }
        var remaining = total
        for (f in files) {
            if (remaining <= maxDiskBytes) break
            val size = f.length()
            if (f.delete()) remaining -= size
        }
    }

    private fun fileFor(url: String): File {
        val hex = runCatching {
            val digest = MessageDigest.getInstance("SHA-256")
            val hashBytes = digest.digest(url.toByteArray(Charsets.UTF_8))
            buildString {
                for (b in hashBytes) append("%02x".format(b))
            }
        }.getOrElse {
            // Fallback to hashCode() if SHA-256 is unavailable (should not
            // happen on Android, but keeps us deterministic on weird runtimes).
            Integer.toHexString(url.hashCode()) + Integer.toHexString(url.length)
        }
        val sub = if (hex.length >= 3) hex.substring(0, 2) else "xx"
        return File(File(diskDir, sub), hex)
    }

    companion object {
        private const val DEFAULT_MAX_MEMORY_BYTES = 64 * 1024 * 1024
        private const val DEFAULT_MAX_DISK_BYTES = 100L * 1024 * 1024
        private const val DIR_NAME = "bitmap_cache"

        fun create(context: Context): BitmapCache {
            val appContext = context.applicationContext
            val maxMem = runCatching {
                // Target up to 1/8 of the app-specific memory class, clamped
                // between 16 MB and the default 64 MB cap.
                val am = appContext.getSystemService(Context.ACTIVITY_SERVICE)
                    as? android.app.ActivityManager
                val memClassMb = am?.memoryClass ?: 32
                ((memClassMb * 1024 * 1024) / 8)
                    .coerceIn(16 * 1024 * 1024, DEFAULT_MAX_MEMORY_BYTES)
            }.getOrDefault(DEFAULT_MAX_MEMORY_BYTES)
            val diskDir = File(appContext.cacheDir, DIR_NAME).apply {
                if (!exists()) mkdirs()
            }
            return BitmapCache(diskDir, maxMem, DEFAULT_MAX_DISK_BYTES)
        }

        /** Entry point usable from tests without a real Context. */
        fun createForTest(
            diskDir: File,
            maxMemoryBytes: Int = DEFAULT_MAX_MEMORY_BYTES,
            maxDiskBytes: Long = DEFAULT_MAX_DISK_BYTES,
        ): BitmapCache {
            if (!diskDir.exists()) diskDir.mkdirs()
            return BitmapCache(diskDir, maxMemoryBytes, maxDiskBytes)
        }
    }
}
