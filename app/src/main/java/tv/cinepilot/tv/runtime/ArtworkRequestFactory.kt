package tv.cinepilot.tv.runtime

import tv.cinepilot.core.protocol.AuthenticatedServer
import tv.cinepilot.core.protocol.MediaBrowserClient
import tv.cinepilot.core.protocol.MediaItemSummary
import tv.cinepilot.core.protocol.PublicUserSummary
import tv.cinepilot.core.protocol.ServerIdentity

data class ArtworkRequestSpec(
    val url: String,
    val cacheKey: String,
    val width: Int,
    val height: Int,
)

class ArtworkRequestFactory(
    private val mediaBrowserClient: MediaBrowserClient,
) {
    fun artwork(
        authenticated: AuthenticatedServer?,
        item: MediaItemSummary?,
        target: ArtworkTarget,
        width: Int,
        height: Int,
    ): ArtworkRequestSpec? {
        val server = authenticated?.server() ?: return null
        val mediaItem = item ?: return null
        val url = when (target) {
            ArtworkTarget.POSTER -> {
                if (mediaItem.imageTags()["Primary"].isNullOrBlank()) return null
                mediaBrowserClient.primaryImageUrl(authenticated, mediaItem, width, height)
            }
            ArtworkTarget.LANDSCAPE,
            ArtworkTarget.BACKDROP,
            ArtworkTarget.COLLECTION -> {
                if (!mediaItem.hasBackdropArtwork()) return null
                mediaBrowserClient.backdropImageUrl(authenticated, mediaItem, width, height)
            }
        }
        if (url.isBlank()) return null
        return ArtworkRequestSpec(
            url = url,
            cacheKey = buildCacheKey(
                serverId = server.serverId(),
                id = mediaItem.id(),
                imageType = imageTypeFor(mediaItem, target),
                tag = tagFor(mediaItem, target),
                width = width,
                height = height,
            ),
            width = width,
            height = height,
        )
    }

    fun publicUser(
        server: ServerIdentity?,
        user: PublicUserSummary?,
        width: Int,
        height: Int,
    ): ArtworkRequestSpec? {
        val identity = server ?: return null
        val publicUser = user ?: return null
        if (publicUser.primaryImageTag().isBlank()) return null
        val url = mediaBrowserClient.publicUserImageUrl(identity, publicUser, width, height)
        if (url.isBlank()) return null
        return ArtworkRequestSpec(
            url = url,
            cacheKey = buildCacheKey(
                serverId = identity.serverId(),
                id = publicUser.id(),
                imageType = "UserPrimary",
                tag = publicUser.primaryImageTag(),
                width = width,
                height = height,
            ),
            width = width,
            height = height,
        )
    }

    private fun imageTypeFor(item: MediaItemSummary, target: ArtworkTarget): String {
        return when (target) {
            ArtworkTarget.POSTER -> "Primary"
            ArtworkTarget.LANDSCAPE,
            ArtworkTarget.BACKDROP,
            ArtworkTarget.COLLECTION -> when {
                item.backdropImageTags().isNotEmpty() -> "Backdrop"
                item.imageTags().containsKey("Thumb") -> "Thumb"
                else -> "Primary"
            }
        }
    }

    private fun tagFor(item: MediaItemSummary, target: ArtworkTarget): String {
        return when (target) {
            ArtworkTarget.POSTER -> item.imageTags()["Primary"].orEmpty()
            ArtworkTarget.LANDSCAPE,
            ArtworkTarget.BACKDROP,
            ArtworkTarget.COLLECTION -> item.backdropImageTags().firstOrNull()
                ?: item.imageTags()["Thumb"]
                ?: item.imageTags()["Primary"]
                ?: ""
        }
    }

    private fun buildCacheKey(
        serverId: String,
        id: String,
        imageType: String,
        tag: String,
        width: Int,
        height: Int,
    ): String = listOf(serverId, id, imageType, tag, width, height).joinToString("|")
}
