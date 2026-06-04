package tv.cinepilot.tv.runtime

import android.os.Handler
import java.util.concurrent.Executor
import tv.cinepilot.core.tv.TvWorkflowController

class QuickConnectPoller(
    private val mainHandler: Handler,
    private val backgroundExecutor: Executor,
    private val controller: TvWorkflowController,
    private val onWaiting: (Int) -> Unit,
    private val onApproved: () -> Unit,
    private val onError: (Throwable) -> Unit,
) {
    @Volatile private var polling = false
    private var generation = 0
    private var attempts = 0
    private var activeRequestGeneration = NO_ACTIVE_REQUEST

    private val pollRunnable = Runnable {
        val currentGeneration = generation
        if (polling) {
            pollOnce(currentGeneration)
        }
    }

    fun start() {
        attempts = 0
        generation += 1
        activeRequestGeneration = NO_ACTIVE_REQUEST
        polling = true
        scheduleNext()
    }

    fun stop() {
        polling = false
        generation += 1
        activeRequestGeneration = NO_ACTIVE_REQUEST
        mainHandler.removeCallbacks(pollRunnable)
    }

    fun checkNow() {
        if (!polling) {
            return
        }
        mainHandler.removeCallbacks(pollRunnable)
        pollOnce(generation)
    }

    private fun scheduleNext() {
        mainHandler.removeCallbacks(pollRunnable)
        mainHandler.postDelayed(pollRunnable, QUICK_CONNECT_POLL_INTERVAL_MS)
    }

    private fun pollOnce(requestGeneration: Int) {
        if (!polling || requestGeneration != generation || activeRequestGeneration != NO_ACTIVE_REQUEST) {
            return
        }
        activeRequestGeneration = requestGeneration
        backgroundExecutor.execute {
            try {
                controller.completeQuickConnect()
                mainHandler.post {
                    clearActiveRequest(requestGeneration)
                    if (!isCurrent(requestGeneration)) {
                        return@post
                    }
                    stop()
                    onApproved()
                }
            } catch (error: Throwable) {
                mainHandler.post {
                    clearActiveRequest(requestGeneration)
                    if (!isCurrent(requestGeneration)) {
                        return@post
                    }
                    if (error.message == TvWorkflowController.QUICK_CONNECT_NOT_APPROVED_MESSAGE) {
                        attempts += 1
                        onWaiting(attempts)
                        scheduleNext()
                    } else {
                        stop()
                        onError(error)
                    }
                }
            }
        }
    }

    private fun isCurrent(requestGeneration: Int): Boolean {
        return polling && requestGeneration == generation
    }

    private fun clearActiveRequest(requestGeneration: Int) {
        if (activeRequestGeneration == requestGeneration) {
            activeRequestGeneration = NO_ACTIVE_REQUEST
        }
    }

    private companion object {
        private const val NO_ACTIVE_REQUEST = -1
        private const val QUICK_CONNECT_POLL_INTERVAL_MS = 2_000L
    }
}
