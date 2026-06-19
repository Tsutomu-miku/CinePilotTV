package tv.cinepilot.tv.playback

import kotlin.math.abs

internal class PlaybackProgressGate(
    private val intervalMs: Long = DEFAULT_INTERVAL_MS,
    private val seekJumpMs: Long = DEFAULT_SEEK_JUMP_MS,
    private val nowMs: () -> Long = System::currentTimeMillis,
) {
    private var lastDispatchWallMs: Long = Long.MIN_VALUE
    private var lastDispatchPositionMs: Long = Long.MIN_VALUE

    fun reset() {
        lastDispatchWallMs = Long.MIN_VALUE
        lastDispatchPositionMs = Long.MIN_VALUE
    }

    fun shouldDispatch(positionMs: Long): Boolean {
        val now = nowMs()
        val first = lastDispatchWallMs == Long.MIN_VALUE
        val intervalElapsed = !first && now - lastDispatchWallMs >= intervalMs
        val jumped = !first && abs(positionMs - lastDispatchPositionMs) >= seekJumpMs
        if (!first && !intervalElapsed && !jumped) return false
        markDispatched(positionMs, now)
        return true
    }

    fun markDispatched(positionMs: Long) {
        markDispatched(positionMs, nowMs())
    }

    private fun markDispatched(positionMs: Long, now: Long) {
        lastDispatchWallMs = now
        lastDispatchPositionMs = positionMs
    }

    companion object {
        const val DEFAULT_INTERVAL_MS = 10_000L
        const val DEFAULT_SEEK_JUMP_MS = 5_000L
    }
}
