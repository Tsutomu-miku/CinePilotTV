package tv.cinepilot.tv.runtime

import android.content.Context
import java.nio.file.Path
import tv.cinepilot.core.protocol.ClientIdentity
import tv.cinepilot.core.protocol.FileSessionRepository
import tv.cinepilot.core.protocol.MediaBrowserClient
import tv.cinepilot.core.protocol.UrlConnectionHttpTransport
import tv.cinepilot.core.tv.TvAppState

class CinePilotRuntime private constructor(
    val clientIdentity: ClientIdentity,
    val mediaBrowserClient: MediaBrowserClient,
    val initialState: TvAppState,
) {
    companion object {
        fun create(context: Context): CinePilotRuntime {
            val appContext = context.applicationContext
            val clientIdentity = ClientIdentity(
                "CinePilot TV",
                android.os.Build.MODEL ?: "Android TV",
                stableDeviceId(),
                "0.1.0",
            )
            val sessionFile: Path = appContext.filesDir.toPath().resolve("sessions.properties")
            return CinePilotRuntime(
                clientIdentity = clientIdentity,
                mediaBrowserClient = MediaBrowserClient(
                    UrlConnectionHttpTransport(),
                    FileSessionRepository(sessionFile),
                    clientIdentity,
                ),
                initialState = TvAppState.initial(),
            )
        }

        private fun stableDeviceId(): String {
            val serial = runCatching { android.os.Build.ID }.getOrNull()
            val model = android.os.Build.MODEL ?: "android-tv"
            return "cinepilot-tv-${model}-${serial ?: "unknown"}"
        }
    }
}

