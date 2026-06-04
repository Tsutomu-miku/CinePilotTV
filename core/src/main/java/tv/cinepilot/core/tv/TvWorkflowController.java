package tv.cinepilot.core.tv;

import java.util.List;
import tv.cinepilot.core.protocol.AuthenticatedServer;
import tv.cinepilot.core.protocol.ItemQuery;
import tv.cinepilot.core.protocol.MediaBrowserClient;
import tv.cinepilot.core.protocol.MediaItemPage;
import tv.cinepilot.core.protocol.MediaItemSummary;
import tv.cinepilot.core.protocol.MediaServerAddress;
import tv.cinepilot.core.protocol.PlayableMedia;
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
    private final BrowseSession browseSession = new BrowseSession();
    private QuickConnectSession pendingQuickConnect;
    private TvAppState state = TvAppState.initial();

    public TvWorkflowController(MediaBrowserClient client, HomeRowsLoader homeRowsLoader) {
        if (client == null) {
            throw new IllegalArgumentException("client is required");
        }
        if (homeRowsLoader == null) {
            throw new IllegalArgumentException("homeRowsLoader is required");
        }
        this.client = client;
        this.homeRowsLoader = homeRowsLoader;
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

    private MediaItemPage folderPage(String parentId, int startIndex) {
        MediaItemPage page = client.items(
                state.authenticated(),
                ItemQuery.browse()
                        .parentId(parentId)
                        .startIndex(startIndex)
                        .limit(BrowseSession.FOLDER_PAGE_SIZE)
                        .build()
        );
        if (page.items().isEmpty()) {
            throw new IllegalStateException(NO_CHILD_ITEM_MESSAGE);
        }
        return page;
    }
}
