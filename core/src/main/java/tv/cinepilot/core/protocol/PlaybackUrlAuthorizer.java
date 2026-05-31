package tv.cinepilot.core.protocol;

public final class PlaybackUrlAuthorizer {
    private PlaybackUrlAuthorizer() {
    }

    public static String withAccessToken(String url, AuthSession session) {
        if (url == null || url.isBlank()) {
            throw new IllegalArgumentException("url is required");
        }
        if (session == null) {
            throw new IllegalArgumentException("session is required");
        }
        if (hasQueryParameter(url, "api_key")) {
            return url;
        }

        int fragmentStart = url.indexOf('#');
        String fragment = fragmentStart >= 0 ? url.substring(fragmentStart) : "";
        String withoutFragment = fragmentStart >= 0 ? url.substring(0, fragmentStart) : url;
        String separator = withoutFragment.contains("?") ? "&" : "?";
        return withoutFragment + separator + "api_key=" + encode(session.accessToken()) + fragment;
    }

    private static boolean hasQueryParameter(String url, String name) {
        int queryStart = url.indexOf('?');
        if (queryStart < 0) {
            return false;
        }
        int fragmentStart = url.indexOf('#', queryStart);
        String query = fragmentStart >= 0
                ? url.substring(queryStart + 1, fragmentStart)
                : url.substring(queryStart + 1);
        for (String pair : query.split("&")) {
            int equals = pair.indexOf('=');
            String key = equals >= 0 ? pair.substring(0, equals) : pair;
            if (name.equals(key)) {
                return true;
            }
        }
        return false;
    }

    private static String encode(String value) {
        return UrlEncoding.encodeComponent(value);
    }
}
