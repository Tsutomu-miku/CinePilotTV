package tv.cinepilot.core.protocol;

public record UserItemData(
        boolean played,
        long playbackPositionTicks,
        int playCount,
        boolean favorite,
        Boolean likes,
        Double userRating
) {
    public UserItemData {
        if (playbackPositionTicks < 0) {
            throw new IllegalArgumentException("playbackPositionTicks must be zero or greater");
        }
        if (playCount < 0) {
            throw new IllegalArgumentException("playCount must be zero or greater");
        }
        if (userRating != null) {
            if (userRating < 0.0 || userRating > 10.0) {
                throw new IllegalArgumentException("userRating must be in [0,10] when present");
            }
        }
    }

    public static UserItemData empty() {
        return new UserItemData(false, 0L, 0, false, null, null);
    }

    /** Backward-compatible constructor for existing callers (pre-likes/rating). */
    public UserItemData(
            boolean played,
            long playbackPositionTicks,
            int playCount,
            boolean favorite
    ) {
        this(played, playbackPositionTicks, playCount, favorite, null, null);
    }

    public UserItemData withFavorite(boolean value) {
        return new UserItemData(played, playbackPositionTicks, playCount, value, likes, userRating);
    }

    public UserItemData withPlayed(boolean value) {
        int count = value ? Math.max(1, playCount) : 0;
        long pos = value ? 0L : playbackPositionTicks;
        return new UserItemData(value, pos, count, favorite, likes, userRating);
    }

    public UserItemData withRating(Double valueZeroToTen) {
        return new UserItemData(played, playbackPositionTicks, playCount, favorite, likes, valueZeroToTen);
    }
}
