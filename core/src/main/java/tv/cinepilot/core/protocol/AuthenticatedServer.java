package tv.cinepilot.core.protocol;

public record AuthenticatedServer(
        ServerIdentity server,
        AuthSession session
) {
    public AuthenticatedServer {
        if (server == null) {
            throw new IllegalArgumentException("server is required");
        }
        if (session == null) {
            throw new IllegalArgumentException("session is required");
        }
        if (!server.serverId().equals(session.serverId())) {
            throw new IllegalArgumentException("server id must match session scope");
        }
    }
}

