package tv.cinepilot.plugin.spi;

import java.util.Objects;

/**
 * Stable descriptor that every plugin exposes via
 * {@link CinePilotPlugin#descriptor()}. Values are used by the plugin host
 * to render settings screens, disambiguate multiple plugins, and match
 * per-plugin OAuth credentials.
 *
 * <p>All fields are required and non-blank. Once a plugin ships with a given
 * {@code id} it must not change it -- that identifier is the stable key used
 * to associate persisted user settings (credentials, enable flags, subject
 * matchers, ...) with the plugin that owns them.
 */
public final class PluginDescriptor {
    private final String id;
    private final String name;
    private final String version;
    private final String description;
    private final String vendor;

    public PluginDescriptor(String id, String name, String version, String description, String vendor) {
        requireNonBlank("id", id);
        requireNonBlank("name", name);
        requireNonBlank("version", version);
        requireNonBlank("description", description);
        requireNonBlank("vendor", vendor);
        this.id = id;
        this.name = name;
        this.version = version;
        this.description = description;
        this.vendor = vendor;
    }

    public String id() { return id; }
    public String name() { return name; }
    public String version() { return version; }
    public String description() { return description; }
    public String vendor() { return vendor; }

    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PluginDescriptor that)) return false;
        return id.equals(that.id) && version.equals(that.version);
    }

    @Override public int hashCode() { return Objects.hash(id, version); }

    @Override public String toString() {
        return "PluginDescriptor[" + id + ":" + version + " '" + name + "']";
    }

    private static void requireNonBlank(String field, String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
    }
}
