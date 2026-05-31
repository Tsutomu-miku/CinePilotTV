package tv.cinepilot.core.protocol;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Objects;

public final class MediaServerAddress {
    private final URI baseUri;

    private MediaServerAddress(URI baseUri) {
        this.baseUri = baseUri;
    }

    public static MediaServerAddress parse(String rawAddress) {
        if (rawAddress == null || rawAddress.isBlank()) {
            throw new IllegalArgumentException("Server address is required");
        }

        String candidate = rawAddress.trim();
        if (!candidate.contains("://")) {
            candidate = "http://" + candidate;
        }

        try {
            URI uri = new URI(candidate).normalize();
            String scheme = uri.getScheme();
            if (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme)) {
                throw new IllegalArgumentException("Only HTTP and HTTPS server addresses are supported");
            }
            if (uri.getHost() == null || uri.getHost().isBlank()) {
                throw new IllegalArgumentException("Server address must include a host");
            }

            String path = normalizePath(uri.getPath());
            URI normalized = new URI(
                    scheme.toLowerCase(),
                    uri.getUserInfo(),
                    uri.getHost().toLowerCase(),
                    uri.getPort(),
                    path,
                    null,
                    null
            );
            return new MediaServerAddress(normalized);
        } catch (URISyntaxException exception) {
            throw new IllegalArgumentException("Invalid server address: " + rawAddress, exception);
        }
    }

    public URI uri() {
        return baseUri;
    }

    public String value() {
        return baseUri.toString();
    }

    public String resolvePath(String path) {
        String suffix = path.startsWith("/") ? path : "/" + path;
        String prefix = baseUri.getPath();
        if ("/".equals(prefix)) {
            prefix = "";
        }
        return baseUri.resolve(prefix + suffix).toString();
    }

    private static String normalizePath(String path) {
        if (path == null || path.isBlank() || "/".equals(path)) {
            return "/";
        }
        String normalized = path;
        while (normalized.endsWith("/") && normalized.length() > 1) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof MediaServerAddress address)) {
            return false;
        }
        return Objects.equals(baseUri, address.baseUri);
    }

    @Override
    public int hashCode() {
        return Objects.hash(baseUri);
    }
}
