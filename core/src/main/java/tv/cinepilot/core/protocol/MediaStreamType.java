package tv.cinepilot.core.protocol;

public enum MediaStreamType {
    VIDEO,
    AUDIO,
    SUBTITLE,
    UNKNOWN;

    public static MediaStreamType fromWireName(String value) {
        if (value == null || value.isBlank()) {
            return UNKNOWN;
        }
        return switch (value.toLowerCase()) {
            case "video" -> VIDEO;
            case "audio" -> AUDIO;
            case "subtitle" -> SUBTITLE;
            default -> UNKNOWN;
        };
    }
}

