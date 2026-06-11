package tv.cinepilot.tv.runtime

import android.content.Context
import android.provider.Settings
import java.nio.file.Path
import tv.cinepilot.core.protocol.ClientIdentity
import tv.cinepilot.core.protocol.FileSessionRepository
import tv.cinepilot.core.protocol.MediaBrowserClient
import tv.cinepilot.core.protocol.OfflineRepository
import tv.cinepilot.core.protocol.UrlConnectionHttpTransport
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
            val mediaBrowserClient = MediaBrowserClient(
                UrlConnectionHttpTransport(),
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
