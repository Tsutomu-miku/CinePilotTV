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
        String subtitleDisplayTitle
) {
    public PlayableMedia {
        require(itemId, "itemId");
        require(mediaSourceId, "mediaSourceId");
        require(playSessionId, "playSessionId");
        if (playMethod == null) {
            throw new IllegalArgumentException("playMethod is required");
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
                ""
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
