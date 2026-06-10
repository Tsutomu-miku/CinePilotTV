package tv.cinepilot.core.tv;

import java.util.List;
import tv.cinepilot.core.AndroidCollections;
import tv.cinepilot.core.protocol.AuthenticatedServer;
import tv.cinepilot.core.protocol.MediaItemSummary;
import tv.cinepilot.core.protocol.MediaServerAddress;
import tv.cinepilot.core.protocol.PlayableMedia;
import tv.cinepilot.core.protocol.PublicUserSummary;
import tv.cinepilot.core.protocol.ServerIdentity;

public final class TvWorkflow {
    private TvWorkflow() {
    }

    public static TvAppState submitServer(TvAppState state, MediaServerAddress address) {
        return state.with(
                TvRoute.SERVER_ENTRY,
                TvStatus.LOADING,
                address,
                null,
                AndroidCollections.emptyList(),
                null,
                AndroidCollections.emptyList(),
                null,
                null,
                null,
                ""
        );
    }

    public static TvAppState serverDiscovered(TvAppState state, ServerIdentity server) {
        return state.with(
                TvRoute.LOGIN,
                TvStatus.READY,
                state.pendingAddress(),
                server,
                AndroidCollections.emptyList(),
                null,
                AndroidCollections.emptyList(),
                null,
                null,
                null,
                ""
        );
    }

    public static TvAppState loginStarted(TvAppState state) {
        return state.with(
                TvRoute.LOGIN,
                TvStatus.LOADING,
                state.pendingAddress(),
                state.server(),
                state.publicUsers(),
                null,
                AndroidCollections.emptyList(),
                null,
                null,
                null,
                ""
        );
    }

    public static TvAppState loginSucceeded(TvAppState state, AuthenticatedServer authenticated) {
        return state.with(
                TvRoute.HOME,
                TvStatus.LOADING,
                state.pendingAddress(),
                authenticated.server(),
                state.publicUsers(),
                authenticated,
                AndroidCollections.emptyList(),
                null,
                null,
                null,
                ""
        );
    }

    public static TvAppState homeLoaded(TvAppState state, List<HomeRow> rows) {
        List<HomeRow> safeRows = AndroidCollections.listCopy(rows);
        FocusedItem focus = firstFocusable(safeRows);
        return state.with(
                TvRoute.HOME,
                TvStatus.READY,
                state.pendingAddress(),
                state.server(),
                state.publicUsers(),
                state.authenticated(),
                safeRows,
                focus,
                null,
                null,
                ""
        );
    }

    public static TvAppState focusItem(TvAppState state, String rowId, String itemId) {
        if (state.homeRows().stream().noneMatch(row -> row.id().equals(rowId) && row.containsItem(itemId))) {
            throw new IllegalArgumentException("Focused item must exist in home rows");
        }
        return state.with(
                state.route(),
                state.status(),
                state.pendingAddress(),
                state.server(),
                state.publicUsers(),
                state.authenticated(),
                state.homeRows(),
                new FocusedItem(rowId, itemId),
                state.selectedItem(),
                state.playableMedia(),
                state.errorMessage()
        );
    }

    public static TvAppState openDetails(TvAppState state, MediaItemSummary item) {
        if (item == null) {
            throw new IllegalArgumentException("item is required");
        }
        return state.with(
                TvRoute.DETAILS,
                TvStatus.READY,
                state.pendingAddress(),
                state.server(),
                state.publicUsers(),
                state.authenticated(),
                state.homeRows(),
                state.focus(),
                item,
                null,
                ""
        );
    }

    public static TvAppState playbackReady(TvAppState state, PlayableMedia playableMedia) {
        if (state.selectedItem() == null) {
            throw new IllegalStateException("selected item is required before playback");
        }
        return state.with(
                TvRoute.PLAYER,
                TvStatus.READY,
                state.pendingAddress(),
                state.server(),
                state.publicUsers(),
                state.authenticated(),
                state.homeRows(),
                state.focus(),
                state.selectedItem(),
                playableMedia,
                ""
        );
    }

    public static TvAppState back(TvAppState state) {
        return switch (state.route()) {
            case PLAYER -> state.with(TvRoute.DETAILS, TvStatus.READY, state.pendingAddress(), state.server(), state.publicUsers(), state.authenticated(), state.homeRows(), state.focus(), state.selectedItem(), null, "");
            case DETAILS -> state.with(TvRoute.HOME, TvStatus.READY, state.pendingAddress(), state.server(), state.publicUsers(), state.authenticated(), state.homeRows(), state.focus(), null, null, "");
            case HOME -> state.with(TvRoute.SERVER_ENTRY, TvStatus.IDLE, null, null, AndroidCollections.emptyList(), null, AndroidCollections.emptyList(), null, null, null, "");
            case LOGIN -> state.with(TvRoute.SERVER_ENTRY, TvStatus.IDLE, state.pendingAddress(), null, AndroidCollections.emptyList(), null, AndroidCollections.emptyList(), null, null, null, "");
            case ERROR -> state.with(TvRoute.SERVER_ENTRY, TvStatus.IDLE, null, null, AndroidCollections.emptyList(), null, AndroidCollections.emptyList(), null, null, null, "");
            case SERVER_ENTRY -> state;
        };
    }

    public static TvAppState publicUsersLoaded(TvAppState state, List<PublicUserSummary> users) {
        return state.with(
                state.route(),
                state.status(),
                state.pendingAddress(),
                state.server(),
                AndroidCollections.listCopy(users),
                state.authenticated(),
                state.homeRows(),
                state.focus(),
                state.selectedItem(),
                state.playableMedia(),
                state.errorMessage()
        );
    }

    public static TvAppState fail(TvAppState state, String message) {
        return state.with(
                TvRoute.ERROR,
                TvStatus.ERROR,
                state.pendingAddress(),
                state.server(),
                state.publicUsers(),
                state.authenticated(),
                state.homeRows(),
                state.focus(),
                state.selectedItem(),
                state.playableMedia(),
                message == null ? "" : message
        );
    }

    /**
     * Replaces the user data on the currently selected item (and any matching
     * cell in the home rows so badges / progress stay in sync) while keeping
     * focus identity, route, and the rest of the state untouched.
     */
    public static TvAppState selectedItemUserDataUpdated(TvAppState state, tv.cinepilot.core.protocol.UserItemData updated) {
        if (state.selectedItem() == null || updated == null) {
            return state;
        }
        MediaItemSummary updatedItem = state.selectedItem().withUserData(updated);
        List<HomeRow> updatedRows = replaceItemInRows(state.homeRows(), updatedItem);
        return state.with(
                state.route(),
                state.status(),
                state.pendingAddress(),
                state.server(),
                state.publicUsers(),
                state.authenticated(),
                updatedRows,
                state.focus(),
                updatedItem,
                state.playableMedia(),
                state.errorMessage()
        );
    }

    private static List<HomeRow> replaceItemInRows(List<HomeRow> rows, MediaItemSummary updated) {
        if (rows == null || rows.isEmpty()) {
            return rows;
        }
        boolean changed = false;
        List<HomeRow> out = new java.util.ArrayList<>(rows.size());
        for (HomeRow row : rows) {
            HomeRow next = row.replaceItem(updated);
            if (next != row) {
                changed = true;
            }
            out.add(next);
        }
        return changed ? AndroidCollections.listCopy(out) : rows;
    }

    private static FocusedItem firstFocusable(List<HomeRow> rows) {
        for (HomeRow row : rows) {
            if (!row.items().isEmpty()) {
                return new FocusedItem(row.id(), row.items().get(0).id());
            }
        }
        return null;
    }
}
