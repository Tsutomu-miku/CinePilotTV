package tv.cinepilot.core.protocol;

import java.util.Map;

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
        buildsPlaybackInfoAndStreamRequests();
        buildsPlaybackCheckInRequests();
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
