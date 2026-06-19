package tv.cinepilot.tv.runtime

import android.graphics.Bitmap
import android.widget.ImageView
import androidx.activity.ComponentActivity
import tv.cinepilot.core.protocol.AuthenticatedServer
import tv.cinepilot.core.protocol.MediaBrowserClient
import tv.cinepilot.core.protocol.MediaItemSummary
import tv.cinepilot.core.protocol.MediaPerson

class ArtworkLoader(
    private val mediaBrowserClient: MediaBrowserClient,
    private val bitmapCache: BitmapCache,
) {
    private val primaryImageLoader = PrimaryImageLoader(bitmapCache)

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

    fun loadArtwork(
        owner: ComponentActivity,
        authenticated: AuthenticatedServer?,
        target: ImageView,
        item: MediaItemSummary,
        artworkTarget: ArtworkTarget,
        width: Int,
        height: Int,
    ) {
        when (artworkTarget) {
            ArtworkTarget.POSTER -> loadPoster(owner, authenticated, target, item, width, height)
            ArtworkTarget.LANDSCAPE,
            ArtworkTarget.BACKDROP,
            ArtworkTarget.COLLECTION -> loadBackdrop(owner, authenticated, target, item, width, height)
        }
    }

    fun loadPerson(
        owner: ComponentActivity,
        authenticated: AuthenticatedServer?,
        target: ImageView,
        person: MediaPerson,
        width: Int,
        height: Int,
    ) {
        if (authenticated == null || person.primaryImageTag().isBlank()) {
            return
        }
        loadUrl(owner, target) {
            mediaBrowserClient.personImageUrl(authenticated, person, width, height)
        }
    }

    fun shutdown() {
        primaryImageLoader.shutdown()
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
        primaryImageLoader.loadAsync(url) { bitmap ->
            if (bitmap != null) {
                owner.runOnUiThread {
                    if (!owner.isFinishing && !owner.isDestroyed && target.tag == url) {
                        target.setImageBitmap(bitmap)
                    }
                }
            }
        }
    }
}
