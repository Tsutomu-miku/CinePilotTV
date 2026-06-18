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
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ExecutionException
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

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
        Thread(runnable, "plugin-host").apply { isDaemon = true }
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
            future.get(30L, TimeUnit.SECONDS)
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
     * Save a credential for a plugin (following successful verification) and reload status. */
    fun savePluginAuth(pluginId: String, credential: String) {
        val loaded = plugins.firstOrNull { it.descriptor.id() == pluginId } ?: return
        runCatching { loaded.plugin.saveAuth(loaded.store, credential) }
    }

    /**
     * Wipe stored credentials for a plugin. */
    fun clearPluginAuth(pluginId: String) {
        val loaded = plugins.firstOrNull { it.descriptor.id() == pluginId } ?: return
        runCatching { loaded.plugin.clearAuth(loaded.store) }
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
     */
    fun searchSubtitles(item: MediaItemSnapshot, language: String = ""): List<SubtitleSearchResult> {
        val results = mutableListOf<SubtitleSearchResult>()
        for (loaded in plugins) {
            val plugin = loaded.plugin as? SubtitleSearchPlugin ?: continue
            runCatching {
                results.addAll(plugin.searchSubtitles(loaded.store, item, language))
            }.onFailure { markErrored(loaded) }
        }
        return results
    }

    /**
     * Downloads a subtitle file from the plugin that produced it.
     * Must be called from a background thread.
     * @return raw subtitle bytes, or null if download fails
     */
    fun downloadSubtitle(result: SubtitleSearchResult): ByteArray? {
        for (loaded in plugins) {
            if (loaded.plugin !is SubtitleSearchPlugin) continue
            if (loaded.descriptor.id() != result.providerId()) continue
            return runCatching {
                (loaded.plugin as SubtitleSearchPlugin).downloadSubtitle(loaded.store, result)
            }.onFailure { markErrored(loaded) }.getOrNull()
        }
        return null
    }

    // --- Per-item sync status / retry (details screen) --------------------

    /**
     * Aggregate per-item sync status across every [UserDataSyncPlugin].
     *
     * <p>The host uses this to render a small sync chip next to the poster
     * on the details screen. The returned list is ordered by plugin name;
     * plugins that return [ItemSyncStatus.UNSUPPORTED] are omitted.
     *
     * <p>Plugins answer from cache only (no network). Safe to call from the
     * UI thread.
     */
    fun itemSyncStatuses(item: MediaItemSnapshot): List<PluginItemSyncState> {
        val result = mutableListOf<PluginItemSyncState>()
        for (loaded in plugins) {
            val plugin = loaded.plugin as? UserDataSyncPlugin ?: continue
            val state = runCatching { plugin.itemSyncStatus(loaded.store, item) }
                .getOrDefault(ItemSyncStatus.UNSUPPORTED)
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
     * <p>The host calls this from a background task; it does NOT wait for
     * completion to unblock the UI, but returns a future so callers can
     * poll progress if needed.
     */
    fun retryItemSync(pluginId: String, item: MediaItemSnapshot) {
        val loaded = plugins.firstOrNull { it.descriptor.id() == pluginId } ?: return
        val plugin = loaded.plugin as? UserDataSyncPlugin ?: return
        executor.submit {
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
                    .onFailure { markErrored(loaded) }
            }
        }
    }

    private fun forPlaybackPlugins(block: (PlaybackSyncPlugin, PluginSettingsStore) -> Unit) {
        for (loaded in plugins) {
            val plugin = loaded.plugin as? PlaybackSyncPlugin ?: continue
            executor.submit {
                runCatching { block(plugin, loaded.store) }
                    .onFailure { markErrored(loaded) }
            }
        }
    }

    private fun markErrored(loaded: LoadedPlugin) {
        runCatching { loaded.plugin.onUnloaded() }
    }

    fun shutdown() {
        executor.submit {
            plugins.forEach { loaded -> runCatching { loaded.plugin.onUnloaded() } }
        }
        executor.shutdown()
    }

    private data class LoadedPlugin(
        val plugin: CinePilotPlugin,
        val descriptor: PluginDescriptor,
        val store: PluginSettingsStore,
    )

    companion object {
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

            override fun clear() = prefs.edit().clear().apply()
        }
    }
}
