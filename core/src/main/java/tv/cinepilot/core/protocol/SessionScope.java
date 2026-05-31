package tv.cinepilot.core.protocol;

public record SessionScope(
        String serverId,
        String serverUrl,
        String userId,
        String clientName,
        String deviceId,
        String appVersion
) {
    public SessionScope {
        require(serverId, "serverId");
        require(serverUrl, "serverUrl");
        require(userId, "userId");
        require(clientName, "clientName");
        require(deviceId, "deviceId");
        require(appVersion, "appVersion");
    }

    public static SessionScope from(ServerIdentity server, AuthSession session) {
        return new SessionScope(
                server.serverId(),
                server.address().value(),
                session.userId(),
                session.client().clientName(),
                session.client().deviceId(),
                session.client().version()
        );
    }

    private static void require(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " is required");
        }
    }
}

