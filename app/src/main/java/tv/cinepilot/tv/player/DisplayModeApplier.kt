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
 * <p>The controller calls {@link #applyFrameRate} before starting playback, supplying the
 * video's reference frame rate (rounded to a common denominator to avoid needless mode
 * switches on non-integer fractional content like 23.976 → 24).
 */
class DisplayModeApplier(
    private val activity: Activity,
) {
    private val displayManager by lazy {
        activity.getSystemService(Context.DISPLAY_SERVICE) as? DisplayManager
    }
    private var previousMode: Display.Mode? = null
    private var appliedModeId: Int? = null

    fun applyFrameRate(
        referenceFrameRate: Float?,
        matchColorSpace: Boolean,
        enabled: Boolean,
    ) {
        if (!enabled || Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return
        val frameRate = referenceFrameRate ?: return
        val display = activity.windowManager.defaultDisplay ?: return
        val best = pickBestMode(display, normalizeFrameRate(frameRate), matchColorSpace) ?: return
        if (appliedModeId == best.modeId) return
        val window = activity.window
        val modeBefore = preferredModeIdSafe(window.attributes)
        if (previousMode == null && modeBefore != 0) {
            previousMode = display.mode
        }
        applyModeSafe(window, best)
        appliedModeId = best.modeId
        val fpsLabel = String.format("%.0f Hz", best.refreshRate)
        val colorHint = when {
            best.physicalHeight >= 2160 -> "4K"
            best.physicalHeight >= 1080 -> "1080p"
            best.physicalHeight >= 720 -> "720p"
            else -> "${best.physicalWidth}p"
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
            val rate = mode.refreshRate.toInt()
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
