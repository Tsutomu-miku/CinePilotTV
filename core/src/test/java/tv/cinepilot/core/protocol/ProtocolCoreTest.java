package tv.cinepilot.core.protocol;

import java.util.Map;
import java.util.List;
import java.util.Optional;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

public final class ProtocolCoreTest {
    public static void main(String[] args) {
        normalizesServerAddress();
        preservesServerPathPrefix();
        detectsServerFlavor();
        buildsAuthorizationHeader();
        buildsFlavorSpecificAuthorizationHeader();
        exposesPlaybackEndpoints();
        convertsPlaybackTicks();
        buildsPlaybackPayload();
        buildsDiscoveryAndLoginRequests();
        buildsBrowseRequests();
        scopesSavedSessions();
        mediaBrowserClientForgetsOnlyAuthenticatedScope();
        buildsPlaybackInfoAndStreamRequests();
        buildsPlaybackCheckInRequests();
        selectsPlayableMediaSources();
        mapsServerAndPlaybackResponses();
        clientRunsDiscoveryLoginAndPlaybackFlow();
        mapsAndFetchesMediaItems();
        persistsSavedSessionsToFile();
        schedulesPlaybackCheckIns();
        clientSendsPlaybackCheckIns();
        playbackSessionControllerSendsPlayerEvents();
        authorizesPlaybackUrls();
        System.out.println("ProtocolCoreTest passed");
    }

    private static void normalizesServerAddress() {
        MediaServerAddress address = MediaServerAddress.parse("Example.COM:8096/");
        assertEquals("https://example.com:8096/", address.value(), "default scheme and host normalization");
    }

    private static void preservesServerPathPrefix() {
        MediaServerAddress address = MediaServerAddress.parse("http://media.local:8096/jellyfin/");
        assertEquals("http://media.local:8096/jellyfin", address.value(), "trailing slash normalization");
        assertEquals(
                "http://media.local:8096/jellyfin/Users/Public",
                address.resolvePath("/Users/Public"),
                "path prefix must survive endpoint resolution"
        );
    }

    private static void detectsServerFlavor() {
        assertEquals(ServerFlavor.JELLYFIN, ServerFlavor.fromServerName("Jellyfin Server"), "jellyfin detection");
        assertEquals(ServerFlavor.EMBY, ServerFlavor.fromServerName("Emby Server"), "emby detection");
        assertEquals(ServerFlavor.UNKNOWN, ServerFlavor.fromServerName("Media Browser"), "unknown detection");
    }

    private static void buildsAuthorizationHeader() {
        ClientIdentity client = new ClientIdentity("CinePilot TV", "Living Room TV", "device-1", "0.1.0");
        AuthSession session = new AuthSession("server-1", "user-1", "token-1", client);
        String value = session.authorizationValue();
        assertTrue(value.startsWith("MediaBrowser "), "auth scheme");
        assertTrue(value.contains("Client=\"CinePilot TV\""), "client field");
        assertTrue(value.contains("UserId=\"user-1\""), "user field");
        assertTrue(value.contains("Token=\"token-1\""), "token field");
        assertEquals("X-Emby-Token", session.legacyTokenHeaderName(), "legacy token header");
    }

    private static void buildsFlavorSpecificAuthorizationHeader() {
        ClientIdentity client = new ClientIdentity("CinePilot TV", "Living Room TV", "device-1", "0.1.0");
        AuthSession session = new AuthSession("server-1", "user-1", "token-1", client);
        assertTrue(
                session.authorizationValue(ServerFlavor.JELLYFIN).startsWith("MediaBrowser "),
                "jellyfin auth scheme"
        );
        assertTrue(
                session.authorizationValue(ServerFlavor.EMBY).startsWith("Emby "),
                "emby auth scheme"
        );
    }

    private static void exposesPlaybackEndpoints() {
        assertEquals("/Sessions/Playing", PlaybackEndpoint.STARTED.path(), "start endpoint");
        assertEquals("/Sessions/Playing/Progress", PlaybackEndpoint.PROGRESS.path(), "progress endpoint");
        assertEquals("/Sessions/Playing/Stopped", PlaybackEndpoint.STOPPED.path(), "stop endpoint");
    }

    private static void convertsPlaybackTicks() {
        assertEquals(10_000L, MediaTicks.fromMilliseconds(1), "one millisecond");
        assertEquals(3_723_000_000_000L, MediaTicks.fromMilliseconds(372_300_000L), "long playback position");
        assertEquals(372_300_000L, MediaTicks.toMilliseconds(3_723_000_000_000L), "ticks back to milliseconds");
    }

    private static void buildsPlaybackPayload() {
        PlaybackReport report = new PlaybackReport(
                "item-1",
                "source-1",
                "play-session-1",
                PlayMethod.DIRECT_PLAY,
                true,
                false,
                MediaTicks.fromMilliseconds(42_000),
                1,
                3,
                1.0f
        );
        Map<String, Object> payload = report.toProgressPayload(PlaybackEvent.TIME_UPDATE);
        assertEquals("item-1", payload.get("ItemId"), "item id");
        assertEquals("source-1", payload.get("MediaSourceId"), "media source id");
        assertEquals("play-session-1", payload.get("PlaySessionId"), "play session id");
        assertEquals("DirectPlay", payload.get("PlayMethod"), "play method");
        assertEquals(420_000_000L, payload.get("PositionTicks"), "position ticks");
        assertEquals("TimeUpdate", payload.get("EventName"), "event name");
    }

    private static void buildsDiscoveryAndLoginRequests() {
        ClientIdentity client = new ClientIdentity("CinePilot TV", "Living Room TV", "device-1", "0.1.0");
        MediaServerAddress address = MediaServerAddress.parse("http://media.local:8096/emby/");

        ProtocolRequest publicInfo = MediaBrowserRequests.publicSystemInfo();
        assertEquals(HttpMethod.GET, publicInfo.method(), "public system info method");
        assertEquals("http://media.local:8096/emby/System/Info/Public", publicInfo.url(address), "public info url");

        ProtocolRequest publicUsers = MediaBrowserRequests.publicUsers(client, ServerFlavor.EMBY);
        assertEquals(HttpMethod.GET, publicUsers.method(), "public users method");
        assertTrue(
                publicUsers.headers().get("X-Emby-Authorization").startsWith("Emby "),
                "public users uses emby authorization scheme"
        );

        ProtocolRequest login = MediaBrowserRequests.authenticateByName(
                client,
                ServerFlavor.JELLYFIN,
                "demo user",
                "secret"
        );
        assertEquals(HttpMethod.POST, login.method(), "login method");
        assertEquals("/Users/AuthenticateByName", login.path(), "login path");
        assertEquals(
                "{\"Username\":\"demo user\",\"Pw\":\"secret\"}",
                login.bodyJson(),
                "login body"
        );
        assertTrue(
                login.headers().get("X-Emby-Authorization").startsWith("MediaBrowser "),
                "login uses jellyfin authorization scheme"
        );
    }

    private static void buildsBrowseRequests() {
        ClientIdentity client = new ClientIdentity("CinePilot TV", "Living Room TV", "device-1", "0.1.0");
        AuthSession session = new AuthSession("server-1", "user 1", "token-1", client);
        MediaServerAddress address = MediaServerAddress.parse("https://example.com/jellyfin");

        ProtocolRequest views = MediaBrowserRequests.userViews(session, ServerFlavor.JELLYFIN);
        assertEquals("/Users/user%201/Views", views.path(), "user views path encodes user id");
        assertTrue(views.headers().containsKey("X-Emby-Token"), "authenticated requests include token header");

        ItemQuery query = ItemQuery.browse()
                .parentId("movies root")
                .includeItemTypes("Movie,Series")
                .recursive(true)
                .sortBy("SortName")
                .sortOrder("Ascending")
                .limit(50)
                .build();
        ProtocolRequest items = MediaBrowserRequests.items(session, ServerFlavor.JELLYFIN, query);
        String url = items.url(address);
        assertTrue(url.startsWith("https://example.com/jellyfin/Users/user%201/Items?"), "items url");
        assertTrue(url.contains("ParentId=movies%20root"), "query encodes parent id");
        assertTrue(url.contains("IncludeItemTypes=Movie%2CSeries"), "query encodes include types");
        assertTrue(url.contains("EnableUserData=true"), "query includes user data");

        ProtocolRequest detail = MediaBrowserRequests.item(session, ServerFlavor.JELLYFIN, "item/with/slash");
        assertEquals("/Users/user%201/Items/item%2Fwith%2Fslash", detail.path(), "item detail path encodes item id");
    }

    private static void scopesSavedSessions() {
        ClientIdentity client = new ClientIdentity("CinePilot TV", "Living Room TV", "device-1", "0.1.0");
        MediaServerAddress firstUrl = MediaServerAddress.parse("https://media.example.com/jellyfin");
        MediaServerAddress secondUrl = MediaServerAddress.parse("https://media.example.com/emby");
        ServerIdentity firstServer = new ServerIdentity(firstUrl, "server-a", ServerFlavor.JELLYFIN, "Jellyfin");
        ServerIdentity secondServer = new ServerIdentity(secondUrl, "server-b", ServerFlavor.EMBY, "Emby");

        AuthSession firstSession = new AuthSession("server-a", "user-1", "token-a", client);
        AuthSession secondSession = new AuthSession("server-b", "user-1", "token-b", client);
        SavedSession firstSaved = new SavedSession(SessionScope.from(firstServer, firstSession), "token-a");
        SavedSession secondSaved = new SavedSession(SessionScope.from(secondServer, secondSession), "token-b");

        InMemorySessionRepository repository = new InMemorySessionRepository();
        repository.save(firstSaved);
        repository.save(secondSaved);

        assertEquals(
                "token-a",
                repository.find(firstSaved.scope()).orElseThrow().accessToken(),
                "first token stays scoped"
        );
        assertEquals(
                "token-b",
                repository.find(secondSaved.scope()).orElseThrow().accessToken(),
                "second token stays scoped"
        );

        ClientIdentity otherDevice = new ClientIdentity("CinePilot TV", "Bedroom TV", "device-2", "0.1.0");
        SessionScope otherDeviceScope = new SessionScope(
                firstSaved.scope().serverId(),
                firstSaved.scope().serverUrl(),
                firstSaved.scope().userId(),
                otherDevice.clientName(),
                otherDevice.deviceId(),
                otherDevice.version()
        );
        assertTrue(repository.find(otherDeviceScope).isEmpty(), "tokens do not cross devices");

        AuthSession restored = firstSaved.restore(client);
        assertEquals("token-a", restored.accessToken(), "saved session restores token");
        repository.revoke(firstSaved.scope());
        assertTrue(repository.find(firstSaved.scope()).isEmpty(), "revoked token is removed");
        assertTrue(repository.find(secondSaved.scope()).isPresent(), "revoking one server keeps another server");
    }

    private static void mediaBrowserClientForgetsOnlyAuthenticatedScope() {
        ClientIdentity client = new ClientIdentity("CinePilot TV", "Living Room TV", "device-1", "0.1.0");
        MediaServerAddress firstUrl = MediaServerAddress.parse("https://media.example.com/jellyfin");
        MediaServerAddress secondUrl = MediaServerAddress.parse("https://media.example.com/emby");
        ServerIdentity firstServer = new ServerIdentity(firstUrl, "server-a", ServerFlavor.JELLYFIN, "Jellyfin");
        ServerIdentity secondServer = new ServerIdentity(secondUrl, "server-b", ServerFlavor.EMBY, "Emby");
        AuthSession firstSession = new AuthSession("server-a", "user-1", "token-a", client);
        AuthSession secondSession = new AuthSession("server-b", "user-1", "token-b", client);

        InMemorySessionRepository repository = new InMemorySessionRepository();
        repository.save(new SavedSession(SessionScope.from(firstServer, firstSession), firstSession.accessToken()));
        repository.save(new SavedSession(SessionScope.from(secondServer, secondSession), secondSession.accessToken()));

        MediaBrowserClient mediaClient = new MediaBrowserClient(new FakeTransport(), repository, client);
        mediaClient.forget(new AuthenticatedServer(firstServer, firstSession));

        assertTrue(mediaClient.restore(firstServer, "user-1").isEmpty(), "forget removes current server token");
        assertTrue(mediaClient.restore(secondServer, "user-1").isPresent(), "forget keeps other server token");
    }

    private static void buildsPlaybackInfoAndStreamRequests() {
        ClientIdentity client = new ClientIdentity("CinePilot TV", "Living Room TV", "device-1", "0.1.0");
        AuthSession session = new AuthSession("server-1", "user 1", "token-1", client);
        MediaServerAddress address = MediaServerAddress.parse("https://media.example.com/jellyfin");

        PlaybackInfoOptions playbackInfoOptions = new PlaybackInfoOptions.Builder()
                .maxStreamingBitrate(80_000_000L)
                .startTimeTicks(MediaTicks.fromMilliseconds(30_000))
                .audioStreamIndex(1)
                .subtitleStreamIndex(3)
                .maxAudioChannels(6)
                .enableDirectPlay(true)
                .enableDirectStream(true)
                .enableTranscoding(true)
                .build();
        ProtocolRequest playbackInfo = MediaBrowserRequests.playbackInfo(
                session,
                ServerFlavor.JELLYFIN,
                "movie 1",
                playbackInfoOptions
        );
        assertEquals(HttpMethod.GET, playbackInfo.method(), "playback info method");
        assertEquals("/Items/movie%201/PlaybackInfo", playbackInfo.path(), "playback info path");
        String playbackInfoUrl = playbackInfo.url(address);
        assertTrue(playbackInfoUrl.contains("UserId=user%201"), "playback info user id");
        assertTrue(playbackInfoUrl.contains("MaxStreamingBitrate=80000000"), "playback info bitrate");
        assertTrue(playbackInfoUrl.contains("StartTimeTicks=300000000"), "playback info start ticks");
        assertTrue(playbackInfoUrl.contains("EnableDirectPlay=true"), "playback info direct play");

        HlsStreamOptions streamOptions = HlsStreamOptions.builder("movie 1", "source 1")
                .playSessionId("play-session-1")
                .startTimeTicks(MediaTicks.fromMilliseconds(45_000))
                .audioStreamIndex(1)
                .subtitleStreamIndex(3)
                .maxAudioChannels(6)
                .maxWidth(3840)
                .maxHeight(2160)
                .videoBitRate(80_000_000)
                .videoCodec("h264,hevc")
                .audioCodec("aac")
                .subtitleMethod("Hls")
                .build();
        ProtocolRequest hls = MediaBrowserRequests.hlsStream(session, ServerFlavor.JELLYFIN, streamOptions);
        assertEquals(HttpMethod.GET, hls.method(), "hls method");
        assertEquals("/Videos/movie%201/master.m3u8", hls.path(), "hls path");
        String hlsUrl = hls.url(address);
        assertTrue(hlsUrl.contains("MediaSourceId=source%201"), "hls media source");
        assertTrue(hlsUrl.contains("DeviceId=device-1"), "hls device id");
        assertTrue(hlsUrl.contains("PlaySessionId=play-session-1"), "hls play session");
        assertTrue(hlsUrl.contains("Container=ts"), "hls container");
        assertTrue(hlsUrl.contains("StartTimeTicks=450000000"), "hls start ticks");
        assertTrue(hlsUrl.contains("VideoCodec=h264%2Chevc"), "hls video codec");
    }

    private static void buildsPlaybackCheckInRequests() {
        ClientIdentity client = new ClientIdentity("CinePilot TV", "Living Room TV", "device-1", "0.1.0");
        AuthSession session = new AuthSession("server-1", "user-1", "token-1", client);
        PlaybackReport report = new PlaybackReport(
                "item-1",
                "source-1",
                "play-session-1",
                PlayMethod.TRANSCODE,
                true,
                true,
                MediaTicks.fromMilliseconds(12_345),
                2,
                null,
                1.25f
        );

        ProtocolRequest started = MediaBrowserRequests.playbackStarted(session, ServerFlavor.EMBY, report);
        assertEquals(HttpMethod.POST, started.method(), "started method");
        assertEquals("/Sessions/Playing", started.path(), "started path");
        assertTrue(started.bodyJson().contains("\"QueueableMediaTypes\":[\"Video\"]"), "started body arrays as json");
        assertTrue(started.headers().get("X-Emby-Authorization").startsWith("Emby "), "started emby auth");

        ProtocolRequest progress = MediaBrowserRequests.playbackProgress(
                session,
                ServerFlavor.EMBY,
                report,
                PlaybackEvent.PAUSE
        );
        assertEquals("/Sessions/Playing/Progress", progress.path(), "progress path");
        assertTrue(progress.bodyJson().contains("\"EventName\":\"Pause\""), "progress event");
        assertTrue(progress.bodyJson().contains("\"PositionTicks\":123450000"), "progress ticks");

        ProtocolRequest stopped = MediaBrowserRequests.playbackStopped(session, ServerFlavor.EMBY, report);
        assertEquals("/Sessions/Playing/Stopped", stopped.path(), "stopped path");
        assertTrue(stopped.bodyJson().contains("\"PlaySessionId\":\"play-session-1\""), "stopped play session");
    }

    private static void selectsPlayableMediaSources() {
        ClientIdentity client = new ClientIdentity("CinePilot TV", "Living Room TV", "device-1", "0.1.0");
        AuthSession session = new AuthSession("server-1", "user-1", "token-1", client);
        MediaServerAddress address = MediaServerAddress.parse("https://media.example.com/jellyfin");

        MediaSourceInfo transcodingOnly = MediaSourceInfo.builder("source-transcode")
                .supportsTranscoding(true)
                .transcodingUrl("/videos/item-1/master.m3u8?MediaSourceId=source-transcode")
                .build();
        MediaSourceInfo directStream = MediaSourceInfo.builder("source-direct")
                .supportsDirectStream(true)
                .supportsTranscoding(true)
                .directStreamUrl("/videos/item-1/stream.mkv?MediaSourceId=source-direct")
                .build();
        MediaSourceInfo directPlay = MediaSourceInfo.builder("source-play")
                .supportsDirectPlay(true)
                .supportsDirectStream(true)
                .directStreamUrl("https://cdn.example.com/movie.mkv")
                .mediaStreams(List.of(
                        new MediaStreamInfo(0, MediaStreamType.VIDEO, "hevc", "", "4K HEVC", true, false, false, null),
                        new MediaStreamInfo(1, MediaStreamType.AUDIO, "eac3", "eng", "English", true, false, false, null),
                        new MediaStreamInfo(2, MediaStreamType.SUBTITLE, "srt", "eng", "English SDH", true, false, true, null)
                ))
                .build();
        PlaybackInfo info = new PlaybackInfo(
                "item-1",
                "play-session-1",
                List.of(transcodingOnly, directStream, directPlay)
        );

        PlayableMedia selected = PlaybackSourceSelector.select(
                address,
                session,
                ServerFlavor.JELLYFIN,
                info,
                PlaybackSelectionPreferences.defaults()
        ).orElseThrow();
        assertEquals(PlayMethod.DIRECT_PLAY, selected.playMethod(), "selector prefers direct play");
        assertEquals("source-play", selected.mediaSourceId(), "selector source id");
        assertEquals("https://cdn.example.com/movie.mkv", selected.url(), "absolute direct play url");
        assertEquals(1, selected.audioStreamIndex(), "selector keeps default audio stream");
        assertEquals(2, selected.subtitleStreamIndex(), "selector keeps default subtitle stream");

        PlaybackInfo directStreamOnly = new PlaybackInfo("item-1", "play-session-1", List.of(transcodingOnly, directStream));
        PlayableMedia streamSelection = PlaybackSourceSelector.select(
                address,
                session,
                ServerFlavor.JELLYFIN,
                directStreamOnly,
                PlaybackSelectionPreferences.defaults()
        ).orElseThrow();
        assertEquals(PlayMethod.DIRECT_STREAM, streamSelection.playMethod(), "selector prefers direct stream over transcode");
        assertEquals(
                "https://media.example.com/jellyfin/videos/item-1/stream.mkv?MediaSourceId=source-direct",
                streamSelection.url(),
                "relative direct stream url resolves against server"
        );

        MediaSourceInfo fallbackTranscode = MediaSourceInfo.builder("source-hls")
                .supportsTranscoding(true)
                .mediaStreams(List.of(
                        new MediaStreamInfo(4, MediaStreamType.AUDIO, "aac", "eng", "English", false, false, false, null)
                ))
                .build();
        PlaybackInfo fallbackInfo = new PlaybackInfo("item-2", "play-session-2", List.of(fallbackTranscode));
        PlaybackSelectionPreferences preferences = new PlaybackSelectionPreferences(
                MediaTicks.fromMilliseconds(60_000),
                2,
                5,
                6,
                3840,
                2160,
                80_000_000
        );
        PlayableMedia fallback = PlaybackSourceSelector.select(
                address,
                session,
                ServerFlavor.JELLYFIN,
                fallbackInfo,
                preferences
        ).orElseThrow();
        assertEquals(PlayMethod.TRANSCODE, fallback.playMethod(), "fallback uses transcode");
        assertTrue(!fallback.hasReadyUrl(), "fallback waits for request url");
        assertEquals("/Videos/item-2/master.m3u8", fallback.request().path(), "fallback hls path");
        assertTrue(fallback.request().url(address).contains("MediaSourceId=source-hls"), "fallback media source query");
        assertTrue(fallback.request().url(address).contains("StartTimeTicks=600000000"), "fallback start ticks query");
        assertEquals(2, fallback.audioStreamIndex(), "explicit audio preference wins");
        assertEquals(5, fallback.subtitleStreamIndex(), "explicit subtitle preference wins");

        Optional<PlayableMedia> noPlayable = PlaybackSourceSelector.select(
                address,
                session,
                ServerFlavor.JELLYFIN,
                new PlaybackInfo("item-3", "play-session-3", List.of(MediaSourceInfo.builder("source-none").build())),
                PlaybackSelectionPreferences.defaults()
        );
        assertTrue(noPlayable.isEmpty(), "selector returns empty for unsupported sources");
    }

    private static void mapsServerAndPlaybackResponses() {
        MediaServerAddress address = MediaServerAddress.parse("https://media.example.com/jellyfin");
        ServerIdentity server = MediaBrowserResponseMapper.serverIdentity(
                address,
                "{\"Id\":\"server-1\",\"ServerName\":\"Jellyfin Living Room\",\"Version\":\"10.10.7\"}"
        );
        assertEquals("server-1", server.serverId(), "server id maps");
        assertEquals(ServerFlavor.JELLYFIN, server.flavor(), "server flavor maps");

        ClientIdentity client = new ClientIdentity("CinePilot TV", "Living Room TV", "device-1", "0.1.0");
        AuthSession session = MediaBrowserResponseMapper.authSession(
                client,
                "{\"AccessToken\":\"token-1\",\"ServerId\":\"server-1\",\"User\":{\"Id\":\"user-1\",\"Name\":\"Demo\"}}"
        );
        assertEquals("server-1", session.serverId(), "auth server id maps");
        assertEquals("user-1", session.userId(), "auth user id maps");
        assertEquals("token-1", session.accessToken(), "auth token maps");

        PlaybackInfo playbackInfo = MediaBrowserResponseMapper.playbackInfo(
                "item-1",
                """
                {
                  "PlaySessionId": "play-session-1",
                  "MediaSources": [
                    {
                      "Id": "source-1",
                      "Container": "mkv",
                      "DirectStreamUrl": "/Videos/item-1/stream.mkv?MediaSourceId=source-1",
                      "TranscodingUrl": "/Videos/item-1/master.m3u8?MediaSourceId=source-1",
                      "SupportsDirectPlay": false,
                      "SupportsDirectStream": true,
                      "SupportsTranscoding": true,
                      "MediaStreams": [
                        {"Index": 0, "Type": "Video", "Codec": "hevc", "DisplayTitle": "4K HEVC", "IsDefault": true},
                        {"Index": 1, "Type": "Audio", "Codec": "eac3", "Language": "eng", "DisplayTitle": "English", "IsDefault": true},
                        {"Index": 2, "Type": "Subtitle", "Codec": "srt", "Language": "eng", "DisplayTitle": "English CC", "IsExternal": true, "DeliveryUrl": "/Videos/item-1/Subtitles/2/Stream.srt"}
                      ]
                    }
                  ]
                }
                """
        );
        assertEquals("play-session-1", playbackInfo.playSessionId(), "play session maps");
        assertEquals(1, playbackInfo.mediaSources().size(), "media source count maps");
        MediaSourceInfo source = playbackInfo.mediaSources().get(0);
        assertEquals("source-1", source.id(), "media source id maps");
        assertTrue(source.supportsDirectStream(), "direct stream flag maps");
        assertEquals(3, source.mediaStreams().size(), "media streams map");
        assertEquals(MediaStreamType.SUBTITLE, source.mediaStreams().get(2).type(), "subtitle type maps");
        assertEquals("/Videos/item-1/Subtitles/2/Stream.srt", source.mediaStreams().get(2).deliveryUrl(), "subtitle url maps");
    }

    private static void clientRunsDiscoveryLoginAndPlaybackFlow() {
        FakeTransport transport = new FakeTransport();
        transport.enqueue(200, "{\"Id\":\"server-1\",\"ServerName\":\"Jellyfin\",\"Version\":\"10.10.7\"}");
        transport.enqueue(
                200,
                "{\"AccessToken\":\"token-1\",\"ServerId\":\"server-1\",\"User\":{\"Id\":\"user-1\",\"Name\":\"Demo\"}}"
        );
        transport.enqueue(
                200,
                """
                {
                  "PlaySessionId": "play-session-1",
                  "MediaSources": [
                    {
                      "Id": "source-1",
                      "DirectStreamUrl": "/Videos/item-1/stream.mkv?MediaSourceId=source-1",
                      "SupportsDirectStream": true,
                      "SupportsTranscoding": true,
                      "MediaStreams": [
                        {"Index": 0, "Type": "Video", "Codec": "h264"},
                        {"Index": 1, "Type": "Audio", "Codec": "aac", "Language": "eng"}
                      ]
                    }
                  ]
                }
                """
        );
        transport.enqueue(204, "");

        ClientIdentity client = new ClientIdentity("CinePilot TV", "Living Room TV", "device-1", "0.1.0");
        InMemorySessionRepository repository = new InMemorySessionRepository();
        MediaBrowserClient mediaClient = new MediaBrowserClient(transport, repository, client);
        MediaServerAddress address = MediaServerAddress.parse("https://media.example.com/jellyfin");

        ServerIdentity server = mediaClient.discover(address);
        assertEquals("server-1", server.serverId(), "client discovers server id");
        assertEquals(ServerFlavor.JELLYFIN, server.flavor(), "client discovers flavor");

        AuthenticatedServer authenticated = mediaClient.authenticate(server, "demo", "secret");
        assertEquals("user-1", authenticated.session().userId(), "client authenticates user");
        assertTrue(
                mediaClient.restore(server, "user-1").isPresent(),
                "client saves session after authentication"
        );

        PlaybackInfo playbackInfo = mediaClient.playbackInfo(
                authenticated,
                "item-1",
                new PlaybackInfoOptions.Builder().maxStreamingBitrate(40_000_000L).build()
        );
        PlayableMedia playable = mediaClient.playableMedia(
                authenticated,
                playbackInfo,
                PlaybackSelectionPreferences.defaults()
        ).orElseThrow();
        assertEquals(PlayMethod.DIRECT_STREAM, playable.playMethod(), "client selects direct stream");
        assertEquals(
                "https://media.example.com/jellyfin/Videos/item-1/stream.mkv?MediaSourceId=source-1",
                playable.url(),
                "client resolves playable url"
        );

        mediaClient.logout(authenticated);
        assertTrue(mediaClient.restore(server, "user-1").isEmpty(), "logout revokes saved session");

        assertEquals("/System/Info/Public", transport.requests.get(0).path(), "first request discovers server");
        assertEquals("/Users/AuthenticateByName", transport.requests.get(1).path(), "second request authenticates");
        assertEquals("/Items/item-1/PlaybackInfo", transport.requests.get(2).path(), "third request gets playback info");
        assertTrue(
                transport.requests.get(2).url(address).contains("MaxStreamingBitrate=40000000"),
                "client forwards playback options"
        );
        assertEquals("/Sessions/Logout", transport.requests.get(3).path(), "fourth request logs out");
    }

    private static void mapsAndFetchesMediaItems() {
        MediaItemPage page = MediaBrowserResponseMapper.itemPage(
                """
                {
                  "Items": [
                    {
                      "Id": "movie-1",
                      "Name": "Arrival",
                      "Type": "Movie",
                      "IsFolder": false,
                      "IsPlayable": true,
                      "RunTimeTicks": 69900000000,
                      "ProductionYear": 2016,
                      "ImageTags": {"Primary": "primary-tag"},
                      "UserData": {"Played": false, "PlaybackPositionTicks": 120000000, "PlayCount": 0, "IsFavorite": true}
                    },
                    {
                      "Id": "series-1",
                      "Name": "Example Show",
                      "Type": "Series",
                      "IsFolder": true,
                      "IsPlayable": false
                    }
                  ],
                  "TotalRecordCount": 2,
                  "StartIndex": 0
                }
                """
        );
        assertEquals(2, page.items().size(), "item page count");
        MediaItemSummary movie = page.items().get(0);
        assertEquals(MediaItemType.MOVIE, movie.type(), "movie type maps");
        assertTrue(movie.playable(), "movie playable maps");
        assertEquals(120000000L, movie.userData().playbackPositionTicks(), "resume ticks map");
        assertTrue(movie.userData().favorite(), "favorite maps");
        assertEquals("primary-tag", movie.imageTags().get("Primary"), "image tags map");
        assertTrue(movie.hasResumePosition(), "resume helper");
        assertEquals(MediaItemType.SERIES, page.items().get(1).type(), "series type maps");

        FakeTransport transport = new FakeTransport();
        transport.enqueue(200, "{\"Items\":[{\"Id\":\"library-1\",\"Name\":\"Movies\",\"Type\":\"CollectionFolder\",\"IsFolder\":true}],\"TotalRecordCount\":1,\"StartIndex\":0}");
        transport.enqueue(200, "{\"Items\":[{\"Id\":\"movie-1\",\"Name\":\"Arrival\",\"Type\":\"Movie\",\"IsPlayable\":true}],\"TotalRecordCount\":1,\"StartIndex\":0}");
        transport.enqueue(200, "{\"Id\":\"movie-1\",\"Name\":\"Arrival\",\"Type\":\"Movie\",\"IsPlayable\":true,\"UserData\":{\"PlaybackPositionTicks\":120000000}}");

        ClientIdentity client = new ClientIdentity("CinePilot TV", "Living Room TV", "device-1", "0.1.0");
        MediaBrowserClient mediaClient = new MediaBrowserClient(transport, new InMemorySessionRepository(), client);
        AuthenticatedServer authenticated = new AuthenticatedServer(
                new ServerIdentity(MediaServerAddress.parse("https://media.example.com/jellyfin"), "server-1", ServerFlavor.JELLYFIN, "Jellyfin"),
                new AuthSession("server-1", "user-1", "token-1", client)
        );

        MediaItemPage views = mediaClient.userViews(authenticated);
        assertEquals("library-1", views.items().get(0).id(), "client fetches views");

        MediaItemPage items = mediaClient.items(
                authenticated,
                ItemQuery.browse().parentId("library-1").limit(20).build()
        );
        assertEquals("movie-1", items.items().get(0).id(), "client fetches items");

        MediaItemSummary detail = mediaClient.item(authenticated, "movie-1");
        assertEquals("Arrival", detail.name(), "client fetches item detail");
        assertTrue(detail.hasResumePosition(), "client detail maps resume position");

        assertEquals("/Users/user-1/Views", transport.requests.get(0).path(), "views request path");
        assertEquals("/Users/user-1/Items", transport.requests.get(1).path(), "items request path");
        assertTrue(transport.requests.get(1).url(authenticated.server().address()).contains("ParentId=library-1"), "items parent query");
        assertEquals("/Users/user-1/Items/movie-1", transport.requests.get(2).path(), "detail request path");
    }

    private static void persistsSavedSessionsToFile() {
        try {
            Path tempDir = Files.createTempDirectory("cinepilot-sessions");
            Path file = tempDir.resolve("sessions.properties");
            ClientIdentity client = new ClientIdentity("CinePilot TV", "Living Room TV", "device-1", "0.1.0");
            ServerIdentity firstServer = new ServerIdentity(
                    MediaServerAddress.parse("https://media.example.com/jellyfin"),
                    "server-1",
                    ServerFlavor.JELLYFIN,
                    "Jellyfin"
            );
            ServerIdentity secondServer = new ServerIdentity(
                    MediaServerAddress.parse("https://media.example.com/emby"),
                    "server-2",
                    ServerFlavor.EMBY,
                    "Emby"
            );

            SavedSession firstSaved = new SavedSession(
                    SessionScope.from(firstServer, new AuthSession("server-1", "user-1", "token-1", client)),
                    "token-1"
            );
            SavedSession secondSaved = new SavedSession(
                    SessionScope.from(secondServer, new AuthSession("server-2", "user-1", "token-2", client)),
                    "token-2"
            );

            FileSessionRepository repository = new FileSessionRepository(file);
            repository.save(firstSaved);
            repository.save(secondSaved);

            FileSessionRepository reloaded = new FileSessionRepository(file);
            assertEquals("token-1", reloaded.find(firstSaved.scope()).orElseThrow().accessToken(), "first token reloads");
            assertEquals("token-2", reloaded.find(secondSaved.scope()).orElseThrow().accessToken(), "second token reloads");

            ClientIdentity otherDevice = new ClientIdentity("CinePilot TV", "Bedroom TV", "device-2", "0.1.0");
            SessionScope otherScope = new SessionScope(
                    firstSaved.scope().serverId(),
                    firstSaved.scope().serverUrl(),
                    firstSaved.scope().userId(),
                    otherDevice.clientName(),
                    otherDevice.deviceId(),
                    otherDevice.version()
            );
            assertTrue(reloaded.find(otherScope).isEmpty(), "file repository respects device scope");

            reloaded.revoke(firstSaved.scope());
            FileSessionRepository afterRevoke = new FileSessionRepository(file);
            assertTrue(afterRevoke.find(firstSaved.scope()).isEmpty(), "file repository revokes one scope");
            assertTrue(afterRevoke.find(secondSaved.scope()).isPresent(), "file repository keeps other server scope");
        } catch (IOException exception) {
            throw new AssertionError("Temp session repository setup failed", exception);
        }
    }

    private static void schedulesPlaybackCheckIns() {
        PlaybackCheckInScheduler scheduler = new PlaybackCheckInScheduler(Duration.ofSeconds(10));
        PlaybackReport startedReport = playbackReportAt(0, false, null);
        PlaybackCheckIn started = scheduler.start(1_000L, startedReport);
        assertEquals(PlaybackEndpoint.STARTED, started.endpoint(), "start check-in endpoint");
        assertEquals(startedReport, started.report(), "start report");
        assertTrue(scheduler.started(), "scheduler started");

        assertTrue(
                scheduler.progressIfDue(10_999L, playbackReportAt(9_999, false, null)).isEmpty(),
                "progress is not due before interval"
        );
        PlaybackCheckIn timedProgress = scheduler.progressIfDue(
                11_000L,
                playbackReportAt(10_000, false, null)
        ).orElseThrow();
        assertEquals(PlaybackEndpoint.PROGRESS, timedProgress.endpoint(), "timed progress endpoint");
        assertEquals(PlaybackEvent.TIME_UPDATE, timedProgress.event(), "timed progress event");

        PlaybackCheckIn pause = scheduler.immediate(
                12_000L,
                PlaybackEvent.PAUSE,
                playbackReportAt(11_000, true, null)
        );
        assertEquals(PlaybackEndpoint.PROGRESS, pause.endpoint(), "pause progress endpoint");
        assertEquals(PlaybackEvent.PAUSE, pause.event(), "pause event");
        assertTrue(pause.report().paused(), "pause report state");

        assertTrue(
                scheduler.progressIfDue(21_999L, playbackReportAt(20_999, true, null)).isEmpty(),
                "immediate event resets progress interval"
        );
        PlaybackCheckIn seek = scheduler.immediate(
                22_000L,
                PlaybackEvent.SEEK,
                playbackReportAt(60_000, false, null)
        );
        assertEquals(PlaybackEvent.SEEK, seek.event(), "seek event");

        PlaybackCheckIn stopped = scheduler.stop(playbackReportAt(61_000, false, null));
        assertEquals(PlaybackEndpoint.STOPPED, stopped.endpoint(), "stop endpoint");
        assertTrue(scheduler.stopped(), "scheduler stopped");
    }

    private static void clientSendsPlaybackCheckIns() {
        FakeTransport transport = new FakeTransport();
        transport.enqueue(204, "");
        transport.enqueue(204, "");
        transport.enqueue(204, "");

        ClientIdentity client = new ClientIdentity("CinePilot TV", "Living Room TV", "device-1", "0.1.0");
        AuthenticatedServer authenticated = new AuthenticatedServer(
                new ServerIdentity(MediaServerAddress.parse("https://media.example.com/jellyfin"), "server-1", ServerFlavor.JELLYFIN, "Jellyfin"),
                new AuthSession("server-1", "user-1", "token-1", client)
        );
        MediaBrowserClient mediaClient = new MediaBrowserClient(transport, new InMemorySessionRepository(), client);

        PlaybackCheckInScheduler scheduler = new PlaybackCheckInScheduler(Duration.ofSeconds(10));
        mediaClient.sendPlaybackCheckIn(authenticated, scheduler.start(0L, playbackReportAt(0, false, null)));
        mediaClient.sendPlaybackCheckIn(authenticated, scheduler.immediate(3_000L, PlaybackEvent.PAUSE, playbackReportAt(3_000, true, null)));
        mediaClient.sendPlaybackCheckIn(authenticated, scheduler.stop(playbackReportAt(3_500, true, null)));

        assertEquals("/Sessions/Playing", transport.requests.get(0).path(), "client sends started path");
        assertTrue(
                transport.requests.get(0).bodyJson().contains("\"PlayMethod\":\"DirectPlay\""),
                "client sends started body"
        );

        assertEquals("/Sessions/Playing/Progress", transport.requests.get(1).path(), "client sends progress path");
        assertTrue(
                transport.requests.get(1).bodyJson().contains("\"EventName\":\"Pause\""),
                "client sends pause event"
        );
        assertTrue(
                transport.requests.get(1).bodyJson().contains("\"IsPaused\":true"),
                "client sends paused state"
        );

        assertEquals("/Sessions/Playing/Stopped", transport.requests.get(2).path(), "client sends stopped path");
        assertTrue(
                transport.requests.get(2).headers().get("X-Emby-Token").equals("token-1"),
                "client sends token header"
        );
    }

    private static void playbackSessionControllerSendsPlayerEvents() {
        FakeTransport transport = new FakeTransport();
        for (int index = 0; index < 8; index++) {
            transport.enqueue(204, "");
        }

        ClientIdentity client = new ClientIdentity("CinePilot TV", "Living Room TV", "device-1", "0.1.0");
        AuthenticatedServer authenticated = new AuthenticatedServer(
                new ServerIdentity(MediaServerAddress.parse("https://media.example.com/jellyfin"), "server-1", ServerFlavor.JELLYFIN, "Jellyfin"),
                new AuthSession("server-1", "user-1", "token-1", client)
        );
        MediaBrowserClient mediaClient = new MediaBrowserClient(transport, new InMemorySessionRepository(), client);
        PlayableMedia playable = new PlayableMedia(
                "item-1",
                "source-1",
                "play-session-1",
                PlayMethod.DIRECT_STREAM,
                "https://media.example.com/Videos/item-1/stream.mkv",
                null,
                1,
                null
        );
        PlaybackSessionController controller = new PlaybackSessionController(
                mediaClient,
                authenticated,
                playable,
                Duration.ofSeconds(10),
                true
        );

        controller.start(0L, 0L);
        assertTrue(!controller.progressIfDue(9_999L, 9_999L), "controller suppresses early progress");
        assertTrue(controller.progressIfDue(10_000L, 10_000L), "controller sends timed progress");
        controller.pause(11_000L, 11_000L);
        controller.seek(12_000L, 60_000L);
        controller.audioTrackChanged(13_000L, 61_000L, 2);
        controller.subtitleTrackChanged(14_000L, 62_000L, 5);
        controller.playbackRateChanged(15_000L, 63_000L, 1.25f);
        controller.stop(64_000L);

        assertEquals(8, transport.requests.size(), "controller request count");
        assertEquals("/Sessions/Playing", transport.requests.get(0).path(), "controller start path");
        assertEquals("/Sessions/Playing/Progress", transport.requests.get(1).path(), "controller timed progress path");
        assertTrue(transport.requests.get(1).bodyJson().contains("\"EventName\":\"TimeUpdate\""), "controller timed event");
        assertTrue(transport.requests.get(2).bodyJson().contains("\"EventName\":\"Pause\""), "controller pause event");
        assertTrue(transport.requests.get(2).bodyJson().contains("\"IsPaused\":true"), "controller pause state");
        assertTrue(transport.requests.get(3).bodyJson().contains("\"PositionTicks\":600000000"), "controller seek position");
        assertTrue(transport.requests.get(4).bodyJson().contains("\"AudioStreamIndex\":2"), "controller audio track");
        assertTrue(transport.requests.get(5).bodyJson().contains("\"SubtitleStreamIndex\":5"), "controller subtitle track");
        assertTrue(transport.requests.get(6).bodyJson().contains("\"PlaybackRate\":1.25"), "controller playback rate event");
        assertTrue(transport.requests.get(7).bodyJson().contains("\"PlaybackRate\":1.25"), "controller playback rate persists to stop");
        assertEquals("/Sessions/Playing/Stopped", transport.requests.get(7).path(), "controller stop path");
    }

    private static void authorizesPlaybackUrls() {
        ClientIdentity client = new ClientIdentity("CinePilot TV", "Living Room TV", "device-1", "0.1.0");
        AuthSession session = new AuthSession("server-1", "user-1", "token value", client);
        assertEquals(
                "https://media.example.com/Videos/item-1/stream.mkv?api_key=token%20value",
                PlaybackUrlAuthorizer.withAccessToken("https://media.example.com/Videos/item-1/stream.mkv", session),
                "adds api key to URL without query"
        );
        assertEquals(
                "https://media.example.com/Videos/item-1/master.m3u8?MediaSourceId=source-1&api_key=token%20value#frag",
                PlaybackUrlAuthorizer.withAccessToken("https://media.example.com/Videos/item-1/master.m3u8?MediaSourceId=source-1#frag", session),
                "adds api key before fragment"
        );
        assertEquals(
                "https://media.example.com/Videos/item-1/master.m3u8?api_key=existing",
                PlaybackUrlAuthorizer.withAccessToken("https://media.example.com/Videos/item-1/master.m3u8?api_key=existing", session),
                "keeps existing api key"
        );
    }

    private static PlaybackReport playbackReportAt(long positionMillis, boolean paused, Float rate) {
        return new PlaybackReport(
                "item-1",
                "source-1",
                "play-session-1",
                PlayMethod.DIRECT_PLAY,
                true,
                paused,
                MediaTicks.fromMilliseconds(positionMillis),
                1,
                null,
                rate
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
