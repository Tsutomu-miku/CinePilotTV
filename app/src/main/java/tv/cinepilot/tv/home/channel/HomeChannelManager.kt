package tv.cinepilot.tv.home.channel

import android.annotation.SuppressLint
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.media.tv.TvContract
import android.net.Uri
import androidx.tvprovider.media.tv.PreviewChannel
import androidx.tvprovider.media.tv.PreviewProgram
import androidx.tvprovider.media.tv.TvContractCompat
import tv.cinepilot.core.protocol.AuthenticatedServer
import tv.cinepilot.core.protocol.MediaBrowserClient
import tv.cinepilot.core.protocol.MediaItemSummary
import tv.cinepilot.tv.home.HomeSettings
import tv.cinepilot.tv.home.HomeSettingsStore

/**
 * Creates, updates, and removes Android TV preview channels for CinePilot.
 *
 * Two channels are managed:
 *   - Continue Watching (from resumeItems)
 *   - Next Up (from nextUpItems)
 *
 * Writing to [TvContract] requires signature-level
 * `com.android.providers.tv.permission.WRITE_EPG_DATA` on stock launchers.
 * All provider interactions are guarded with try/catch so the rest of the
 * app stays functional on platforms that lock the EPG provider down.
 */
@SuppressLint("RestrictedApi")
class HomeChannelManager(
    private val context: Context,
    private val mediaBrowserClient: MediaBrowserClient,
    private val homeSettingsStore: HomeSettingsStore,
) {
    fun isAvailable(): Boolean = runCatching {
        if (!hasTvProviderWriteAccess()) return@runCatching false
        context.contentResolver.acquireContentProviderClient(TvContract.AUTHORITY)
            ?.use { true }
            ?: false
    }.getOrDefault(false)

    fun sync(settings: HomeSettings, authenticated: AuthenticatedServer) {
        if (!isAvailable()) return
        runCatching {
            val rowLimit = 10
            if (settings.showContinueWatchingInLauncher) {
                val channelId = ensureChannel(CONTINUE_WATCHING, "继续观看")
                replacePrograms(
                    channelId,
                    authenticated,
                    mediaBrowserClient.resumeItems(authenticated, rowLimit).items(),
                )
            } else {
                deleteChannel(CONTINUE_WATCHING)
            }
            if (settings.showNextUpInLauncher) {
                val channelId = ensureChannel(NEXT_UP, "下一集")
                replacePrograms(
                    channelId,
                    authenticated,
                    mediaBrowserClient.nextUpItems(authenticated, rowLimit).items(),
                )
            } else {
                deleteChannel(NEXT_UP)
            }
        }
    }

    private fun ensureChannel(internalId: String, displayName: String): Long {
        val resolver = context.contentResolver
        val existing = findChannelByInternalId(internalId)
        if (existing != null) return existing
        val builder = PreviewChannel.Builder()
            .setDisplayName(displayName)
            .setAppLinkIntentUri(Uri.parse("cinepilot://home"))
            .setInternalProviderId(internalId)
        // PreviewChannel rows insert through the Channels URI.
        val uri = resolver.insert(
            TvContractCompat.Channels.CONTENT_URI,
            builder.build().toContentValues(),
        ) ?: error("Preview channel insert returned null")
        return ContentUris.parseId(uri)
    }

    private fun findChannelByInternalId(internalId: String): Long? {
        val resolver = context.contentResolver
        val projection = arrayOf(
            TvContractCompat.Channels._ID,
            TvContractCompat.Channels.COLUMN_INTERNAL_PROVIDER_ID,
        )
        resolver.query(
            TvContractCompat.Channels.CONTENT_URI,
            projection,
            null,
            null,
            null,
        ).use { cursor ->
            if (cursor == null) return null
            while (cursor.moveToNext()) {
                if (cursor.getString(1) == internalId) {
                    return cursor.getLong(0)
                }
            }
        }
        return null
    }

    private fun deleteChannel(internalId: String) {
        val channelId = findChannelByInternalId(internalId) ?: return
        context.contentResolver.delete(
            ContentUris.withAppendedId(TvContractCompat.Channels.CONTENT_URI, channelId),
            null, null,
        )
    }

    private fun replacePrograms(
        channelId: Long,
        authenticated: AuthenticatedServer,
        items: List<MediaItemSummary>,
    ) {
        val resolver = context.contentResolver
        val channelUri = TvContractCompat.buildPreviewProgramsUriForChannel(channelId)
        resolver.delete(channelUri, null, null)
        val posterWidth = 480
        val posterHeight = 720
        val valuesBatch = items.map { item ->
            val posterUrl = mediaBrowserClient.primaryImageUrl(
                authenticated, item, posterWidth, posterHeight,
            )
            val deepLink = "cinepilot://play?item=${item.id()}"
            val positionMs = (item.userData().playbackPositionTicks() / 10_000L).toInt()
            val durationMs = (item.runTimeTicks()?.let { it / 10_000L }
                ?.takeIf { it > 0L }
                ?: (positionMs * 2L).coerceAtLeast(1L)).toInt()
            val builder = PreviewProgram.Builder()
                .setChannelId(channelId)
                .setTitle(item.name())
                .setContentId(item.id())
                .setInternalProviderId(item.id())
                .setPosterArtUri(Uri.parse(posterUrl))
                .setIntentUri(Uri.parse(deepLink))
                .setType(itemToProgramType(item))
                .setDurationMillis(durationMs)
            if (positionMs > 0) {
                builder.setLastPlaybackPositionMillis(positionMs)
            }
            item.productionYear()?.takeIf { it > 0 }?.let { year ->
                builder.setReleaseDate(year.toString())
            }
            builder.build().toContentValues()
        }
        if (valuesBatch.isNotEmpty()) {
            resolver.bulkInsert(channelUri, valuesBatch.toTypedArray())
        }
    }

    private fun itemToProgramType(item: MediaItemSummary): Int = when (item.typeWireName()) {
        "movie" -> TvContractCompat.PreviewPrograms.TYPE_MOVIE
        "episode" -> TvContractCompat.PreviewPrograms.TYPE_TV_EPISODE
        "series", "season" -> TvContractCompat.PreviewPrograms.TYPE_TV_SERIES
        else -> TvContractCompat.PreviewPrograms.TYPE_CLIP
    }

    private fun hasTvProviderWriteAccess(): Boolean {
        val permission = "com.android.providers.tv.permission.WRITE_EPG_DATA"
        return context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED
    }

    companion object {
        private const val CONTINUE_WATCHING = "cinepilot:continue-watching"
        private const val NEXT_UP = "cinepilot:next-up"
    }
}
