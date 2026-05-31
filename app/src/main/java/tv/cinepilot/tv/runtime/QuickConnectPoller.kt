package tv.cinepilot.tv.runtime

import android.os.Handler
import java.util.concurrent.Executor
import tv.cinepilot.core.tv.TvWorkflowController

class QuickConnectPoller(
    private val mainHandler: Handler,
    private val backgroundExecutor: Executor,
    private val controller: TvWorkflowController,
    private val onApproved: () -> Unit,
    private val onError: (Throwable) -> Unit,
) {
    @Volatile private var polling = false

    private val pollRunnable = Runnable {
        if (polling) {
            pollOnce()
        }
    }

    fun start() {
        polling = true
        scheduleNext()
    }

    fun stop() {
        polling = false
        mainHandler.removeCallbacks(pollRunnable)
    }

    private fun scheduleNext() {
        mainHandler.removeCallbacks(pollRunnable)
        mainHandler.postDelayed(pollRunnable, QUICK_CONNECT_POLL_INTERVAL_MS)
    }

    private fun pollOnce() {
        backgroundExecutor.execute {
            try {
                controller.completeQuickConnect()
                mainHandler.post {
                    if (!polling) {
                        return@post
                    }
                    stop()
                    onApproved()
                }
            } catch (error: Throwable) {
                mainHandler.post {
                    if (!polling) {
                        return@post
                    }
                    if (error.message == TvWorkflowController.QUICK_CONNECT_NOT_APPROVED_MESSAGE) {
                        scheduleNext()
                    } else {
                        stop()
                        onError(error)
                    }
                }
            }
        }
    }

    private companion object {
        private const val QUICK_CONNECT_POLL_INTERVAL_MS = 2_000L
    }
}
