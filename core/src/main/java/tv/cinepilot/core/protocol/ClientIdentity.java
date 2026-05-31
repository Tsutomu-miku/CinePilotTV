package tv.cinepilot.core.protocol;

public record ClientIdentity(
        String clientName,
        String deviceName,
        String deviceId,
        String version
) {
    public ClientIdentity {
        require(clientName, "clientName");
        require(deviceName, "deviceName");
        require(deviceId, "deviceId");
        require(version, "version");
    }

    private static void require(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " is required");
        }
    }
}

