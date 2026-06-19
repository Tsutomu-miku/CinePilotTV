package tv.cinepilot.tv.playback

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaybackProgressGateTest {
    @Test fun throttlesProgressButAllowsSeekJumpsAndReset() {
        var now = 1_000L
        val gate = PlaybackProgressGate(
            intervalMs = 10_000L,
            seekJumpMs = 5_000L,
            nowMs = { now },
        )

        assertTrue(gate.shouldDispatch(0L))
        now += 500L
        assertFalse(gate.shouldDispatch(500L))

        now += 500L
        assertTrue(gate.shouldDispatch(8_000L))

        now += 9_000L
        assertFalse(gate.shouldDispatch(9_000L))

        now += 1_000L
        assertTrue(gate.shouldDispatch(10_000L))

        gate.reset()
        assertTrue(gate.shouldDispatch(10_500L))
    }
}
