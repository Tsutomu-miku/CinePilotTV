package tv.cinepilot.core.tv;

import java.util.List;
import tv.cinepilot.core.protocol.AuthenticatedServer;
import tv.cinepilot.core.protocol.ItemQuery;
import tv.cinepilot.core.protocol.MediaBrowserClient;
import tv.cinepilot.core.protocol.MediaItemSummary;
import tv.cinepilot.core.protocol.MediaServerAddress;
import tv.cinepilot.core.protocol.PlayableMedia;
import tv.cinepilot.core.protocol.PlaybackInfo;
import tv.cinepilot.core.protocol.PlaybackInfoOptions;
import tv.cinepilot.core.protocol.PlaybackSelectionPreferences;
import tv.cinepilot.core.protocol.ServerIdentity;

public final class TvWorkflowController {
    public static final String NO_CHILD_ITEM_MESSAGE = "No child media item is available";
    public static final String NO_PLAYABLE_SOURCE_MESSAGE = "No playable media source is available";

    private final MediaBrowserClient client;
    private final HomeRowsLoader homeRowsLoader;
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
                new PlaybackInfoOptions.Builder()
                        .startTimeTicks(playbackStartTimeTicks(preferences))
                        .build()
        );
        PlayableMedia playable = client.playableMedia(
                state.authenticated(),
                playbackInfo,
                safePreferences
        ).orElseThrow(() -> new IllegalStateException(NO_PLAYABLE_SOURCE_MESSAGE));
        state = TvWorkflow.playbackReady(state, playable);
        return state;
    }

    private long playbackStartTimeTicks(PlaybackSelectionPreferences preferences) {
        if (preferences != null) {
            return preferences.startTimeTicks();
        }
        return state.selectedItem().userData().playbackPositionTicks();
    }

    public TvAppState back() {
        state = TvWorkflow.back(state);
        return state;
    }

    public TvAppState forgetAuthenticatedSession() {
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
}
