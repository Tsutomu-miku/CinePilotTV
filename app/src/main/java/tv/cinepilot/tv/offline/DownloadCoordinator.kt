package tv.cinepilot.tv.offline

import android.content.Context
import android.net.Uri
import androidx.media3.common.MimeTypes
import androidx.media3.database.DatabaseProvider
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.HttpDataSource
import androidx.media3.datasource.cache.Cache
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadManager
import androidx.media3.exoplayer.offline.DownloadRequest
import androidx.media3.exoplayer.offline.DownloadService
import androidx.media3.exoplayer.scheduler.Requirements
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.common.MediaItem as ExoMediaItem
import java.io.File
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import okhttp3.OkHttpClient
import tv.cinepilot.core.protocol.AuthenticatedServer
import tv.cinepilot.core.protocol.MediaItemSummary
import tv.cinepilot.core.protocol.OfflineRepository
import tv.cinepilot.core.protocol.PlayableMedia
import tv.cinepilot.core.protocol.PlaybackUrlAuthorizer

/**
 * Central coordinator for the offline subsystem. Owns:
 *
 *  * The Media3 [Cache], [DownloadIndex], and [DownloadManager].
 *  * An [OfflineRepository] instance loaded at process startup and persisted
 *    on every state transition.
 *  * The bidirectional bridge: [OfflineRepository] changes dispatched to
 *    [DownloadService], and [DownloadManager.Listener] events written back
 *    into [OfflineRepository].
 *
 * <p>The coordinator runs as an application-process singleton; there is no
 * public constructor. Instead, callers reach it via [getInstance]. The
 * underlying media3 components are constructed lazily on first access so
 * process-critical code paths (login, settings) do not pay the cache-open
 * cost before the user actually schedules a download.
 */
class DownloadCoordinator private constructor(
    private val appContext: Context,
    private val offlineRepository: OfflineRepository,
    private val okHttpClient: OkHttpClient,
) {

    private val executor: Executor = Executors.newSingleThreadExecutor()
    private val persistRunnable = Runnable { OfflineRepositoryStore.save(appContext, offlineRepository) }

    // --- media3 dependencies (lazily constructed) -----------------------------

    private val databaseProvider: DatabaseProvider by lazy { StandaloneDatabaseProvider(appContext) }

    private val downloadCache: Cache by lazy {
        val dir = File(appContext.filesDir, "cinepilot_downloads").apply { mkdirs() }
        SimpleCache(
            dir,
            androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor(
                offlineRepository.quotaFor("__cache__").bytesPerServer().takeIf { it > 0 }
                    ?: (64L * 1024 * 1024 * 1024), // 64GB hard cap
            ),
            databaseProvider,
        )
    }

    private val httpDataSourceFactory: HttpDataSource.Factory by lazy {
        OkHttpDataSource.Factory(okHttpClient)
            .setUserAgent(USER_AGENT)
    }

    private val upstreamDataSourceFactory: DataSource.Factory by lazy {
        CacheDataSource.Factory()
            .setCache(downloadCache)
            .setUpstreamDataSourceFactory(httpDataSourceFactory)
            .setCacheWriteDataSinkFactory(null) // writes only via download service path
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
    }

    val cacheDataSourceFactory: DataSource.Factory get() = upstreamDataSourceFactory

    val downloadManager: DownloadManager by lazy {
        DownloadManager(
            appContext,
            databaseProvider,
            downloadCache,
            httpDataSourceFactory,
            executor,
        ).apply {
            minRetryCount = 3
            // NETWORK is a hard requirement; the rest are optional.
            requirements = Requirements(Requirements.NETWORK or Requirements.DEVICE_STORAGE_NOT_LOW)
            addListener(DownloadManagerBridge())
        }
    }

    // --- public API (runs tasks on the private executor) ----------------------

    /**
     * Try to enqueue a download. Returns {@code null} on success, otherwise a
     * human-readable Chinese error string suitable for a toast.
     */
    fun enqueue(
        authenticated: AuthenticatedServer,
        summary: MediaItemSummary,
        playable: PlayableMedia,
        quality: Int,
        onResult: (String?) -> Unit,
    ) {
        executor.execute {
            val serverId = authenticated.server().serverId()
            val itemId = summary.id()
            val entry = OfflineRepository.Entry(
                serverId,
                itemId,
                summary.name(),
                quality,
                mediaSourceIdHash(playable),
            )
            val reject = offlineRepository.enqueue(entry)
            if (reject != null) {
                schedulePersist()
                post(onResult, reject)
                return@execute
            }
            val result = runCatching {
                val url = playbackUrl(authenticated, playable)
                val request = buildDownloadRequest(playable, url, serverId, itemId, quality)
                DownloadService.sendAddDownload(
                    appContext,
                    CinePilotDownloadService::class.java,
                    request,
                    true,
                )
            }
            val errorMsg = result.exceptionOrNull()?.let { err ->
                val msg = "启动下载失败: ${err.message ?: err::class.java.simpleName}"
                offlineRepository.markFailed(serverId, itemId, quality, msg)
                msg
            }
            schedulePersist()
            post(onResult, errorMsg)
        }
    }

    fun pause(serverId: String, itemId: String, quality: Int) {
        executor.execute {
            offlineRepository.pause(serverId, itemId, quality)
            val key = mediaSourceContentId(serverId, itemId, quality)
            DownloadService.sendSetStopReason(
                appContext, CinePilotDownloadService::class.java, key, STOP_REASON_USER, true,
            )
            schedulePersist()
        }
    }

    fun resume(serverId: String, itemId: String, quality: Int) {
        executor.execute {
            offlineRepository.resume(serverId, itemId, quality)
            val key = mediaSourceContentId(serverId, itemId, quality)
            DownloadService.sendSetStopReason(
                appContext, CinePilotDownloadService::class.java, key, Download.STOP_REASON_NONE, true,
            )
            schedulePersist()
        }
    }

    fun pauseAll(serverId: String) {
        executor.execute {
            for (entry in offlineRepository.listForServer(serverId)) {
                pause(serverId, entry.itemId(), entry.quality())
            }
        }
    }

    fun resumeAll(serverId: String) {
        executor.execute {
            for (entry in offlineRepository.listForServer(serverId)) {
                resume(serverId, entry.itemId(), entry.quality())
            }
        }
    }

    fun remove(serverId: String, itemId: String, quality: Int) {
        executor.execute {
            val key = mediaSourceContentId(serverId, itemId, quality)
            DownloadService.sendRemoveDownload(
                appContext, CinePilotDownloadService::class.java, key, false,
            )
            offlineRepository.delete(serverId, itemId, quality)
            schedulePersist()
        }
    }

    /**
     * If the item is ready for offline playback, returns a Media3 MediaItem
     * wrapping a [CacheDataSource] URI. Otherwise returns null so the caller
     * can fall back to the network playback path.
     */
    fun localMediaItem(serverId: String, itemId: String): ExoMediaItem? {
        val ready = offlineRepository.readyFor(serverId, itemId) ?: return null
        val key = mediaSourceContentId(serverId, itemId, ready.quality())
        val cachedSpans = runCatching { downloadCache.getCachedSpans(key) }.getOrNull()
        if (cachedSpans == null) {
            offlineRepository.markFailed(serverId, itemId, ready.quality(), "缓存文件已被清理")
            schedulePersist()
            return null
        }
        return ExoMediaItem.Builder()
            .setMediaId(key)
            .setUri(Uri.Builder().scheme(CACHE_SCHEME).authority(CACHE_AUTHORITY).path(key).build())
            .build()
    }

    /** Wrap a URI in a cache-aware media source. Used by the player path. */
    fun cacheMediaSource(uri: String): androidx.media3.exoplayer.source.MediaSource {
        val factory = DefaultMediaSourceFactory(upstreamDataSourceFactory)
        return factory.createMediaSource(ExoMediaItem.fromUri(uri))
    }

    fun runWhenInitialized(block: () -> Unit) {
        executor.execute {
            @Suppress("UNUSED_VARIABLE")
            val init = downloadManager
            block()
        }
    }

    fun saveNow() {
        executor.execute(persistRunnable)
    }

    // --- internals -----------------------------------------------------------

    private fun schedulePersist() {
        persistRunnable.run()
    }

    private fun <T> post(cb: (T) -> Unit, value: T) {
        android.os.Handler(android.os.Looper.getMainLooper()).post { cb(value) }
    }

    private inner class DownloadManagerBridge : DownloadManager.Listener {
        override fun onDownloadChanged(
            manager: DownloadManager,
            download: Download,
            finalException: Exception?,
        ) {
            val (serverId, itemId, quality) = parseContentId(download.request.id)
                ?: return
            when (download.state) {
                Download.STATE_QUEUED -> {
                    offlineRepository.updateProgress(
                        serverId, itemId, quality,
                        0L,
                        if (download.contentLength > 0) download.contentLength else -1L,
                    )
                }
                Download.STATE_DOWNLOADING -> {
                    offlineRepository.updateProgress(
                        serverId, itemId, quality,
                        download.bytesDownloaded,
                        if (download.contentLength > 0) download.contentLength else -1L,
                    )
                }
                Download.STATE_COMPLETED -> {
                    offlineRepository.updateProgress(
                        serverId, itemId, quality,
                        download.bytesDownloaded,
                        download.bytesDownloaded,
                    )
                    offlineRepository.markReady(
                        serverId, itemId, quality,
                        localContentIdFrom(download),
                    )
                }
                Download.STATE_FAILED -> {
                    offlineRepository.markFailed(
                        serverId, itemId, quality,
                        finalException?.message ?: "下载失败",
                    )
                }
                Download.STATE_REMOVING -> {
                    offlineRepository.delete(serverId, itemId, quality)
                }
                Download.STATE_RESTARTING -> {
                    offlineRepository.updateProgress(
                        serverId, itemId, quality, download.bytesDownloaded, -1L,
                    )
                }
                Download.STATE_STOPPED -> {
                    offlineRepository.pause(serverId, itemId, quality)
                }
            }
            schedulePersist()
        }
    }

    companion object {
        private const val CACHE_SCHEME = "cinepilot-cache"
        private const val CACHE_AUTHORITY = "local"
        private const val USER_AGENT = "CinePilotTV/1.0 (offline-download)"
        private const val STOP_REASON_USER = 1001

        @Volatile private var instance: DownloadCoordinator? = null

        fun getInstance(
            context: Context,
            offlineRepository: OfflineRepository,
            okHttpClient: OkHttpClient,
        ): DownloadCoordinator {
            val cached = instance
            if (cached != null) return cached
            return synchronized(this) {
                val existing = instance
                existing ?: DownloadCoordinator(
                    context.applicationContext, offlineRepository, okHttpClient,
                ).also { instance = it }
            }
        }

        fun getExisting(): DownloadCoordinator? = instance

        // Content id format: serverId$itemId$quality
        // Note: ${'$'} inserts a literal $ so the following identifier
        // is treated as a template variable, not plain text.
        internal fun mediaSourceContentId(serverId: String, itemId: String, quality: Int): String =
            "$serverId${'$'}$itemId${'$'}$quality"

        internal fun parseContentId(id: String): Triple<String, String, Int>? {
            val parts = id.split('$', limit = 3)
            if (parts.size != 3) return null
            val quality = parts[2].toIntOrNull() ?: return null
            return Triple(parts[0], parts[1], quality)
        }

        private fun localContentIdFrom(download: Download): Long {
            return (download.request.id.hashCode().toLong() and 0xFFFFFFFFL) xor
                    (download.bytesDownloaded and 0xFFFFFFFFL shl 32)
        }

        private fun mediaSourceIdHash(playable: PlayableMedia): Long {
            val url = (playable.url() ?: playable.request()?.path() ?: "").toByteArray()
            var h = 1125899906842597L
            for (b in url) {
                h = h * 31 + b
            }
            return h
        }

        private fun playbackUrl(authenticated: AuthenticatedServer, playable: PlayableMedia): String {
            val raw = playable.url() ?: playable.request()?.url(authenticated.server().address())
                ?: throw IllegalStateException("playback url is required for download")
            return PlaybackUrlAuthorizer.withAccessToken(raw, authenticated.session())
        }

        private fun buildDownloadRequest(
            playable: PlayableMedia,
            authorizedUrl: String,
            serverId: String,
            itemId: String,
            quality: Int,
        ): DownloadRequest {
            val contentId = mediaSourceContentId(serverId, itemId, quality)
            val ext = runCatching {
                Uri.parse(authorizedUrl).path.orEmpty()
                    .substringAfterLast('.', "")
                    .lowercase().trim()
            }.getOrDefault("")
            val mimeType = when (ext) {
                "mp4", "m4v" -> MimeTypes.VIDEO_MP4
                "mkv" -> MimeTypes.VIDEO_MATROSKA
                "mov" -> "video/quicktime"
                "webm" -> MimeTypes.VIDEO_WEBM
                "ts", "m2ts", "mpegts" -> MimeTypes.VIDEO_MP2T
                "m3u8" -> MimeTypes.APPLICATION_M3U8
                "mpd" -> MimeTypes.APPLICATION_MPD
                else -> MimeTypes.VIDEO_UNKNOWN // let exoplayer probe
            }
            val uri = Uri.parse(authorizedUrl)
            return DownloadRequest.Builder(contentId, uri)
                .setMimeType(mimeType)
                .setCustomCacheKey(contentId)
                .build()
        }
    }
}
