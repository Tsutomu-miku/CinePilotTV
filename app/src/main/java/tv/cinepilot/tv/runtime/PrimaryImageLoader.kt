package tv.cinepilot.tv.runtime

import android.graphics.BitmapFactory
import android.widget.ImageView
import androidx.activity.ComponentActivity
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors
import tv.cinepilot.core.protocol.AuthenticatedServer
import tv.cinepilot.core.protocol.MediaBrowserClient
import tv.cinepilot.core.protocol.MediaItemSummary

class PrimaryImageLoader(
    private val mediaBrowserClient: MediaBrowserClient,
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
        executor.execute {
            runCatching {
                val imageUrl = mediaBrowserClient.primaryImageUrl(authenticated, item, width, height)
                val connection = URL(imageUrl).openConnection() as HttpURLConnection
                connection.connectTimeout = 3_000
                connection.readTimeout = 5_000
                try {
                    connection.inputStream.use(BitmapFactory::decodeStream)
                } finally {
                    connection.disconnect()
                }
            }.getOrNull()?.let { bitmap ->
                owner.runOnUiThread {
                    if (!owner.isFinishing && !owner.isDestroyed) {
                        target.setImageBitmap(bitmap)
                    }
                }
            }
        }
    }

    fun shutdown() {
        executor.shutdownNow()
    }
}
