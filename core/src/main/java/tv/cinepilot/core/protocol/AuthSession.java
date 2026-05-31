package tv.cinepilot.core.protocol;

public record AuthSession(
        String serverId,
        String userId,
        String accessToken,
        ClientIdentity client
) {
    public AuthSession {
        require(serverId, "serverId");
        require(userId, "userId");
        require(accessToken, "accessToken");
        if (client == null) {
            throw new IllegalArgumentException("client is required");
        }
    }

    public String authorizationValue() {
        return authorizationValue(ServerFlavor.JELLYFIN);
    }

    public String authorizationValue(ServerFlavor flavor) {
        String scheme = flavor == ServerFlavor.EMBY ? "Emby" : "MediaBrowser";
        return scheme + " " +
                "Client=\"" + escape(client.clientName()) + "\", " +
                "Device=\"" + escape(client.deviceName()) + "\", " +
                "DeviceId=\"" + escape(client.deviceId()) + "\", " +
                "Version=\"" + escape(client.version()) + "\", " +
                "UserId=\"" + escape(userId) + "\", " +
                "Token=\"" + escape(accessToken) + "\"";
    }

    public String legacyTokenHeaderName() {
        return "X-Emby-Token";
    }

    private static String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static void require(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " is required");
        }
    }
}
