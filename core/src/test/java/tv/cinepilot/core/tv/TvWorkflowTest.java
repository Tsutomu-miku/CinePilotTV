package tv.cinepilot.core.tv;

import java.util.List;
import java.util.Map;
import tv.cinepilot.core.protocol.AuthSession;
import tv.cinepilot.core.protocol.AuthenticatedServer;
import tv.cinepilot.core.protocol.ClientIdentity;
import tv.cinepilot.core.protocol.HttpTransport;
import tv.cinepilot.core.protocol.InMemorySessionRepository;
import tv.cinepilot.core.protocol.MediaBrowserClient;
import tv.cinepilot.core.protocol.MediaItemSummary;
import tv.cinepilot.core.protocol.MediaItemType;
import tv.cinepilot.core.protocol.MediaServerAddress;
import tv.cinepilot.core.protocol.PlayMethod;
import tv.cinepilot.core.protocol.PlayableMedia;
import tv.cinepilot.core.protocol.ProtocolRequest;
import tv.cinepilot.core.protocol.ProtocolResponse;
import tv.cinepilot.core.protocol.ServerFlavor;
import tv.cinepilot.core.protocol.ServerIdentity;
import tv.cinepilot.core.protocol.UserItemData;
import java.io.IOException;
import java.util.ArrayList;

public final class TvWorkflowTest {
    public static void main(String[] args) {
        runsServerLoginHomeDetailsPlayerFlow();
        rejectsFocusForMissingItems();
        loadsHomeRowsFromMediaBrowserClient();
        System.out.println("TvWorkflowTest passed");
    }

    private static void runsServerLoginHomeDetailsPlayerFlow() {
        MediaServerAddress address = MediaServerAddress.parse("https://media.example.com/jellyfin");
        ServerIdentity server = new ServerIdentity(address, "server-1", ServerFlavor.JELLYFIN, "Jellyfin");
        ClientIdentity client = new ClientIdentity("CinePilot TV", "Living Room TV", "device-1", "0.1.0");
        AuthenticatedServer authenticated = new AuthenticatedServer(
                server,
                new AuthSession("server-1", "user-1", "token-1", client)
        );

        TvAppState state = TvAppState.initial();
        assertEquals(TvRoute.SERVER_ENTRY, state.route(), "initial route");

        state = TvWorkflow.submitServer(state, address);
        assertEquals(TvStatus.LOADING, state.status(), "server submit loads");
        assertEquals(address, state.pendingAddress(), "pending address");

        state = TvWorkflow.serverDiscovered(state, server);
        assertEquals(TvRoute.LOGIN, state.route(), "discovery routes to login");

        state = TvWorkflow.loginStarted(state);
        assertEquals(TvStatus.LOADING, state.status(), "login loading");

        state = TvWorkflow.loginSucceeded(state, authenticated);
        assertEquals(TvRoute.HOME, state.route(), "login routes home");
        assertEquals(TvStatus.LOADING, state.status(), "home loading");

        MediaItemSummary movie = item("movie-1", "Arrival", MediaItemType.MOVIE, true);
        MediaItemSummary episode = item("episode-1", "Pilot", MediaItemType.EPISODE, true);
        state = TvWorkflow.homeLoaded(state, List.of(
                new HomeRow("continue", "继续观看", List.of(movie)),
                new HomeRow("latest", "最新", List.of(episode))
        ));
        assertEquals(TvStatus.READY, state.status(), "home ready");
        assertEquals(new FocusedItem("continue", "movie-1"), state.focus(), "first item receives focus");

        state = TvWorkflow.focusItem(state, "latest", "episode-1");
        assertEquals(new FocusedItem("latest", "episode-1"), state.focus(), "focus moves by item identity");

        state = TvWorkflow.openDetails(state, episode);
        assertEquals(TvRoute.DETAILS, state.route(), "details route");
        assertEquals("episode-1", state.selectedItem().id(), "selected item preserved");

        PlayableMedia playable = new PlayableMedia(
                "episode-1",
                "source-1",
                "play-session-1",
                PlayMethod.DIRECT_PLAY,
                "https://media.example.com/episode.mkv",
                null,
                1,
                null
        );
        state = TvWorkflow.playbackReady(state, playable);
        assertEquals(TvRoute.PLAYER, state.route(), "player route");
        assertEquals(playable, state.playableMedia(), "playable media preserved");

        state = TvWorkflow.back(state);
        assertEquals(TvRoute.DETAILS, state.route(), "back from player");
        state = TvWorkflow.back(state);
        assertEquals(TvRoute.HOME, state.route(), "back from details");
        assertEquals(new FocusedItem("latest", "episode-1"), state.focus(), "home focus restored");
    }

    private static void rejectsFocusForMissingItems() {
        TvAppState state = TvWorkflow.homeLoaded(
                TvAppState.initial(),
                List.of(new HomeRow("row-1", "Row", List.of(item("item-1", "One", MediaItemType.MOVIE, true))))
        );
        try {
            TvWorkflow.focusItem(state, "row-1", "missing");
            throw new AssertionError("Expected missing focus to fail");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("Focused item"), "missing focus error");
        }
    }

    private static void loadsHomeRowsFromMediaBrowserClient() {
        FakeTransport transport = new FakeTransport();
        transport.enqueue(200, """
                {"Items":[
                  {"Id":"movies","Name":"Movies","Type":"CollectionFolder","IsFolder":true},
                  {"Id":"series","Name":"Series","Type":"CollectionFolder","IsFolder":true}
                ],"TotalRecordCount":2,"StartIndex":0}
                """);
        transport.enqueue(200, """
                {"Items":[
                  {"Id":"resume-1","Name":"Resume Movie","Type":"Movie","IsPlayable":true,"UserData":{"PlaybackPositionTicks":50000000}}
                ],"TotalRecordCount":1,"StartIndex":0}
                """);
        transport.enqueue(200, """
                {"Items":[
                  {"Id":"movies","Name":"Movies","Type":"CollectionFolder","IsFolder":true},
                  {"Id":"series","Name":"Series","Type":"CollectionFolder","IsFolder":true}
                ],"TotalRecordCount":2,"StartIndex":0}
                """);
        transport.enqueue(200, """
                {"Items":[
                  {"Id":"movie-1","Name":"New Movie","Type":"Movie","IsPlayable":true}
                ],"TotalRecordCount":1,"StartIndex":0}
                """);
        transport.enqueue(200, """
                {"Items":[
                  {"Id":"episode-1","Name":"New Episode","Type":"Episode","IsPlayable":true}
                ],"TotalRecordCount":1,"StartIndex":0}
                """);

        ClientIdentity client = new ClientIdentity("CinePilot TV", "Living Room TV", "device-1", "0.1.0");
        MediaBrowserClient mediaClient = new MediaBrowserClient(transport, new InMemorySessionRepository(), client);
        AuthenticatedServer authenticated = authenticated(client);

        HomeRowsLoader loader = new HomeRowsLoader(mediaClient, 12);
        List<HomeRow> rows = loader.load(authenticated);
        assertEquals(4, rows.size(), "home row count");
        assertEquals("views", rows.get(0).id(), "views row id");
        assertEquals("resume", rows.get(1).id(), "resume row id");
        assertEquals("latest:movies", rows.get(2).id(), "movies latest row id");
        assertEquals("latest:series", rows.get(3).id(), "series latest row id");
        assertEquals("resume-1", rows.get(1).items().get(0).id(), "resume item id");

        TvAppState state = TvWorkflow.homeLoaded(
                TvWorkflow.loginSucceeded(TvAppState.initial(), authenticated),
                rows
        );
        assertEquals(new FocusedItem("views", "movies"), state.focus(), "loaded rows establish stable focus");

        assertEquals("/Users/user-1/Views", transport.requests.get(0).path(), "loader fetches views first");
        assertEquals("/Users/user-1/Items/Resume", transport.requests.get(1).path(), "loader fetches resume");
        assertTrue(transport.requests.get(1).url(authenticated.server().address()).contains("Limit=12"), "resume limit");
        assertEquals("/Users/user-1/Views", transport.requests.get(2).path(), "loader fetches views for latest rows");
        assertTrue(transport.requests.get(3).url(authenticated.server().address()).contains("ParentId=movies"), "movies latest");
        assertTrue(transport.requests.get(4).url(authenticated.server().address()).contains("ParentId=series"), "series latest");
    }

    private static AuthenticatedServer authenticated(ClientIdentity client) {
        ServerIdentity server = new ServerIdentity(
                MediaServerAddress.parse("https://media.example.com/jellyfin"),
                "server-1",
                ServerFlavor.JELLYFIN,
                "Jellyfin"
        );
        return new AuthenticatedServer(server, new AuthSession("server-1", "user-1", "token-1", client));
    }

    private static MediaItemSummary item(String id, String name, MediaItemType type, boolean playable) {
        return new MediaItemSummary(
                id,
                "",
                name,
                type,
                false,
                playable,
                null,
                null,
                null,
                null,
                "",
                UserItemData.empty(),
                Map.of()
        );
    }

    private static final class FakeTransport implements HttpTransport {
        private final List<ProtocolRequest> requests = new ArrayList<>();
        private final List<ProtocolResponse> responses = new ArrayList<>();

        void enqueue(int statusCode, String body) {
            responses.add(new ProtocolResponse(statusCode, Map.of(), body));
        }

        @Override
        public ProtocolResponse send(MediaServerAddress address, ProtocolRequest request) throws IOException {
            requests.add(request);
            if (responses.isEmpty()) {
                throw new IOException("No fake response queued for " + request.path());
            }
            return responses.remove(0);
        }
    }

    private static void assertEquals(Object expected, Object actual, String message) {
        if (!expected.equals(actual)) {
            throw new AssertionError(message + ": expected <" + expected + "> but was <" + actual + ">");
        }
    }

    private static void assertTrue(boolean value, String message) {
        if (!value) {
            throw new AssertionError(message);
        }
    }
}
