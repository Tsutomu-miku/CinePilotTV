package tv.cinepilot.tv.runtime

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import android.widget.ImageView
import androidx.activity.ComponentActivity
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors
import tv.cinepilot.core.protocol.AuthenticatedServer
import tv.cinepilot.core.protocol.MediaBrowserClient
import tv.cinepilot.core.protocol.MediaItemSummary

class ArtworkLoader(
    private val mediaBrowserClient: MediaBrowserClient,
) {
    private val executor = Executors.newFixedThreadPool(3)
    private val memoryCache = object : LruCache<String, Bitmap>(12 * 1024 * 1024) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount
    }

    fun loadPoster(
        owner: ComponentActivity,
        authenticated: AuthenticatedServer?,
        target: ImageView,
        item: MediaItemSummary,
        width: Int,
        height: Int,
    ) {
        if (authenticated == null || item.imageTags()["Primary"].isNullOrBlank()) {
            return
        }
        loadUrl(owner, target) {
            mediaBrowserClient.primaryImageUrl(authenticated, item, width, height)
        }
    }

    fun loadBackdrop(
        owner: ComponentActivity,
        authenticated: AuthenticatedServer?,
        target: ImageView,
        item: MediaItemSummary,
        width: Int,
        height: Int,
    ) {
        if (authenticated == null || !item.hasBackdropArtwork()) {
            return
        }
        loadUrl(owner, target) {
            mediaBrowserClient.backdropImageUrl(authenticated, item, width, height)
        }
    }

    fun shutdown() {
        executor.shutdownNow()
    }

    private fun loadUrl(owner: ComponentActivity, target: ImageView, imageUrl: () -> String) {
        val url = imageUrl()
        if (url.isBlank()) {
            return
        }
        target.tag = url
        memoryCache.get(url)?.let { cached ->
            target.setImageBitmap(cached)
            return
        }
        executor.execute {
            val bitmap = runCatching {
                val connection = URL(url).openConnection() as HttpURLConnection
                connection.connectTimeout = 3_000
                connection.readTimeout = 5_000
                try {
                    connection.inputStream.use(BitmapFactory::decodeStream)
                } finally {
                    connection.disconnect()
                }
            }.getOrNull()
            if (bitmap != null) {
                memoryCache.put(url, bitmap)
                owner.runOnUiThread {
                    if (!owner.isFinishing && !owner.isDestroyed && target.tag == url) {
                        target.setImageBitmap(bitmap)
                    }
                }
            }
        }
    }
}
