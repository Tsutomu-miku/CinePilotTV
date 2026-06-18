package tv.cinepilot.tv.runtime

import java.util.concurrent.Executor

class CinePilotTaskRunner(
    private val executor: Executor,
    private val postToMain: (() -> Unit) -> Unit,
    private val showBlocking: (String) -> Unit,
    private val beginBusy: (String) -> () -> Unit,
    private val showBlockingError: (Throwable) -> Unit,
    private val showInPlaceError: (Throwable) -> Unit,
) {
    fun runBlockingTask(message: String, task: () -> Unit, onSuccess: () -> Unit) {
        showBlocking(message)
        runTask(task = task, onSuccess = onSuccess, onError = showBlockingError)
    }

    fun runInPlaceTask(message: String, task: () -> Unit, onSuccess: () -> Unit) {
        val clearBusy = beginBusy(message)
        runTask(
            task = task,
            onSuccess = {
                clearBusy()
                onSuccess()
            },
            onError = { error ->
                clearBusy()
                showInPlaceError(error)
            },
        )
    }

    fun runSilentTask(
        task: () -> Unit,
        onSuccess: () -> Unit = {},
        onError: (Throwable) -> Unit = showInPlaceError,
    ) {
        runTask(task = task, onSuccess = onSuccess, onError = onError)
    }

    private fun runTask(
        task: () -> Unit,
        onSuccess: () -> Unit,
        onError: (Throwable) -> Unit,
    ) {
        executor.execute {
            try {
                task()
                postToMain { onSuccess() }
            } catch (error: Throwable) {
                postToMain { onError(error) }
            }
        }
    }
}
