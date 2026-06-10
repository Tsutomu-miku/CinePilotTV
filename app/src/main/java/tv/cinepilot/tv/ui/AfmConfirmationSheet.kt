package tv.cinepilot.tv.ui

import android.text.TextUtils
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.ComponentActivity

data class DisplayModeSwitchPrompt(
    val resolutionLabel: String,
    val refreshRateLabel: String,
) {
    companion object {
        fun from(mode: android.view.Display.Mode): DisplayModeSwitchPrompt {
            val resolution = when {
                mode.physicalHeight >= 2160 -> "4K (${mode.physicalWidth}×${mode.physicalHeight})"
                mode.physicalHeight >= 1080 -> "1080p (${mode.physicalWidth}×${mode.physicalHeight})"
                mode.physicalHeight >= 720 -> "720p (${mode.physicalWidth}×${mode.physicalHeight})"
                else -> "${mode.physicalWidth}×${mode.physicalHeight}"
            }
            return DisplayModeSwitchPrompt(resolution, String.format("%.0f Hz", mode.refreshRate))
        }
    }
}

/**
 * Focused right-side confirmation sheet shown before applying an AFM display-mode switch.
 * Presents three choices:
 *   1. 立即切换 — commit the pending switch
 *   2. 本次不切换 — skip this single playback
 *   3. 不再提醒 — persist a "skip forever" flag, then commit the switch
 */
fun ComponentActivity.afmConfirmationSheet(
    prompt: DisplayModeSwitchPrompt,
    currentModeLabel: String,
    onSwitchNow: () -> Unit,
    onSkipOnce: () -> Unit,
    onAlwaysSkip: () -> Unit,
): View {
    return sideSheet {
        addView(infusePanelTitle("切换显示模式"))
        addView(afmMetaRow("当前模式", currentModeLabel))
        addView(afmMetaRow("建议分辨率", prompt.resolutionLabel))
        addView(afmMetaRow("建议帧率", prompt.refreshRateLabel))
        addView(TextView(this@afmConfirmationSheet).apply {
            text = "内容帧率与当前屏幕模式不匹配。\n切换后可消除画面抖动与撕裂。"
            textSize = 12.5f
            setTextColor(TvColors.TextSecondary)
            includeFontPadding = false
            setPadding(0, dp(6), 0, dp(14))
            setLineSpacing(2f, 1.05f)
        })
        addView(detailsActions(listOf(
            InfuseAction("立即切换", TvIcon.PLAY, InfuseActionEmphasis.PRIMARY, onSwitchNow),
            InfuseAction("本次不切换", TvIcon.BACK, InfuseActionEmphasis.SECONDARY, onSkipOnce),
            InfuseAction("不再提醒", TvIcon.CHECK, InfuseActionEmphasis.QUIET, onAlwaysSkip),
        )))
    }
}

private fun ComponentActivity.afmMetaRow(label: String, value: String): View {
    return LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        setPadding(0, dp(6), 0, dp(4))
        addView(TextView(this@afmMetaRow).apply {
            text = label
            textSize = 12f
            setTextColor(TvColors.TextMuted)
            includeFontPadding = false
            maxLines = 1
            ellipsize = TextUtils.TruncateAt.END
            layoutParams = LinearLayout.LayoutParams(
                dp(140),
                ViewGroup.LayoutParams.WRAP_CONTENT,
            )
        })
        addView(TextView(this@afmMetaRow).apply {
            text = value
            textSize = 12f
            setTextColor(TvColors.TextPrimary)
            includeFontPadding = false
            maxLines = 1
            ellipsize = TextUtils.TruncateAt.END
        })
    }
}
