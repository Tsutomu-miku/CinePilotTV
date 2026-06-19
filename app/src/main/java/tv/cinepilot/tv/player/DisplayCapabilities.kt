package tv.cinepilot.tv.player

import android.app.Activity
import android.content.Context
import android.hardware.display.DisplayManager
import android.os.Build
import android.view.Display
import android.view.Display.HdrCapabilities

/**
 * Detects HDR capabilities of the current display.
 *
 * Uses [Display.HdrCapabilities] on API 24+ to determine which HDR types
 * the display supports. On older devices, all checks return false.
 */
class DisplayCapabilities(
    private val activity: Activity,
) {
    private val displayManager by lazy {
        activity.getSystemService(Context.DISPLAY_SERVICE) as? DisplayManager
    }

    data class HdrSupport(
        val dolbyVision: Boolean,
        val hdr10: Boolean,
        val hdr10Plus: Boolean,
        val hlg: Boolean,
    ) {
        val anyHdr: Boolean get() = dolbyVision || hdr10 || hdr10Plus || hlg

        fun supports(format: HdrFormat): Boolean = when (format) {
            HdrFormat.DOLBY_VISION -> dolbyVision
            HdrFormat.HDR10 -> hdr10
            HdrFormat.HDR10_PLUS -> hdr10Plus
            HdrFormat.HLG -> hlg
            HdrFormat.UNKNOWN -> false
        }
    }

    enum class HdrFormat {
        DOLBY_VISION,
        HDR10,
        HDR10_PLUS,
        HLG,
        UNKNOWN,
    }

    @Suppress("DEPRECATION")
    fun hdrSupport(): HdrSupport {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            return HdrSupport(false, false, false, false)
        }
        val display = displayManager?.getDisplay(Display.DEFAULT_DISPLAY)
            ?: return HdrSupport(false, false, false, false)
        val caps = runCatching { display.hdrCapabilities }.getOrNull()
            ?: return HdrSupport(false, false, false, false)
        val types = caps.supportedHdrTypes ?: intArrayOf()
        return HdrSupport(
            dolbyVision = types.contains(HdrCapabilities.HDR_TYPE_DOLBY_VISION),
            hdr10 = types.contains(HdrCapabilities.HDR_TYPE_HDR10),
            hdr10Plus = types.contains(HdrCapabilities.HDR_TYPE_HDR10_PLUS),
            hlg = types.contains(HdrCapabilities.HDR_TYPE_HLG),
        )
    }

    fun describe(): String {
        val support = hdrSupport()
        val types = mutableListOf<String>()
        if (support.dolbyVision) types.add("Dolby Vision")
        if (support.hdr10) types.add("HDR10")
        if (support.hdr10Plus) types.add("HDR10+")
        if (support.hlg) types.add("HLG")
        return if (types.isEmpty()) "SDR (无 HDR)" else types.joinToString(" / ")
    }

    companion object {
        /** Detect HDR format from a video stream's technical metadata. */
        fun detectHdrFormat(rangeType: String, range: String, profile: String, displayTitle: String): HdrFormat {
            val value = listOf(rangeType, range, profile, displayTitle).joinToString(" ").lowercase()
            return when {
                value.contains("dovi") || value.contains("dolby vision") -> HdrFormat.DOLBY_VISION
                value.contains("hdr10+") || value.contains("hdr10plus") -> HdrFormat.HDR10_PLUS
                value.contains("hdr10") -> HdrFormat.HDR10
                value.contains("hlg") -> HdrFormat.HLG
                value.contains("hdr") -> HdrFormat.HDR10 // best guess
                else -> HdrFormat.UNKNOWN
            }
        }
    }
}
