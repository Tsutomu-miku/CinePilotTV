package tv.cinepilot.core.protocol;

/** Helpers for formatting, counting, and mapping media-tick based time values. */
public final class MediaTicks {
    public static final long TICKS_PER_MILLISECOND = 10_000L;
    public static final long TICKS_PER_SECOND = 10_000_000L;
    public static final long TICKS_PER_MINUTE = 600_000_000L;
    public static final long TICKS_PER_HOUR = 36_000_000_000L;

    private MediaTicks() {
    }

    public static long fromMilliseconds(long milliseconds) {
        if (milliseconds < 0) {
            throw new IllegalArgumentException("milliseconds must be zero or greater");
        }
        return Math.multiplyExact(milliseconds, TICKS_PER_MILLISECOND);
    }

    public static long toMilliseconds(long ticks) {
        if (ticks < 0) {
            throw new IllegalArgumentException("ticks must be zero or greater");
        }
        return ticks / TICKS_PER_MILLISECOND;
    }

    public static long fromSeconds(long seconds) {
        if (seconds < 0) {
            throw new IllegalArgumentException("seconds must be zero or greater");
        }
        return Math.multiplyExact(seconds, TICKS_PER_SECOND);
    }

    public static long toSeconds(long ticks) {
        if (ticks < 0) return 0L;
        return ticks / TICKS_PER_SECOND;
    }

    /** Short chapter marker: M:SS or H:MM:SS. */
    public static String formatShort(long ticks) {
        if (ticks < 0) ticks = 0L;
        long seconds = ticks / TICKS_PER_SECOND;
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        long secs = seconds % 60;
        if (hours > 0) {
            return String.format("%d:%02d:%02d", hours, minutes, secs);
        }
        return String.format("%d:%02d", minutes, secs);
    }
}
