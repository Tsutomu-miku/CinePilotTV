package tv.cinepilot.core.protocol;

public record PlayableMedia(
        String itemId,
        String mediaSourceId,
        String playSessionId,
        PlayMethod playMethod,
        String url,
        ProtocolRequest request,
        Integer audioStreamIndex,
        Integer subtitleStreamIndex,
        String subtitleDeliveryUrl,
        String subtitleCodec,
        String subtitleLanguage,
        String subtitleDisplayTitle,
        long startTimeTicks,
        Float playbackRate
) {
    public PlayableMedia {
        require(itemId, "itemId");
        require(mediaSourceId, "mediaSourceId");
        require(playSessionId, "playSessionId");
        if (playMethod == null) {
            throw new IllegalArgumentException("playMethod is required");
        }
        if (startTimeTicks < 0) {
            throw new IllegalArgumentException("startTimeTicks must be zero or greater");
        }
        if ((url == null || url.isBlank()) && request == null) {
            throw new IllegalArgumentException("either url or request is required");
        }
        if (subtitleDeliveryUrl == null) {
            subtitleDeliveryUrl = "";
        }
        if (subtitleCodec == null) {
            subtitleCodec = "";
        }
        if (subtitleLanguage == null) {
            subtitleLanguage = "";
        }
        if (subtitleDisplayTitle == null) {
            subtitleDisplayTitle = "";
        }
        if (playbackRate != null && playbackRate <= 0f) {
            playbackRate = null;
        }
    }

    public PlayableMedia(
            String itemId,
            String mediaSourceId,
            String playSessionId,
            PlayMethod playMethod,
            String url,
            ProtocolRequest request,
            Integer audioStreamIndex,
            Integer subtitleStreamIndex
    ) {
        this(
                itemId,
                mediaSourceId,
                playSessionId,
                playMethod,
                url,
                request,
                audioStreamIndex,
                subtitleStreamIndex,
                "",
                "",
                "",
                "",
                0L,
                null
        );
    }

    public boolean hasReadyUrl() {
        return url != null && !url.isBlank();
    }

    private static void require(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " is required");
        }
    }
}
