package tv.cinepilot.tv.details

import android.view.View
import android.widget.ImageView
import androidx.activity.ComponentActivity
import tv.cinepilot.core.protocol.MediaItemSummary
import tv.cinepilot.core.protocol.PlaybackInfo
import tv.cinepilot.core.protocol.PlaybackSelectionPreferences
import tv.cinepilot.tv.ui.InfuseAction
import tv.cinepilot.tv.ui.InfuseActionEmphasis
import tv.cinepilot.tv.ui.TvIcon
import tv.cinepilot.tv.ui.detailsScreen
import tv.cinepilot.tv.ui.mediaTechnicalPills

fun ComponentActivity.detailsRouteScreen(
    item: MediaItemSummary,
    playbackInfo: PlaybackInfo?,
    loadPosterImage: (ImageView, MediaItemSummary, Int, Int) -> Unit,
    loadBackdropImage: (ImageView, MediaItemSummary, Int, Int) -> Unit,
    trackSelection: DetailTrackSelection,
    onPreparePlayback: (PlaybackSelectionPreferences?) -> Unit,
    onTrackSelection: (DetailTrackSelection) -> Unit,
    onSubtitleStyle: () -> Unit,
    onPlaybackSpeed: () -> Unit,
    onSeriesNextUp: () -> Unit,
    onOpenFolder: () -> Unit,
): View {
    return detailsScreen(
        item = item,
        playbackActions = if (item.playable()) {
            playbackActions(item, onPreparePlayback, onSubtitleStyle, onPlaybackSpeed, onSeriesNextUp)
        } else {
            emptyList()
        },
        trackControls = detailTrackControls(playbackInfo, trackSelection, onTrackSelection),
        technicalInfo = mediaTechnicalPills(playbackInfo),
        folderAction = InfuseAction("打开子项目", TvIcon.FORWARD, InfuseActionEmphasis.PRIMARY, onOpenFolder),
        loadPoster = { poster, mediaItem, width, height ->
            loadPosterImage(poster, mediaItem, width, height)
        },
        loadBackdrop = { backdrop, mediaItem ->
            loadBackdropImage(backdrop, mediaItem, 1280, 720)
        },
    )
}

private fun ComponentActivity.playbackActions(
    item: MediaItemSummary,
    onPreparePlayback: (PlaybackSelectionPreferences?) -> Unit,
    onSubtitleStyle: () -> Unit,
    onPlaybackSpeed: () -> Unit,
    onSeriesNextUp: () -> Unit,
): List<InfuseAction> {
    val actions = mutableListOf<InfuseAction>()
    if (item.hasResumePosition()) {
        actions.add(playbackAction("继续播放", TvIcon.PLAY, InfuseActionEmphasis.PRIMARY, null, onPreparePlayback))
        actions.add(playbackAction("从头播放", TvIcon.PLAY, InfuseActionEmphasis.SECONDARY, PlaybackSelectionPreferences.defaults(), onPreparePlayback))
    } else {
        actions.add(playbackAction("播放", TvIcon.PLAY, InfuseActionEmphasis.PRIMARY, null, onPreparePlayback))
    }
    actions.add(playbackAction("低码率", TvIcon.SPEED, InfuseActionEmphasis.QUIET, lowBitratePreferences(item), onPreparePlayback))
    actions.add(InfuseAction("字幕样式", TvIcon.SUBTITLES, InfuseActionEmphasis.QUIET, onSubtitleStyle))
    actions.add(InfuseAction("速度", TvIcon.SPEED, InfuseActionEmphasis.QUIET, onPlaybackSpeed))
    if (item.seriesId().isNotBlank()) {
        actions.add(InfuseAction("本剧下一集", TvIcon.PLAY, InfuseActionEmphasis.QUIET, onSeriesNextUp))
    }
    return actions
}

private fun playbackAction(
    text: String,
    icon: TvIcon,
    emphasis: InfuseActionEmphasis,
    preferences: PlaybackSelectionPreferences?,
    onPreparePlayback: (PlaybackSelectionPreferences?) -> Unit,
): InfuseAction {
    return InfuseAction(text, icon, emphasis) {
        onPreparePlayback(preferences)
    }
}

private fun lowBitratePreferences(item: MediaItemSummary): PlaybackSelectionPreferences {
    val startTimeTicks = if (item.hasResumePosition()) item.userData().playbackPositionTicks() else 0L
    return PlaybackSelectionPreferences.lowBitrate(startTimeTicks)
}
