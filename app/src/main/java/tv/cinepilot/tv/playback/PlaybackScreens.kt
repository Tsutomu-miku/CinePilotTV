package tv.cinepilot.tv.playback

import android.view.View
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.ScrollView
import androidx.activity.ComponentActivity
import tv.cinepilot.core.tv.TvAppState
import tv.cinepilot.tv.ui.TvIcon
import tv.cinepilot.tv.ui.action
import tv.cinepilot.tv.ui.choiceAction
import tv.cinepilot.tv.ui.iconAction
import tv.cinepilot.tv.ui.label
import tv.cinepilot.tv.ui.playbackSpeedOptions
import tv.cinepilot.tv.ui.requestInitialFocus
import tv.cinepilot.tv.ui.screen

fun ComponentActivity.playbackSpeedScreen(
    onSpeed: (Float) -> Unit,
): ScrollView {
    return screen("播放速度") {
        addView(playbackSpeedChoiceRow(onSpeed))
    }
}

private fun ComponentActivity.playbackSpeedChoiceRow(
    onSpeed: (Float) -> Unit,
): HorizontalScrollView {
    val row = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        playbackSpeedOptions().forEach { option ->
            val selected = option.rate == DEFAULT_PLAYBACK_RATE
            val optionAction = choiceAction(option.label, selected) { onSpeed(option.rate) }
            addView(if (selected) optionAction.requestInitialFocus() else optionAction)
        }
    }
    return HorizontalScrollView(this).apply {
        isHorizontalScrollBarEnabled = false
        addView(row)
    }
}

fun ComponentActivity.playerReadyScreen(
    state: TvAppState,
    onOpenPlayer: () -> Unit,
    onDiagnostics: () -> Unit,
): ScrollView {
    val playable = state.playableMedia()
    return screen("准备播放") {
        addView(label("播放方式：${playable?.playMethod() ?: ""}"))
        addView(label("媒体源：${playable?.mediaSourceId() ?: ""}"))
        addView(label("播放地址已准备"))
        addView(iconAction("打开播放器", TvIcon.PLAY, onOpenPlayer).requestInitialFocus())
        addView(action("诊断信息", onDiagnostics))
    }
}

fun ComponentActivity.diagnosticsScreen(
    diagnostics: String,
    returnToPlayer: Boolean,
    backLabel: String? = null,
    onExport: () -> Unit,
    onShare: () -> Unit,
    onBackDiagnosticsTarget: () -> Unit,
): ScrollView {
    return screen("诊断信息") {
        addView(label(diagnostics))
        addView(action("导出诊断", onExport).requestInitialFocus())
        addView(action("分享诊断", onShare))
        addView(diagnosticsBackAction(returnToPlayer, backLabel, onBackDiagnosticsTarget))
    }
}

fun ComponentActivity.diagnosticsExportedScreen(
    path: String,
    returnToPlayer: Boolean,
    backLabel: String? = null,
    onShare: () -> Unit,
    onBackDiagnostics: () -> Unit,
    onBackDiagnosticsTarget: () -> Unit,
): ScrollView {
    return screen("诊断信息") {
        addView(label("诊断已导出：$path"))
        addView(action("分享诊断", onShare).requestInitialFocus())
        addView(iconAction("返回诊断信息", TvIcon.BACK, onBackDiagnostics))
        addView(diagnosticsBackAction(returnToPlayer, backLabel, onBackDiagnosticsTarget))
    }
}

private fun ComponentActivity.diagnosticsBackAction(
    returnToPlayer: Boolean,
    backLabel: String?,
    onBackDiagnosticsTarget: () -> Unit,
): View {
    val label = if (backLabel != null) {
        backLabel
    } else if (returnToPlayer) {
        "返回播放器"
    } else {
        "返回播放准备"
    }
    return iconAction(label, TvIcon.BACK, onBackDiagnosticsTarget)
}

private const val DEFAULT_PLAYBACK_RATE = 1.0f
