package tv.cinepilot.tv.details

import android.graphics.Color
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.activity.ComponentActivity
import tv.cinepilot.core.protocol.MediaItemSummary
import tv.cinepilot.core.protocol.PlaybackInfo
import tv.cinepilot.core.protocol.PlaybackSelectionPreferences
import tv.cinepilot.tv.ui.TvIcon
import tv.cinepilot.tv.ui.TvSize
import tv.cinepilot.tv.ui.action
import tv.cinepilot.tv.ui.detailsScreen
import tv.cinepilot.tv.ui.dp
import tv.cinepilot.tv.ui.episodeLabel
import tv.cinepilot.tv.ui.formatPlaybackPosition
import tv.cinepilot.tv.ui.iconAction
import tv.cinepilot.tv.ui.mediaTechnicalPills
import tv.cinepilot.tv.ui.rounded

fun ComponentActivity.detailsRouteScreen(
    item: MediaItemSummary,
    playbackInfo: PlaybackInfo?,
    loadPosterImage: (ImageView, MediaItemSummary, Int, Int) -> Unit,
    onPreparePlayback: (PlaybackSelectionPreferences?) -> Unit,
    onPlaybackOptions: () -> Unit,
    onSubtitleStyle: () -> Unit,
    onPlaybackSpeed: () -> Unit,
    onSeriesNextUp: () -> Unit,
    onOpenFolder: () -> Unit,
): View {
    return detailsScreen(
        item = item,
        episodeLabel = episodeLabel(item),
        formatTicks = ::formatPlaybackPosition,
        playbackActions = if (item.playable()) {
            playbackActions(item, onPreparePlayback, onPlaybackOptions, onSubtitleStyle, onPlaybackSpeed, onSeriesNextUp)
        } else {
            emptyList()
        },
        technicalInfo = mediaTechnicalPills(playbackInfo),
        folderAction = action("打开子项目", onOpenFolder),
        loadPoster = { container, mediaItem ->
            addPosterIfAvailable(container, mediaItem, loadPosterImage)
        },
    )
}

private fun ComponentActivity.playbackActions(
    item: MediaItemSummary,
    onPreparePlayback: (PlaybackSelectionPreferences?) -> Unit,
    onPlaybackOptions: () -> Unit,
    onSubtitleStyle: () -> Unit,
    onPlaybackSpeed: () -> Unit,
    onSeriesNextUp: () -> Unit,
): List<View> {
    val actions = mutableListOf<View>()
    if (item.hasResumePosition()) {
        actions.add(playbackAction("继续播放", TvIcon.PLAY, null, onPreparePlayback))
        actions.add(playbackAction("从头播放", TvIcon.PLAY, PlaybackSelectionPreferences.defaults(), onPreparePlayback))
    } else {
        actions.add(playbackAction("播放", TvIcon.PLAY, null, onPreparePlayback))
    }
    actions.add(playbackAction("低码率播放", TvIcon.SPEED, lowBitratePreferences(item), onPreparePlayback))
    actions.add(iconAction("音轨 / 字幕", TvIcon.SUBTITLES, onPlaybackOptions))
    actions.add(iconAction("字幕样式", TvIcon.SUBTITLES, onSubtitleStyle))
    actions.add(iconAction("播放速度", TvIcon.SPEED, onPlaybackSpeed))
    if (item.seriesId().isNotBlank()) {
        actions.add(iconAction("本剧下一集", TvIcon.PLAY, onSeriesNextUp))
    }
    return actions
}

private fun ComponentActivity.playbackAction(
    text: String,
    icon: TvIcon,
    preferences: PlaybackSelectionPreferences?,
    onPreparePlayback: (PlaybackSelectionPreferences?) -> Unit,
): View {
    return iconAction(text, icon) {
        onPreparePlayback(preferences)
    }
}

private fun lowBitratePreferences(item: MediaItemSummary): PlaybackSelectionPreferences {
    val startTimeTicks = if (item.hasResumePosition()) item.userData().playbackPositionTicks() else 0L
    return PlaybackSelectionPreferences.lowBitrate(startTimeTicks)
}

private fun ComponentActivity.addPosterIfAvailable(
    container: LinearLayout,
    item: MediaItemSummary,
    loadPosterImage: (ImageView, MediaItemSummary, Int, Int) -> Unit,
) {
    val poster = ImageView(this).apply {
        contentDescription = "${item.name()} 海报"
        scaleType = ImageView.ScaleType.CENTER_CROP
        setBackground(rounded(Color.rgb(30, 41, 59), dp(8)))
        adjustViewBounds = false
    }
    container.addView(poster, LinearLayout.LayoutParams(dp(TvSize.DetailPosterWidth), dp(TvSize.DetailPosterHeight)).apply {
        rightMargin = dp(22)
        bottomMargin = dp(16)
    })
    loadPosterImage(poster, item, 240, 360)
}
