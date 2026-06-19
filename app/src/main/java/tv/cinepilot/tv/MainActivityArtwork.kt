package tv.cinepilot.tv

import android.widget.ImageView
import tv.cinepilot.core.protocol.MediaBrowserClient
import tv.cinepilot.core.protocol.MediaItemSummary
import tv.cinepilot.core.protocol.MediaPerson
import tv.cinepilot.core.tv.TvWorkflowController
import tv.cinepilot.tv.runtime.ArtworkLoader
import tv.cinepilot.tv.runtime.ArtworkRequestFactory
import tv.cinepilot.tv.runtime.ArtworkTarget
import tv.cinepilot.tv.runtime.BitmapCache

internal class MainActivityArtwork(
    private val activity: MainActivity,
    private val workflowController: TvWorkflowController,
    mediaBrowserClient: MediaBrowserClient,
    bitmapCache: BitmapCache,
) {
    private val loader = ArtworkLoader(mediaBrowserClient, bitmapCache)
    val factory = ArtworkRequestFactory(mediaBrowserClient)

    fun shutdown() = loader.shutdown()

    fun loadPoster(target: ImageView, item: MediaItemSummary, width: Int, height: Int) {
        loader.loadPoster(activity, workflowController.state().authenticated(), target, item, width, height)
    }

    fun loadArtwork(target: ImageView, item: MediaItemSummary, targetType: ArtworkTarget, width: Int, height: Int) {
        loader.loadArtwork(activity, workflowController.state().authenticated(), target, item, targetType, width, height)
    }

    fun loadBackdrop(target: ImageView, item: MediaItemSummary, width: Int, height: Int) {
        loader.loadBackdrop(activity, workflowController.state().authenticated(), target, item, width, height)
    }

    fun loadPerson(target: ImageView, person: MediaPerson, width: Int, height: Int) {
        loader.loadPerson(activity, workflowController.state().authenticated(), target, person, width, height)
    }
}
