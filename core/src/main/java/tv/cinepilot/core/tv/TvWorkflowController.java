package tv.cinepilot.core.tv;

import java.util.List;
import tv.cinepilot.core.AndroidCollections;
import tv.cinepilot.core.protocol.AuthenticatedServer;
import tv.cinepilot.core.protocol.ItemQuery;
import tv.cinepilot.core.protocol.MediaBrowserClient;
import tv.cinepilot.core.protocol.MediaItemPage;
import tv.cinepilot.core.protocol.MediaItemSummary;
import tv.cinepilot.core.protocol.MediaItemType;
import tv.cinepilot.core.protocol.MediaServerAddress;
import tv.cinepilot.core.protocol.PlayableMedia;
import tv.cinepilot.core.protocol.PlaybackDeviceProfile;
import tv.cinepilot.core.protocol.PlaybackInfo;
import tv.cinepilot.core.protocol.PlaybackInfoOptions;
import tv.cinepilot.core.protocol.PlaybackSelectionPreferences;
import tv.cinepilot.core.protocol.PublicUserSummary;
import tv.cinepilot.core.protocol.QuickConnectSession;
import tv.cinepilot.core.protocol.ServerIdentity;

public final class TvWorkflowController {
    public static final String NO_CHILD_ITEM_MESSAGE = "No child media item is available";
    public static final String NO_PLAYABLE_SOURCE_MESSAGE = "No playable media source is available";
    public static final String QUICK_CONNECT_DISABLED_MESSAGE = "Quick Connect is not enabled on this server";
    public static final String QUICK_CONNECT_NOT_APPROVED_MESSAGE = "Quick Connect has not been approved yet";

    private final MediaBrowserClient client;
    private final HomeRowsLoader homeRowsLoader;
    private final PlaybackDeviceProfile deviceProfile;
    private final BrowseSession browseSession = new BrowseSession();
    private QuickConnectSession pendingQuickConnect;
    private TvAppState state = TvAppState.initial();

    public TvWorkflowController(MediaBrowserClient client, HomeRowsLoader homeRowsLoader) {
        this(client, homeRowsLoader, null);
    }

    public TvWorkflowController(
            MediaBrowserClient client,
            HomeRowsLoader homeRowsLoader,
            PlaybackDeviceProfile deviceProfile
    ) {
        if (client == null) {
            throw new IllegalArgumentException("client is required");
        }
        if (homeRowsLoader == null) {
            throw new IllegalArgumentException("homeRowsLoader is required");
        }
        this.client = client;
        this.homeRowsLoader = homeRowsLoader;
        this.deviceProfile = deviceProfile;
    }

    public TvAppState state() {
        return state;
    }

    public TvAppState submitServer(String rawAddress) {
        browseSession.clear();
        MediaServerAddress address = MediaServerAddress.parse(rawAddress);
        state = TvWorkflow.submitServer(state, address);
        ServerIdentity server = client.discover(address);
        state = TvWorkflow.serverDiscovered(state, server);
        return state;
    }

    public TvAppState login(String username, String password) {
        if (state.server() == null) {
            throw new IllegalStateException("server must be discovered before login");
        }
        state = TvWorkflow.loginStarted(state);
        AuthenticatedServer authenticated = client.authenticate(state.server(), username, password);
        state = TvWorkflow.loginSucceeded(state, authenticated);
        return loadHome();
    }

    public boolean quickConnectEnabled() {
        if (state.server() == null) {
            throw new IllegalStateException("server must be discovered before Quick Connect");
        }
        return client.quickConnectEnabled(state.server());
    }

    public QuickConnectSession startQuickConnect() {
        if (state.server() == null) {
            throw new IllegalStateException("server must be discovered before Quick Connect");
        }
        if (!client.quickConnectEnabled(state.server())) {
            throw new IllegalStateException(QUICK_CONNECT_DISABLED_MESSAGE);
        }
        pendingQuickConnect = client.initiateQuickConnect(state.server());
        return pendingQuickConnect;
    }

    public TvAppState completeQuickConnect() {
        if (state.server() == null || pendingQuickConnect == null) {
            throw new IllegalStateException("Quick Connect must be started before completion");
        }
        QuickConnectSession latest = client.quickConnectState(state.server(), pendingQuickConnect.secret());
        pendingQuickConnect = latest;
        if (!latest.authenticated()) {
            throw new IllegalStateException(QUICK_CONNECT_NOT_APPROVED_MESSAGE);
        }
        AuthenticatedServer authenticated = client.authenticateWithQuickConnect(state.server(), latest.secret());
        state = TvWorkflow.loginSucceeded(state, authenticated);
        pendingQuickConnect = null;
        return loadHome();
    }

    public TvAppState loadPublicUsers() {
        if (state.server() == null) {
            throw new IllegalStateException("server must be discovered before loading public users");
        }
        List<PublicUserSummary> users = client.publicUsers(state.server());
        state = TvWorkflow.publicUsersLoaded(state, users);
        return state;
    }

    public TvAppState restoreSession(String userId) {
        if (state.server() == null) {
            throw new IllegalStateException("server must be discovered before restoring a session");
        }
        AuthenticatedServer authenticated = client.restore(state.server(), userId)
                .map(session -> new AuthenticatedServer(state.server(), session))
                .orElseThrow(() -> new IllegalStateException("saved session was not found"));
        state = TvWorkflow.loginSucceeded(state, authenticated);
        return loadHome();
    }

    public TvAppState loadHome() {
        if (state.authenticated() == null) {
            throw new IllegalStateException("authenticated session is required before loading home");
        }
        browseSession.clear();
        List<HomeRow> rows = homeRowsLoader.load(state.authenticated());
        state = TvWorkflow.homeLoaded(state, rows);
        return state;
    }

    /**
     * Push cached home rows into the state without touching the network.
     * Returns true when the cache was accepted (i.e. the controller is
     * authenticated and the cache was non-empty). Callers typically invoke
     * this at cold-start so the UI can paint instantly from disk while a
     * subsequent {@link #loadHome()} fetches fresh data in the background.
     */
    public boolean restoreHomeFromCache(List<HomeRow> cachedRows) {
        if (state.authenticated() == null || cachedRows == null || cachedRows.isEmpty()) {
            return false;
        }
        browseSession.clear();
        state = TvWorkflow.homeLoaded(state, cachedRows);
        return true;
    }

    /**
     * True when the controller is authenticated and the state already has
     * non-empty home rows (either freshly loaded or restored from cache).
     * Used by the Android layer to skip a redundant paint when a cached
     * restore was accepted just before the network load returns.
     */
    public boolean hasHomeRows() {
        List<HomeRow> rows = state.homeRows();
        return rows != null && !rows.isEmpty();
    }

    public TvAppState openItem(String itemId) {
        if (state.authenticated() == null) {
            throw new IllegalStateException("authenticated session is required before opening an item");
        }
        MediaItemSummary item = client.item(state.authenticated(), itemId);
        state = TvWorkflow.openDetails(state, item);
        return state;
    }

    public TvAppState focusItem(String rowId, String itemId) {
        state = TvWorkflow.focusItem(state, rowId, itemId);
        return state;
    }

    public TvAppState openFirstChild(String parentId) {
        if (state.authenticated() == null) {
            throw new IllegalStateException("authenticated session is required before opening a child item");
        }
        MediaItemSummary item = client.items(
                state.authenticated(),
                ItemQuery.browse().parentId(parentId).limit(1).build()
        ).items().stream().findFirst().orElseThrow(() -> new IllegalStateException(NO_CHILD_ITEM_MESSAGE));
        state = TvWorkflow.openDetails(state, item);
        return state;
    }

    public TvAppState openFolder(String parentId, String title) {
        if (state.authenticated() == null) {
            throw new IllegalStateException("authenticated session is required before opening a folder");
        }
        MediaItemPage page = folderPage(parentId, 0);
        state = browseSession.openFolder(state, parentId, title, page);
        return state;
    }

    public TvAppState search(String term) {
        return search(term, SearchFilter.ALL);
    }

    public TvAppState search(String term, SearchFilter filter) {
        if (state.authenticated() == null) {
            throw new IllegalStateException("authenticated session is required before searching");
        }
        if (term == null || term.isBlank()) {
            return state;
        }
        SearchFilter safeFilter = SearchFilter.safe(filter);
        MediaItemPage page = client.items(
                state.authenticated(),
                ItemQuery.search(term.trim(), safeFilter.includeItemTypes()).limit(BrowseSession.FOLDER_PAGE_SIZE).build()
        );
        state = browseSession.openSearch(state, term, safeFilter, page);
        return state;
    }

    public boolean canGoBackInBrowse() {
        return browseSession.canGoBack();
    }

    public boolean canPageBackwardInBrowse() {
        return browseSession.canPageBackward();
    }

    public boolean canPageForwardInBrowse() {
        return browseSession.canPageForward();
    }

    public TvAppState previousBrowsePage() {
        if (!canPageBackwardInBrowse()) {
            return state;
        }
        MediaItemPage page = folderPage(browseSession.currentFolderParentId(), browseSession.previousPageStartIndex());
        state = browseSession.updateFolderPage(state, page);
        return state;
    }

    public TvAppState nextBrowsePage() {
        if (!canPageForwardInBrowse()) {
            return state;
        }
        MediaItemPage page = folderPage(browseSession.currentFolderParentId(), browseSession.nextPageStartIndex());
        state = browseSession.updateFolderPage(state, page);
        return state;
    }

    public TvAppState preparePlayback(PlaybackSelectionPreferences preferences) {
        if (state.authenticated() == null || state.selectedItem() == null) {
            throw new IllegalStateException("authenticated selected item is required before playback");
        }
        PlaybackSelectionPreferences safePreferences = preferences == null
                ? PlaybackSelectionPreferences.defaults().withStartTimeTicks(state.selectedItem().userData().playbackPositionTicks())
                : preferences;
        PlaybackInfo playbackInfo = loadPlaybackChoices(safePreferences);
        PlayableMedia playable = client.playableMedia(
                state.authenticated(),
                playbackInfo,
                safePreferences
        ).orElseThrow(() -> new IllegalStateException(NO_PLAYABLE_SOURCE_MESSAGE));
        state = TvWorkflow.playbackReady(state, playable);
        return state;
    }

    public PlaybackInfo loadPlaybackChoices(PlaybackSelectionPreferences preferences) {
        if (state.authenticated() == null || state.selectedItem() == null) {
            throw new IllegalStateException("authenticated selected item is required before playback choices");
        }
        PlaybackSelectionPreferences safePreferences = preferences == null
                ? PlaybackSelectionPreferences.defaults()
                : preferences;
        return client.playbackInfo(
                state.authenticated(),
                state.selectedItem().id(),
                playbackInfoOptions(safePreferences, preferences == null)
        );
    }

    public MediaItemSummary nextUpForSelectedSeries() {
        if (state.authenticated() == null || state.selectedItem() == null) {
            throw new IllegalStateException("authenticated selected item is required before loading next up");
        }
        String seriesId = state.selectedItem().seriesId();
        if (seriesId == null || seriesId.isBlank()) {
            throw new IllegalStateException(NO_CHILD_ITEM_MESSAGE);
        }
        return client.nextUpItems(state.authenticated(), seriesId, 1)
                .items()
                .stream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(NO_CHILD_ITEM_MESSAGE));
    }

    public ShowStructure loadSeriesStructure(String seriesId) {
        if (state.authenticated() == null) {
            throw new IllegalStateException("authenticated session is required before loading a series");
        }
        MediaItemSummary series = client.item(state.authenticated(), seriesId);
        MediaItemPage seasonsPage = childPage(series.id(), 0, BrowseSession.FOLDER_PAGE_SIZE, false);
        List<MediaItemSummary> seasons = seasonsPage.items();
        MediaItemSummary selectedSeason = seasons.stream()
                .filter(item -> item.type() == MediaItemType.SEASON)
                .findFirst()
                .orElse(null);
        List<MediaItemSummary> episodes = selectedSeason == null
                ? AndroidCollections.emptyList()
                : childPage(selectedSeason.id(), 0, BrowseSession.FOLDER_PAGE_SIZE, false).items();
        MediaItemSummary nextUp = client.nextUpItems(state.authenticated(), series.id(), 1).items()
                .stream()
                .findFirst()
                .orElse(null);
        MediaItemSummary resume = episodes.stream()
                .filter(MediaItemSummary::hasResumePosition)
                .findFirst()
                .orElse(nextUp);
        return new ShowStructure(series, seasons, selectedSeason, episodes, nextUp, resume);
    }

    public ShowStructure loadSeasonStructure(String seasonId) {
        if (state.authenticated() == null) {
            throw new IllegalStateException("authenticated session is required before loading a season");
        }
        MediaItemSummary season = client.item(state.authenticated(), seasonId);
        String seriesId = season.seriesId().isBlank() ? season.parentId() : season.seriesId();
        MediaItemSummary series = client.item(state.authenticated(), seriesId);
        MediaItemPage seasonsPage = childPage(series.id(), 0, BrowseSession.FOLDER_PAGE_SIZE, false);
        List<MediaItemSummary> seasons = seasonsPage.items();
        List<MediaItemSummary> episodes = childPage(season.id(), 0, BrowseSession.FOLDER_PAGE_SIZE, false).items();
        MediaItemSummary nextUp = client.nextUpItems(state.authenticated(), series.id(), 1).items()
                .stream()
                .findFirst()
                .orElse(null);
        MediaItemSummary resume = episodes.stream()
                .filter(MediaItemSummary::hasResumePosition)
                .findFirst()
                .orElse(nextUp);
        return new ShowStructure(series, seasons, season, episodes, nextUp, resume);
    }

    public ShowStructure loadEpisodeContext(MediaItemSummary episode) {
        if (state.authenticated() == null) {
            throw new IllegalStateException("authenticated session is required before loading episode context");
        }
        String seasonId = episode.parentId();
        if (seasonId.isBlank()) {
            throw new IllegalStateException(NO_CHILD_ITEM_MESSAGE);
        }
        MediaItemSummary season = client.item(state.authenticated(), seasonId);
        String seriesId = episode.seriesId().isBlank() ? season.seriesId() : episode.seriesId();
        MediaItemSummary series = client.item(state.authenticated(), seriesId);
        MediaItemPage seasonsPage = childPage(series.id(), 0, BrowseSession.FOLDER_PAGE_SIZE, false);
        List<MediaItemSummary> episodes = childPage(season.id(), 0, BrowseSession.FOLDER_PAGE_SIZE, false).items();
        MediaItemSummary nextUp = client.nextUpItems(state.authenticated(), series.id(), 1).items()
                .stream()
                .findFirst()
                .orElse(null);
        return new ShowStructure(series, seasonsPage.items(), season, episodes, nextUp, episode);
    }

    private PlaybackInfoOptions playbackInfoOptions(
            PlaybackSelectionPreferences preferences,
            boolean useResumePosition
    ) {
        PlaybackInfoOptions.Builder builder = new PlaybackInfoOptions.Builder()
                .startTimeTicks(useResumePosition
                        ? state.selectedItem().userData().playbackPositionTicks()
                        : preferences.startTimeTicks());
        if (preferences.maxBitRate() > 0) {
            builder.maxStreamingBitrate(preferences.maxBitRate());
        }
        if (preferences.audioStreamIndex() != null) {
            builder.audioStreamIndex(preferences.audioStreamIndex());
        }
        if (preferences.subtitleStreamIndex() != null) {
            builder.subtitleStreamIndex(preferences.subtitleStreamIndex());
        }
        if (preferences.maxAudioChannels() != null) {
            builder.maxAudioChannels(preferences.maxAudioChannels());
        }
        if (preferences.mediaSourceId() != null) {
            builder.mediaSourceId(preferences.mediaSourceId());
        }
        if (Boolean.TRUE.equals(preferences.alwaysBurnInSubtitleWhenTranscoding())) {
            builder.alwaysBurnInSubtitleWhenTranscoding(true);
        }
        if (deviceProfile != null) {
            builder.deviceProfile(deviceProfile);
        }
        return builder.build();
    }

    public TvAppState back() {
        TvAppState browseBackState = browseSession.backOrNull(state);
        if (browseBackState != null) {
            state = browseBackState;
        } else {
            state = TvWorkflow.back(state);
        }
        return state;
    }

    public TvAppState forgetAuthenticatedSession() {
        browseSession.clear();
        if (state.authenticated() != null) {
            client.forget(state.authenticated());
        }
        if (state.server() != null) {
            state = TvWorkflow.serverDiscovered(state, state.server());
        } else {
            state = TvAppState.initial();
        }
        return state;
    }

    public TvAppState logout() {
        browseSession.clear();
        if (state.authenticated() != null) {
            client.logout(state.authenticated());
        }
        state = TvAppState.initial();
        return state;
    }

    public TvAppState fail(String message) {
        state = TvWorkflow.fail(state, message);
        return state;
    }

    // ---- P1-10 / P1-11 user state toggles (favorite / watched / rating) ----

    public TvAppState toggleFavorite() {
        requireSelectedItem("favorite");
        MediaItemSummary item = state.selectedItem();
        boolean nextFavorite = !item.userData().favorite();
        client.toggleFavorite(state.authenticated(), item.id(), nextFavorite);
        tv.cinepilot.core.protocol.UserItemData updated = item.userData().withFavorite(nextFavorite);
        state = TvWorkflow.selectedItemUserDataUpdated(state, updated);
        return state;
    }

    public TvAppState toggleWatched() {
        requireSelectedItem("watched");
        MediaItemSummary item = state.selectedItem();
        boolean nextWatched = !item.userData().played();
        client.markWatched(state.authenticated(), item.id(), nextWatched);
        tv.cinepilot.core.protocol.UserItemData updated = item.userData().withPlayed(nextWatched);
        state = TvWorkflow.selectedItemUserDataUpdated(state, updated);
        return state;
    }

    public TvAppState setUserRating(Double ratingZeroToTen) {
        requireSelectedItem("rating");
        MediaItemSummary item = state.selectedItem();
        client.setRating(state.authenticated(), item.id(), ratingZeroToTen);
        tv.cinepilot.core.protocol.UserItemData updated = item.userData().withRating(ratingZeroToTen);
        state = TvWorkflow.selectedItemUserDataUpdated(state, updated);
        return state;
    }

    // ---- P1-13 person drill-down (casts same person's movies/shows as a browse row) ----

    public TvAppState openPerson(String personId, String personName) {
        if (state.authenticated() == null) {
            throw new IllegalStateException("authenticated session is required before opening a person");
        }
        if (personId == null || personId.isBlank()) {
            throw new IllegalArgumentException("personId is required");
        }
        String safeName = personName == null || personName.isBlank() ? personId : personName;
        MediaItemPage page = client.personItems(state.authenticated(), personId, BrowseSession.FOLDER_PAGE_SIZE);
        state = browseSession.openSearch(state, safeName, SearchFilter.ALL, page);
        return state;
    }

    private MediaItemPage folderPage(String parentId, int startIndex) {
        return childPage(parentId, startIndex, BrowseSession.FOLDER_PAGE_SIZE, true);
    }

    private MediaItemPage childPage(String parentId, int startIndex, int limit, boolean requireItems) {
        MediaItemPage page = client.items(
                state.authenticated(),
                ItemQuery.browse()
                        .parentId(parentId)
                        .startIndex(startIndex)
                        .limit(limit)
                        .build()
        );
        if (requireItems && page.items().isEmpty()) {
            throw new IllegalStateException(NO_CHILD_ITEM_MESSAGE);
        }
        return page;
    }

    private void requireSelectedItem(String action) {
        if (state.authenticated() == null) {
            throw new IllegalStateException("authenticated session is required before " + action);
        }
        if (state.selectedItem() == null) {
            throw new IllegalStateException("selected item is required before " + action);
        }
    }
}
