package tv.cinepilot.core.protocol;

public final class MediaBrowserException extends RuntimeException {
    private final int statusCode;

    public MediaBrowserException(String message, int statusCode) {
        super(message);
        this.statusCode = statusCode;
    }

    public MediaBrowserException(String message, Throwable cause) {
        super(message, cause);
        this.statusCode = 0;
    }

    public int statusCode() {
        return statusCode;
    }
}

