package tv.cinepilot.core.protocol;

public record SavedSession(
        SessionScope scope,
        String accessToken
) {
    public SavedSession {
        if (scope == null) {
            throw new IllegalArgumentException("scope is required");
        }
        if (accessToken == null || accessToken.isBlank()) {
            throw new IllegalArgumentException("accessToken is required");
        }
    }

    public AuthSession restore(ClientIdentity client) {
        if (!scope.clientName().equals(client.clientName())
                || !scope.deviceId().equals(client.deviceId())
                || !scope.appVersion().equals(client.version())) {
            throw new IllegalArgumentException("client identity does not match saved session scope");
        }
        return new AuthSession(scope.serverId(), scope.userId(), accessToken, client);
    }
}

