package tv.cinepilot.tv.playback

import android.view.View
import android.widget.LinearLayout
import android.widget.ScrollView
import androidx.activity.ComponentActivity
import tv.cinepilot.core.protocol.MediaItemSummary
import tv.cinepilot.core.protocol.MediaSourceInfo
import tv.cinepilot.core.protocol.MediaStreamInfo
import tv.cinepilot.core.protocol.MediaStreamType
import tv.cinepilot.core.protocol.PlaybackInfo
import tv.cinepilot.core.tv.TvAppState
import tv.cinepilot.tv.ui.TvIcon
import tv.cinepilot.tv.ui.action
import tv.cinepilot.tv.ui.iconAction
import tv.cinepilot.tv.ui.label
import tv.cinepilot.tv.ui.playbackSpeedOptions
import tv.cinepilot.tv.ui.screen
import tv.cinepilot.tv.ui.section
import tv.cinepilot.tv.ui.sourceLabel
import tv.cinepilot.tv.ui.streamLabel

fun ComponentActivity.playbackOptionsScreen(
    item: MediaItemSummary,
    playbackInfo: PlaybackInfo,
    onDefault: () -> Unit,
    onSource: (String) -> Unit,
    onAudio: (String, Int) -> Unit,
    onDisableSubtitles: (String) -> Unit,
    onSubtitle: (String, Int) -> Unit,
    onBackDetails: () -> Unit,
): ScrollView {
    return screen("音轨 / 字幕") {
        addView(action("按服务器默认播放", onDefault))
        playbackInfo.mediaSources().forEachIndexed { index, source ->
            addMediaSourceOptions(
                activity = this@playbackOptionsScreen,
                index = index,
                playbackInfo = playbackInfo,
                source = source,
                onSource = onSource,
                onAudio = onAudio,
                onDisableSubtitles = onDisableSubtitles,
                onSubtitle = onSubtitle,
            )
        }
        addView(action("返回详情", onBackDetails))
    }
}

fun ComponentActivity.playbackSpeedScreen(
    onSpeed: (Float) -> Unit,
    onBackDetails: () -> Unit,
): ScrollView {
    return screen("播放速度") {
        playbackSpeedOptions().forEach { option ->
            addView(action(option.label) { onSpeed(option.rate) })
        }
        addView(action("返回详情", onBackDetails))
    }
}

fun ComponentActivity.subtitleOptionsForAudioScreen(
    source: MediaSourceInfo,
    audioStreamIndex: Int,
    onDefaultSubtitles: () -> Unit,
    onDisableSubtitles: () -> Unit,
    onSubtitle: (Int) -> Unit,
    onBackTracks: () -> Unit,
): ScrollView {
    val selectedAudio = source.mediaStreams()
        .firstOrNull { stream -> stream.type() == MediaStreamType.AUDIO && stream.index() == audioStreamIndex }
    val subtitleStreams = source.streamsOf(MediaStreamType.SUBTITLE)
    return screen("选择字幕") {
        addView(label("已选择音轨：${selectedAudio?.let(::streamLabel) ?: "音轨 $audioStreamIndex"}"))
        addView(action("使用服务器默认字幕") { onDefaultSubtitles() })
        addView(action("关闭字幕播放") { onDisableSubtitles() })
        if (subtitleStreams.isEmpty()) {
            addView(label("服务器未返回可选字幕"))
        } else {
            subtitleStreams.forEach { stream ->
                addView(action("字幕 ${stream.index()}：${streamLabel(stream)}") {
                    onSubtitle(stream.index())
                })
            }
        }
        addView(iconAction("返回音轨 / 字幕", TvIcon.BACK, onBackTracks))
    }
}

fun ComponentActivity.playerReadyScreen(
    state: TvAppState,
    onOpenPlayer: () -> Unit,
    onDiagnostics: () -> Unit,
    onBackDetails: () -> Unit,
): ScrollView {
    val playable = state.playableMedia()
    return screen("准备播放") {
        addView(label("播放方式：${playable?.playMethod() ?: ""}"))
        addView(label("媒体源：${playable?.mediaSourceId() ?: ""}"))
        addView(label("播放地址已准备"))
        addView(iconAction("打开播放器", TvIcon.PLAY, onOpenPlayer))
        addView(action("诊断信息", onDiagnostics))
        addView(iconAction("返回详情", TvIcon.BACK, onBackDetails))
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
        addView(action("导出诊断", onExport))
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
        addView(action("分享诊断", onShare))
        addView(iconAction("返回诊断信息", TvIcon.BACK, onBackDiagnostics))
        addView(diagnosticsBackAction(returnToPlayer, backLabel, onBackDiagnosticsTarget))
    }
}

private fun LinearLayout.addMediaSourceOptions(
    activity: ComponentActivity,
    index: Int,
    playbackInfo: PlaybackInfo,
    source: MediaSourceInfo,
    onSource: (String) -> Unit,
    onAudio: (String, Int) -> Unit,
    onDisableSubtitles: (String) -> Unit,
    onSubtitle: (String, Int) -> Unit,
) {
    val audioStreams = source.streamsOf(MediaStreamType.AUDIO)
    val subtitleStreams = source.streamsOf(MediaStreamType.SUBTITLE)
    val sourceTitle = if (playbackInfo.mediaSources().size > 1) {
        "媒体源 ${index + 1}"
    } else {
        "媒体源"
    }
    addView(activity.section(sourceTitle))
    addView(activity.action(sourceLabel(source)) { onSource(source.id()) })
    addView(activity.section("音轨"))
    if (audioStreams.isEmpty()) {
        addView(activity.label("服务器未返回可选音轨"))
    } else {
        audioStreams.forEach { stream ->
            addView(activity.action("音轨 ${stream.index()}：${streamLabel(stream)} / 继续选择字幕") {
                onAudio(source.id(), stream.index())
            })
        }
    }
    addView(activity.section("字幕"))
    addView(activity.action("关闭字幕播放") { onDisableSubtitles(source.id()) })
    if (subtitleStreams.isEmpty()) {
        addView(activity.label("服务器未返回可选字幕"))
    } else {
        subtitleStreams.forEach { stream ->
            addView(activity.action("字幕 ${stream.index()}：${streamLabel(stream)}") {
                onSubtitle(source.id(), stream.index())
            })
        }
    }
}

private fun MediaSourceInfo.streamsOf(type: MediaStreamType): List<MediaStreamInfo> {
    return mediaStreams().filter { stream -> stream.type() == type }
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
