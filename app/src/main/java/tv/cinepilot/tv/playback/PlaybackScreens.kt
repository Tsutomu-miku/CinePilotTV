package tv.cinepilot.tv.playback

import android.view.View
import android.widget.HorizontalScrollView
import android.widget.ScrollView
import androidx.activity.ComponentActivity
import tv.cinepilot.tv.ui.TvIcon
import tv.cinepilot.tv.ui.actionColumn
import tv.cinepilot.tv.ui.iconAction
import tv.cinepilot.tv.ui.label
import tv.cinepilot.tv.ui.playbackSpeedOptions
import tv.cinepilot.tv.ui.primaryIconAction
import tv.cinepilot.tv.ui.radioChoice
import tv.cinepilot.tv.ui.requestInitialFocus
import tv.cinepilot.tv.ui.screen
import tv.cinepilot.tv.ui.settingChoiceRow

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
    return settingChoiceRow(playbackSpeedOptions().map { option ->
        val selected = option.rate == DEFAULT_PLAYBACK_RATE
        val optionAction = radioChoice(option.label, selected) { onSpeed(option.rate) }
        if (selected) optionAction.requestInitialFocus() else optionAction
    })
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
        addView(actionColumn(listOf(
            primaryIconAction("导出诊断", TvIcon.DOWNLOAD, onExport).requestInitialFocus(),
            iconAction("分享诊断", TvIcon.SHARE, onShare),
            diagnosticsBackAction(returnToPlayer, backLabel, onBackDiagnosticsTarget),
        )))
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
        addView(actionColumn(listOf(
            primaryIconAction("分享诊断", TvIcon.SHARE, onShare).requestInitialFocus(),
            iconAction("返回诊断信息", TvIcon.BACK, onBackDiagnostics),
            diagnosticsBackAction(returnToPlayer, backLabel, onBackDiagnosticsTarget),
        )))
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
        "返回错误页"
    }
    return iconAction(label, TvIcon.BACK, onBackDiagnosticsTarget)
}

private const val DEFAULT_PLAYBACK_RATE = 1.0f
