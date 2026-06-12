package tv.cinepilot.core.protocol;

import java.util.LinkedHashMap;
import java.util.Map;
import tv.cinepilot.core.AndroidCollections;

public final class ItemQuery {
    private static final String ITEM_FIELDS = "PrimaryImageAspectRatio,MediaSources,MediaStreams,Overview,"
            + "ParentId,Genres,ProductionYear,SeriesId,PremiereDate,CommunityRating,OfficialRating,People";
    private final Map<String, String> values;

    private ItemQuery(Map<String, String> values) {
        this.values = AndroidCollections.mapCopy(values);
    }

    public Map<String, String> values() {
        return values;
    }

    public static Builder browse() {
        return new Builder()
                .enableImages(true)
                .enableUserData(true)
                .imageTypeLimit(1)
                .enableImageTypes("Primary,Backdrop,Thumb")
                .sortBy("SortName")
                .sortOrder("Ascending")
                .fields(ITEM_FIELDS);
    }

    public static Builder resume() {
        return browse()
                .recursive(true)
                .mediaTypes("Video")
                .fields(ITEM_FIELDS);
    }

    public static Builder search(String term) {
        return search(term, "Movie,Episode,Series,Video");
    }

    public static Builder search(String term, String includeItemTypes) {
        return browse()
                .recursive(true)
                .searchTerm(term)
                .includeItemTypes(includeItemTypes);
    }

    public static final class Builder {
        private final Map<String, String> values = new LinkedHashMap<>();

        public Builder parentId(String value) {
            return put("ParentId", value);
        }

        public Builder startIndex(int value) {
            return put("StartIndex", Integer.toString(value));
        }

        public Builder limit(int value) {
            return put("Limit", Integer.toString(value));
        }

        public Builder recursive(boolean value) {
            return put("Recursive", Boolean.toString(value));
        }

        public Builder includeItemTypes(String value) {
            return put("IncludeItemTypes", value);
        }

        public Builder mediaTypes(String value) {
            return put("MediaTypes", value);
        }

        public Builder sortBy(String value) {
            return put("SortBy", value);
        }

        public Builder sortOrder(String value) {
            return put("SortOrder", value);
        }

        public Builder filters(String value) {
            return put("Filters", value);
        }

        public Builder personIds(String value) {
            return put("PersonIds", value);
        }

        public Builder genres(String value) {
            return put("Genres", value);
        }

        public Builder genreIds(String value) {
            return put("GenreIds", value);
        }

        public Builder years(String value) {
            return put("Years", value);
        }

        public Builder minCommunityRating(float value) {
            return put("MinCommunityRating", String.format(java.util.Locale.US, "%.1f", value));
        }

        public Builder officialRatings(String value) {
            return put("OfficialRatings", value);
        }

        /**
         * Require the media stream to have at least this horizontal pixel count.
         * Pass 3840 to enforce 4K UHD.
         */
        public Builder minWidth(int pixels) {
            if (pixels <= 0) return this;
            return put("MinWidth", Integer.toString(pixels));
        }

        /**
         * Jellyfin supports a loose HDR filter via the {@code Video3DFormat!=None} and
         * VideoRange fields. No single server-side parameter maps perfectly; clients
         * typically query broadly and filter locally. We expose an explicit {@code IsHDR}
         * marker that can be consumed by post-query filtering when server support is
         * available, and pass-through as a hint otherwise.
         */
        public Builder isHdr(boolean on) {
            if (!on) return this;
            return put("IsHdr", "true");
        }

        public Builder searchTerm(String value) {
            return put("SearchTerm", value);
        }

        public Builder fields(String value) {
            return put("Fields", value);
        }

        public Builder enableImages(boolean value) {
            return put("EnableImages", Boolean.toString(value));
        }

        public Builder enableUserData(boolean value) {
            return put("EnableUserData", Boolean.toString(value));
        }

        public Builder imageTypeLimit(int value) {
            return put("ImageTypeLimit", Integer.toString(value));
        }

        public Builder enableImageTypes(String value) {
            return put("EnableImageTypes", value);
        }

        public ItemQuery build() {
            return new ItemQuery(values);
        }

        private Builder put(String key, String value) {
            if (value != null && !value.isBlank()) {
                values.put(key, value);
            }
            return this;
        }
    }
}
