package tv.cinepilot.core.protocol;

public record ServerIdentity(
        MediaServerAddress address,
        String serverId,
        ServerFlavor flavor,
        String serverName
) {
    public ServerIdentity {
        if (address == null) {
            throw new IllegalArgumentException("address is required");
        }
        if (serverId == null || serverId.isBlank()) {
            throw new IllegalArgumentException("serverId is required");
        }
        if (flavor == null) {
            flavor = ServerFlavor.UNKNOWN;
        }
        if (serverName == null) {
            serverName = "";
        }
    }
}

