package tv.cinepilot.tv.compose.screens

import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import tv.cinepilot.core.protocol.AuthenticatedServer
import tv.cinepilot.core.protocol.MediaItemSummary
import tv.cinepilot.core.protocol.PlaybackInfo
import tv.cinepilot.core.protocol.PlaybackSelectionPreferences
import tv.cinepilot.tv.compose.theme.CinePilotPalette
import tv.cinepilot.tv.details.DetailTrackSelection
import tv.cinepilot.tv.runtime.ArtworkRequestFactory
import tv.cinepilot.tv.compose.screens.details.ComposeDetailsScreen as DetailsScreenImpl

/**
 * Thin facade that delegates to the [tv.cinepilot.tv.compose.screens.details] sub-package
 * implementation. Keeps the public API stable for existing callers.
 */
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
    DetailsScreenImpl(
        palette = palette,
        owner = owner,
        artworkFactory = artworkFactory,
        authenticated = authenticated,
        item = item,
        playbackInfo = playbackInfo,
        siblingEpisodes = siblingEpisodes,
        sameCollectionItems = sameCollectionItems,
        trackSelection = trackSelection,
        supportedHdrTypes = supportedHdrTypes,
        supportedPassthroughCodecs = supportedPassthroughCodecs,
        onPreparePlayback = onPreparePlayback,
        onTrackSelection = onTrackSelection,
        onSubtitleStyle = onSubtitleStyle,
        onPlaybackSpeed = onPlaybackSpeed,
        onSeriesNextUp = onSeriesNextUp,
        onOpenEpisodePicker = onOpenEpisodePicker,
        onOpenSeries = onOpenSeries,
        onOpenEpisode = onOpenEpisode,
        onOpenCollectionItem = onOpenCollectionItem,
        onOpenFolder = onOpenFolder,
        onToggleFavorite = onToggleFavorite,
        onToggleWatched = onToggleWatched,
        onSetUserRating = onSetUserRating,
        onOpenProviderIdsEditor = onOpenProviderIdsEditor,
        onProviderBadgeClick = onProviderBadgeClick,
        onChooseDownloadQuality = onChooseDownloadQuality,
        onManageOffline = onManageOffline,
        offlineActionLabel = offlineActionLabel,
        offlineActionIsReady = offlineActionIsReady,
        onAddToPlaylist = onAddToPlaylist,
        onSearchSubtitles = onSearchSubtitles,
        hasSubtitleSearch = hasSubtitleSearch,
    )
}
