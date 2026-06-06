package tv.cinepilot.core.protocol;

import java.util.LinkedHashMap;
import java.util.Map;

final class PlaybackUrlQuery {
    private PlaybackUrlQuery() {
    }

    static String withOverrides(String url, Map<String, String> overrides) {
        if (url == null || url.isBlank() || overrides == null || overrides.isEmpty()) {
            return url;
        }

        String fragment = "";
        String withoutFragment = url;
        int fragmentIndex = url.indexOf('#');
        if (fragmentIndex >= 0) {
            fragment = url.substring(fragmentIndex);
            withoutFragment = url.substring(0, fragmentIndex);
        }

        String base = withoutFragment;
        String query = "";
        int queryIndex = withoutFragment.indexOf('?');
        if (queryIndex >= 0) {
            base = withoutFragment.substring(0, queryIndex);
            query = withoutFragment.substring(queryIndex + 1);
        }

        Map<String, String> merged = parseQuery(query);
        for (Map.Entry<String, String> entry : overrides.entrySet()) {
            removeExistingKey(merged, entry.getKey());
            if (entry.getValue() != null) {
                merged.put(entry.getKey(), entry.getValue());
            }
        }
        if (merged.isEmpty()) {
            return base + fragment;
        }
        return base + "?" + encodeQuery(merged) + fragment;
    }

    private static Map<String, String> parseQuery(String query) {
        Map<String, String> values = new LinkedHashMap<>();
        if (query == null || query.isBlank()) {
            return values;
        }
        for (String pair : query.split("&")) {
            if (pair.isBlank()) {
                continue;
            }
            int equals = pair.indexOf('=');
            String name = equals >= 0 ? pair.substring(0, equals) : pair;
            String value = equals >= 0 ? pair.substring(equals + 1) : "";
            values.put(name, value);
        }
        return values;
    }

    private static void removeExistingKey(Map<String, String> values, String key) {
        values.keySet().removeIf(existing ->
                existing.equalsIgnoreCase(key) ||
                        existing.equalsIgnoreCase(UrlEncoding.encodeComponent(key)));
    }

    private static String encodeQuery(Map<String, String> values) {
        StringBuilder builder = new StringBuilder();
        boolean first = true;
        for (Map.Entry<String, String> entry : values.entrySet()) {
            if (!first) {
                builder.append('&');
            }
            builder.append(encodeIfNeeded(entry.getKey()))
                    .append('=')
                    .append(encodeIfNeeded(entry.getValue()));
            first = false;
        }
        return builder.toString();
    }

    private static String encodeIfNeeded(String value) {
        if (value.indexOf('%') >= 0) {
            return value;
        }
        return UrlEncoding.encodeComponent(value);
    }
}
