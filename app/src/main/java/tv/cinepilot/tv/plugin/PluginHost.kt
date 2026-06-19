package tv.cinepilot.tv.plugin

import android.content.Context
import tv.cinepilot.plugin.spi.AuthVerificationResult
import tv.cinepilot.plugin.spi.CinePilotPlugin
import tv.cinepilot.plugin.spi.ItemSyncStatus
import tv.cinepilot.plugin.spi.MediaItemSnapshot
import tv.cinepilot.plugin.spi.PlaybackSyncPlugin
import tv.cinepilot.plugin.spi.PluginDescriptor
import tv.cinepilot.plugin.spi.PluginSettingsStore
import tv.cinepilot.plugin.spi.PluginStatus
import tv.cinepilot.plugin.spi.SubtitleSearchPlugin
import tv.cinepilot.plugin.spi.SubtitleSearchResult
import tv.cinepilot.plugin.spi.UserDataSyncPlugin
import java.util.ServiceLoader
import java.util.Collections
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executor
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import android.util.Log as AndroidLog

/**
 * Discovers and drives all installed CinePilot plugins via
 * [ServiceLoader]. Plugins live in separate Gradle modules
 * (e.g. `:plugins:bangumi`) and register themselves through a
 * `META-INF/services/tv.cinepilot.plugin.spi.CinePilotPlugin` file.
 *
 * The host owns two responsibilities:
 *   1. Dispatch: route UI-layer events (rating, favorite, playback) to
 *      every plugin that implements the matching SPI interface.
 *      Dispatches happen on a dedicated single-threaded executor so a
 *      misbehaving plugin cannot block the UI or the player thread.
 *   2. Storage: hand every plugin a namespaced [PluginSettingsStore] so
 *      credentials and matched subject ids stay isolated.
 *
 * All plugin interactions are wrapped in try/catch. Exceptions are logged
 * and swallowed; the plugin reports its own degraded status via
 * [CinePilotPlugin.status] so the settings screen can surface it to the
 * user.
 */
class PluginHost private constructor(
    private val context: Context,
    private val plugins: List<LoadedPlugin>,
) {
    private val executor: ExecutorService = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, PLUGIN_THREAD_NAME).apply { isDaemon = true }
    }

    data class PluginInfo(
        val descriptor: PluginDescriptor,
        val status: PluginStatus,
        val store: PluginSettingsStore,
        val supportsUserDataSync: Boolean,
        val supportsPlaybackSync: Boolean,
        val supportsSubtitleSearch: Boolean,
    )

    /** Snapshot used by the plugin settings screen to render rows. */
    fun listPlugins(): List<PluginInfo> = plugins.map { loaded ->
        PluginInfo(
            descriptor = loaded.descriptor,
            status = runCatching { loaded.plugin.status(loaded.store) }
                .getOrDefault(PluginStatus.TEMPORARILY_UNAVAILABLE),
            store = loaded.store,
            supportsUserDataSync = loaded.plugin is UserDataSyncPlugin,
            supportsPlaybackSync = loaded.plugin is PlaybackSyncPlugin,
            supportsSubtitleSearch = loaded.plugin is SubtitleSearchPlugin,
        )
    }

    /**
     * Verify a raw credential against the given plugin. Runs synchronously on
     * the plugin worker executor so token verification (network I/O) never blocks
     * the UI thread. Must be called from a background thread.
     */
    fun verifyPluginAuth(pluginId: String, credential: String): AuthVerificationResult {
        val loaded = plugins.firstOrNull { it.descriptor.id() == pluginId }
            ?: return AuthVerificationResult.failure("插件未加载")
        val future = executor.submit<AuthVerificationResult> {
            runCatching { loaded.plugin.verifyAuth(loaded.store, credential) }
                .getOrElse { t -> AuthVerificationResult.failure(t.message ?: "验证失败") }
        }
        return try {
            future.get(VERIFY_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        } catch (_: TimeoutException) {
            AuthVerificationResult.failure("验证超时")
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
            AuthVerificationResult.failure("验证被中断")
        } catch (e: ExecutionException) {
            AuthVerificationResult.failure(e.cause?.message ?: "验证失败")
        }
    }

    /**
     * Save a credential for a plugin (following successful verification) and reload status.
     * Any exception thrown by the plugin is logged; the caller still sees a successful
     * verification result on the UI side. */
    fun savePluginAuth(pluginId: String, credential: String) {
        val loaded = plugins.firstOrNull { it.descriptor.id() == pluginId } ?: return
        runCatching { loaded.plugin.saveAuth(loaded.store, credential) }
            .onFailure { t -> AndroidLog.e(TAG, "saveAuth failed plugin=$pluginId", t) }
    }

    /**
     * Wipe stored credentials for a plugin. Any exception thrown by the plugin
     * is logged; the UI state has already been cleared by the caller.
     */
    fun clearPluginAuth(pluginId: String) {
        val loaded = plugins.firstOrNull { it.descriptor.id() == pluginId } ?: return
        runCatching { loaded.plugin.clearAuth(loaded.store) }
            .onFailure { t -> AndroidLog.e(TAG, "clearAuth failed plugin=$pluginId", t) }
    }

    // --- UserDataSyncPlugin dispatch -----------------------------------

    fun dispatchRating(item: MediaItemSnapshot, ratingZeroToTen: Double) {
        forUserPlugins { plugin, store -> plugin.onRatingChanged(store, item, ratingZeroToTen) }
    }

    fun dispatchFavorite(item: MediaItemSnapshot, isFavorite: Boolean) {
        forUserPlugins { plugin, store -> plugin.onFavoriteChanged(store, item, isFavorite) }
    }

    fun dispatchWatched(item: MediaItemSnapshot, isWatched: Boolean) {
        forUserPlugins { plugin, store -> plugin.onWatchedChanged(store, item, isWatched) }
    }

    // --- PlaybackSyncPlugin dispatch ----------------------------------

    fun dispatchPlaybackStarted(item: MediaItemSnapshot, durationMillis: Long) {
        forPlaybackPlugins { plugin, store -> plugin.onPlaybackStarted(store, item, durationMillis) }
    }

    fun dispatchPlaybackProgress(item: MediaItemSnapshot, positionMillis: Long, durationMillis: Long) {
        forPlaybackPlugins { plugin, store ->
            plugin.onPlaybackProgress(store, item, positionMillis, durationMillis)
        }
    }

    fun dispatchPlaybackStopped(item: MediaItemSnapshot, finalPositionMillis: Long, durationMillis: Long) {
        forPlaybackPlugins { plugin, store ->
            plugin.onPlaybackStopped(store, item, finalPositionMillis, durationMillis)
        }
    }

    // --- SubtitleSearchPlugin query -------------------------------------
    //
    // Subtitle search is a synchronous query pattern (not fire-and-forget
    // dispatch). Callers MUST invoke these from a background thread; plugin
    // code may do network I/O. Exceptions are caught and the plugin is
    // treated as returning no results.

    /** Returns true if any plugin supports subtitle search. */
    fun hasSubtitleSearchPlugins(): Boolean =
        plugins.any { it.plugin is SubtitleSearchPlugin }

    /**
     * Searches all subtitle plugins and returns aggregated results.
     * Must be called from a background thread.
     *
     * <p>Calls are serialized through the single-threaded plugin executor to
     * prevent concurrent callers from racing inside a plugin implementation.
     * Plugins are NOT required to be thread-safe.
     */
    fun searchSubtitles(item: MediaItemSnapshot, language: String = ""): List<SubtitleSearchResult> {
        val future = executor.submit<List<SubtitleSearchResult>> {
            val results = mutableListOf<SubtitleSearchResult>()
            for (loaded in plugins) {
                val plugin = loaded.plugin as? SubtitleSearchPlugin ?: continue
                runCatching {
                    results.addAll(plugin.searchSubtitles(loaded.store, item, language))
                }.onFailure { t ->
                    AndroidLog.w(TAG, "searchSubtitles failed plugin=${loaded.descriptor.id()}", t)
                    markErrored(loaded)
                }
            }
            return@submit results
        }
        return try {
            future.get(SUBTITLE_QUERY_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        } catch (e: TimeoutException) {
            future.cancel(true)
            AndroidLog.w(TAG, "searchSubtitles timed out after ${SUBTITLE_QUERY_TIMEOUT_SECONDS}s")
            emptyList()
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
            emptyList()
        } catch (e: ExecutionException) {
            AndroidLog.w(TAG, "searchSubtitles execution failed", e.cause ?: e)
            emptyList()
        }
    }

    /**
     * Downloads a subtitle file from the plugin that produced it.
     * Must be called from a background thread.
     * @return raw subtitle bytes, or null if download fails
     */
    fun downloadSubtitle(result: SubtitleSearchResult): ByteArray? {
        val future = executor.submit<ByteArray?> {
            for (loaded in plugins) {
                if (loaded.plugin !is SubtitleSearchPlugin) continue
                if (loaded.descriptor.id() != result.providerId()) continue
                return@submit runCatching<ByteArray?> {
                    (loaded.plugin as SubtitleSearchPlugin).downloadSubtitle(loaded.store, result)
                }.onFailure { t ->
                    AndroidLog.w(TAG, "downloadSubtitle failed plugin=${loaded.descriptor.id()}", t)
                    markErrored(loaded)
                }.getOrNull()
            }
            return@submit null
        }
        return try {
            future.get(SUBTITLE_QUERY_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        } catch (e: TimeoutException) {
            future.cancel(true)
            AndroidLog.w(TAG, "downloadSubtitle timed out")
            null
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
            null
        } catch (e: ExecutionException) {
            AndroidLog.w(TAG, "downloadSubtitle execution failed", e.cause ?: e)
            null
        }
    }

    // --- Per-item sync status / retry (details screen) --------------------

    /**
     * Aggregate per-item sync status across every [UserDataSyncPlugin].
     *
     * <p>The host uses this to render a small sync chip next to the poster
     * on the details screen. The returned list is ordered by plugin name;
     * plugins that return [ItemSyncStatus.UNSUPPORTED] are omitted.
     *
     * <p>Plugs <em>should</em> answer from cache only (no network). To
     * prevent a misbehaving plugin from freezing the details UI, each call
     * is dispatched through the plugin executor with a short per-call
     * timeout; a slow plugin is treated as [ItemSyncStatus.AUTH_REQUIRED]
     * so its row is rendered in a known-degraded state.
     */
    fun itemSyncStatusesAsync(
        item: MediaItemSnapshot,
        callbackExecutor: Executor,
        callback: (List<PluginItemSyncState>) -> Unit,
    ): Future<*> = executor.submit {
        val result = computeItemSyncStatuses(item)
        callbackExecutor.execute { callback(result) }
    }

    fun itemSyncStatuses(item: MediaItemSnapshot): List<PluginItemSyncState> {
        if (Thread.currentThread().name == PLUGIN_THREAD_NAME) {
            return computeItemSyncStatuses(item)
        }
        val future = executor.submit<List<PluginItemSyncState>> { computeItemSyncStatuses(item) }
        val userPluginCount = plugins.count { it.plugin is UserDataSyncPlugin }.coerceAtLeast(1)
        return try {
            future.get(ITEM_SYNC_TIMEOUT_SECONDS * userPluginCount, TimeUnit.SECONDS)
        } catch (e: TimeoutException) {
            future.cancel(true)
            AndroidLog.w(TAG, "itemSyncStatuses timed out after aggregate wait")
            emptyList()
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
            emptyList()
        } catch (e: ExecutionException) {
            AndroidLog.w(TAG, "itemSyncStatuses execution failed", e.cause ?: e)
            emptyList()
        }
    }

    private fun computeItemSyncStatuses(item: MediaItemSnapshot): List<PluginItemSyncState> {
        val result = mutableListOf<PluginItemSyncState>()
        for (loaded in plugins) {
            val plugin = loaded.plugin as? UserDataSyncPlugin ?: continue
            val state: ItemSyncStatus = runCatching { plugin.itemSyncStatus(loaded.store, item) }
                .getOrElse { e ->
                    AndroidLog.w(
                        TAG,
                        "itemSyncStatuses failed plugin=${loaded.descriptor.id()}",
                        e,
                    )
                    ItemSyncStatus.UNSUPPORTED
                }
            if (state == ItemSyncStatus.UNSUPPORTED) continue
            result.add(PluginItemSyncState(loaded.descriptor.id(), loaded.descriptor.name(), state))
        }
        result.sortBy { it.pluginName }
        return result
    }

    /**
     * Retry sync for a single plugin + item pair (user clicked "重试" on the
     * failed chip). Runs on the plugin worker executor so it is safe for
     * plugins to do network I/O inside [UserDataSyncPlugin.retrySyncItem].
     *
     * <p>Callers running on a background thread can call [java.util.concurrent.Future.get]
     * on the returned future to block until the retry finishes, then re-read
     * [itemSyncStatuses] for a fresh snapshot. UI callers must NOT block the
     * main thread on this future.
     */
    fun retryItemSync(pluginId: String, item: MediaItemSnapshot): java.util.concurrent.Future<*>? {
        val loaded = plugins.firstOrNull { it.descriptor.id() == pluginId } ?: return null
        val plugin = loaded.plugin as? UserDataSyncPlugin ?: return null
        return executor.submit<Unit> {
            runCatching { plugin.retrySyncItem(loaded.store, item) }
                .onFailure { markErrored(loaded) }
        }
    }

    data class PluginItemSyncState(
        val pluginId: String,
        val pluginName: String,
        val status: ItemSyncStatus,
    )

    private fun forUserPlugins(block: (UserDataSyncPlugin, PluginSettingsStore) -> Unit) {
        for (loaded in plugins) {
            val plugin = loaded.plugin as? UserDataSyncPlugin ?: continue
            executor.submit {
                runCatching { block(plugin, loaded.store) }
                    .onFailure { t ->
                        AndroidLog.w(TAG, "user-plugin callback failed plugin=${loaded.descriptor.id()}", t)
                        markErrored(loaded)
                    }
            }
        }
    }

    private fun forPlaybackPlugins(block: (PlaybackSyncPlugin, PluginSettingsStore) -> Unit) {
        for (loaded in plugins) {
            val plugin = loaded.plugin as? PlaybackSyncPlugin ?: continue
            executor.submit {
                runCatching { block(plugin, loaded.store) }
                    .onFailure { t ->
                        AndroidLog.w(TAG, "playback-plugin callback failed plugin=${loaded.descriptor.id()}", t)
                        markErrored(loaded)
                    }
            }
        }
    }

    private fun markErrored(loaded: LoadedPlugin) {
        runCatching { loaded.plugin.onUnloaded() }
            .onFailure { t -> AndroidLog.e(TAG, "onUnloaded (during markErrored) failed plugin=${loaded.descriptor.id()}", t) }
    }

    fun shutdown() {
        executor.submit {
            plugins.forEach { loaded ->
                runCatching { loaded.plugin.onUnloaded() }
                    .onFailure { t -> AndroidLog.w(TAG, "onUnloaded (shutdown) failed plugin=${loaded.descriptor.id()}", t) }
            }
        }
        executor.shutdown()
        val done = runCatching {
            executor.awaitTermination(5L, TimeUnit.SECONDS)
        }.getOrDefault(false)
        if (!done) {
            val dropped = executor.shutdownNow().size
            if (dropped > 0) {
                AndroidLog.w(TAG, "plugin executor shutdown: dropped $dropped pending tasks")
            }
        }
    }

    private data class LoadedPlugin(
        val plugin: CinePilotPlugin,
        val descriptor: PluginDescriptor,
        val store: PluginSettingsStore,
    )

    companion object {
        private const val TAG = "PluginHost"
        private const val PLUGIN_THREAD_NAME = "plugin-host"
        /** Max wait (seconds) for a PAT / credential verification round-trip. */
        private const val VERIFY_TIMEOUT_SECONDS = 30L
        /** Max wait (seconds) for a cached-status read; the SPI contract
         *  says plugins must answer from cache only, so the cap is tight. */
        private const val ITEM_SYNC_TIMEOUT_SECONDS = 2L
        /** Max wait (seconds) for subtitle-search queries. */
        private const val SUBTITLE_QUERY_TIMEOUT_SECONDS = 20L

        fun create(context: Context): PluginHost {
            val loader = ServiceLoader.load(CinePilotPlugin::class.java)
            val loaded: MutableList<LoadedPlugin> = ArrayList()
            for (plugin in loader) {
                val descriptor = plugin.descriptor()
                val store = SharedPreferencesPluginStore(
                    context.applicationContext,
                    descriptor.id(),
                )
                runCatching { plugin.onLoaded(store) }
                loaded.add(LoadedPlugin(plugin, descriptor, store))
            }
            return PluginHost(context.applicationContext, loaded)
        }

        private class SharedPreferencesPluginStore(
            context: Context,
            pluginId: String,
        ) : PluginSettingsStore {
            private val prefs = context.getSharedPreferences(
                "cinepilot_plugin_$pluginId",
                Context.MODE_PRIVATE,
            )

            override fun getString(key: String, defaultValue: String): String =
                prefs.getString(key, defaultValue) ?: defaultValue

            override fun getLong(key: String, defaultValue: Long): Long =
                prefs.getLong(key, defaultValue)

            override fun getBoolean(key: String, defaultValue: Boolean): Boolean =
                prefs.getBoolean(key, defaultValue)

            override fun putString(key: String, value: String) =
                prefs.edit().putString(key, value).apply()

            override fun putLong(key: String, value: Long) =
                prefs.edit().putLong(key, value).apply()

            override fun putBoolean(key: String, value: Boolean) =
                prefs.edit().putBoolean(key, value).apply()

            override fun remove(key: String) = prefs.edit().remove(key).apply()

            override fun removeByPrefix(prefix: String) {
                if (prefix.isEmpty()) return
                val editor = prefs.edit()
                var removed = 0
                for (k in prefs.all.keys) {
                    if (k.startsWith(prefix)) {
                        editor.remove(k)
                        removed++
                    }
                }
                if (removed > 0) editor.apply()
            }

            override fun keySet(): Set<String> =
                Collections.unmodifiableSet(prefs.all.keys)

            override fun clear() = prefs.edit().clear().apply()
        }
    }
}
