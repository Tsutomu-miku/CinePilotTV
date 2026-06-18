package tv.cinepilot.tv.runtime

import java.util.concurrent.Executor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Test

class CinePilotTaskRunnerTest {
    private val directExecutor = Executor { command -> command.run() }

    @Test fun inPlaceTaskShowsBusyWithoutBlockingScreen() {
        val events = mutableListOf<String>()
        val runner = runner(events)

        runner.runInPlaceTask("刷新中", {
            events.add("task")
        }) {
            events.add("success")
        }

        assertEquals(listOf("busy:刷新中", "task", "clear:刷新中", "success"), events)
        assertFalse(events.any { it.startsWith("blocking:") })
    }

    @Test fun inPlaceTaskFailureClearsBusyAndUsesInPlaceError() {
        val events = mutableListOf<String>()
        val failure = IllegalStateException("boom")
        val runner = runner(events)

        runner.runInPlaceTask("更新中", {
            throw failure
        }) {
            events.add("success")
        }

        assertEquals(listOf("busy:更新中", "clear:更新中", "in-place-error:boom"), events)
        assertFalse(events.any { it.startsWith("blocking-error:") })
    }

    @Test fun blockingTaskKeepsBlockingLoadingContract() {
        val events = mutableListOf<String>()
        val runner = runner(events)

        runner.runBlockingTask("启动中", {
            events.add("task")
        }) {
            events.add("success")
        }

        assertEquals(listOf("blocking:启动中", "task", "success"), events)
    }

    @Test fun silentTaskDoesNotShowBusyOrBlockingLoading() {
        val events = mutableListOf<String>()
        val runner = runner(events)

        runner.runSilentTask(
            task = {
                events.add("task")
            },
            onSuccess = {
                events.add("success")
            },
        )

        assertEquals(listOf("task", "success"), events)
    }

    @Test fun silentTaskCanOverrideErrorHandler() {
        val events = mutableListOf<String>()
        val failure = IllegalArgumentException("quiet")
        val runner = runner(events)
        var captured: Throwable? = null

        runner.runSilentTask(
            task = { throw failure },
            onError = { error ->
                captured = error
                events.add("custom-error:${error.message}")
            },
        )

        assertSame(failure, captured)
        assertEquals(listOf("custom-error:quiet"), events)
    }

    private fun runner(events: MutableList<String>) = CinePilotTaskRunner(
        executor = directExecutor,
        postToMain = { action -> action() },
        showBlocking = { message -> events.add("blocking:$message") },
        beginBusy = { message ->
            events.add("busy:$message")
            val clear: () -> Unit = { events.add("clear:$message") }
            clear
        },
        showBlockingError = { error -> events.add("blocking-error:${error.message}") },
        showInPlaceError = { error -> events.add("in-place-error:${error.message}") },
    )
}
