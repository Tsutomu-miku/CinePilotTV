package tv.cinepilot.core.protocol;

public enum PlaybackEndpoint {
    STARTED("/Sessions/Playing"),
    PROGRESS("/Sessions/Playing/Progress"),
    STOPPED("/Sessions/Playing/Stopped");

    private final String path;

    PlaybackEndpoint(String path) {
        this.path = path;
    }

    public String path() {
        return path;
    }
}

