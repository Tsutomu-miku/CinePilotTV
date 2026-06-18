package tv.cinepilot.tv.compose.screens.details

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import tv.cinepilot.core.protocol.AuthenticatedServer
import tv.cinepilot.core.protocol.MediaItemSummary
import tv.cinepilot.core.protocol.PlaybackInfo
import tv.cinepilot.core.protocol.PlaybackSelectionPreferences
import tv.cinepilot.tv.compose.artwork.rememberArtworkRequest
import tv.cinepilot.tv.compose.components.MediaRail
import tv.cinepilot.tv.compose.components.PosterCard
import tv.cinepilot.tv.compose.theme.CinePilotPalette
import tv.cinepilot.tv.compose.theme.TvDp
import tv.cinepilot.tv.compose.theme.TvText
import tv.cinepilot.tv.details.DetailTrackSelection
import tv.cinepilot.tv.runtime.ArtworkRequestFactory
import tv.cinepilot.tv.runtime.ArtworkTarget
import tv.cinepilot.tv.ui.isEpisode
import tv.cinepilot.tv.ui.mediaTechnicalPills
import tv.cinepilot.tv.ui.toDetailPresentation

// ── Text size constants ─────────────────────────────────────────────────

private val OverviewTextSize = 12.sp

/**
 * Calculate a 0..1 playback progress from an item's resume position and
 * runtime. Returns 0 for items without a resume point or runtime metadata.
 */
internal fun calculateProgress(item: MediaItemSummary): Float {
    if (!item.hasResumePosition()) return 0f
    val runtime = item.runTimeTicks() ?: return 0f
    if (runtime <= 0L) return 0f
    val position = item.userData().playbackPositionTicks()
    return (position.toFloat() / runtime.toFloat()).coerceIn(0f, 1f)
}

// ── Top-level screen (public entry point) ──────────────────────────────

@Composable
fun ComposeDetailsScreen(
    palette: CinePilotPalette,
    owner: ComponentActivity,
    artworkFactory: ArtworkRequestFactory,
    authenticated: AuthenticatedServer?,
    item: MediaItemSummary,
    playbackInfo: PlaybackInfo?,
    siblingEpisodes: List<MediaItemSummary>,
    sameCollectionItems: List<MediaItemSummary>,
    trackSelection: DetailTrackSelection,
    supportedHdrTypes: Set<String>,
    supportedPassthroughCodecs: Set<String>,
    onPreparePlayback: (PlaybackSelectionPreferences?) -> Unit,
    onTrackSelection: (DetailTrackSelection) -> Unit,
    onSubtitleStyle: () -> Unit,
    onPlaybackSpeed: () -> Unit,
    onSeriesNextUp: () -> Unit,
    onOpenEpisodePicker: () -> Unit,
    onOpenSeries: () -> Unit,
    onOpenEpisode: (MediaItemSummary) -> Unit,
    onOpenCollectionItem: (MediaItemSummary) -> Unit,
    onOpenFolder: () -> Unit,
    onToggleFavorite: () -> Unit,
    onToggleWatched: () -> Unit,
    onSetUserRating: (Double?) -> Unit,
    onOpenProviderIdsEditor: () -> Unit,
    onProviderBadgeClick: (String) -> Unit,
    onChooseDownloadQuality: (Int) -> Unit,
    onManageOffline: () -> Unit,
    offlineActionLabel: String?,
    offlineActionIsReady: Boolean,
    onAddToPlaylist: () -> Unit,
    onSearchSubtitles: () -> Unit,
    hasSubtitleSearch: Boolean,
) {
    val technicalInfo = mediaTechnicalPills(playbackInfo, supportedHdrTypes, supportedPassthroughCodecs)
    val presentation = item.toDetailPresentation(technicalTags = technicalInfo)
    val backdrop = rememberArtworkRequest(
        factory = artworkFactory,
        authenticated = authenticated,
        item = item,
        target = ArtworkTarget.BACKDROP,
        width = 1280,
        height = 720,
    )
    val actions = rememberDetailActions(
        item = item,
        onPreparePlayback = onPreparePlayback,
        onSubtitleStyle = onSubtitleStyle,
        onPlaybackSpeed = onPlaybackSpeed,
        onSeriesNextUp = onSeriesNextUp,
        onOpenEpisodePicker = onOpenEpisodePicker,
        onOpenSeries = onOpenSeries,
        onOpenFolder = onOpenFolder,
        onToggleFavorite = onToggleFavorite,
        onToggleWatched = onToggleWatched,
        onOpenProviderIdsEditor = onOpenProviderIdsEditor,
        onChooseDownloadQuality = onChooseDownloadQuality,
        onManageOffline = onManageOffline,
        offlineActionLabel = offlineActionLabel,
        offlineActionIsReady = offlineActionIsReady,
        onAddToPlaylist = onAddToPlaylist,
        onSearchSubtitles = onSearchSubtitles,
        hasSubtitleSearch = hasSubtitleSearch,
    )

    val playbackProgress = remember(item) {
        calculateProgress(item)
    }

    DetailsStage(
        backdrop = backdrop,
        palette = palette,
    ) {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(18.dp),
            contentPadding = PaddingValues(
                start = TvDp.ScreenX,
                top = TvDp.ScreenTop,
                end = TvDp.ScreenX,
                bottom = TvDp.ScreenBottom,
            ),
            modifier = Modifier.fillMaxSize(),
        ) {
            item {
                DetailsHero(
                    palette = palette,
                    artworkFactory = artworkFactory,
                    authenticated = authenticated,
                    item = item,
                    title = presentation.title,
                    contextLine = presentation.contextLine,
                    qualityBadges = presentation.qualityBadges,
                    providerBadges = presentation.providerBadges.map { it.label to it.externalUrl },
                    actions = actions,
                    playbackProgress = playbackProgress,
                    onProviderBadgeClick = onProviderBadgeClick,
                )
            }
            item {
                UserRatingRow(
                    palette = palette,
                    currentRating = item.userData().userRating(),
                    communityRating = item.communityRating(),
                    onChange = onSetUserRating,
                )
            }
            val hasTracks = playbackInfo?.mediaSources().orEmpty().isNotEmpty()
            if (hasTracks) {
                item {
                    DetailTrackOptions(
                        palette = palette,
                        playbackInfo = playbackInfo,
                        selection = trackSelection,
                        onSelection = onTrackSelection,
                    )
                }
            }
            if (presentation.overview.isNotBlank()) {
                item {
                    BasicText(
                        text = presentation.overview,
                        style = TextStyle(
                            color = palette.textMuted,
                            fontSize = OverviewTextSize,
                            lineHeight = (OverviewTextSize.value * 1.4f).sp,
                        ),
                    )
                }
            }
            if (item.isEpisode() && siblingEpisodes.isNotEmpty()) {
                item {
                    EpisodeRow(
                        palette = palette,
                        artworkFactory = artworkFactory,
                        authenticated = authenticated,
                        episodes = siblingEpisodes,
                        currentItem = item,
                        onEpisodeClick = { ep -> onOpenEpisode(ep) },
                    )
                }
            }
            val related = sameCollectionItems.filterNot { it.id() == item.id() }
            if (related.isNotEmpty()) {
                item {
                    DetailSection(title = "同系列其他", palette = palette) {
                        MediaRail(
                            palette = palette,
                            title = "",
                            items = related,
                            key = { it.id() },
                        ) { rel ->
                            PosterCard(
                                palette = palette,
                                item = rel,
                                artwork = rememberArtworkRequest(
                                    factory = artworkFactory,
                                    authenticated = authenticated,
                                    item = rel,
                                    target = ArtworkTarget.POSTER,
                                    width = 300,
                                    height = 450,
                                ),
                                onFocus = {},
                                onClick = { onOpenCollectionItem(rel) },
                            )
                        }
                    }
                }
            }
        }
    }
}

