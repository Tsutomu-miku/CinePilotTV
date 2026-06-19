package tv.cinepilot.tv.runtime

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors

internal class PrimaryImageLoader(
    private val bitmapCache: BitmapCache,
) {
    private val executor = Executors.newFixedThreadPool(3)

    fun loadAsync(url: String, onLoaded: (Bitmap?) -> Unit) {
        executor.execute { onLoaded(load(url)) }
    }

    fun load(url: String): Bitmap? {
        if (url.isBlank()) return null
        bitmapCache.get(url)?.let { return it }
        return runCatching {
            val connection = URL(url).openConnection() as HttpURLConnection
            val connectTimeout = 3_000
            connection.connectTimeout = connectTimeout
            connection.readTimeout = 5_000
            try {
                connection.inputStream.use(BitmapFactory::decodeStream)
            } finally {
                connection.disconnect()
            }
        }.getOrNull()?.also { bitmapCache.put(url, it) }
    }

    fun shutdown() {
        executor.shutdownNow()
    }
}
