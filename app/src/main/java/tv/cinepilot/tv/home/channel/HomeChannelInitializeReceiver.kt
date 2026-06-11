package tv.cinepilot.tv.home.channel

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

/**
 * Triggered by the Android TV launcher after a cold launch when the user
 * presses "Add channel" on the CinePilot card, or when the OEM launcher
 * broadcasts [android.media.tv.action.INITIALIZE_PROGRAMS].
 *
 * We simply enqueue the periodic [HomeChannelSyncWorker] so the channels
 * are created lazily once we have network + credentials. A best-effort
 * immediate push is also scheduled so newly-granted channels appear
 * quickly.
 */
class HomeChannelInitializeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            HomeChannelSyncWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            PeriodicWorkRequestBuilder<HomeChannelSyncWorker>(
                HomeChannelSyncWorker.REPEAT_INTERVAL_HOURS, TimeUnit.HOURS,
            )
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .setRequiresBatteryNotLow(true)
                        .build(),
                )
                .build(),
        )
        HomeChannelSyncWorker.scheduleImmediate(context)
    }

    companion object {
        /** Bootstraps the periodic work on app launch if it is not already scheduled. */
        fun ensureScheduled(context: Context) {
            val wm = WorkManager.getInstance(context)
            val request = PeriodicWorkRequestBuilder<HomeChannelSyncWorker>(
                HomeChannelSyncWorker.REPEAT_INTERVAL_HOURS, TimeUnit.HOURS,
            )
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .setRequiresBatteryNotLow(true)
                        .build(),
                )
                .build()
            wm.enqueueUniquePeriodicWork(
                HomeChannelSyncWorker.WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request,
            )
        }
    }
}
