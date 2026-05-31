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
import tv.cinepilot.core.protocol.PlaybackSelectionPreferences;
import tv.cinepilot.core.protocol.ProtocolRequest;
import tv.cinepilot.core.protocol.ProtocolResponse;
import tv.cinepilot.core.protocol.PublicUserSummary;
import tv.cinepilot.core.protocol.SavedSession;
import tv.cinepilot.core.protocol.ServerFlavor;
import tv.cinepilot.core.protocol.ServerIdentity;
import tv.cinepilot.core.protocol.SessionScope;
import tv.cinepilot.core.protocol.UserItemData;
import java.io.IOException;
import java.util.ArrayList;

public final class TvWorkflowTest {
    public static void main(String[] args) {
        runsServerLoginHomeDetailsPlayerFlow();
        rejectsFocusForMissingItems();
        loadsHomeRowsFromMediaBrowserClient();
        controllerLoadsPublicUsersForTvLogin();
        controllerRunsServerLoginBrowseAndPlaybackUseCase();
        controllerStartsPlaybackFromBeginningWhenRequested();
        controllerLogoutRevokesSavedSession();
        controllerRestoresSavedSessionAndLoadsHome();
        controllerBrowsesFolderRowsAndReturnsToParent();
        controllerOpensFirstChildForFolderBrowse();
        controllerReportsEmptyFolderBrowse();
        controllerReportsUnsupportedPlayback();
        describesDiagnosticsWithoutToken();
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

    private static void controllerLoadsPublicUsersForTvLogin() {
        FakeTransport transport = new FakeTransport();
        transport.enqueue(200, "{\"Id\":\"server-1\",\"ServerName\":\"Jellyfin\"}");
        transport.enqueue(200, """
                [
                  {"Id":"user-1","Name":"Demo","HasPassword":true},
                  {"Id":"user-2","Name":"Kids","HasPassword":false}
                ]
                """);

        ClientIdentity client = new ClientIdentity("CinePilot TV", "Living Room TV", "device-1", "0.1.0");
        MediaBrowserClient mediaClient = new MediaBrowserClient(transport, new InMemorySessionRepository(), client);
        TvWorkflowController controller = new TvWorkflowController(mediaClient, new HomeRowsLoader(mediaClient, 12));

        controller.submitServer("https://media.example.com/jellyfin");
        TvAppState state = controller.loadPublicUsers();
        List<PublicUserSummary> users = state.publicUsers();

        assertEquals(TvRoute.LOGIN, state.route(), "public users stay on login route");
        assertEquals(2, users.size(), "controller public user count");
        assertEquals("Demo", users.get(0).name(), "controller public user name");
        assertTrue(users.get(0).passwordRequired(), "controller public user password flag");
        assertEquals("/Users/Public", transport.requests.get(1).path(), "controller public users request");
    }

    private static void controllerRunsServerLoginBrowseAndPlaybackUseCase() {
        FakeTransport transport = new FakeTransport();
        transport.enqueue(200, "{\"Id\":\"server-1\",\"ServerName\":\"Jellyfin\"}");
        transport.enqueue(200, "{\"AccessToken\":\"token-1\",\"ServerId\":\"server-1\",\"User\":{\"Id\":\"user-1\"}}");
        enqueueHomeResponses(transport);
        transport.enqueue(200, "{\"Id\":\"movie-1\",\"Name\":\"Arrival\",\"Type\":\"Movie\",\"IsPlayable\":true,\"UserData\":{\"PlaybackPositionTicks\":120000000}}");
        transport.enqueue(200, """
                {"PlaySessionId":"play-session-1","MediaSources":[
                  {"Id":"source-1","DirectStreamUrl":"/Videos/movie-1/stream.mkv?MediaSourceId=source-1","SupportsDirectStream":true}
                ]}
                """);

        ClientIdentity client = new ClientIdentity("CinePilot TV", "Living Room TV", "device-1", "0.1.0");
        MediaBrowserClient mediaClient = new MediaBrowserClient(transport, new InMemorySessionRepository(), client);
        TvWorkflowController controller = new TvWorkflowController(mediaClient, new HomeRowsLoader(mediaClient, 12));

        TvAppState state = controller.submitServer("https://media.example.com/jellyfin");
        assertEquals(TvRoute.LOGIN, state.route(), "controller discovers server");

        state = controller.login("demo", "secret");
        assertEquals(TvRoute.HOME, state.route(), "controller logs in and loads home");
        assertEquals(new FocusedItem("views", "movies"), state.focus(), "controller home focus");

        state = controller.focusItem("latest:movies", "movie-1");
        assertEquals(new FocusedItem("latest:movies", "movie-1"), state.focus(), "controller records item focus");

        state = controller.openItem("movie-1");
        assertEquals(TvRoute.DETAILS, state.route(), "controller opens item details");
        assertEquals(new FocusedItem("latest:movies", "movie-1"), state.focus(), "controller keeps focus through details");
        assertEquals("movie-1", state.selectedItem().id(), "controller selected item");

        state = controller.preparePlayback(null);
        assertEquals(TvRoute.PLAYER, state.route(), "controller prepares playback");
        assertEquals("source-1", state.playableMedia().mediaSourceId(), "controller playable source");
        assertEquals("https://media.example.com/jellyfin/Videos/movie-1/stream.mkv?MediaSourceId=source-1", state.playableMedia().url(), "controller playable url");

        assertEquals("/System/Info/Public", transport.requests.get(0).path(), "controller discover request");
        assertEquals("/Users/AuthenticateByName", transport.requests.get(1).path(), "controller login request");
        assertEquals("/Users/user-1/Items/movie-1", transport.requests.get(6).path(), "controller item detail request");
        assertEquals("/Items/movie-1/PlaybackInfo", transport.requests.get(7).path(), "controller playback info request");
        assertTrue(transport.requests.get(7).url(MediaServerAddress.parse("https://media.example.com/jellyfin")).contains("StartTimeTicks=120000000"), "controller resume ticks");
    }

    private static void controllerStartsPlaybackFromBeginningWhenRequested() {
        FakeTransport transport = new FakeTransport();
        transport.enqueue(200, "{\"Id\":\"server-1\",\"ServerName\":\"Jellyfin\"}");
        transport.enqueue(200, "{\"AccessToken\":\"token-1\",\"ServerId\":\"server-1\",\"User\":{\"Id\":\"user-1\"}}");
        enqueueHomeResponses(transport);
        transport.enqueue(200, "{\"Id\":\"movie-1\",\"Name\":\"Arrival\",\"Type\":\"Movie\",\"IsPlayable\":true,\"UserData\":{\"PlaybackPositionTicks\":120000000}}");
        transport.enqueue(200, """
                {"PlaySessionId":"play-session-1","MediaSources":[
                  {"Id":"source-1","DirectStreamUrl":"/Videos/movie-1/stream.mkv?MediaSourceId=source-1","SupportsDirectStream":true}
                ]}
                """);

        ClientIdentity client = new ClientIdentity("CinePilot TV", "Living Room TV", "device-1", "0.1.0");
        MediaBrowserClient mediaClient = new MediaBrowserClient(transport, new InMemorySessionRepository(), client);
        TvWorkflowController controller = new TvWorkflowController(mediaClient, new HomeRowsLoader(mediaClient, 12));

        controller.submitServer("https://media.example.com/jellyfin");
        controller.login("demo", "secret");
        controller.openItem("movie-1");
        controller.preparePlayback(PlaybackSelectionPreferences.defaults());

        String playbackInfoUrl = transport.requests.get(7).url(MediaServerAddress.parse("https://media.example.com/jellyfin"));
        assertTrue(playbackInfoUrl.contains("StartTimeTicks=0"), "explicit playback preferences start from beginning");
    }

    private static void controllerLogoutRevokesSavedSession() {
        FakeTransport transport = new FakeTransport();
        transport.enqueue(200, "{\"Id\":\"server-1\",\"ServerName\":\"Jellyfin\"}");
        transport.enqueue(200, "{\"AccessToken\":\"token-1\",\"ServerId\":\"server-1\",\"User\":{\"Id\":\"user-1\"}}");
        enqueueHomeResponses(transport);
        transport.enqueue(204, "");

        ClientIdentity client = new ClientIdentity("CinePilot TV", "Living Room TV", "device-1", "0.1.0");
        InMemorySessionRepository repository = new InMemorySessionRepository();
        MediaBrowserClient mediaClient = new MediaBrowserClient(transport, repository, client);
        TvWorkflowController controller = new TvWorkflowController(mediaClient, new HomeRowsLoader(mediaClient, 12));

        controller.submitServer("https://media.example.com/jellyfin");
        TvAppState state = controller.login("demo", "secret");
        ServerIdentity server = state.server();
        assertTrue(mediaClient.restore(server, "user-1").isPresent(), "login saved session before logout");

        state = controller.logout();

        assertEquals(TvRoute.SERVER_ENTRY, state.route(), "logout returns to server entry");
        assertTrue(mediaClient.restore(server, "user-1").isEmpty(), "logout revokes saved session");
        assertEquals("/Sessions/Logout", transport.requests.get(6).path(), "controller sends logout request");
    }

    private static void controllerRestoresSavedSessionAndLoadsHome() {
        FakeTransport transport = new FakeTransport();
        transport.enqueue(200, "{\"Id\":\"server-1\",\"ServerName\":\"Jellyfin\"}");
        enqueueHomeResponses(transport);

        ClientIdentity client = new ClientIdentity("CinePilot TV", "Living Room TV", "device-1", "0.1.0");
        InMemorySessionRepository repository = new InMemorySessionRepository();
        ServerIdentity server = new ServerIdentity(
                MediaServerAddress.parse("https://media.example.com/jellyfin"),
                "server-1",
                ServerFlavor.JELLYFIN,
                "Jellyfin"
        );
        AuthSession session = new AuthSession("server-1", "user-1", "token-1", client);
        repository.save(new SavedSession(SessionScope.from(server, session), session.accessToken()));

        MediaBrowserClient mediaClient = new MediaBrowserClient(transport, repository, client);
        TvWorkflowController controller = new TvWorkflowController(mediaClient, new HomeRowsLoader(mediaClient, 12));

        controller.submitServer("https://media.example.com/jellyfin");
        TvAppState state = controller.restoreSession("user-1");

        assertEquals(TvRoute.HOME, state.route(), "restore routes home");
        assertEquals("user-1", state.authenticated().session().userId(), "restore keeps user scope");
        assertEquals("token-1", state.authenticated().session().accessToken(), "restore uses saved token");
        assertEquals("/System/Info/Public", transport.requests.get(0).path(), "restore discovers server first");
        assertEquals("/Users/user-1/Views", transport.requests.get(1).path(), "restore loads home after token lookup");
    }

    private static void controllerOpensFirstChildForFolderBrowse() {
        FakeTransport transport = new FakeTransport();
        transport.enqueue(200, "{\"Id\":\"server-1\",\"ServerName\":\"Jellyfin\"}");
        transport.enqueue(200, "{\"AccessToken\":\"token-1\",\"ServerId\":\"server-1\",\"User\":{\"Id\":\"user-1\"}}");
        enqueueHomeResponses(transport);
        transport.enqueue(200, """
                {"Items":[
                  {"Id":"movie-1","Name":"Arrival","Type":"Movie","IsPlayable":true}
                ],"TotalRecordCount":1,"StartIndex":0}
                """);

        ClientIdentity client = new ClientIdentity("CinePilot TV", "Living Room TV", "device-1", "0.1.0");
        MediaBrowserClient mediaClient = new MediaBrowserClient(transport, new InMemorySessionRepository(), client);
        TvWorkflowController controller = new TvWorkflowController(mediaClient, new HomeRowsLoader(mediaClient, 12));

        controller.submitServer("https://media.example.com/jellyfin");
        controller.login("demo", "secret");
        TvAppState state = controller.openFirstChild("movies");

        assertEquals(TvRoute.DETAILS, state.route(), "folder browse opens child details");
        assertEquals("movie-1", state.selectedItem().id(), "folder browse selects first child");
        assertEquals("/Users/user-1/Items", transport.requests.get(6).path(), "folder browse uses items endpoint");
        assertTrue(transport.requests.get(6).url(MediaServerAddress.parse("https://media.example.com/jellyfin")).contains("ParentId=movies"), "folder browse passes parent id");
        assertTrue(transport.requests.get(6).url(MediaServerAddress.parse("https://media.example.com/jellyfin")).contains("Limit=1"), "folder browse limits to first child");
    }

    private static void controllerBrowsesFolderRowsAndReturnsToParent() {
        FakeTransport transport = new FakeTransport();
        transport.enqueue(200, "{\"Id\":\"server-1\",\"ServerName\":\"Jellyfin\"}");
        transport.enqueue(200, "{\"AccessToken\":\"token-1\",\"ServerId\":\"server-1\",\"User\":{\"Id\":\"user-1\"}}");
        enqueueHomeResponses(transport);
        transport.enqueue(200, """
                {"Items":[
                  {"Id":"season-1","Name":"Season 1","Type":"Season","IsFolder":true},
                  {"Id":"episode-1","Name":"Pilot","Type":"Episode","IsPlayable":true}
                ],"TotalRecordCount":2,"StartIndex":0}
                """);

        ClientIdentity client = new ClientIdentity("CinePilot TV", "Living Room TV", "device-1", "0.1.0");
        MediaBrowserClient mediaClient = new MediaBrowserClient(transport, new InMemorySessionRepository(), client);
        TvWorkflowController controller = new TvWorkflowController(mediaClient, new HomeRowsLoader(mediaClient, 12));

        controller.submitServer("https://media.example.com/jellyfin");
        TvAppState root = controller.login("demo", "secret");
        TvAppState folder = controller.openFolder("series", "Series");

        assertEquals(TvRoute.HOME, folder.route(), "folder browse stays on home route");
        assertTrue(controller.canGoBackInBrowse(), "folder browse enables back stack");
        assertEquals(1, folder.homeRows().size(), "folder browse uses one row");
        assertEquals("folder:series", folder.homeRows().get(0).id(), "folder row id");
        assertEquals("season-1", folder.homeRows().get(0).items().get(0).id(), "folder child item");
        assertTrue(transport.requests.get(6).url(MediaServerAddress.parse("https://media.example.com/jellyfin")).contains("ParentId=series"), "folder browse passes parent id");
        assertTrue(transport.requests.get(6).url(MediaServerAddress.parse("https://media.example.com/jellyfin")).contains("Limit=50"), "folder browse requests a page");

        TvAppState restored = controller.back();

        assertEquals(root.homeRows().get(0).id(), restored.homeRows().get(0).id(), "folder back restores root rows");
        assertTrue(!controller.canGoBackInBrowse(), "folder back clears one stack level");
    }

    private static void controllerReportsEmptyFolderBrowse() {
        FakeTransport transport = new FakeTransport();
        transport.enqueue(200, "{\"Id\":\"server-1\",\"ServerName\":\"Jellyfin\"}");
        transport.enqueue(200, "{\"AccessToken\":\"token-1\",\"ServerId\":\"server-1\",\"User\":{\"Id\":\"user-1\"}}");
        enqueueHomeResponses(transport);
        transport.enqueue(200, "{\"Items\":[],\"TotalRecordCount\":0,\"StartIndex\":0}");

        ClientIdentity client = new ClientIdentity("CinePilot TV", "Living Room TV", "device-1", "0.1.0");
        MediaBrowserClient mediaClient = new MediaBrowserClient(transport, new InMemorySessionRepository(), client);
        TvWorkflowController controller = new TvWorkflowController(mediaClient, new HomeRowsLoader(mediaClient, 12));

        controller.submitServer("https://media.example.com/jellyfin");
        controller.login("demo", "secret");
        try {
            controller.openFirstChild("empty-folder");
            throw new AssertionError("Expected empty folder browse to fail");
        } catch (IllegalStateException expected) {
            assertEquals(TvWorkflowController.NO_CHILD_ITEM_MESSAGE, expected.getMessage(), "empty folder message");
        }
    }

    private static void controllerReportsUnsupportedPlayback() {
        FakeTransport transport = new FakeTransport();
        transport.enqueue(200, "{\"Id\":\"server-1\",\"ServerName\":\"Jellyfin\"}");
        transport.enqueue(200, "{\"AccessToken\":\"token-1\",\"ServerId\":\"server-1\",\"User\":{\"Id\":\"user-1\"}}");
        enqueueHomeResponses(transport);
        transport.enqueue(200, "{\"Id\":\"movie-1\",\"Name\":\"Arrival\",\"Type\":\"Movie\",\"IsPlayable\":true}");
        transport.enqueue(200, """
                {"PlaySessionId":"play-session-1","MediaSources":[
                  {"Id":"source-1","SupportsDirectPlay":false,"SupportsDirectStream":false,"SupportsTranscoding":false}
                ]}
                """);

        ClientIdentity client = new ClientIdentity("CinePilot TV", "Living Room TV", "device-1", "0.1.0");
        MediaBrowserClient mediaClient = new MediaBrowserClient(transport, new InMemorySessionRepository(), client);
        TvWorkflowController controller = new TvWorkflowController(mediaClient, new HomeRowsLoader(mediaClient, 12));

        controller.submitServer("https://media.example.com/jellyfin");
        controller.login("demo", "secret");
        controller.openItem("movie-1");
        try {
            controller.preparePlayback(null);
            throw new AssertionError("Expected unsupported playback to fail");
        } catch (IllegalStateException expected) {
            assertEquals(TvWorkflowController.NO_PLAYABLE_SOURCE_MESSAGE, expected.getMessage(), "unsupported playback message");
        }
    }

    private static void describesDiagnosticsWithoutToken() {
        ClientIdentity client = new ClientIdentity("CinePilot TV", "Living Room TV", "device-1", "0.1.0");
        AuthenticatedServer authenticated = authenticated(client);
        MediaItemSummary movie = item("movie-1", "Arrival", MediaItemType.MOVIE, true);
        PlayableMedia playable = new PlayableMedia(
                "movie-1",
                "source-1",
                "play-session-1",
                PlayMethod.DIRECT_PLAY,
                "https://media.example.com/movie.mkv",
                null,
                1,
                2
        );
        TvAppState state = TvWorkflow.playbackReady(
                TvWorkflow.openDetails(
                        TvWorkflow.homeLoaded(
                                TvWorkflow.loginSucceeded(TvAppState.initial(), authenticated),
                                List.of(new HomeRow("continue", "继续观看", List.of(movie)))
                        ),
                        movie
                ),
                playable
        );
        String diagnostics = TvDiagnostics.describe(state);
        assertTrue(diagnostics.contains("serverId=server-1"), "diagnostics includes server");
        assertTrue(diagnostics.contains("itemId=movie-1"), "diagnostics includes item");
        assertTrue(diagnostics.contains("playMethod=DIRECT_PLAY"), "diagnostics includes play method");
        assertTrue(!diagnostics.contains("token-1"), "diagnostics does not include token");
    }

    private static void enqueueHomeResponses(FakeTransport transport) {
        transport.enqueue(200, """
                {"Items":[
                  {"Id":"movies","Name":"Movies","Type":"CollectionFolder","IsFolder":true}
                ],"TotalRecordCount":1,"StartIndex":0}
                """);
        transport.enqueue(200, """
                {"Items":[
                  {"Id":"resume-1","Name":"Resume Movie","Type":"Movie","IsPlayable":true}
                ],"TotalRecordCount":1,"StartIndex":0}
                """);
        transport.enqueue(200, """
                {"Items":[
                  {"Id":"movies","Name":"Movies","Type":"CollectionFolder","IsFolder":true}
                ],"TotalRecordCount":1,"StartIndex":0}
                """);
        transport.enqueue(200, """
                {"Items":[
                  {"Id":"movie-1","Name":"New Movie","Type":"Movie","IsPlayable":true}
                ],"TotalRecordCount":1,"StartIndex":0}
                """);
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
