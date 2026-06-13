package tv.cinepilot.tv.compose.artwork

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import tv.cinepilot.core.protocol.AuthenticatedServer
import tv.cinepilot.core.protocol.MediaItemSummary
import tv.cinepilot.core.protocol.PublicUserSummary
import tv.cinepilot.core.protocol.ServerIdentity
import tv.cinepilot.tv.runtime.ArtworkRequestFactory
import tv.cinepilot.tv.runtime.ArtworkRequestSpec
import tv.cinepilot.tv.runtime.ArtworkTarget

@Composable
fun rememberArtworkRequest(
    factory: ArtworkRequestFactory,
    authenticated: AuthenticatedServer?,
    item: MediaItemSummary?,
    target: ArtworkTarget,
    width: Int,
    height: Int,
) = remember(factory, authenticated, item, target, width, height) {
    factory.artwork(
        authenticated = authenticated,
        item = item,
        target = target,
        width = width,
        height = height,
    )
}

@Composable
fun rememberPublicUserRequest(
    factory: ArtworkRequestFactory,
    server: ServerIdentity?,
    user: PublicUserSummary?,
    width: Int,
    height: Int,
) = remember(factory, server, user, width, height) {
    factory.publicUser(
        server = server,
        user = user,
        width = width,
        height = height,
    )
}

@Composable
fun CinePilotAsyncImage(
    request: ArtworkRequestSpec?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
) {
    if (request == null) return
    val context = LocalContext.current
    val model = remember(context, request) {
        ImageRequest.Builder(context)
            .data(request.url)
            .memoryCacheKey(request.cacheKey)
            .diskCacheKey(request.cacheKey)
            .size(request.width, request.height)
            .build()
    }
    AsyncImage(
        model = model,
        contentDescription = contentDescription,
        contentScale = contentScale,
        modifier = modifier,
    )
}
