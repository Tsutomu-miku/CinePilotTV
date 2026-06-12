package tv.cinepilot.core.protocol;

/**
 * Minimal genre descriptor returned by the /Genres endpoint. The server also exposes image tags
 * and item counts when the right query parameters are set; we only keep the fields that
 * participate in client-side rendering so the record stays cheap to pass through workflow state.
 */
public record GenreInfo(
        String id,
        String name,
        String primaryImageTag
) {
    public static GenreInfo empty() {
        return new GenreInfo("", "", "");
    }

    public String displayName() {
        return (name == null || name.isBlank()) ? "(未分类)" : name;
    }

    public boolean hasImage() {
        return primaryImageTag != null && !primaryImageTag.isBlank();
    }
}
