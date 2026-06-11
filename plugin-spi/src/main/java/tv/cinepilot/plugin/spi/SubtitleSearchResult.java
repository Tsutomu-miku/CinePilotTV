package tv.cinepilot.plugin.spi;

import java.util.Objects;

/**
 * A single subtitle search result from a {@link SubtitleSearchPlugin}.
 *
 * <p>Only carries display metadata and a provider-specific opaque identifier
 * that the plugin can use to download the actual subtitle file. Plugins must
 * not do heavy work during search -- fetching file contents happens via
 * {@link SubtitleSearchPlugin#downloadSubtitle(PluginSettingsStore, SubtitleSearchResult)}.
 */
public final class SubtitleSearchResult {

    public enum Format { SRT, ASS, SSA, VTT, PGS, UNKNOWN }

    private final String providerId;
    private final String id;
    private final String name;
    private final String language;
    private final Format format;
    private final String rating;
    private final int downloadCount;
    private final String author;

    public SubtitleSearchResult(
            String providerId,
            String id,
            String name,
            String language,
            Format format,
            String rating,
            int downloadCount,
            String author
    ) {
        if (providerId == null || providerId.isBlank())
            throw new IllegalArgumentException("providerId required");
        if (id == null || id.isBlank())
            throw new IllegalArgumentException("id required");
        if (name == null || name.isBlank())
            throw new IllegalArgumentException("name required");
        this.providerId = providerId;
        this.id = id;
        this.name = name;
        this.language = language == null ? "" : language;
        this.format = format == null ? Format.UNKNOWN : format;
        this.rating = rating == null ? "" : rating;
        this.downloadCount = Math.max(0, downloadCount);
        this.author = author == null ? "" : author;
    }

    /** Which plugin / provider this result came from. */
    public String providerId() { return providerId; }

    /** Opaque identifier that the originating plugin understands. */
    public String id() { return id; }

    /** Display name, e.g. "Arrival.2016.1080p.BluRay.x264-SPARKS.chs". */
    public String name() { return name; }

    /** Language code or label, e.g. "zh-CN", "eng", "简体中文". */
    public String language() { return language; }

    /** Subtitle file format. */
    public Format format() { return format; }

    /** Rating string, e.g. "4.5 / 5" or "★★★★☆". Empty if not available. */
    public String rating() { return rating; }

    /** Number of downloads. 0 if not available. */
    public int downloadCount() { return downloadCount; }

    /** Author / uploader name. Empty if not available. */
    public String author() { return author; }

    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SubtitleSearchResult that)) return false;
        return providerId.equals(that.providerId) && id.equals(that.id);
    }

    @Override public int hashCode() {
        return Objects.hash(providerId, id);
    }

    @Override public String toString() {
        return "SubtitleSearchResult[" + providerId + "/" + id + " '" + name + "']";
    }
}
