package tv.cinepilot.tv.runtime

import android.content.Context
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Keeps a process-wide reference to the [CinePilotRuntime] so Android
 * components that outlive the MainActivity (foreground download service,
 * WorkManager workers, broadcast receivers) can reach it.
 *
 * <p>The runtime is created exactly once and never nulled out; the process
 * itself dies when Android reclaims memory. Callers on background threads
 * can use [await] with a bounded wait to recover from races during app startup.
 */
internal object CinePilotRuntimeHolder {

    @Volatile private var runtime: CinePilotRuntime? = null
    private val latch = CountDownLatch(1)

    fun attach(runtime: CinePilotRuntime) {
        if (this.runtime != null) return
        synchronized(this) {
            if (this.runtime != null) return
            this.runtime = runtime
            latch.countDown()
        }
    }

    fun get(): CinePilotRuntime? = runtime

    fun await(timeoutMs: Long = 30_000L): CinePilotRuntime {
        val cached = runtime
        if (cached != null) return cached
        latch.await(timeoutMs, TimeUnit.MILLISECONDS)
        return runtime
            ?: error("CinePilotRuntime was never attached within ${timeoutMs}ms")
    }

    /** Creates (if needed) and attaches the runtime. Called from MainActivity / app startup. */
    fun ensure(context: Context): CinePilotRuntime {
        val cached = runtime
        if (cached != null) return cached
        return synchronized(this) {
            val existing = runtime
            if (existing != null) {
                existing
            } else {
                CinePilotRuntime.create(context.applicationContext).also(::attach)
            }
        }
    }
}
