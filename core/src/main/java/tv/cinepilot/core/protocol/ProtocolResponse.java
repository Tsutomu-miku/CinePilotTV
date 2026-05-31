package tv.cinepilot.core.protocol;

import java.util.Map;

public record ProtocolResponse(
        int statusCode,
        Map<String, String> headers,
        String body
) {
    public ProtocolResponse {
        headers = Map.copyOf(headers == null ? Map.of() : headers);
        body = body == null ? "" : body;
    }

    public boolean successful() {
        return statusCode >= 200 && statusCode < 300;
    }

    public boolean unauthorized() {
        return statusCode == 401;
    }
}

