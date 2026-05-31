package tv.cinepilot.core.protocol;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

public final class JavaNetHttpTransport implements HttpTransport {
    private final HttpClient client;
    private final Duration timeout;

    public JavaNetHttpTransport() {
        this(HttpClient.newHttpClient(), Duration.ofSeconds(20));
    }

    public JavaNetHttpTransport(HttpClient client, Duration timeout) {
        this.client = client;
        this.timeout = timeout;
    }

    @Override
    public ProtocolResponse send(MediaServerAddress address, ProtocolRequest request)
            throws IOException, InterruptedException {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(request.url(address)))
                .timeout(timeout);
        for (Map.Entry<String, String> entry : request.headers().entrySet()) {
            builder.header(entry.getKey(), entry.getValue());
        }

        if (request.method() == HttpMethod.POST) {
            String body = request.bodyJson() == null ? "" : request.bodyJson();
            builder.POST(HttpRequest.BodyPublishers.ofString(body));
        } else {
            builder.GET();
        }

        HttpResponse<String> response = client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
        Map<String, String> headers = new LinkedHashMap<>();
        response.headers().map().forEach((name, values) -> headers.put(name, String.join(",", values)));
        return new ProtocolResponse(response.statusCode(), headers, response.body());
    }
}

