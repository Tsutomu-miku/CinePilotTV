package tv.cinepilot.core.protocol;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;

final class UrlEncoding {
    private UrlEncoding() {
    }

    static String encodeComponent(String value) {
        try {
            return URLEncoder.encode(value, "UTF-8").replace("+", "%20");
        } catch (UnsupportedEncodingException exception) {
            throw new IllegalStateException("UTF-8 is not available", exception);
        }
    }

    static String decodeComponent(String value) {
        try {
            return URLDecoder.decode(value, "UTF-8");
        } catch (UnsupportedEncodingException exception) {
            throw new IllegalStateException("UTF-8 is not available", exception);
        }
    }
}
