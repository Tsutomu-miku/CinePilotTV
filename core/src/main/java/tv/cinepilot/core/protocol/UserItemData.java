package tv.cinepilot.core.protocol;

public record UserItemData(
        boolean played,
        long playbackPositionTicks,
        int playCount,
        boolean favorite
) {
    public UserItemData {
        if (playbackPositionTicks < 0) {
            throw new IllegalArgumentException("playbackPositionTicks must be zero or greater");
        }
        if (playCount < 0) {
            throw new IllegalArgumentException("playCount must be zero or greater");
        }
    }

    public static UserItemData empty() {
        return new UserItemData(false, 0L, 0, false);
    }
}

