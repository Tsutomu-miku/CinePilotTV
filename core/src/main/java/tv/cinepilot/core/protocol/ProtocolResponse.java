package tv.cinepilot.core.protocol;

import java.util.Map;
import tv.cinepilot.core.AndroidCollections;

public record ProtocolResponse(
        int statusCode,
        Map<String, String> headers,
        String body
) {
    public ProtocolResponse {
        headers = AndroidCollections.mapCopy(headers);
        body = body == null ? "" : body;
    }

    public boolean successful() {
        return statusCode >= 200 && statusCode < 300;
    }

    public boolean unauthorized() {
        return statusCode == 401;
    }
}
