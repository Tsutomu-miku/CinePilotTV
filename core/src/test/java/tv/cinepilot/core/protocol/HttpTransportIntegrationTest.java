package tv.cinepilot.core.protocol;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import tv.cinepilot.core.tv.HomeRowsLoader;

public final class HttpTransportIntegrationTest {
    public static void main(String[] args) throws Exception {
        verifiesRealHttpTransportClientFlow();
        System.out.println("HttpTransportIntegrationTest passed");
    }

    private static void verifiesRealHttpTransportClientFlow() throws Exception {
        List<String> requests = new ArrayList<>();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> handle(exchange, requests));
        server.start();
        try {
            MediaServerAddress address = MediaServerAddress.parse("http://127.0.0.1:" + server.getAddress().getPort());
            ClientIdentity client = new ClientIdentity("CinePilot TV", "Test TV", "device-1", "0.1.0");
            MediaBrowserClient mediaClient = new MediaBrowserClient(
                    new UrlConnectionHttpTransport(Duration.ofSeconds(5), Duration.ofSeconds(5)),
                    new InMemorySessionRepository(),
                    client
            );

            ServerIdentity identity = mediaClient.discover(address);
            assertEquals("server-1", identity.serverId(), "discovers server id over HTTP");
            assertEquals(1, mediaClient.publicUsers(identity).size(), "loads public users over HTTP");
            AuthenticatedServer authenticated = mediaClient.authenticate(identity, "demo", "secret");
            assertEquals("user-1", authenticated.session().userId(), "authenticates user over HTTP");

            HomeRowsLoader loader = new HomeRowsLoader(mediaClient, 12);
            assertEquals(6, loader.load(authenticated).size(), "loads TV home rows over HTTP");

            MediaItemSummary detail = mediaClient.item(authenticated, "movie-1");
            assertEquals("Arrival", detail.name(), "loads detail over HTTP");

            PlaybackInfo playbackInfo = mediaClient.playbackInfo(authenticated, "movie-1", PlaybackInfoOptions.defaults());
            PlayableMedia playable = mediaClient.playableMedia(
                    authenticated,
                    playbackInfo,
                    PlaybackSelectionPreferences.defaults()
            ).orElseThrow();
            assertEquals(PlayMethod.DIRECT_STREAM, playable.playMethod(), "selects direct stream from HTTP playback info");

            PlaybackReport report = new PlaybackReport(
                    "movie-1",
                    "source-1",
                    "play-session-1",
                    PlayMethod.DIRECT_STREAM,
                    true,
                    false,
                    MediaTicks.fromMilliseconds(10_000),
                    1,
                    null,
                    null
            );
            mediaClient.sendPlaybackCheckIn(
                    authenticated,
                    new PlaybackCheckIn(PlaybackEndpoint.PROGRESS, PlaybackEvent.TIME_UPDATE, report)
            );

            assertTrue(requests.stream().anyMatch(value -> value.startsWith("GET /System/Info/Public")), "server info request observed");
            assertTrue(requests.stream().anyMatch(value -> value.startsWith("GET /Users/Public")), "public users request observed");
            assertTrue(requests.stream().anyMatch(value -> value.startsWith("POST /Users/AuthenticateByName")), "auth request observed");
            assertTrue(
                    requests.stream().anyMatch(value -> value.startsWith("GET /Users/user-1/Items/Latest?") && value.contains("ParentId=movies")),
                    "latest request observed"
            );
            assertTrue(requests.stream().anyMatch(value -> value.startsWith("GET /Shows/NextUp?")), "next up request observed");
            assertTrue(requests.stream().anyMatch(value -> value.startsWith("POST /Sessions/Playing/Progress")), "progress check-in observed");
            assertTrue(requests.stream().anyMatch(value -> value.contains("X-Emby-Token=token-1")), "token header observed");
        } finally {
            server.stop(0);
        }
    }

    private static void handle(HttpExchange exchange, List<String> requests) throws IOException {
        String requestBody = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        String requestLine = exchange.getRequestMethod() + " " + exchange.getRequestURI();
        String token = exchange.getRequestHeaders().getFirst("X-Emby-Token");
        requests.add(requestLine + " X-Emby-Token=" + token + " BODY=" + requestBody);

        String path = exchange.getRequestURI().getPath();
        String query = exchange.getRequestURI().getRawQuery() == null ? "" : exchange.getRequestURI().getRawQuery();
        String response;
        int status = 200;
        if (path.equals("/System/Info/Public")) {
            response = "{\"Id\":\"server-1\",\"ServerName\":\"Jellyfin\"}";
        } else if (path.equals("/Users/Public")) {
            response = "[{\"Id\":\"user-1\",\"Name\":\"Demo\",\"HasPassword\":true}]";
        } else if (path.equals("/Users/AuthenticateByName")) {
            response = "{\"AccessToken\":\"token-1\",\"ServerId\":\"server-1\",\"User\":{\"Id\":\"user-1\"}}";
        } else if (path.equals("/Users/user-1/Views")) {
            response = "{\"Items\":[{\"Id\":\"movies\",\"Name\":\"Movies\",\"Type\":\"CollectionFolder\",\"IsFolder\":true}],\"TotalRecordCount\":1,\"StartIndex\":0}";
        } else if (path.equals("/Users/user-1/Items/Resume")) {
            response = "{\"Items\":[{\"Id\":\"resume-1\",\"Name\":\"Resume\",\"Type\":\"Movie\",\"IsPlayable\":true}],\"TotalRecordCount\":1,\"StartIndex\":0}";
        } else if (path.equals("/Shows/NextUp")) {
            response = "{\"Items\":[{\"Id\":\"episode-2\",\"Name\":\"Next Episode\",\"Type\":\"Episode\",\"IsPlayable\":true}],\"TotalRecordCount\":1,\"StartIndex\":0}";
        } else if (path.equals("/Users/user-1/Items/Latest")) {
            response = "{\"Items\":[{\"Id\":\"movie-1\",\"Name\":\"Arrival\",\"Type\":\"Movie\",\"IsPlayable\":true}],\"TotalRecordCount\":1,\"StartIndex\":0}";
        } else if (path.equals("/Users/user-1/Items") && query.contains("Filters=IsFavorite")) {
            response = "{\"Items\":[{\"Id\":\"fav-1\",\"Name\":\"Fav\",\"Type\":\"Movie\",\"IsPlayable\":true}],\"TotalRecordCount\":1,\"StartIndex\":0}";
        } else if (path.equals("/Users/user-1/Items") && query.contains("IncludeItemTypes=BoxSet")) {
            response = "{\"Items\":[{\"Id\":\"coll-1\",\"Name\":\"Collection\",\"Type\":\"BoxSet\",\"IsFolder\":true,\"IsPlayable\":false}],\"TotalRecordCount\":1,\"StartIndex\":0}";
        } else if (path.equals("/Users/user-1/Items/movie-1")) {
            response = "{\"Id\":\"movie-1\",\"Name\":\"Arrival\",\"Type\":\"Movie\",\"IsPlayable\":true}";
        } else if (path.equals("/Items/movie-1/PlaybackInfo")) {
            response = "{\"PlaySessionId\":\"play-session-1\",\"MediaSources\":[{\"Id\":\"source-1\",\"DirectStreamUrl\":\"/Videos/movie-1/stream.mkv?MediaSourceId=source-1\",\"SupportsDirectStream\":true}]}";
        } else if (path.equals("/Sessions/Playing/Progress")) {
            response = "";
            status = 204;
        } else {
            response = "{\"error\":\"not found\"}";
            status = 404;
        }

        byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        if (status == 204) {
            exchange.sendResponseHeaders(status, -1);
        } else {
            exchange.sendResponseHeaders(status, bytes.length);
            exchange.getResponseBody().write(bytes);
        }
        exchange.close();
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
