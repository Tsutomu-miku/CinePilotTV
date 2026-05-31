package tv.cinepilot.core.protocol;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public final class ProtocolRequest {
    private final HttpMethod method;
    private final String path;
    private final Map<String, String> headers;
    private final Map<String, String> query;
    private final String bodyJson;

    public ProtocolRequest(
            HttpMethod method,
            String path,
            Map<String, String> headers,
            Map<String, String> query,
            String bodyJson
    ) {
        this.method = Objects.requireNonNull(method, "method");
        this.path = requirePath(path);
        this.headers = Map.copyOf(headers);
        this.query = Map.copyOf(query);
        this.bodyJson = bodyJson;
    }

    public HttpMethod method() {
        return method;
    }

    public String path() {
        return path;
    }

    public Map<String, String> headers() {
        return headers;
    }

    public Map<String, String> query() {
        return query;
    }

    public String bodyJson() {
        return bodyJson;
    }

    public String url(MediaServerAddress address) {
        String url = address.resolvePath(path);
        if (query.isEmpty()) {
            return url;
        }

        StringBuilder builder = new StringBuilder(url).append('?');
        boolean first = true;
        for (Map.Entry<String, String> entry : query.entrySet()) {
            if (entry.getValue() == null) {
                continue;
            }
            if (!first) {
                builder.append('&');
            }
            builder.append(encodeQuery(entry.getKey()))
                    .append('=')
                    .append(encodeQuery(entry.getValue()));
            first = false;
        }
        return builder.toString();
    }

    public static Builder get(String path) {
        return new Builder(HttpMethod.GET, path);
    }

    public static Builder post(String path) {
        return new Builder(HttpMethod.POST, path);
    }

    public static String encodePathSegment(String value) {
        return encode(value);
    }

    private static String encodeQuery(String value) {
        return encode(value);
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }

    private static String requirePath(String path) {
        if (path == null || path.isBlank() || !path.startsWith("/")) {
            throw new IllegalArgumentException("path must start with /");
        }
        return path;
    }

    public static final class Builder {
        private final HttpMethod method;
        private final String path;
        private final Map<String, String> headers = new LinkedHashMap<>();
        private final Map<String, String> query = new LinkedHashMap<>();
        private String bodyJson;

        private Builder(HttpMethod method, String path) {
            this.method = method;
            this.path = requirePath(path);
        }

        public Builder header(String name, String value) {
            if (value != null) {
                headers.put(name, value);
            }
            return this;
        }

        public Builder query(String name, String value) {
            if (value != null) {
                query.put(name, value);
            }
            return this;
        }

        public Builder jsonBody(String json) {
            bodyJson = json;
            header("Content-Type", "application/json");
            header("Accept", "application/json");
            return this;
        }

        public ProtocolRequest build() {
            return new ProtocolRequest(method, path, headers, query, bodyJson);
        }
    }
}

