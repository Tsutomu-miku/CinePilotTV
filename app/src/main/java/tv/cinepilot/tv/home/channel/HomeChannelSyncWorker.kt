package tv.cinepilot.tv.home.channel

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import tv.cinepilot.core.protocol.AuthenticatedServer
import tv.cinepilot.core.protocol.AuthSession
import tv.cinepilot.core.protocol.MediaBrowserClient
import tv.cinepilot.core.protocol.MediaServerAddress
import tv.cinepilot.core.protocol.SavedSession
import tv.cinepilot.core.protocol.ServerFlavor
import tv.cinepilot.core.protocol.ServerIdentity
import tv.cinepilot.core.protocol.SessionScope
import tv.cinepilot.tv.home.HomeSettingsStore
import tv.cinepilot.tv.runtime.CinePilotRuntime
import tv.cinepilot.tv.runtime.CinePilotRuntimeHolder

/**
 * Background worker that syncs the active Jellyfin/Emby account's resume and
 * next-up lists to the Android TV preview channels.
 *
 * The immediate variant ([scheduleImmediate]) is enqueued after every
 * successful home paint so the launcher rows stay fresh right after the
 * user browses. The periodic variant is scheduled by
 * [HomeChannelInitializeReceiver] and [ensureScheduled] so that even if
 * the app is killed the background job still refreshes the channels a
 * few times per day.
 *
 * All provider writes are wrapped in try/catch so missing WRITE_EPG_DATA
 * permission on stock launchers cannot crash the worker.
 */
class HomeChannelSyncWorker(
    private val ctx: Context,
    private val params: WorkerParameters,
) : CoroutineWorker(ctx, params) {

    override suspend fun doWork(): Result {
        return runCatching {
            val runtime = CinePilotRuntimeHolder.ensure(applicationContext)
            val settingsStore = HomeSettingsStore(applicationContext)
            val settings = settingsStore.load()
            if (!settings.showContinueWatchingInLauncher && !settings.showNextUpInLauncher) {
                return Result.success()
            }
            val manager = HomeChannelManager(
                applicationContext,
                runtime.mediaBrowserClient,
                settingsStore,
            )
            if (!manager.isAvailable()) {
                return Result.success()
            }
            val authenticated = readActiveAuthenticated(runtime) ?: return Result.retry()
            manager.sync(settings, authenticated)
            Result.success()
        }.getOrElse { throwable ->
            val root = generateSequence(throwable) { it.cause }
                .lastOrNull() ?: throwable
            when (root) {
                is SecurityException -> Result.success()
                else -> Result.retry()
            }
        }
    }

    private fun readActiveAuthenticated(runtime: CinePilotRuntime): AuthenticatedServer? {
        val client = runtime.clientIdentity
        val filesDir = applicationContext.filesDir.toPath()
        val sessionFile = filesDir.resolve("sessions.properties")
        val sessionRepo = tv.cinepilot.core.protocol.FileSessionRepository(sessionFile)
        val sessions: List<SavedSession> = sessionRepo.listAll(client)
        if (sessions.isEmpty()) return null
        // Prefer the session whose server has an __active__ marker matching its
        // user id; fall back to the most-recently-saved session (first in list).
        val byServerId: Map<String, List<SavedSession>> =
            sessions.groupBy { it.scope().serverId() }
        val activeScopes: List<SessionScope> = byServerId.keys
            .mapNotNull { serverId -> sessionRepo.activeScope(serverId, client).orElse(null) }
        val chosen: SavedSession = activeScopes
            .firstNotNullOfOrNull { active ->
                sessions.firstOrNull { saved -> saved.scope() == active }
            }
            ?: sessions.first()
        val scope = chosen.scope()
        val server = ServerIdentity(
            MediaServerAddress.parse(scope.serverUrl()),
            scope.serverId(),
            detectServerFlavor(scope.serverUrl()),
            "",
        )
        return AuthenticatedServer(server, chosen.toAuthSession(client))
    }

    companion object {
        const val WORK_NAME = "cinepilot-home-channel-sync"
        private const val IMMEDIATE_WORK_NAME = "cinepilot-home-channel-sync-now"
        const val REPEAT_INTERVAL_HOURS: Long = 6

        fun scheduleImmediate(context: Context) {
            val request = OneTimeWorkRequestBuilder<HomeChannelSyncWorker>()
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                IMMEDIATE_WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                request,
            )
        }

        /**
         * Synchronously refresh home channels using an already-authenticated
         * server. Called inline after a successful home paint so that the
         * launcher rows update immediately even if WorkManager throttles the
         * expedited job.
         */
        fun syncNow(
            context: Context,
            mediaBrowserClient: MediaBrowserClient,
            authenticated: AuthenticatedServer,
            homeSettingsStore: HomeSettingsStore,
        ) {
            runCatching {
                val settings = homeSettingsStore.load()
                if (!settings.showContinueWatchingInLauncher && !settings.showNextUpInLauncher) {
                    return@runCatching
                }
                HomeChannelManager(context, mediaBrowserClient, homeSettingsStore)
                    .sync(settings, authenticated)
            }
        }
    }
}

private fun detectServerFlavor(url: String): ServerFlavor {
    val lowered = url.lowercase()
    return when {
        lowered.contains("emby") -> ServerFlavor.EMBY
        else -> ServerFlavor.JELLYFIN
    }
}

private fun SavedSession.toAuthSession(
    client: tv.cinepilot.core.protocol.ClientIdentity,
): AuthSession = AuthSession(
    scope().serverId(),
    scope().userId(),
    accessToken(),
    client,
)
