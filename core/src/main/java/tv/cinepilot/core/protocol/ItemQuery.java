package tv.cinepilot.core.protocol;

import java.util.LinkedHashMap;
import java.util.Map;

public final class ItemQuery {
    private final Map<String, String> values;

    private ItemQuery(Map<String, String> values) {
        this.values = Map.copyOf(values);
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
                .fields("PrimaryImageAspectRatio,MediaSources,MediaStreams,Overview,ParentId,Genres,ProductionYear");
    }

    public static Builder resume() {
        return browse()
                .recursive(true)
                .mediaTypes("Video")
                .fields("PrimaryImageAspectRatio,MediaSources,MediaStreams,Overview,ParentId,Genres,ProductionYear");
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

