package tv.cinepilot.core.tv;

import java.util.ArrayDeque;
import java.util.Deque;
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
import tv.cinepilot.core.protocol.ServerIdentity;

public final class TvWorkflowController {
    public static final String NO_CHILD_ITEM_MESSAGE = "No child media item is available";
    public static final String NO_PLAYABLE_SOURCE_MESSAGE = "No playable media source is available";
    private static final int FOLDER_PAGE_SIZE = 50;

    private final MediaBrowserClient client;
    private final HomeRowsLoader homeRowsLoader;
    private final Deque<TvAppState> browseBackStack = new ArrayDeque<>();
    private FolderBrowseContext folderBrowseContext;
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
        browseBackStack.clear();
        folderBrowseContext = null;
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
        browseBackStack.clear();
        folderBrowseContext = null;
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
        browseBackStack.push(state);
        String rowTitle = title == null || title.isBlank() ? "子项目" : title;
        folderBrowseContext = new FolderBrowseContext(parentId, rowTitle, page.totalRecordCount(), page.startIndex());
        state = folderState(folderBrowseContext, page);
        return state;
    }

    public TvAppState search(String term) {
        if (state.authenticated() == null) {
            throw new IllegalStateException("authenticated session is required before searching");
        }
        if (term == null || term.isBlank()) {
            return state;
        }
        MediaItemPage page = client.items(
                state.authenticated(),
                ItemQuery.search(term.trim()).limit(FOLDER_PAGE_SIZE).build()
        );
        browseBackStack.push(state);
        folderBrowseContext = null;
        state = TvWorkflow.homeLoaded(
                state,
                List.of(new HomeRow("search:" + term.trim(), "搜索：" + term.trim(), page.items()))
        );
        return state;
    }

    public boolean canGoBackInBrowse() {
        return !browseBackStack.isEmpty();
    }

    public boolean canPageBackwardInBrowse() {
        return folderBrowseContext != null && folderBrowseContext.startIndex() > 0;
    }

    public boolean canPageForwardInBrowse() {
        return folderBrowseContext != null
                && folderBrowseContext.startIndex() + FOLDER_PAGE_SIZE < folderBrowseContext.totalRecordCount();
    }

    public TvAppState previousBrowsePage() {
        if (!canPageBackwardInBrowse()) {
            return state;
        }
        int startIndex = Math.max(0, folderBrowseContext.startIndex() - FOLDER_PAGE_SIZE);
        MediaItemPage page = folderPage(folderBrowseContext.parentId(), startIndex);
        folderBrowseContext = folderBrowseContext.withPage(page.totalRecordCount(), page.startIndex());
        state = folderState(folderBrowseContext, page);
        return state;
    }

    public TvAppState nextBrowsePage() {
        if (!canPageForwardInBrowse()) {
            return state;
        }
        int startIndex = folderBrowseContext.startIndex() + FOLDER_PAGE_SIZE;
        MediaItemPage page = folderPage(folderBrowseContext.parentId(), startIndex);
        folderBrowseContext = folderBrowseContext.withPage(page.totalRecordCount(), page.startIndex());
        state = folderState(folderBrowseContext, page);
        return state;
    }

    public TvAppState preparePlayback(PlaybackSelectionPreferences preferences) {
        if (state.authenticated() == null || state.selectedItem() == null) {
            throw new IllegalStateException("authenticated selected item is required before playback");
        }
        PlaybackSelectionPreferences safePreferences = preferences == null
                ? PlaybackSelectionPreferences.defaults()
                : preferences;
        PlaybackInfo playbackInfo = client.playbackInfo(
                state.authenticated(),
                state.selectedItem().id(),
                playbackInfoOptions(safePreferences, preferences == null)
        );
        PlayableMedia playable = client.playableMedia(
                state.authenticated(),
                playbackInfo,
                safePreferences
        ).orElseThrow(() -> new IllegalStateException(NO_PLAYABLE_SOURCE_MESSAGE));
        state = TvWorkflow.playbackReady(state, playable);
        return state;
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
        return builder.build();
    }

    public TvAppState back() {
        if (state.route() == TvRoute.HOME && !browseBackStack.isEmpty()) {
            state = browseBackStack.pop();
            folderBrowseContext = null;
        } else {
            state = TvWorkflow.back(state);
        }
        return state;
    }

    public TvAppState forgetAuthenticatedSession() {
        browseBackStack.clear();
        folderBrowseContext = null;
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
        browseBackStack.clear();
        folderBrowseContext = null;
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
                        .limit(FOLDER_PAGE_SIZE)
                        .build()
        );
        if (page.items().isEmpty()) {
            throw new IllegalStateException(NO_CHILD_ITEM_MESSAGE);
        }
        return page;
    }

    private TvAppState folderState(FolderBrowseContext context, MediaItemPage page) {
        String title = context.title();
        if (context.totalRecordCount() > FOLDER_PAGE_SIZE) {
            int first = context.startIndex() + 1;
            int last = Math.min(context.startIndex() + page.items().size(), context.totalRecordCount());
            title = title + " " + first + "-" + last + "/" + context.totalRecordCount();
        }
        return TvWorkflow.homeLoaded(
                state,
                List.of(new HomeRow("folder:" + context.parentId(), title, page.items()))
        );
    }

    private record FolderBrowseContext(
            String parentId,
            String title,
            int totalRecordCount,
            int startIndex
    ) {
        FolderBrowseContext withPage(int nextTotalRecordCount, int nextStartIndex) {
            return new FolderBrowseContext(parentId, title, nextTotalRecordCount, nextStartIndex);
        }
    }
}
