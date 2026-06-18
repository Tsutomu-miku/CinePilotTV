package tv.cinepilot.tv.compose.screens

import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import tv.cinepilot.core.protocol.MediaBrowseFilters
import tv.cinepilot.core.protocol.MediaItemSummary
import tv.cinepilot.core.tv.HomeRow
import tv.cinepilot.core.tv.TvAppState
import tv.cinepilot.tv.compose.screens.home.ComposeHomeNavigation as HomeComposeHomeNavigation
import tv.cinepilot.tv.compose.screens.home.ComposeHomeScreen as HomeComposeHomeScreen
import tv.cinepilot.tv.compose.theme.CinePilotPalette
import tv.cinepilot.tv.runtime.ArtworkRequestFactory

typealias ComposeHomeNavigation = HomeComposeHomeNavigation

@Composable
fun ComposeHomeScreen(
    palette: CinePilotPalette,
    owner: ComponentActivity,
    artworkFactory: ArtworkRequestFactory,
    state: TvAppState,
    browseFilters: MediaBrowseFilters,
    availableGenreNames: List<String>,
    navigation: ComposeHomeNavigation,
    onOpen: (HomeRow, MediaItemSummary) -> Unit,
    onFocusItem: (HomeRow, MediaItemSummary) -> Unit,
    onLibraryOverview: (viewId: String, title: String, isSeries: Boolean) -> Unit,
    onFiltersChanged: (MediaBrowseFilters) -> Unit,
) {
    HomeComposeHomeScreen(
        palette = palette,
        owner = owner,
        artworkFactory = artworkFactory,
        state = state,
        browseFilters = browseFilters,
        availableGenreNames = availableGenreNames,
        navigation = navigation,
        onOpen = onOpen,
        onFocusItem = onFocusItem,
        onLibraryOverview = onLibraryOverview,
        onFiltersChanged = onFiltersChanged,
    )
}
