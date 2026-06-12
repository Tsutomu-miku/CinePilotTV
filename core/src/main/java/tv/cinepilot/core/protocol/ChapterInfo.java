package tv.cinepilot.core.protocol;

/** A single chapter marker returned with the media item detail payload. */
public record ChapterInfo(
        long startPositionTicks,
        String name,
        String imageTag
) {
    public ChapterInfo {
        if (name == null) {
            name = "";
        }
        if (imageTag == null) {
            imageTag = "";
        }
    }

    public static ChapterInfo empty() {
        return new ChapterInfo(0L, "", "");
    }
}
