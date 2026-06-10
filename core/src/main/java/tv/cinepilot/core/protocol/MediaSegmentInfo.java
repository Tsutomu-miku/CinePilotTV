package tv.cinepilot.core.protocol;

/**
 * Describes a labeled time segment of a video (intro, credits, preview, recap, ...).
 *
 * <p>Jellyfin 10.10 exposes these via the {@code /Items/{id}/MediaSegments} endpoint;
 * Emby Premiere stores equivalent fields directly on the item payload
 * ({@code IntroStart}/{"@type":"Intro"} mapping through {@link MediaBrowserResponseMapper}).
 *
 * <p>The UI layer consumes only the two semantics it understands:
 * {@link Type#INTRO} and {@link Type#CREDITS}. Unknown types are preserved so future
 * skip rules can be added without changing the mapper.
 */
public record MediaSegmentInfo(
        Type type,
        long startPositionTicks,
        long endPositionTicks
) {
    public enum Type {
        INTRO,
        CREDITS,
        PREVIEW,
        RECAP,
        UNKNOWN;

        public static Type fromWireName(String value) {
            if (value == null || value.isBlank()) return UNKNOWN;
            return switch (value.toLowerCase()) {
                case "intro" -> INTRO;
                case "credits", "outro" -> CREDITS;
                case "preview" -> PREVIEW;
                case "recap" -> RECAP;
                default -> UNKNOWN;
            };
        }
    }

    public boolean containsTicks(long ticks) {
        return ticks >= startPositionTicks && ticks < endPositionTicks;
    }

    public long remainingTicks(long ticks) {
        long remaining = endPositionTicks - ticks;
        return remaining < 0L ? 0L : remaining;
    }

    public long lengthTicks() {
        long length = endPositionTicks - startPositionTicks;
        return length < 0L ? 0L : length;
    }
}
