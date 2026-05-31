package tv.cinepilot.core.protocol;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class MediaBrowserResponseMapper {
    private MediaBrowserResponseMapper() {
    }

    public static ServerIdentity serverIdentity(MediaServerAddress address, String json) {
        Map<String, Object> root = JsonValue.object(json);
        String serverId = firstString(root, "Id", "ServerId");
        String serverName = firstString(root, "ServerName", "Name");
        String productName = firstString(root, "ProductName", "ServerName", "Name");
        return new ServerIdentity(address, serverId, ServerFlavor.fromServerName(productName), serverName);
    }

    public static AuthSession authSession(ClientIdentity client, String json) {
        Map<String, Object> root = JsonValue.object(json);
        Map<String, Object> user = JsonValue.childObject(root, "User");
        String accessToken = firstString(root, "AccessToken", "Token");
        String serverId = firstString(root, "ServerId");
        if (serverId == null) {
            serverId = JsonValue.string(JsonValue.childObject(root, "SessionInfo"), "ServerId");
        }
        String userId = firstString(user, "Id", "UserId");
        return new AuthSession(serverId, userId, accessToken, client);
    }

    public static PlaybackInfo playbackInfo(String itemId, String json) {
        Map<String, Object> root = JsonValue.object(json);
        String playSessionId = firstString(root, "PlaySessionId");
        List<MediaSourceInfo> mediaSources = new ArrayList<>();
        for (Object value : JsonValue.array(root, "MediaSources")) {
            if (value instanceof Map<?, ?> map) {
                mediaSources.add(mediaSource(JsonValueMap.cast(map)));
            }
        }
        return new PlaybackInfo(itemId, playSessionId, mediaSources);
    }

    private static MediaSourceInfo mediaSource(Map<String, Object> source) {
        List<MediaStreamInfo> streams = new ArrayList<>();
        for (Object value : JsonValue.array(source, "MediaStreams")) {
            if (value instanceof Map<?, ?> map) {
                streams.add(mediaStream(JsonValueMap.cast(map)));
            }
        }
        return MediaSourceInfo.builder(requiredString(source, "Id"))
                .container(valueOrEmpty(JsonValue.string(source, "Container")))
                .directStreamUrl(JsonValue.string(source, "DirectStreamUrl"))
                .transcodingUrl(JsonValue.string(source, "TranscodingUrl"))
                .supportsDirectPlay(JsonValue.bool(source, "SupportsDirectPlay"))
                .supportsDirectStream(JsonValue.bool(source, "SupportsDirectStream"))
                .supportsTranscoding(JsonValue.bool(source, "SupportsTranscoding"))
                .mediaStreams(streams)
                .build();
    }

    private static MediaStreamInfo mediaStream(Map<String, Object> stream) {
        return new MediaStreamInfo(
                number(stream, "Index").intValue(),
                MediaStreamType.fromWireName(JsonValue.string(stream, "Type")),
                JsonValue.string(stream, "Codec"),
                JsonValue.string(stream, "Language"),
                JsonValue.string(stream, "DisplayTitle"),
                JsonValue.bool(stream, "IsDefault"),
                JsonValue.bool(stream, "IsForced"),
                JsonValue.bool(stream, "IsExternal"),
                JsonValue.string(stream, "DeliveryUrl")
        );
    }

    private static String firstString(Map<String, Object> object, String... keys) {
        for (String key : keys) {
            String value = JsonValue.string(object, key);
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private static String requiredString(Map<String, Object> object, String key) {
        String value = JsonValue.string(object, key);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(key + " is required");
        }
        return value;
    }

    private static Number number(Map<String, Object> object, String key) {
        Object value = object.get(key);
        if (value instanceof Number number) {
            return number;
        }
        return 0;
    }

    private static String valueOrEmpty(String value) {
        return value == null ? "" : value;
    }

    private static final class JsonValueMap {
        @SuppressWarnings("unchecked")
        static Map<String, Object> cast(Map<?, ?> map) {
            return (Map<String, Object>) map;
        }
    }
}

