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
import tv.cinepilot.core.protocol.MediaStreamType;
import tv.cinepilot.core.protocol.PlayMethod;
import tv.cinepilot.core.protocol.PlayableMedia;
import tv.cinepilot.core.protocol.PlaybackDeviceProfile;
import tv.cinepilot.core.protocol.PlaybackInfo;
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
        controllerCompletesQuickConnectLogin();
        controllerRunsServerLoginBrowseAndPlaybackUseCase();
        controllerStartsPlaybackFromBeginningWhenRequested();
        controllerForwardsPlaybackPreferencesToPlaybackInfo();
        controllerForwardsDeviceProfileToPlaybackInfo();
        controllerLoadsPlaybackChoicesForTrackSelection();
        controllerLoadsNextUpForSelectedSeries();
        controllerLoadsShowStructuresWithoutChangingState();
        controllerLogoutRevokesSavedSession();
        controllerRestoresSavedSessionAndLoadsHome();
        controllerBrowsesFolderRowsAndReturnsToParent();
        controllerPaginatesFolderRows();
        controllerSearchesMediaRows();
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
                  {"Id":"next-1","Name":"Next Episode","Type":"Episode","IsPlayable":true}
                ],"TotalRecordCount":1,"StartIndex":0}
                """);
        transport.enqueue(200, """
                {"Items":[
                  {"Id":"fav-1","Name":"Fav Movie","Type":"Movie","IsPlayable":true}
                ],"TotalRecordCount":1,"StartIndex":0}
                """);
        transport.enqueue(200, """
                {"Items":[
                  {"Id":"coll-1","Name":"Coll","Type":"BoxSet","IsFolder":true}
                ],"TotalRecordCount":1,"StartIndex":0}
                """);
        transport.enqueue(200, """
                {"Items":[
                  {"Id":"playlist-1","Name":"My Playlist","Type":"Playlist","IsFolder":true}
                ],"TotalRecordCount":1,"StartIndex":0}
                """);
        transport.enqueue(200, """
                {"Items":[
                  {"Id":"coll-child-1","Name":"Collection Child","Type":"Movie","IsPlayable":true}
                ],"TotalRecordCount":1,"StartIndex":0}
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
        assertEquals(9, rows.size(), "home row count");
        assertEquals("views", rows.get(0).id(), "views row id");
        assertEquals("resume", rows.get(1).id(), "resume row id");
        assertEquals("next-up", rows.get(2).id(), "next up row id");
        assertEquals("favorites", rows.get(3).id(), "favorites row id");
        assertEquals("collections", rows.get(4).id(), "collections row id");
        assertEquals("playlists", rows.get(5).id(), "playlists row id");
        assertEquals("smart-collection:coll-1", rows.get(6).id(), "smart collection row id");
        assertEquals("latest:movies", rows.get(7).id(), "movie latest row id");
        assertEquals("latest:series", rows.get(8).id(), "series latest row id");
        assertEquals("resume-1", rows.get(1).items().get(0).id(), "resume item id");
        assertEquals("next-1", rows.get(2).items().get(0).id(), "next up item id");
        assertEquals("fav-1", rows.get(3).items().get(0).id(), "favorites item id");
        assertEquals("coll-1", rows.get(4).items().get(0).id(), "collections item id");
        assertEquals("playlist-1", rows.get(5).items().get(0).id(), "playlists item id");
        assertEquals("coll-child-1", rows.get(6).items().get(0).id(), "smart collection child id");

        TvAppState state = TvWorkflow.homeLoaded(
                TvWorkflow.loginSucceeded(TvAppState.initial(), authenticated),
                rows
        );
        assertEquals(new FocusedItem("views", "movies"), state.focus(), "loaded rows establish stable focus");

        assertEquals("/Users/user-1/Views", transport.requests.get(0).path(), "loader fetches views first");
        assertEquals("/Users/user-1/Items/Resume", transport.requests.get(1).path(), "loader fetches resume");
        assertTrue(transport.requests.get(1).url(authenticated.server().address()).contains("Limit=12"), "resume limit");
        assertEquals("/Shows/NextUp", transport.requests.get(2).path(), "loader fetches next up");
        assertTrue(transport.requests.get(2).url(authenticated.server().address()).contains("UserId=user-1"), "next up user id");
        assertTrue(
                transport.requests.get(3).url(authenticated.server().address()).contains("Filters=IsFavorite"),
                "favorites requests IsFavorite filter");
        assertTrue(
                transport.requests.get(4).url(authenticated.server().address()).contains("IncludeItemTypes=BoxSet"),
                "collections requests BoxSet");
        assertTrue(
                transport.requests.get(5).url(authenticated.server().address()).contains("IncludeItemTypes=Playlist"),
                "playlists requests Playlist type");
        assertTrue(
                transport.requests.get(6).url(authenticated.server().address()).contains("coll-1"),
                "smart collection fetches boxset children");
        assertTrue(transport.requests.get(7).url(authenticated.server().address()).contains("ParentId=movies"), "movie latest request uses view id");
        assertTrue(transport.requests.get(8).url(authenticated.server().address()).contains("ParentId=series"), "series latest request uses view id");
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

    private static void controllerCompletesQuickConnectLogin() {
        FakeTransport transport = new FakeTransport();
        transport.enqueue(200, "{\"Id\":\"server-1\",\"ServerName\":\"Jellyfin\"}");
        transport.enqueue(200, "true");
        transport.enqueue(200, "{\"Code\":\"ABCD12\",\"Secret\":\"secret-1\",\"Authenticated\":false}");
        transport.enqueue(200, "{\"Code\":\"ABCD12\",\"Secret\":\"secret-1\",\"Authenticated\":true}");
        transport.enqueue(200, "{\"AccessToken\":\"token-1\",\"ServerId\":\"server-1\",\"User\":{\"Id\":\"user-1\"}}");
        enqueueHomeResponses(transport);

        ClientIdentity client = new ClientIdentity("CinePilot TV", "Living Room TV", "device-1", "0.1.0");
        MediaBrowserClient mediaClient = new MediaBrowserClient(transport, new InMemorySessionRepository(), client);
        TvWorkflowController controller = new TvWorkflowController(mediaClient, new HomeRowsLoader(mediaClient, 12));

        controller.submitServer("https://media.example.com/jellyfin");
        assertEquals("ABCD12", controller.startQuickConnect().code(), "controller quick connect code");
        TvAppState state = controller.completeQuickConnect();

        assertEquals(TvRoute.HOME, state.route(), "quick connect routes home");
        assertEquals("user-1", state.authenticated().session().userId(), "quick connect authenticated user");
        assertEquals("/QuickConnect/Enabled", transport.requests.get(1).path(), "controller quick connect enabled");
        assertEquals("/QuickConnect/Initiate", transport.requests.get(2).path(), "controller quick connect initiate");
        assertEquals("/QuickConnect/Connect", transport.requests.get(3).path(), "controller quick connect state");
        assertEquals("/Users/AuthenticateWithQuickConnect", transport.requests.get(4).path(), "controller quick connect auth");
        assertEquals("/Users/user-1/Views", transport.requests.get(5).path(), "controller quick connect loads home");
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
        assertEquals(120000000L, state.playableMedia().startTimeTicks(), "controller playable keeps resume ticks");
        assertEquals("https://media.example.com/jellyfin/Videos/movie-1/stream.mkv?MediaSourceId=source-1", state.playableMedia().url(), "controller playable url");

        assertEquals("/System/Info/Public", transport.requests.get(0).path(), "controller discover request");
        assertEquals("/Users/AuthenticateByName", transport.requests.get(1).path(), "controller login request");
        assertEquals("/Users/user-1/Items/movie-1", transport.requests.get(10).path(), "controller item detail request");
        assertEquals("/Items/movie-1/PlaybackInfo", transport.requests.get(11).path(), "controller playback info request");
        assertTrue(transport.requests.get(11).url(MediaServerAddress.parse("https://media.example.com/jellyfin")).contains("StartTimeTicks=120000000"), "controller resume ticks");
    }

    private static void controllerStartsPlaybackFromBeginningWhenRequested() {
        FakeTransport transport = new FakeTransport();
        transport.enqueue(200, "{\"Id\":\"server-1\",\"ServerName\":\"Jellyfin\"}");
        transport.enqueue(200, "{\"AccessToken\":\"token-1\",\"ServerId\":\"server-1\",\"User\":{\"Id\":\"user-1\"}}");
        enqueueHomeResponses(transport);
        transport.enqueue(200, "{\"Id\":\"movie-1\",\"Name\":\"Arrival\",\"Type\":\"Movie\",\"IsPlayable\":true,\"UserData\":{\"PlaybackPositionTicks\":120000000}}");
        transport.enqueue(200, """
                {"PlaySessionId":"play-session-1","MediaSources":[
                  {"Id":"source-2","DirectStreamUrl":"/Videos/movie-1/stream.mkv?MediaSourceId=source-2","SupportsDirectStream":true}
                ]}
                """);

        ClientIdentity client = new ClientIdentity("CinePilot TV", "Living Room TV", "device-1", "0.1.0");
        MediaBrowserClient mediaClient = new MediaBrowserClient(transport, new InMemorySessionRepository(), client);
        TvWorkflowController controller = new TvWorkflowController(mediaClient, new HomeRowsLoader(mediaClient, 12));

        controller.submitServer("https://media.example.com/jellyfin");
        controller.login("demo", "secret");
        controller.openItem("movie-1");
        controller.preparePlayback(PlaybackSelectionPreferences.defaults());

        String playbackInfoUrl = transport.requests.get(11).url(MediaServerAddress.parse("https://media.example.com/jellyfin"));
        assertTrue(playbackInfoUrl.contains("StartTimeTicks=0"), "explicit playback preferences start from beginning");
        assertEquals(0L, controller.state().playableMedia().startTimeTicks(), "explicit defaults keep start from beginning");
    }

    private static void controllerForwardsPlaybackPreferencesToPlaybackInfo() {
        FakeTransport transport = new FakeTransport();
        transport.enqueue(200, "{\"Id\":\"server-1\",\"ServerName\":\"Jellyfin\"}");
        transport.enqueue(200, "{\"AccessToken\":\"token-1\",\"ServerId\":\"server-1\",\"User\":{\"Id\":\"user-1\"}}");
        enqueueHomeResponses(transport);
        transport.enqueue(200, "{\"Id\":\"movie-1\",\"Name\":\"Arrival\",\"Type\":\"Movie\",\"IsPlayable\":true,\"UserData\":{\"PlaybackPositionTicks\":120000000}}");
        transport.enqueue(200, """
                {"PlaySessionId":"play-session-1","MediaSources":[
                  {"Id":"source-2","DirectStreamUrl":"/Videos/movie-1/stream.mkv?MediaSourceId=source-2","SupportsDirectStream":true}
                ]}
                """);

        ClientIdentity client = new ClientIdentity("CinePilot TV", "Living Room TV", "device-1", "0.1.0");
        MediaBrowserClient mediaClient = new MediaBrowserClient(transport, new InMemorySessionRepository(), client);
        TvWorkflowController controller = new TvWorkflowController(mediaClient, new HomeRowsLoader(mediaClient, 12));

        controller.submitServer("https://media.example.com/jellyfin");
        controller.login("demo", "secret");
        controller.openItem("movie-1");
        controller.preparePlayback(new PlaybackSelectionPreferences(0L, 2, 5, 2, 1280, 720, 4_000_000)
                .withMediaSourceId("source-2")
                .withPlaybackRate(1.5f)
                .withAlwaysBurnInSubtitleWhenTranscoding(true));

        String playbackInfoUrl = transport.requests.get(11).url(MediaServerAddress.parse("https://media.example.com/jellyfin"));
        assertTrue(playbackInfoUrl.contains("StartTimeTicks=0"), "controller forwards preferred start ticks");
        assertTrue(playbackInfoUrl.contains("MaxStreamingBitrate=4000000"), "controller forwards max bitrate");
        assertTrue(playbackInfoUrl.contains("AudioStreamIndex=2"), "controller forwards audio stream");
        assertTrue(playbackInfoUrl.contains("SubtitleStreamIndex=5"), "controller forwards subtitle stream");
        assertTrue(
                playbackInfoUrl.contains("AlwaysBurnInSubtitleWhenTranscoding=true"),
                "controller forwards subtitle burn-in preference"
        );
        assertTrue(playbackInfoUrl.contains("MaxAudioChannels=2"), "controller forwards audio channel limit");
        assertTrue(playbackInfoUrl.contains("MediaSourceId=source-2"), "controller forwards media source preference");
        assertEquals("source-2", controller.state().playableMedia().mediaSourceId(), "controller keeps selected media source");
        assertEquals(0L, controller.state().playableMedia().startTimeTicks(), "controller keeps selected start ticks");
        assertEquals(Float.valueOf(1.5f), controller.state().playableMedia().playbackRate(), "controller keeps selected playback speed");
    }

    private static void controllerForwardsDeviceProfileToPlaybackInfo() {
        FakeTransport transport = new FakeTransport();
        transport.enqueue(200, "{\"Id\":\"server-1\",\"ServerName\":\"Jellyfin\"}");
        transport.enqueue(200, "{\"AccessToken\":\"token-1\",\"ServerId\":\"server-1\",\"User\":{\"Id\":\"user-1\"}}");
        enqueueHomeResponses(transport);
        transport.enqueue(200, "{\"Id\":\"movie-1\",\"Name\":\"Arrival\",\"Type\":\"Movie\",\"IsPlayable\":true}");
        transport.enqueue(200, """
                {"PlaySessionId":"play-session-1","MediaSources":[
                  {"Id":"source-1","DirectStreamUrl":"/Videos/movie-1/stream.mkv?MediaSourceId=source-1","SupportsDirectStream":true}
                ]}
                """);

        ClientIdentity client = new ClientIdentity("CinePilot TV", "Living Room TV", "device-1", "0.1.0");
        MediaBrowserClient mediaClient = new MediaBrowserClient(transport, new InMemorySessionRepository(), client);
        PlaybackDeviceProfile profile = new PlaybackDeviceProfile(
                "CinePilot TV Living Room",
                List.of("h264", "hevc"),
                List.of("aac", "eac3"),
                List.of("srt")
        );
        TvWorkflowController controller = new TvWorkflowController(mediaClient, new HomeRowsLoader(mediaClient, 12), profile);

        controller.submitServer("https://media.example.com/jellyfin");
        controller.login("demo", "secret");
        controller.openItem("movie-1");
        controller.preparePlayback(null);

        ProtocolRequest playbackInfo = transport.requests.get(11);
        assertEquals("/Items/movie-1/PlaybackInfo", playbackInfo.path(), "profile playback info path");
        assertEquals("POST", playbackInfo.method().name(), "profile playback info method");
        assertTrue(playbackInfo.bodyJson().contains("\"DeviceProfile\""), "controller forwards device profile");
        assertTrue(playbackInfo.bodyJson().contains("\"VideoCodec\":\"h264,hevc\""), "controller forwards video codecs");
        assertTrue(playbackInfo.bodyJson().contains("\"AudioCodec\":\"aac,eac3\""), "controller forwards audio codecs");
        assertTrue(playbackInfo.bodyJson().contains("\"StartTimeTicks\":0"), "controller keeps default start ticks in profile body");
    }

    private static void controllerLoadsPlaybackChoicesForTrackSelection() {
        FakeTransport transport = new FakeTransport();
        transport.enqueue(200, "{\"Id\":\"server-1\",\"ServerName\":\"Jellyfin\"}");
        transport.enqueue(200, "{\"AccessToken\":\"token-1\",\"ServerId\":\"server-1\",\"User\":{\"Id\":\"user-1\"}}");
        enqueueHomeResponses(transport);
        transport.enqueue(200, "{\"Id\":\"movie-1\",\"Name\":\"Arrival\",\"Type\":\"Movie\",\"IsPlayable\":true,\"UserData\":{\"PlaybackPositionTicks\":120000000}}");
        transport.enqueue(200, """
                {"PlaySessionId":"play-session-1","MediaSources":[
                  {"Id":"source-1","DirectStreamUrl":"/Videos/movie-1/stream.mkv?MediaSourceId=source-1","SupportsDirectStream":true,
                    "MediaStreams":[
                      {"Index":0,"Type":"Video","Codec":"h264","DisplayTitle":"1080p H.264"},
                      {"Index":1,"Type":"Audio","Codec":"aac","Language":"eng","DisplayTitle":"English","IsDefault":true},
                      {"Index":2,"Type":"Audio","Codec":"aac","Language":"jpn","DisplayTitle":"Japanese"},
                      {"Index":3,"Type":"Subtitle","Codec":"srt","Language":"eng","DisplayTitle":"English","IsExternal":true}
                    ]
                  }
                ]}
                """);

        ClientIdentity client = new ClientIdentity("CinePilot TV", "Living Room TV", "device-1", "0.1.0");
        MediaBrowserClient mediaClient = new MediaBrowserClient(transport, new InMemorySessionRepository(), client);
        TvWorkflowController controller = new TvWorkflowController(mediaClient, new HomeRowsLoader(mediaClient, 12));

        controller.submitServer("https://media.example.com/jellyfin");
        controller.login("demo", "secret");
        controller.openItem("movie-1");
        PlaybackInfo choices = controller.loadPlaybackChoices(null);

        assertEquals("play-session-1", choices.playSessionId(), "playback choices keep session");
        assertEquals(4, choices.mediaSources().get(0).mediaStreams().size(), "playback choices keep streams");
        assertEquals(MediaStreamType.AUDIO, choices.mediaSources().get(0).mediaStreams().get(1).type(), "audio stream type maps");
        assertEquals(MediaStreamType.SUBTITLE, choices.mediaSources().get(0).mediaStreams().get(3).type(), "subtitle stream type maps");
        String playbackInfoUrl = transport.requests.get(11).url(MediaServerAddress.parse("https://media.example.com/jellyfin"));
        assertTrue(playbackInfoUrl.contains("StartTimeTicks=120000000"), "choices use resume ticks by default");
    }

    private static void controllerLoadsNextUpForSelectedSeries() {
        FakeTransport transport = new FakeTransport();
        transport.enqueue(200, "{\"Id\":\"server-1\",\"ServerName\":\"Jellyfin\"}");
        transport.enqueue(200, "{\"AccessToken\":\"token-1\",\"ServerId\":\"server-1\",\"User\":{\"Id\":\"user-1\"}}");
        enqueueHomeResponses(transport);
        transport.enqueue(200, """
                {"Id":"episode-1","Name":"Episode 1","Type":"Episode","IsPlayable":true,"SeriesId":"series-1"}
                """);
        transport.enqueue(200, """
                {"Items":[
                  {"Id":"episode-2","Name":"Episode 2","Type":"Episode","IsPlayable":true,"SeriesId":"series-1"}
                ],"TotalRecordCount":1,"StartIndex":0}
                """);

        ClientIdentity client = new ClientIdentity("CinePilot TV", "Living Room TV", "device-1", "0.1.0");
        MediaBrowserClient mediaClient = new MediaBrowserClient(transport, new InMemorySessionRepository(), client);
        TvWorkflowController controller = new TvWorkflowController(mediaClient, new HomeRowsLoader(mediaClient, 12));

        controller.submitServer("https://media.example.com/jellyfin");
        controller.login("demo", "secret");
        controller.openItem("episode-1");
        MediaItemSummary nextUp = controller.nextUpForSelectedSeries();

        assertEquals("episode-2", nextUp.id(), "controller loads next up episode for selected series");
        ProtocolRequest nextUpRequest = transport.requests.get(transport.requests.size() - 1);
        String nextUpUrl = nextUpRequest.url(MediaServerAddress.parse("https://media.example.com/jellyfin"));
        assertEquals("/Shows/NextUp", nextUpRequest.path(), "selected series next up path");
        assertTrue(nextUpUrl.contains("SeriesId=series-1"), "selected series id query");
        assertTrue(nextUpUrl.contains("Limit=1"), "selected series limit");
    }

    private static void controllerLoadsShowStructuresWithoutChangingState() {
        FakeTransport transport = new FakeTransport();
        transport.enqueue(200, "{\"Id\":\"server-1\",\"ServerName\":\"Jellyfin\"}");
        transport.enqueue(200, "{\"AccessToken\":\"token-1\",\"ServerId\":\"server-1\",\"User\":{\"Id\":\"user-1\"}}");
        enqueueHomeResponses(transport);
        enqueueSeriesStructureResponses(transport);
        enqueueSeasonStructureResponses(transport);
        enqueueEpisodeContextResponses(transport);

        ClientIdentity client = new ClientIdentity("CinePilot TV", "Living Room TV", "device-1", "0.1.0");
        MediaBrowserClient mediaClient = new MediaBrowserClient(transport, new InMemorySessionRepository(), client);
        TvWorkflowController controller = new TvWorkflowController(mediaClient, new HomeRowsLoader(mediaClient, 12));

        controller.submitServer("https://media.example.com/jellyfin");
        TvAppState home = controller.login("root", "secret");
        ShowStructure series = controller.loadSeriesStructure("series-1");
        ShowStructure season = controller.loadSeasonStructure("season-1");
        ShowStructure episode = controller.loadEpisodeContext(episodeItem());

        assertEquals(TvRoute.HOME, controller.state().route(), "show structure loads do not route away from home");
        assertEquals(home.focus(), controller.state().focus(), "show structure loads preserve home focus");
        assertEquals("series-1", series.series().id(), "series structure series id");
        assertEquals("season-1", series.selectedSeason().id(), "series structure selected season");
        assertEquals(2, series.episodes().size(), "series structure episode preview count");
        assertEquals("season-1", season.selectedSeason().id(), "season structure selected season");
        assertEquals(2, season.seasons().size(), "season structure still has all seasons");
        assertEquals("episode-1", episode.resumeEpisode().id(), "episode context keeps current episode");
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
        assertEquals("/Sessions/Logout", transport.requests.get(10).path(), "controller sends logout request");
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
        assertEquals("/Users/user-1/Items", transport.requests.get(10).path(), "folder browse uses items endpoint");
        assertTrue(transport.requests.get(10).url(MediaServerAddress.parse("https://media.example.com/jellyfin")).contains("ParentId=movies"), "folder browse passes parent id");
        assertTrue(transport.requests.get(10).url(MediaServerAddress.parse("https://media.example.com/jellyfin")).contains("Limit=1"), "folder browse limits to first child");
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
        assertTrue(transport.requests.get(10).url(MediaServerAddress.parse("https://media.example.com/jellyfin")).contains("ParentId=series"), "folder browse passes parent id");
        assertTrue(transport.requests.get(10).url(MediaServerAddress.parse("https://media.example.com/jellyfin")).contains("Limit=50"), "folder browse requests a page");

        TvAppState restored = controller.back();

        assertEquals(root.homeRows().get(0).id(), restored.homeRows().get(0).id(), "folder back restores root rows");
        assertTrue(!controller.canGoBackInBrowse(), "folder back clears one stack level");
    }

    private static void controllerPaginatesFolderRows() {
        FakeTransport transport = new FakeTransport();
        transport.enqueue(200, "{\"Id\":\"server-1\",\"ServerName\":\"Jellyfin\"}");
        transport.enqueue(200, "{\"AccessToken\":\"token-1\",\"ServerId\":\"server-1\",\"User\":{\"Id\":\"user-1\"}}");
        enqueueHomeResponses(transport);
        transport.enqueue(200, """
                {"Items":[
                  {"Id":"episode-1","Name":"Episode 1","Type":"Episode","IsPlayable":true}
                ],"TotalRecordCount":75,"StartIndex":0}
                """);
        transport.enqueue(200, """
                {"Items":[
                  {"Id":"episode-51","Name":"Episode 51","Type":"Episode","IsPlayable":true}
                ],"TotalRecordCount":75,"StartIndex":50}
                """);
        transport.enqueue(200, """
                {"Items":[
                  {"Id":"episode-1","Name":"Episode 1","Type":"Episode","IsPlayable":true}
                ],"TotalRecordCount":75,"StartIndex":0}
                """);

        ClientIdentity client = new ClientIdentity("CinePilot TV", "Living Room TV", "device-1", "0.1.0");
        MediaBrowserClient mediaClient = new MediaBrowserClient(transport, new InMemorySessionRepository(), client);
        TvWorkflowController controller = new TvWorkflowController(mediaClient, new HomeRowsLoader(mediaClient, 12));

        controller.submitServer("https://media.example.com/jellyfin");
        controller.login("demo", "secret");
        TvAppState firstPage = controller.openFolder("series", "Series");
        assertTrue(controller.canPageForwardInBrowse(), "first page has next page");
        assertTrue(!controller.canPageBackwardInBrowse(), "first page has no previous page");
        assertTrue(firstPage.homeRows().get(0).title().contains("1-1/75"), "first page title includes range");

        TvAppState secondPage = controller.nextBrowsePage();
        assertTrue(!controller.canPageForwardInBrowse(), "last page has no next page");
        assertTrue(controller.canPageBackwardInBrowse(), "last page has previous page");
        assertEquals("episode-51", secondPage.homeRows().get(0).items().get(0).id(), "next page item");
        assertTrue(transport.requests.get(11).url(MediaServerAddress.parse("https://media.example.com/jellyfin")).contains("StartIndex=50"), "next page start index");

        TvAppState backToFirstPage = controller.previousBrowsePage();
        assertEquals("episode-1", backToFirstPage.homeRows().get(0).items().get(0).id(), "previous page item");
        assertTrue(transport.requests.get(12).url(MediaServerAddress.parse("https://media.example.com/jellyfin")).contains("StartIndex=0"), "previous page start index");
    }

    private static void controllerSearchesMediaRows() {
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
        TvAppState home = controller.login("demo", "secret");
        TvAppState search = controller.search(" arrival ", SearchFilter.MOVIES);

        assertEquals(TvRoute.HOME, search.route(), "search stays on home route");
        assertEquals(1, search.homeRows().size(), "search uses one row");
        assertEquals("搜索：arrival / 电影", search.homeRows().get(0).title(), "search title includes term and filter");
        assertEquals("movie-1", search.homeRows().get(0).items().get(0).id(), "search row item");
        assertTrue(controller.canGoBackInBrowse(), "search enables back stack");
        assertTrue(transport.requests.get(10).url(MediaServerAddress.parse("https://media.example.com/jellyfin")).contains("SearchTerm=arrival"), "search query passes term");
        assertTrue(transport.requests.get(10).url(MediaServerAddress.parse("https://media.example.com/jellyfin")).contains("IncludeItemTypes=Movie"), "search query passes filter");
        assertTrue(transport.requests.get(10).url(MediaServerAddress.parse("https://media.example.com/jellyfin")).contains("Limit=50"), "search query limits page size");

        TvAppState restored = controller.back();
        assertEquals(home.homeRows().get(0).id(), restored.homeRows().get(0).id(), "search back restores previous home");
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
        state = TvWorkflow.fail(
                state,
                "解码失败，请尝试低码率播放 https://media.example.com/movie.mkv?api_key=token-1 " +
                        "X-Emby-Token=token-2 MediaBrowser Token=\"token-3\""
        );
        String diagnostics = TvDiagnostics.describe(state);
        assertTrue(diagnostics.contains("serverId=server-1"), "diagnostics includes server");
        assertTrue(diagnostics.contains("itemId=movie-1"), "diagnostics includes item");
        assertTrue(diagnostics.contains("playMethod=DIRECT_PLAY"), "diagnostics includes play method");
        assertTrue(diagnostics.contains("errorMessage=解码失败，请尝试低码率播放"), "diagnostics includes error message");
        assertTrue(diagnostics.contains("api_key=<redacted>"), "diagnostics redacts playback URL token");
        assertTrue(diagnostics.contains("X-Emby-Token=<redacted>"), "diagnostics redacts token header");
        assertTrue(diagnostics.contains("Token=\"<redacted>\""), "diagnostics redacts authorization token");
        assertTrue(!diagnostics.contains("token-1"), "diagnostics does not include token");
        assertTrue(!diagnostics.contains("token-2"), "diagnostics does not include header token");
        assertTrue(!diagnostics.contains("token-3"), "diagnostics does not include authorization token");
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
                  {"Id":"next-1","Name":"Next Episode","Type":"Episode","IsPlayable":true}
                ],"TotalRecordCount":1,"StartIndex":0}
                """);
        transport.enqueue(200, """
                {"Items":[
                  {"Id":"fav-1","Name":"Favorite","Type":"Movie","IsPlayable":true,"UserData":{"IsFavorite":true}}
                ],"TotalRecordCount":1,"StartIndex":0}
                """);
        transport.enqueue(200, """
                {"Items":[
                  {"Id":"coll-1","Name":"Collection","Type":"BoxSet","IsFolder":true}
                ],"TotalRecordCount":1,"StartIndex":0}
                """);
        transport.enqueue(200, """
                {"Items":[],"TotalRecordCount":0,"StartIndex":0}
                """);
        transport.enqueue(200, """
                {"Items":[],"TotalRecordCount":0,"StartIndex":0}
                """);
        transport.enqueue(200, """
                {"Items":[
                  {"Id":"movie-1","Name":"New Movie","Type":"Movie","IsPlayable":true}
                ],"TotalRecordCount":1,"StartIndex":0}
                """);
    }

    private static void enqueueSeriesStructureResponses(FakeTransport transport) {
        transport.enqueue(200, "{\"Id\":\"series-1\",\"Name\":\"Show\",\"Type\":\"Series\",\"IsFolder\":true}");
        transport.enqueue(200, seasonsJson());
        transport.enqueue(200, episodesJson());
        transport.enqueue(200, nextUpJson());
    }

    private static void enqueueSeasonStructureResponses(FakeTransport transport) {
        transport.enqueue(200, "{\"Id\":\"season-1\",\"Name\":\"Season 1\",\"Type\":\"Season\",\"ParentId\":\"series-1\",\"SeriesId\":\"series-1\",\"IsFolder\":true}");
        transport.enqueue(200, "{\"Id\":\"series-1\",\"Name\":\"Show\",\"Type\":\"Series\",\"IsFolder\":true}");
        transport.enqueue(200, seasonsJson());
        transport.enqueue(200, episodesJson());
        transport.enqueue(200, nextUpJson());
    }

    private static void enqueueEpisodeContextResponses(FakeTransport transport) {
        transport.enqueue(200, "{\"Id\":\"season-1\",\"Name\":\"Season 1\",\"Type\":\"Season\",\"ParentId\":\"series-1\",\"SeriesId\":\"series-1\",\"IsFolder\":true}");
        transport.enqueue(200, "{\"Id\":\"series-1\",\"Name\":\"Show\",\"Type\":\"Series\",\"IsFolder\":true}");
        transport.enqueue(200, seasonsJson());
        transport.enqueue(200, episodesJson());
        transport.enqueue(200, nextUpJson());
    }

    private static String seasonsJson() {
        return """
                {"Items":[
                  {"Id":"season-1","Name":"Season 1","Type":"Season","ParentId":"series-1","SeriesId":"series-1","IsFolder":true},
                  {"Id":"season-2","Name":"Season 2","Type":"Season","ParentId":"series-1","SeriesId":"series-1","IsFolder":true}
                ],"TotalRecordCount":2,"StartIndex":0}
                """;
    }

    private static String episodesJson() {
        return """
                {"Items":[
                  {"Id":"episode-1","Name":"Episode 1","Type":"Episode","ParentId":"season-1","SeriesId":"series-1","IsPlayable":true,"UserData":{"PlaybackPositionTicks":10000000}},
                  {"Id":"episode-2","Name":"Episode 2","Type":"Episode","ParentId":"season-1","SeriesId":"series-1","IsPlayable":true}
                ],"TotalRecordCount":2,"StartIndex":0}
                """;
    }

    private static String nextUpJson() {
        return """
                {"Items":[
                  {"Id":"episode-2","Name":"Episode 2","Type":"Episode","ParentId":"season-1","SeriesId":"series-1","IsPlayable":true}
                ],"TotalRecordCount":1,"StartIndex":0}
                """;
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
                "",
                List.of(),
                UserItemData.empty(),
                Map.of()
        );
    }

    private static MediaItemSummary episodeItem() {
        return new MediaItemSummary(
                "episode-1",
                "season-1",
                "Episode 1",
                MediaItemType.EPISODE,
                false,
                true,
                null,
                null,
                1,
                1,
                "Show",
                "series-1",
                "",
                List.of(),
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
