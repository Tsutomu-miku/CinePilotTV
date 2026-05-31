package tv.cinepilot.core.protocol;

public enum ServerFlavor {
    JELLYFIN("jellyfin"),
    EMBY("emby"),
    UNKNOWN("unknown");

    private final String wireName;

    ServerFlavor(String wireName) {
        this.wireName = wireName;
    }

    public String wireName() {
        return wireName;
    }

    public static ServerFlavor fromServerName(String value) {
        if (value == null || value.isBlank()) {
            return UNKNOWN;
        }
        String normalized = value.toLowerCase();
        if (normalized.contains("jellyfin")) {
            return JELLYFIN;
        }
        if (normalized.contains("emby")) {
            return EMBY;
        }
        return UNKNOWN;
    }
}

