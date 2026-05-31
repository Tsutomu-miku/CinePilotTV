package tv.cinepilot.core.protocol;

public record MediaStreamInfo(
        int index,
        MediaStreamType type,
        String codec,
        String language,
        String displayTitle,
        boolean defaultStream,
        boolean forced,
        boolean external,
        String deliveryUrl
) {
    public MediaStreamInfo {
        if (index < 0) {
            throw new IllegalArgumentException("index must be zero or greater");
        }
        if (type == null) {
            type = MediaStreamType.UNKNOWN;
        }
        if (codec == null) {
            codec = "";
        }
        if (language == null) {
            language = "";
        }
        if (displayTitle == null) {
            displayTitle = "";
        }
    }
}

