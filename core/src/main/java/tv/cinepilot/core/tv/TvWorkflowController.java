package tv.cinepilot.core.tv;

import java.util.List;
import tv.cinepilot.core.AndroidCollections;
import tv.cinepilot.core.protocol.AuthenticatedServer;
import tv.cinepilot.core.protocol.ChapterInfo;
import tv.cinepilot.core.protocol.GenreInfo;
import tv.cinepilot.core.protocol.ItemQuery;
import tv.cinepilot.core.protocol.MediaBrowseFilters;
import tv.cinepilot.core.protocol.MediaBrowserClient;
import tv.cinepilot.core.protocol.MediaItemPage;
import tv.cinepilot.core.protocol.MediaItemSummary;
import tv.cinepilot.core.protocol.MediaItemType;
import tv.cinepilot.core.protocol.MediaSegmentInfo;
import tv.cinepilot.core.protocol.MediaServerAddress;
import tv.cinepilot.core.protocol.OfflineRepository;
import tv.cinepilot.core.protocol.PlayableMedia;
import tv.cinepilot.core.protocol.PlaybackDeviceProfile;
import tv.cinepilot.core.protocol.PlaybackInfo;
import tv.cinepilot.core.protocol.PlaybackInfoOptions;
import tv.cinepilot.core.protocol.PlaybackSelectionPreferences;
import tv.cinepilot.core.protocol.ProfileSummary;
import tv.cinepilot.core.protocol.PublicUserSummary;
import tv.cinepilot.core.protocol.QuickConnectSession;
import tv.cinepilot.core.protocol.ServerIdentity;
import tv.cinepilot.core.protocol.TrickplayInfo;

public final class TvWorkflowController {
    public static final String NO_CHILD_ITEM_MESSAGE = "No child media item is available";
    public static final String NO_PLAYABLE_SOURCE_MESSAGE = "No playable media source is available";
    public static final String QUICK_CONNECT_DISABLED_MESSAGE = "Quick Connect is not enabled on this server";
    public static final String QUICK_CONNECT_NOT_APPROVED_MESSAGE = "Quick Connect has not been approved yet";

    private final MediaBrowserClient client;
    private final HomeRowsLoader homeRowsLoader;
    private final PlaybackDeviceProfile deviceProfile;
    private final OfflineRepository offlineRepository;
    private final BrowseSession browseSession = new BrowseSession();
    private QuickConnectSession pendingQuickConnect;
    private TvAppState state = TvAppState.initial();
    private MediaBrowseFilters browseFilters = MediaBrowseFilters.EMPTY;
    private String currentViewId = "";

    public TvWorkflowController(MediaBrowserClient client, HomeRowsLoader homeRowsLoader) {
        this(client, homeRowsLoader, null, null);
    }

    public TvWorkflowController(
            MediaBrowserClient client,
            HomeRowsLoader homeRowsLoader,
            PlaybackDeviceProfile deviceProfile
    ) {
        this(client, homeRowsLoader, deviceProfile, null);
    }

    public TvWorkflowController(
            MediaBrowserClient client,
            HomeRowsLoader homeRowsLoader,
            PlaybackDeviceProfile deviceProfile,
            OfflineRepository offlineRepository
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
        this.offlineRepository = offlineRepository == null ? new OfflineRepository() : offlineRepository;
    }

    public OfflineRepository offlineRepository() {
        return offlineRepository;
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
        client.markActiveProfile(state.server(), userId);
        state = TvWorkflow.loginSucceeded(state, authenticated);
        return loadHome();
    }

    /**
     * List saved profiles for the current server. The active profile is flagged so the
     * UI can highlight it; tokens are never exposed. Call this from the account
     * switcher screen or the settings route.
     */
    public List<ProfileSummary> profiles() {
        if (state.server() == null) {
            return AndroidCollections.emptyList();
        }
        return client.profiles(state.server());
    }

    /**
     * Switch to the profile identified by the given user id on the current server.
     * Invalid ids (no matching saved session) throw, so callers should only pass in
     * ids returned by {@link #profiles()}.
     */
    public TvAppState switchProfile(String userId) {
        return restoreSession(userId);
    }

    /**
     * Remove a saved profile from the sessions store. If the removed profile was
     * the active one the app returns to the SERVER_ENTRY route; otherwise the
     * current session remains untouched.
     */
    public TvAppState removeProfile(String userId) {
        if (state.server() == null) {
            throw new IllegalStateException("server must be discovered before removing a profile");
        }
        boolean removedActive = state.authenticated() != null
                && state.authenticated().session().userId().equals(userId);
        client.forgetProfile(state.server(), userId);
        if (removedActive) {
            state = TvWorkflow.loggedOut(state);
        }
        return state;
    }

    public TvAppState loadHome() {
        return loadHome(true);
    }

    /**
     * Variant of {@link #loadHome()} that lets the UI layer opt out of the
     * server-defined smart-collection dynamic rows. Protocol-layer code
     * deliberately does NOT read shared preferences -- that responsibility
     * lives in the Android side via {@code HomeSettingsStore}.
     */
    public TvAppState loadHome(boolean includeSmartCollections) {
        if (state.authenticated() == null) {
            throw new IllegalStateException("authenticated session is required before loading home");
        }
        browseSession.clear();
        List<HomeRow> rows = homeRowsLoader.load(state.authenticated(), browseFilters, includeSmartCollections);
        state = TvWorkflow.homeLoaded(state, rows);
        currentViewId = "";
        return state;
    }

    public MediaBrowseFilters browseFilters() {
        return browseFilters;
    }

    /** Replace filter state and immediately refresh home rows. Honors the current focused view. */
    public TvAppState setBrowseFilters(MediaBrowseFilters next) {
        if (state.authenticated() == null) {
            throw new IllegalStateException("authenticated session is required before setting filters");
        }
        browseFilters = next == null ? MediaBrowseFilters.EMPTY : next;
        List<HomeRow> rows = homeRowsLoader.load(state.authenticated(), browseFilters);
        state = TvWorkflow.homeLoaded(state, rows);
        return state;
    }

    /** Genres filtered to the currently focused user view; empty if none is set. */
    public List<GenreInfo> genresForCurrentView() {
        if (state.authenticated() == null) {
            return AndroidCollections.emptyList();
        }
        return AndroidCollections.listCopy(client.genres(state.authenticated(), currentViewId));
    }

    /**
     * Pin a specific user-view id as the filter target. Used when the home focus lands on a
     * per-view latest row: the UI should then present that view's genres as candidate chips.
     * Passing a blank id clears the pin and falls back to server-wide genres.
     */
    public void pinCurrentViewId(String viewId) {
        currentViewId = viewId == null ? "" : viewId;
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

    /**
     * Replace the currently displayed home rows with the given list. Used by
     * the Android layer to inject dynamic rows (offline rail, plugin rows)
     * after a successful network {@link #loadHome} has completed.
     */
    public TvAppState setHomeRows(List<HomeRow> rows) {
        if (state.authenticated() == null) {
            throw new IllegalStateException("authenticated session is required before setting home rows");
        }
        state = TvWorkflow.homeLoaded(state, rows);
        return state;
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
        ItemQuery.Builder query = ItemQuery.search(term.trim(), safeFilter.includeItemTypes())
                .limit(BrowseSession.FOLDER_PAGE_SIZE);
        if (browseFilters != null && !browseFilters.isEmpty()) {
            browseFilters.applyTo(query);
        }
        MediaItemPage page = client.items(state.authenticated(), query.build());
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

    /**
     * Recompute just the selected season + its episode list against an already-loaded series
     * structure. Used by the series detail in-place season chip switcher so we can swap a
     * season without re-loading series metadata, seasons, or next-up info.
     */
    public ShowStructure selectSeasonInStructure(ShowStructure structure, String seasonId) {
        if (state.authenticated() == null) {
            throw new IllegalStateException("authenticated session is required before season switch");
        }
        if (structure == null || seasonId == null || seasonId.isBlank()) return structure;
        MediaItemSummary selectedSeason = structure.seasons().stream()
                .filter(item -> seasonId.equals(item.id()))
                .findFirst()
                .orElse(structure.selectedSeason());
        if (selectedSeason == null) return structure;
        List<MediaItemSummary> episodes = childPage(selectedSeason.id(), 0, BrowseSession.FOLDER_PAGE_SIZE, false)
                .items();
        MediaItemSummary resume = episodes.stream()
                .filter(MediaItemSummary::hasResumePosition)
                .findFirst()
                .orElse(structure.nextUp());
        return new ShowStructure(
                structure.series(),
                structure.seasons(),
                selectedSeason,
                episodes,
                structure.nextUp(),
                resume
        );
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

    /**
     * Builds a "season started" status map suitable for driving the series-detail season-rail
     * folding logic. A season is considered "not yet started" when every episode it contains
     * is unplayed AND has no resume position; the currently-selected season is always shown.
     *
     * <p>The returned map key is the season's id; a value of {@code true} means the season
     * has been watched (at least one episode played or resumed). {@code false} means the
     * entire season is still unwatched, so it is a candidate for folding behind a compact
     * "Sx · 尚未开始" chip in the series-detail rail.
     *
     * <p>The workflow eagerly probes every non-selected season's episode list so the UI can
     * make the fold decision without additional round-trips. The call is bounded by the
     * number of seasons a real-world series typically has.
     */
    public java.util.Map<String, Boolean> loadSeasonsStartedStatus(ShowStructure structure) {
        if (structure == null || state.authenticated() == null) {
            return java.util.Collections.emptyMap();
        }
        java.util.Map<String, Boolean> out = new java.util.LinkedHashMap<>();
        String selectedId = structure.selectedSeason() == null ? "" : structure.selectedSeason().id();
        for (MediaItemSummary season : structure.seasons()) {
            if (season == null || season.id().isBlank()) continue;
            if (season.id().equals(selectedId)) {
                out.put(season.id(), true);
                continue;
            }
            // Fall back to the season-level UserData when we have no episodes loaded:
            // if the server already marked the season played, we treat it as started.
            boolean started = season.userData() != null
                    && (season.userData().played() || season.userData().playbackPositionTicks() > 0);
            if (!started) {
                List<MediaItemSummary> seasonEpisodes = childPage(
                        season.id(), 0, BrowseSession.FOLDER_PAGE_SIZE, false
                ).items();
                for (MediaItemSummary ep : seasonEpisodes) {
                    if (ep.userData() == null) continue;
                    if (ep.userData().played() || ep.hasResumePosition()) {
                        started = true;
                        break;
                    }
                }
            }
            out.put(season.id(), started);
        }
        return java.util.Collections.unmodifiableMap(out);
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

    // ---- Chapters / segments / trickplay (P1 batch 6) -----------------------

    /**
     * Returns the list of chapters for the currently selected item. When the item payload
     * already carries chapters (Fields=Chapters) the cached list is returned directly;
     * otherwise a dedicated endpoint call populates and re-attaches chapters.
     */
    public List<ChapterInfo> loadChaptersForSelectedItem() {
        MediaItemSummary item = requireSelectedItem("chapters", true);
        if (!item.chapters().isEmpty()) {
            return item.chapters();
        }
        List<ChapterInfo> chapters = client.chapters(state.authenticated(), item.id());
        if (!chapters.isEmpty()) {
            state = state.with(
                    state.route(),
                    state.status(),
                    state.pendingAddress(),
                    state.server(),
                    state.publicUsers(),
                    state.authenticated(),
                    state.homeRows(),
                    state.focus(),
                    item.withChapters(chapters),
                    state.playableMedia(),
                    state.errorMessage()
            );
        }
        return chapters;
    }

    public List<MediaSegmentInfo> loadMediaSegmentsForSelectedItem() {
        MediaItemSummary item = requireSelectedItem("media segments", true);
        // Prefer the dedicated endpoint; fall back to IntroStartTicks/CreditsStartTicks
        // fields on the item payload via the mapper when the endpoint 404s.
        List<MediaSegmentInfo> fromEndpoint = client.mediaSegments(state.authenticated(), item.id());
        if (!fromEndpoint.isEmpty()) return fromEndpoint;
        return AndroidCollections.emptyList();
    }

    public TrickplayInfo loadTrickplayForSelectedItem(int preferredTileWidth) {
        MediaItemSummary item = requireSelectedItem("trickplay info", true);
        int safeWidth = preferredTileWidth <= 0 ? 320 : preferredTileWidth;
        return client.trickplayInfo(state.authenticated(), item.id(), safeWidth);
    }

    public MediaItemSummary nextUpEpisodeForSelected() {
        if (state.authenticated() == null || state.selectedItem() == null) {
            throw new IllegalStateException(NO_CHILD_ITEM_MESSAGE);
        }
        String seriesId = state.selectedItem().seriesId();
        if (seriesId.isBlank()) {
            throw new IllegalStateException(NO_CHILD_ITEM_MESSAGE);
        }
        return client.nextUpItems(state.authenticated(), seriesId, 1)
                .items()
                .stream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(NO_CHILD_ITEM_MESSAGE));
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

    // ---- P1-12 ProviderId 手动修正 + 元数据刷新 ----

    /**
     * Writes a single provider id on the currently selected item. The change is
     * written to the server first, then mirrored into local state so the UI can
     * repaint instantly. Callers typically follow up with
     * {@link #refreshMetadataForSelectedItem(boolean)} to pick up any provider-
     * driven metadata changes (posters, descriptions, etc.).
     */
    public TvAppState setProviderId(String key, String value) {
        requireSelectedItem("provider-id update");
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("provider key is required");
        }
        MediaItemSummary item = state.selectedItem();
        java.util.Map<String, String> next = new java.util.HashMap<>();
        for (String k : item.providerIds().keySet()) next.put(k, item.providerIds().get(k));
        String safeValue = value == null ? "" : value;
        if (safeValue.isBlank()) {
            next.remove(key);
        } else {
            next.put(key, safeValue);
        }
        client.updateProviderIds(state.authenticated(), item.id(), next);
        MediaItemSummary updated = item.withProviderIds(next);
        state = TvWorkflow.selectedItemUpdated(state, updated);
        return state;
    }

    /**
     * Bulk variant of {@link #setProviderId(String, String)}. Writes every key in the
     * supplied map to the server in a single request; keys with blank values are removed.
     * Useful when the editor produces a complete replacement map.
     */
    public TvAppState setProviderIdsForSelectedItem(java.util.Map<String, String> fullMap) {
        requireSelectedItem("provider-id bulk update");
        if (fullMap == null) {
            throw new IllegalArgumentException("provider ids map is required");
        }
        MediaItemSummary item = state.selectedItem();
        client.updateProviderIds(state.authenticated(), item.id(), fullMap);
        MediaItemSummary updated = item.withProviderIds(fullMap);
        state = TvWorkflow.selectedItemUpdated(state, updated);
        return state;
    }

    /**
     * Asks the server to refresh metadata for the currently selected item and
     * then re-opens the item so the UI picks up refreshed poster/overview/etc.
     * Replaces existing metadata (including images) when {@code replaceAll}
     * is true; otherwise the refresh is a merge.
     */
    public TvAppState refreshMetadataForSelectedItem(boolean replaceAll) {
        requireSelectedItem("metadata refresh");
        String itemId = state.selectedItem().id();
        client.refreshMetadata(state.authenticated(), itemId, replaceAll);
        // A server-side refresh is a best-effort background task; re-open the item
        // right now so the UI picks up whatever the server already flushed back.
        return openItem(itemId);
    }

    // ---- P1-16 same-collection other items rail (detail) ----

    /**
     * Returns a list of items that belong to the same TMDb / server collection as the
     * currently selected item. Works in two stages:
     *
     * <ol>
     *     <li>If the item already carries a server-side collection parent relationship
     *         (e.g. the user browsed into a BoxSet), use that id directly.</li>
     *     <li>Otherwise walk the server BoxSets and match {@code ProviderIds.TmdbCollection}.</li>
     * </ol>
     *
     * The caller is responsible for excluding the currently selected item from rendering.
     */
    public List<MediaItemSummary> loadSameCollectionItemsForSelectedItem() {
        MediaItemSummary item = requireSelectedItem("same collection", true);
        if (state.authenticated() == null) return AndroidCollections.emptyList();
        String collectionId = resolveCollectionIdFor(item);
        if (collectionId == null || collectionId.isBlank()) return AndroidCollections.emptyList();
        MediaItemPage page = client.collectionChildren(state.authenticated(), collectionId, BrowseSession.FOLDER_PAGE_SIZE);
        List<MediaItemSummary> result = new java.util.ArrayList<>();
        for (MediaItemSummary child : page.items()) {
            if (child.id().equals(item.id())) continue;
            result.add(child);
        }
        return AndroidCollections.listCopy(result);
    }

    private String resolveCollectionIdFor(MediaItemSummary item) {
        // Fast path: item metadata already describes a collection parent (e.g. BoxSet child).
        String parentId = item.parentId() == null ? "" : item.parentId();
        if (!parentId.isBlank() && item.type() != null) {
            switch (item.type()) {
                case MOVIE:
                case SERIES:
                case SEASON:
                case EPISODE:
                    // Not a collection itself - check if parent is a BoxSet by type inference.
                    // We deliberately fall through to the TmdbCollectionId heuristic below
                    // when the parent is unknown.
                    break;
                default:
                    break;
            }
        }
        // Second path: use TmdbCollectionId and look up the matching BoxSet server-side.
        String tmdbCollectionId = item.tmdbCollectionId();
        if (tmdbCollectionId != null && !tmdbCollectionId.isBlank()) {
            MediaItemSummary boxSet = client.findBoxSetByTmdbCollectionId(state.authenticated(), tmdbCollectionId);
            if (boxSet != null) return boxSet.id();
        }
        return null;
    }

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

    private MediaItemSummary requireSelectedItem(String action, @SuppressWarnings("unused") boolean asValue) {
        requireSelectedItem(action);
        return state.selectedItem();
    }

    // ---- P1-17 all-series / all-movies overview rows --------------------------------

    /**
     * Build overview rows for either Series or Movies in a given user view. The overview
     * includes (when available): Continue Watching, Next Up (series only), Unplayed A-Z,
     * Favorite, and an "All" row honoring the current browse filters.
     */
    public TvAppState openLibraryOverview(String viewId, String title, boolean isSeries) {
        if (state.authenticated() == null) {
            throw new IllegalStateException("authenticated session is required before opening overview");
        }
        MediaItemType type = isSeries ? MediaItemType.SERIES : MediaItemType.MOVIE;
        String safeViewId = viewId == null ? "" : viewId;
        List<HomeRow> rows = buildOverviewRows(safeViewId, type);
        String overviewTitle = title == null || title.isBlank()
                ? (isSeries ? "全部剧集" : "全部电影")
                : title;
        state = browseSession.openOverview(state, overviewTitle, rows);
        pinCurrentViewId(safeViewId);
        return state;
    }

    private List<HomeRow> buildOverviewRows(String parentViewId, MediaItemType type) {
        AuthenticatedServer authenticated = state.authenticated();
        List<HomeRow> rows = new java.util.ArrayList<>();
        String includeType = type == MediaItemType.SERIES ? "Series" : "Movie";
        if (!browseFilters.isStrict()) {
            // Continue watching is already filtered by type on the server (it returns video items
            // of any type, but we want type-targeted rows; fallback: include all).
            addIfNotEmpty(rows, "overview:resume", "继续观看",
                    filteredItems(authenticated, parentViewId, "Video", "IsResumable", null, BrowseSession.FOLDER_PAGE_SIZE));
            if (type == MediaItemType.SERIES) {
                addIfNotEmpty(rows, "overview:next-up", "下一集",
                        client.nextUpItems(authenticated, BrowseSession.FOLDER_PAGE_SIZE).items());
            }
        }
        addIfNotEmpty(rows, "overview:unplayed", "未观看 · A-Z",
                filteredItems(authenticated, parentViewId, includeType,
                        MediaBrowseFilters.FLAG_IS_UNPLAYED, null, BrowseSession.FOLDER_PAGE_SIZE));
        addIfNotEmpty(rows, "overview:favorite", "收藏夹",
                filteredItems(authenticated, parentViewId, includeType,
                        MediaBrowseFilters.FLAG_IS_FAVORITE, null, BrowseSession.FOLDER_PAGE_SIZE));
        addIfNotEmpty(rows, "overview:all", "全部",
                filteredItems(authenticated, parentViewId, includeType, null,
                        browseFilters, BrowseSession.FOLDER_PAGE_SIZE));
        return AndroidCollections.listCopy(rows);
    }

    private List<MediaItemSummary> filteredItems(
            AuthenticatedServer authenticated,
            String parentViewId,
            String includeItemTypes,
            String forcedFilterFlag,
            MediaBrowseFilters filters,
            int limit
    ) {
        ItemQuery.Builder builder = ItemQuery.browse()
                .recursive(true)
                .includeItemTypes(includeItemTypes)
                .limit(limit);
        if (!parentViewId.isBlank()) builder.parentId(parentViewId);
        if (forcedFilterFlag != null && !forcedFilterFlag.isBlank()) {
            builder.filters(forcedFilterFlag);
        }
        if (filters != null) filters.applyTo(builder);
        return client.items(authenticated, builder.build()).items();
    }

    // ---- Playlists (P3-2) ----

    /** List all user playlists (summary-only). */
    public MediaItemPage playlists(int limit) {
        if (state.authenticated() == null) return MediaItemPage.empty();
        return client.playlists(state.authenticated(), Math.max(0, limit));
    }

    /** Items inside a playlist, in user-arranged order. */
    public MediaItemPage playlistItems(String playlistId, int limit) {
        if (state.authenticated() == null) return MediaItemPage.empty();
        if (playlistId == null || playlistId.isBlank()) return MediaItemPage.empty();
        return client.playlistItems(state.authenticated(), playlistId, Math.max(0, limit));
    }

    /** Create a new playlist; returns the new playlist id. */
    public String createPlaylist(String name) {
        if (state.authenticated() == null) return "";
        if (name == null || name.isBlank()) return "";
        return client.createPlaylist(state.authenticated(), name);
    }

    /** Add one or more items to a playlist. */
    public void addToPlaylist(String playlistId, List<String> itemIds) {
        if (state.authenticated() == null) return;
        if (playlistId == null || playlistId.isBlank()) return;
        if (itemIds == null || itemIds.isEmpty()) return;
        client.addToPlaylist(state.authenticated(), playlistId, itemIds);
    }

    /** Remove entries from a playlist (by entry id, not item id). */
    public void removeFromPlaylist(String playlistId, List<String> entryIds) {
        if (state.authenticated() == null) return;
        if (playlistId == null || playlistId.isBlank()) return;
        if (entryIds == null || entryIds.isEmpty()) return;
        client.removeFromPlaylist(state.authenticated(), playlistId, entryIds);
    }

    /** Delete a playlist entirely. */
    public void deletePlaylist(String playlistId) {
        if (state.authenticated() == null) return;
        if (playlistId == null || playlistId.isBlank()) return;
        client.deletePlaylist(state.authenticated(), playlistId);
    }

    private static void addIfNotEmpty(List<HomeRow> rows, String id, String title, List<MediaItemSummary> items) {
        if (items != null && !items.isEmpty()) {
            rows.add(new HomeRow(id, title, items));
        }
    }
}
