package tv.cinepilot.core.protocol;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class UrlConnectionHttpTransport implements HttpTransport {
    private final Duration connectTimeout;
    private final Duration readTimeout;

    public UrlConnectionHttpTransport() {
        this(Duration.ofSeconds(15), Duration.ofSeconds(30));
    }

    public UrlConnectionHttpTransport(Duration connectTimeout, Duration readTimeout) {
        this.connectTimeout = requirePositive(connectTimeout, "connectTimeout");
        this.readTimeout = requirePositive(readTimeout, "readTimeout");
    }

    @Override
    public ProtocolResponse send(MediaServerAddress address, ProtocolRequest request) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) URI.create(request.url(address)).toURL().openConnection();
        connection.setConnectTimeout(Math.toIntExact(connectTimeout.toMillis()));
        connection.setReadTimeout(Math.toIntExact(readTimeout.toMillis()));
        connection.setRequestMethod(request.method().name());
        for (Map.Entry<String, String> entry : request.headers().entrySet()) {
            connection.setRequestProperty(entry.getKey(), entry.getValue());
        }

        if (request.method() == HttpMethod.POST) {
            byte[] body = (request.bodyJson() == null ? "" : request.bodyJson()).getBytes(StandardCharsets.UTF_8);
            connection.setDoOutput(true);
            connection.setFixedLengthStreamingMode(body.length);
            try (OutputStream output = connection.getOutputStream()) {
                output.write(body);
            }
        }

        int statusCode = connection.getResponseCode();
        String body = readBody(connection, statusCode);
        Map<String, String> headers = flattenHeaders(connection.getHeaderFields());
        connection.disconnect();
        return new ProtocolResponse(statusCode, headers, body);
    }

    private static String readBody(HttpURLConnection connection, int statusCode) throws IOException {
        InputStream stream = statusCode >= 400 ? connection.getErrorStream() : connection.getInputStream();
        if (stream == null) {
            return "";
        }
        try (InputStream input = stream) {
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static Map<String, String> flattenHeaders(Map<String, List<String>> source) {
        Map<String, String> headers = new LinkedHashMap<>();
        for (Map.Entry<String, List<String>> entry : source.entrySet()) {
            if (entry.getKey() != null && entry.getValue() != null) {
                headers.put(entry.getKey(), String.join(",", entry.getValue()));
            }
        }
        return headers;
    }

    private static Duration requirePositive(Duration value, String name) {
        if (value == null || value.isNegative() || value.isZero()) {
            throw new IllegalArgumentException(name + " must be positive");
        }
        return value;
    }
}

