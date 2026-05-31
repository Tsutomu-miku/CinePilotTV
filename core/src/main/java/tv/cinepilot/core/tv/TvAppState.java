package tv.cinepilot.core.tv;

import java.util.List;
import tv.cinepilot.core.AndroidCollections;
import tv.cinepilot.core.protocol.AuthenticatedServer;
import tv.cinepilot.core.protocol.MediaItemSummary;
import tv.cinepilot.core.protocol.MediaServerAddress;
import tv.cinepilot.core.protocol.PlayableMedia;
import tv.cinepilot.core.protocol.PublicUserSummary;
import tv.cinepilot.core.protocol.ServerIdentity;

public record TvAppState(
        TvRoute route,
        TvStatus status,
        MediaServerAddress pendingAddress,
        ServerIdentity server,
        List<PublicUserSummary> publicUsers,
        AuthenticatedServer authenticated,
        List<HomeRow> homeRows,
        FocusedItem focus,
        MediaItemSummary selectedItem,
        PlayableMedia playableMedia,
        String errorMessage
) {
    public TvAppState {
        if (route == null) {
            route = TvRoute.SERVER_ENTRY;
        }
        if (status == null) {
            status = TvStatus.IDLE;
        }
        publicUsers = AndroidCollections.listCopy(publicUsers);
        homeRows = AndroidCollections.listCopy(homeRows);
        if (errorMessage == null) {
            errorMessage = "";
        }
    }

    public static TvAppState initial() {
        return new TvAppState(
                TvRoute.SERVER_ENTRY,
                TvStatus.IDLE,
                null,
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

    TvAppState with(
            TvRoute route,
            TvStatus status,
            MediaServerAddress pendingAddress,
            ServerIdentity server,
            List<PublicUserSummary> publicUsers,
            AuthenticatedServer authenticated,
            List<HomeRow> homeRows,
            FocusedItem focus,
            MediaItemSummary selectedItem,
            PlayableMedia playableMedia,
            String errorMessage
    ) {
        return new TvAppState(
                route,
                status,
                pendingAddress,
                server,
                publicUsers,
                authenticated,
                homeRows,
                focus,
                selectedItem,
                playableMedia,
                errorMessage
        );
    }
}
