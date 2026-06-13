package tv.cinepilot.tv.runtime

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.widget.ImageView
import androidx.activity.ComponentActivity
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors
import tv.cinepilot.core.protocol.AuthenticatedServer
import tv.cinepilot.core.protocol.MediaBrowserClient
import tv.cinepilot.core.protocol.MediaItemSummary
import tv.cinepilot.core.protocol.PublicUserSummary
import tv.cinepilot.core.protocol.ServerIdentity

class PrimaryImageLoader(
    private val mediaBrowserClient: MediaBrowserClient,
    private val bitmapCache: BitmapCache,
) {
    private val executor = Executors.newFixedThreadPool(2)

    fun load(
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

    fun loadPublicUser(
        owner: ComponentActivity,
        server: ServerIdentity?,
        target: ImageView,
        user: PublicUserSummary,
        width: Int,
        height: Int,
    ) {
        if (server == null || user.primaryImageTag().isBlank()) {
            return
        }
        loadUrl(owner, target) {
            mediaBrowserClient.publicUserImageUrl(server, user, width, height)
        }
    }

    fun loadPublicUserBitmap(
        owner: ComponentActivity,
        server: ServerIdentity?,
        user: PublicUserSummary,
        width: Int,
        height: Int,
        onLoaded: (Bitmap?) -> Unit,
    ) {
        if (server == null || user.primaryImageTag().isBlank()) {
            onLoaded(null)
            return
        }
        loadBitmap(owner, mediaBrowserClient.publicUserImageUrl(server, user, width, height), onLoaded)
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
        bitmapCache.getMemory(url)?.let { cached ->
            target.setImageBitmap(cached)
            return
        }
        executor.execute {
            // Re-check cache: another worker may have written it while we were
            // sitting in the thread pool queue.
            bitmapCache.get(url)?.let { cached ->
                owner.runOnUiThread {
                    if (!owner.isFinishing && !owner.isDestroyed && target.tag == url) {
                        target.setImageBitmap(cached)
                    }
                }
                return@execute
            }
            runCatching {
                val connection = URL(url).openConnection() as HttpURLConnection
                connection.connectTimeout = 3_000
                connection.readTimeout = 5_000
                try {
                    connection.inputStream.use(BitmapFactory::decodeStream)
                } finally {
                    connection.disconnect()
                }
            }.getOrNull()?.let { bitmap ->
                bitmapCache.put(url, bitmap)
                owner.runOnUiThread {
                    if (!owner.isFinishing && !owner.isDestroyed && target.tag == url) {
                        target.setImageBitmap(bitmap)
                    }
                }
            }
        }
    }

    private fun loadBitmap(owner: ComponentActivity, url: String, onLoaded: (Bitmap?) -> Unit) {
        if (url.isBlank()) {
            onLoaded(null)
            return
        }
        bitmapCache.getMemory(url)?.let {
            onLoaded(it)
            return
        }
        executor.execute {
            bitmapCache.get(url)?.let { cached ->
                owner.runOnUiThread {
                    if (!owner.isFinishing && !owner.isDestroyed) {
                        onLoaded(cached)
                    }
                }
                return@execute
            }
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
                bitmapCache.put(url, bitmap)
            }
            owner.runOnUiThread {
                if (!owner.isFinishing && !owner.isDestroyed) {
                    onLoaded(bitmap)
                }
            }
        }
    }
}
