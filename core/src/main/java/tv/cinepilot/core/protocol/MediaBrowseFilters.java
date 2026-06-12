package tv.cinepilot.core.protocol;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Immutable filter selection that can be applied to any {@link ItemQuery.Builder}.
 *
 * <p>The workflow controller stores one instance per browse context (home / folder / overview)
 * and the UI toggles individual fields on copies. Separating filter state from item queries
 * lets chips be re-applied transparently when the user switches views or opens an overview.</p>
 */
public final class MediaBrowseFilters {
    public static final MediaBrowseFilters EMPTY = new MediaBrowseFilters(
            "", "", Collections.emptySet(), 0f, "", false, false, false
    );

    private final String genres;
    private final String years;
    private final Set<String> filterFlags;
    private final float minCommunityRating;
    private final String officialRatings;
    private final boolean only4K;
    private final boolean onlyHdr;
    private final boolean strict;

    private MediaBrowseFilters(
            String genres,
            String years,
            Set<String> filterFlags,
            float minCommunityRating,
            String officialRatings,
            boolean only4K,
            boolean onlyHdr,
            boolean strict
    ) {
        this.genres = genres == null ? "" : genres;
        this.years = years == null ? "" : years;
        this.filterFlags = filterFlags == null
                ? Collections.emptySet()
                : Collections.unmodifiableSet(new LinkedHashSet<>(filterFlags));
        this.minCommunityRating = minCommunityRating;
        this.officialRatings = officialRatings == null ? "" : officialRatings;
        this.only4K = only4K;
        this.onlyHdr = onlyHdr;
        this.strict = strict;
    }

    public boolean isEmpty() {
        return genres.isBlank()
                && years.isBlank()
                && filterFlags.isEmpty()
                && minCommunityRating <= 0f
                && officialRatings.isBlank()
                && !only4K
                && !onlyHdr;
    }

    /** Jellyfin {@code Filters} parameter combinations that should hide "resume / next-up" rows. */
    public boolean isStrict() {
        return strict || !filterFlags.isEmpty() || minCommunityRating > 0f || !officialRatings.isBlank() || only4K || onlyHdr;
    }

    public ItemQuery.Builder applyTo(ItemQuery.Builder builder) {
        if (!genres.isBlank()) builder.genres(genres);
        if (!years.isBlank()) builder.years(years);
        if (minCommunityRating > 0f) builder.minCommunityRating(minCommunityRating);
        if (!officialRatings.isBlank()) builder.officialRatings(officialRatings);
        if (!filterFlags.isEmpty()) builder.filters(String.join(",", filterFlags));
        if (only4K) builder.minWidth(3840);
        if (onlyHdr) builder.isHdr(true);
        return builder;
    }

    public String genres() { return genres; }
    public String years() { return years; }
    public Set<String> filterFlags() { return filterFlags; }
    public float minCommunityRating() { return minCommunityRating; }
    public String officialRatings() { return officialRatings; }
    public boolean only4K() { return only4K; }
    public boolean onlyHdr() { return onlyHdr; }

    public MediaBrowseFilters withGenre(String genreName) {
        return new MediaBrowseFilters(
                genreName == null ? "" : genreName,
                years, filterFlags, minCommunityRating, officialRatings,
                only4K, onlyHdr, strict || (genreName != null && !genreName.isBlank())
        );
    }

    public MediaBrowseFilters withYear(int year) {
        return new MediaBrowseFilters(
                genres,
                year > 0 ? Integer.toString(year) : "",
                filterFlags, minCommunityRating, officialRatings,
                only4K, onlyHdr, strict || year > 0
        );
    }

    /**
     * Replace the years CSV verbatim (e.g. {@code "2020,2021,...,2029"} for a decade bucket).
     * A blank string clears the year filter.
     */
    public MediaBrowseFilters withYearsCsv(String commaYears) {
        String safe = commaYears == null ? "" : commaYears;
        return new MediaBrowseFilters(
                genres,
                safe,
                filterFlags, minCommunityRating, officialRatings,
                only4K, onlyHdr, strict || !safe.isBlank()
        );
    }

    public MediaBrowseFilters withMinRating(float stars) {
        float value = stars >= 1f ? stars : 0f;
        return new MediaBrowseFilters(
                genres, years, filterFlags, value, officialRatings,
                only4K, onlyHdr, strict || value > 0f
        );
    }

    public MediaBrowseFilters withFilterFlag(String jellyfinFilter, boolean on) {
        Set<String> next = new LinkedHashSet<>(filterFlags);
        if (on) next.add(jellyfinFilter); else next.remove(jellyfinFilter);
        return new MediaBrowseFilters(
                genres, years, next, minCommunityRating, officialRatings,
                only4K, onlyHdr, strict
        );
    }

    public MediaBrowseFilters withOnly4K(boolean on) {
        return new MediaBrowseFilters(
                genres, years, filterFlags, minCommunityRating, officialRatings,
                on, onlyHdr, strict || on
        );
    }

    public MediaBrowseFilters withOnlyHdr(boolean on) {
        return new MediaBrowseFilters(
                genres, years, filterFlags, minCommunityRating, officialRatings,
                only4K, on, strict || on
        );
    }

    /** Well-known Jellyfin {@code Filters} values we expose as filter chips. */
    public static final String FLAG_IS_UNPLAYED = "IsUnplayed";
    public static final String FLAG_IS_PLAYED = "IsPlayed";
    public static final String FLAG_IS_FAVORITE = "IsFavorite";

    /**
     * Convenience year buckets. The server does not expose numeric year ranges directly,
     * so we let the UI offer decade buckets and translate them into comma-separated years.
     */
    public static List<Integer> decadeBuckets() {
        List<Integer> decades = new ArrayList<>();
        for (int year = 2020; year >= 1970; year -= 10) decades.add(year);
        return Collections.unmodifiableList(decades);
    }

    public static MediaBrowseFilters of() {
        return EMPTY;
    }
}
