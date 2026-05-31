package tv.cinepilot.core.protocol;

public enum PlayMethod {
    DIRECT_PLAY("DirectPlay"),
    DIRECT_STREAM("DirectStream"),
    TRANSCODE("Transcode");

    private final String wireName;

    PlayMethod(String wireName) {
        this.wireName = wireName;
    }

    public String wireName() {
        return wireName;
    }
}

