package tv.cinepilot.tv.runtime

import android.content.Context
import android.provider.Settings
import java.nio.file.Path
import okhttp3.OkHttpClient
import tv.cinepilot.core.protocol.ClientIdentity
import tv.cinepilot.core.protocol.FileSessionRepository
import tv.cinepilot.core.protocol.MediaBrowserClient
import tv.cinepilot.core.protocol.OfflineRepository
import tv.cinepilot.core.tv.FileHomeRowsCache
import tv.cinepilot.core.tv.HomeRowsLoader
import tv.cinepilot.core.tv.TvAppState
import tv.cinepilot.core.tv.TvWorkflowController
import tv.cinepilot.tv.BuildConfig
import tv.cinepilot.tv.offline.OfflineRepositoryStore

class CinePilotRuntime private constructor(
    val clientIdentity: ClientIdentity,
    val mediaBrowserClient: MediaBrowserClient,
    val workflowController: TvWorkflowController,
    val offlineRepository: OfflineRepository,
    val deviceCodecDiagnostics: DeviceCodecDiagnostics,
    val initialState: TvAppState,
    /** OkHttpClient for API calls (no HTTP cache). */
    val apiOkHttpClient: OkHttpClient,
    /** OkHttpClient for image loading (dedicated image HTTP cache). */
    val imageOkHttpClient: OkHttpClient,
    /** OkHttpClient for video streaming / downloads (no HTTP cache). */
    val streamingOkHttpClient: OkHttpClient,
    val bitmapCache: BitmapCache,
    val homeRowsCache: FileHomeRowsCache,
) {
    companion object {
        fun create(context: Context): CinePilotRuntime {
            val appContext = context.applicationContext
            val clientIdentity = ClientIdentity(
                "CinePilot TV",
                android.os.Build.MODEL ?: "Android TV",
                stableDeviceId(appContext),
                BuildConfig.VERSION_NAME,
            )
            val filesDir: Path = appContext.filesDir.toPath()
            val sessionFile: Path = filesDir.resolve("sessions.properties")
            val homeRowsDir: Path = filesDir.resolve("home-rows")
            val apiClient = CinePilotNetwork.createApiClient(appContext)
            val imageClient = CinePilotNetwork.createImageClient(appContext)
            val streamingClient = CinePilotNetwork.createStreamingClient(appContext)
            CinePilotNetwork.installImageLoader(appContext, imageClient)
            val mediaBrowserClient = MediaBrowserClient(
                OkHttpTransport(apiClient),
                FileSessionRepository(sessionFile),
                clientIdentity,
            )
            val offlineRepository = OfflineRepositoryStore.load(appContext)
            val bitmapCache = BitmapCache.create(appContext)
            val homeRowsCache = FileHomeRowsCache(homeRowsDir)
            val deviceCodecDiagnostics = DeviceCodecDiagnostics(appContext)
            return CinePilotRuntime(
                clientIdentity = clientIdentity,
                mediaBrowserClient = mediaBrowserClient,
                workflowController = TvWorkflowController(
                    mediaBrowserClient,
                    HomeRowsLoader(mediaBrowserClient),
                    deviceCodecDiagnostics.playbackDeviceProfile(),
                    offlineRepository,
                ),
                offlineRepository = offlineRepository,
                deviceCodecDiagnostics = deviceCodecDiagnostics,
                initialState = TvAppState.initial(),
                apiOkHttpClient = apiClient,
                imageOkHttpClient = imageClient,
                streamingOkHttpClient = streamingClient,
                bitmapCache = bitmapCache,
                homeRowsCache = homeRowsCache,
            ).also { CinePilotRuntimeHolder.attach(it) }
        }

        private fun stableDeviceId(context: Context): String {
            val androidId = runCatching {
                Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
            }.getOrNull()
            if (!androidId.isNullOrBlank()) {
                return "cinepilot-tv-$androidId"
            }
            val model = android.os.Build.MODEL ?: "android-tv"
            val buildId = runCatching { android.os.Build.ID }.getOrNull()
            return "cinepilot-tv-${model}-${buildId ?: "unknown"}"
        }
    }
}
