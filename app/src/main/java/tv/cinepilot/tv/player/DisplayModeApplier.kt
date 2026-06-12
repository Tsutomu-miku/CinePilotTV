package tv.cinepilot.tv.player

import android.app.Activity
import android.content.Context
import android.hardware.display.DisplayManager
import android.os.Build
import android.view.Display
import android.view.WindowManager
import android.widget.Toast
import androidx.annotation.RequiresApi
import tv.cinepilot.core.protocol.MediaTicks

/**
 * Applies automatic frame / color-space matching (AFM) on Android TV devices that expose
 * multiple Display.Mode entries. Best-effort: older TVs without alternate modes, or
 * devices below API 23, silently no-op.
 *
 * <p>Two-stage usage for callers that want to show a confirmation notice before switching:
 *
 * <ol>
 *     <li>{@link #computePendingMode(Float, boolean, boolean)} returns the candidate
 *         {@link Display.Mode} that matches the content reference frame rate; {@code null}
 *         when no switch is needed.</li>
 *     <li>{@link #commitPendingMode(Display.Mode)} actually applies the mode and shows the
 *         usual "已切换到" toast. Callers that skip user confirmation can invoke
 *         {@link #applyFrameRate(Float, boolean, boolean)} directly.</li>
 * </ol>
 */
class DisplayModeApplier(
    private val activity: Activity,
) {
    private val displayManager by lazy {
        activity.getSystemService(Context.DISPLAY_SERVICE) as? DisplayManager
    }
    private var previousMode: Display.Mode? = null
    private var appliedModeId: Int? = null

    /** Returns true if we have explicitly applied a display mode during this session. */
    fun hasAppliedMode(): Boolean = appliedModeId != null

    /**
     * One-shot helper for callers that do not want a confirmation step. Identical to
     * {@code computePendingMode(...)} followed by {@code commitPendingMode(mode)} when
     * a different mode is found.
     */
    fun applyFrameRate(
        referenceFrameRate: Float?,
        matchColorSpace: Boolean,
        enabled: Boolean,
    ) {
        val pending = computePendingMode(referenceFrameRate, matchColorSpace, enabled)
        if (pending != null) commitPendingMode(pending)
    }

    /**
     * Returns the best-matching {@link Display.Mode} for the supplied content, or
     * {@code null} when AFM is disabled, the API level is too low, the device does not
     * list alternate modes, or the currently applied mode already matches best.
     */
    fun computePendingMode(
        referenceFrameRate: Float?,
        matchColorSpace: Boolean,
        enabled: Boolean,
    ): Display.Mode? {
        if (!enabled || Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return null
        val frameRate = referenceFrameRate ?: return null
        val display = activity.windowManager.defaultDisplay ?: return null
        val best = pickBestMode(display, normalizeFrameRate(frameRate), matchColorSpace) ?: return null
        if (appliedModeId == best.modeId) return null
        val current = display.mode
        if (current.modeId == best.modeId && preferredModeIdSafe(activity.window.attributes) == best.modeId) {
            appliedModeId = best.modeId
            return null
        }
        return best
    }

    /**
     * Apply a mode previously returned from {@link #computePendingMode}. Safe to call
     * multiple times with the same mode (subsequent calls no-op).
     */
    fun commitPendingMode(mode: Display.Mode) {
        if (appliedModeId == mode.modeId) return
        val window = activity.window
        // Capture the current display mode on first switch so restorePrevious() can
        // always return to the pre-playback mode, even when starting from the default
        // (where preferredDisplayModeId is 0).
        if (previousMode == null) {
            previousMode = activity.windowManager.defaultDisplay?.mode
        }
        applyModeSafe(window, mode)
        appliedModeId = mode.modeId
        val fpsLabel = String.format("%.0f Hz", mode.refreshRate)
        val colorHint = when {
            mode.physicalHeight >= 2160 -> "4K"
            mode.physicalHeight >= 1080 -> "1080p"
            mode.physicalHeight >= 720 -> "720p"
            else -> "${mode.physicalWidth}p"
        }
        Toast.makeText(
            activity,
            "已切换到 $colorHint $fpsLabel",
            Toast.LENGTH_SHORT,
        ).show()
    }

    /** Restore the pre-playback display mode. Call when releasing the player. */
    fun restorePrevious() {
        val prev = previousMode ?: return
        applyModeSafe(activity.window, prev)
        previousMode = null
        appliedModeId = null
    }

    private fun applyModeSafe(window: android.view.Window, mode: Display.Mode) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return
        val attrs = window.attributes
        attrs.preferredDisplayModeId = mode.modeId
        attrs.preferredRefreshRate = mode.refreshRate
        window.attributes = attrs
    }

    private fun preferredModeIdSafe(layoutParams: WindowManager.LayoutParams): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            layoutParams.preferredDisplayModeId
        } else {
            0
        }
    }

    @Suppress("ReturnCount", "ComplexMethod")
    private fun pickBestMode(
        display: Display,
        targetFrameRate: Int,
        @Suppress("UNUSED_PARAMETER") matchColorSpace: Boolean,
    ): Display.Mode? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return null
        val modes = display.supportedModes ?: return null
        if (modes.isEmpty()) return null
        val current = display.mode
        data class Score(val mode: Display.Mode, val score: Int)
        val scored = modes.map { mode ->
            var score = 0
            // Round the mode refresh rate before comparing — fractional rates like
            // 23.976 Hz / 59.94 Hz should match content at 24 fps / 60 fps.
            val rate = Math.round(mode.refreshRate).toInt()
            if (rate == targetFrameRate) score += 10
            else if (targetFrameRate > 0 && rate % targetFrameRate == 0) score += 4
            if (mode.physicalHeight >= current.physicalHeight &&
                mode.physicalWidth >= current.physicalWidth) score += 3
            if (mode.physicalHeight == current.physicalHeight &&
                mode.physicalWidth == current.physicalWidth) score += 2
            Score(mode, score)
        }
        val best = scored.maxByOrNull { it.score } ?: return null
        if (best.score <= 0) return null
        return best.mode
    }

    private fun normalizeFrameRate(rate: Float): Int {
        val rounded = rate.toInt()
        val fractional = rate - rounded
        return when {
            fractional > 0.4f -> rounded + 1
            fractional < -0.4f -> (rounded - 1).coerceAtLeast(0)
            else -> rounded
        }
    }

    fun formatCurrentModeForDiagnostics(): String {
        val display = activity.windowManager.defaultDisplay
        val mode = display.mode
        return String.format("%dx%d %.1fHz",
            mode.physicalWidth, mode.physicalHeight, mode.refreshRate)
    }

    companion object {
        fun durationSecondsToTicks(seconds: Int): Long = MediaTicks.fromSeconds(seconds.toLong())
    }
}
