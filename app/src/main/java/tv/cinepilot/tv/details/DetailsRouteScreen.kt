package tv.cinepilot.tv.details

import android.view.View
import android.widget.ImageView
import androidx.activity.ComponentActivity
import tv.cinepilot.core.protocol.MediaItemSummary
import tv.cinepilot.core.protocol.PlaybackInfo
import tv.cinepilot.core.protocol.PlaybackSelectionPreferences
import tv.cinepilot.core.tv.HomeRow
import tv.cinepilot.tv.runtime.ArtworkTarget
import tv.cinepilot.tv.ui.HomeRowPresentation
import tv.cinepilot.tv.ui.InfuseAction
import tv.cinepilot.tv.ui.InfuseActionEmphasis
import tv.cinepilot.tv.ui.RowVisualStyle
import tv.cinepilot.tv.ui.TvIcon
import tv.cinepilot.tv.ui.browseChildrenLabel
import tv.cinepilot.tv.ui.detailsScreen
import tv.cinepilot.tv.ui.isEpisode
import tv.cinepilot.tv.ui.mediaWallRow
import tv.cinepilot.tv.ui.mediaTechnicalPills
import tv.cinepilot.tv.ui.userRatingRow

fun ComponentActivity.detailsRouteScreen(
    item: MediaItemSummary,
    playbackInfo: PlaybackInfo?,
    loadPosterImage: (ImageView, MediaItemSummary, Int, Int) -> Unit,
    loadBackdropImage: (ImageView, MediaItemSummary, Int, Int) -> Unit,
    loadArtworkImage: (ImageView, MediaItemSummary, ArtworkTarget, Int, Int) -> Unit,
    siblingEpisodes: List<MediaItemSummary>,
    sameCollectionItems: List<MediaItemSummary> = emptyList(),
    trackSelection: DetailTrackSelection,
    onPreparePlayback: (PlaybackSelectionPreferences?) -> Unit,
    onTrackSelection: (DetailTrackSelection) -> Unit,
    onSubtitleStyle: () -> Unit,
    onPlaybackSpeed: () -> Unit,
    onSeriesNextUp: () -> Unit,
    onOpenEpisodePicker: () -> Unit,
    onOpenSeries: () -> Unit,
    onOpenEpisode: (MediaItemSummary) -> Unit,
    onOpenCollectionItem: (MediaItemSummary) -> Unit = {},
    onOpenFolder: () -> Unit,
    onToggleFavorite: () -> Unit,
    onToggleWatched: () -> Unit,
    onSetUserRating: (Double?) -> Unit = {},
    onProviderBadgeClick: (String) -> Unit,
): View {
    val ratingExtra = listOf(userRatingRow(
        currentRating = item.userData().userRating(),
        onChange = onSetUserRating,
        communityRating = item.communityRating(),
    ))
    return detailsScreen(
        item = item,
        playbackActions = if (item.playable()) {
            playbackActions(
                item,
                onPreparePlayback,
                onSubtitleStyle,
                onPlaybackSpeed,
                onSeriesNextUp,
                onOpenEpisodePicker,
                onOpenSeries,
                onToggleFavorite,
                onToggleWatched,
            )
        } else {
            emptyList()
        },
        trackControls = detailTrackControls(playbackInfo, trackSelection, onTrackSelection),
        technicalInfo = mediaTechnicalPills(playbackInfo),
        extraSections = ratingExtra +
            episodeStrip(item, siblingEpisodes, onOpenEpisode, loadArtworkImage) +
            collectionStrip(item, sameCollectionItems, onOpenCollectionItem, loadArtworkImage),
        folderAction = InfuseAction(folderActionLabel(item), TvIcon.FORWARD, InfuseActionEmphasis.PRIMARY, onOpenFolder),
        onProviderBadgeClick = onProviderBadgeClick,
        loadPoster = { poster, mediaItem, width, height ->
            loadPosterImage(poster, mediaItem, width, height)
        },
        loadBackdrop = { backdrop, mediaItem ->
            loadBackdropImage(backdrop, mediaItem, 1280, 720)
        },
    )
}

private fun ComponentActivity.episodeStrip(
    item: MediaItemSummary,
    episodes: List<MediaItemSummary>,
    onOpenEpisode: (MediaItemSummary) -> Unit,
    loadArtworkImage: (ImageView, MediaItemSummary, ArtworkTarget, Int, Int) -> Unit,
): List<View> {
    if (!item.isEpisode() || episodes.isEmpty()) {
        return emptyList()
    }
    return listOf(mediaWallRow(
        presentation = HomeRowPresentation(
            row = HomeRow("episode-detail:${item.parentId()}", "本季集数", episodes),
            title = "本季集数",
            visualStyle = RowVisualStyle.LANDSCAPE_RAIL,
            wrapItems = false,
        ),
        onCell = { _, _ -> },
        onFocus = { _, _ -> },
        onOpen = { _, episode -> onOpenEpisode(episode) },
        loadArtwork = loadArtworkImage,
    ))
}

private fun ComponentActivity.collectionStrip(
    item: MediaItemSummary,
    items: List<MediaItemSummary>,
    onOpen: (MediaItemSummary) -> Unit,
    loadArtwork: (ImageView, MediaItemSummary, ArtworkTarget, Int, Int) -> Unit,
): List<View> {
    val filtered = items.filterNot { it.id() == item.id() }
    if (filtered.isEmpty()) return emptyList()
    return listOf(mediaWallRow(
        presentation = HomeRowPresentation(
            row = HomeRow("detail:collection:${item.id()}", "同系列其他", filtered),
            title = "同系列其他",
            visualStyle = RowVisualStyle.POSTER_RAIL,
            wrapItems = false,
        ),
        onCell = { _, _ -> },
        onFocus = { _, _ -> },
        onOpen = { _, entry -> onOpen(entry) },
        loadArtwork = loadArtwork,
    ))
}

private fun ComponentActivity.playbackActions(
    item: MediaItemSummary,
    onPreparePlayback: (PlaybackSelectionPreferences?) -> Unit,
    onSubtitleStyle: () -> Unit,
    onPlaybackSpeed: () -> Unit,
    onSeriesNextUp: () -> Unit,
    onOpenEpisodePicker: () -> Unit,
    onOpenSeries: () -> Unit,
    onToggleFavorite: () -> Unit,
    onToggleWatched: () -> Unit,
): List<InfuseAction> {
    val actions = mutableListOf<InfuseAction>()
    if (item.hasResumePosition()) {
        actions.add(playbackAction("继续播放", TvIcon.PLAY, InfuseActionEmphasis.PRIMARY, null, onPreparePlayback))
        actions.add(playbackAction("从头播放", TvIcon.PLAY, InfuseActionEmphasis.SECONDARY, PlaybackSelectionPreferences.defaults(), onPreparePlayback))
    } else {
        actions.add(playbackAction("播放", TvIcon.PLAY, InfuseActionEmphasis.PRIMARY, null, onPreparePlayback))
    }
    actions.add(InfuseAction(
        if (item.userData().favorite()) "已收藏" else "收藏",
        TvIcon.HEART,
        InfuseActionEmphasis.SECONDARY,
        onToggleFavorite,
    ))
    actions.add(InfuseAction(
        if (item.userData().played()) "取消已看" else "标记已看",
        TvIcon.CHECK,
        InfuseActionEmphasis.SECONDARY,
        onToggleWatched,
    ))
    actions.add(playbackAction("省流量", TvIcon.SPEED, InfuseActionEmphasis.QUIET, lowBitratePreferences(item), onPreparePlayback))
    actions.add(InfuseAction("字幕样式", TvIcon.SUBTITLES, InfuseActionEmphasis.QUIET, onSubtitleStyle))
    actions.add(InfuseAction("速度", TvIcon.SPEED, InfuseActionEmphasis.QUIET, onPlaybackSpeed))
    if (item.isEpisode() && item.parentId().isNotBlank()) {
        actions.add(InfuseAction("选集", TvIcon.FORWARD, InfuseActionEmphasis.QUIET, onOpenEpisodePicker))
    }
    if (item.seriesId().isNotBlank()) {
        actions.add(InfuseAction("剧集", TvIcon.FORWARD, InfuseActionEmphasis.QUIET, onOpenSeries))
    }
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

private fun folderActionLabel(item: MediaItemSummary): String {
    return item.browseChildrenLabel().ifBlank { "打开子项目" }
}
