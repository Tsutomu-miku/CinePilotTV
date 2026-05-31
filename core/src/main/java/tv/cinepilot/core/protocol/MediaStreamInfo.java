package tv.cinepilot.core.protocol;

public record MediaStreamInfo(
        int index,
        MediaStreamType type,
        String codec,
        String language,
        String displayTitle,
        Integer width,
        Integer height,
        Integer channels,
        Integer bitDepth,
        long bitRate,
        String profile,
        String videoRange,
        String videoRangeType,
        boolean defaultStream,
        boolean forced,
        boolean external,
        String deliveryUrl
) {
    public MediaStreamInfo(
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
        this(
                index,
                type,
                codec,
                language,
                displayTitle,
                null,
                null,
                null,
                null,
                0L,
                "",
                "",
                "",
                defaultStream,
                forced,
                external,
                deliveryUrl
        );
    }

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
        if (bitRate < 0) {
            bitRate = 0;
        }
        if (profile == null) {
            profile = "";
        }
        if (videoRange == null) {
            videoRange = "";
        }
        if (videoRangeType == null) {
            videoRangeType = "";
        }
    }
}
