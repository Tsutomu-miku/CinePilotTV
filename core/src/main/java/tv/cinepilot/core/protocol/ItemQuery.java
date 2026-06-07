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
