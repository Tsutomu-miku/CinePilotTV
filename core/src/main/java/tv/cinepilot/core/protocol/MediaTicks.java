package tv.cinepilot.core.protocol;

public final class MediaTicks {
    private static final long TICKS_PER_MILLISECOND = 10_000L;

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
}

