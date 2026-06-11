package tv.cinepilot.tv.offline

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadManager
import androidx.media3.exoplayer.offline.DownloadNotificationHelper
import androidx.media3.exoplayer.offline.DownloadService
import androidx.media3.exoplayer.scheduler.PlatformScheduler
import tv.cinepilot.tv.MainActivity
import tv.cinepilot.tv.R

/**
 * Media3 [DownloadService] implementation. Runs in the app process, owns a
 * single foreground service notification while downloads are in progress,
 * and wakes itself up via [WorkManagerScheduler] when requirements are
 * re-satisfied after an interruption.
 */
class CinePilotDownloadService : DownloadService(
    FOREGROUND_NOTIFICATION_ID,
    DEFAULT_FOREGROUND_NOTIFICATION_UPDATE_INTERVAL,
    CHANNEL_ID,
    R.string.cinepilot_download_channel_name,
    R.string.cinepilot_download_channel_desc,
) {

    override fun getDownloadManager(): DownloadManager {
        val runtime = tv.cinepilot.tv.runtime.CinePilotRuntimeHolder.await()
        return DownloadCoordinator.getInstance(
            applicationContext,
            runtime.offlineRepository,
        ).downloadManager
    }

    override fun getScheduler(): PlatformScheduler? =
        if (Build.VERSION.SDK_INT >= 21) PlatformScheduler(this, JOB_ID) else null

    override fun getForegroundNotification(
        downloads: MutableList<Download>,
        notMetRequirements: Int,
    ): Notification {
        ensureChannel()
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val helper = DownloadNotificationHelper(this, CHANNEL_ID)
        return helper.buildProgressNotification(
            /* context = */ this,
            R.drawable.ic_download,
            /* contentIntent = */ pendingIntent,
            /* message = */ null,
            downloads,
            notMetRequirements,
        )
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.cinepilot_download_channel_name),
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = getString(R.string.cinepilot_download_channel_desc)
            setShowBadge(false)
        }
        manager.createNotificationChannel(channel)
    }

    private companion object {
        private const val CHANNEL_ID = "cinepilot_downloads"
        private const val FOREGROUND_NOTIFICATION_ID = 10042
        private const val JOB_ID = 10042
    }
}
