package tv.cinepilot.plugin.spi;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Stable, SPI-only snapshot of the media item being acted upon (rating,
 * favorite toggle, scrobble, ...). Exposed because plugins cannot import
 * :core domain types.
 *
 * <p>Only primitives, strings, and provider-id map keys are carried. If a
 * plugin needs more detail (episodes of a series, full cast list, ...) it
 * is responsible for fetching it from its upstream provider via the
 * provider ids listed here.
 */
public final class MediaItemSnapshot {
    public enum Kind { MOVIE, SERIES, SEASON, EPISODE, VIDEO, OTHER }

    private final String itemId;
    private final String serverId;
    private final String name;
    private final Kind kind;
    private final int productionYear;
    private final int indexNumber;
    private final int parentIndexNumber;
    private final String parentId;
    private final String seriesId;
    private final long runTimeMillis;
    private final Map<String, String> providerIds;

    public MediaItemSnapshot(
            String itemId,
            String serverId,
            String name,
            Kind kind,
            int productionYear,
            int indexNumber,
            int parentIndexNumber,
            String parentId,
            String seriesId,
            long runTimeMillis,
            Map<String, String> providerIds
    ) {
        if (itemId == null || itemId.isBlank()) throw new IllegalArgumentException("itemId required");
        if (serverId == null || serverId.isBlank()) throw new IllegalArgumentException("serverId required");
        if (name == null) throw new IllegalArgumentException("name required");
        this.itemId = itemId;
        this.serverId = serverId;
        this.name = name;
        this.kind = kind == null ? Kind.OTHER : kind;
        this.productionYear = Math.max(0, productionYear);
        this.indexNumber = indexNumber;
        this.parentIndexNumber = parentIndexNumber;
        this.parentId = parentId == null ? "" : parentId;
        this.seriesId = seriesId == null ? "" : seriesId;
        this.runTimeMillis = Math.max(0L, runTimeMillis);
        Map<String, String> copy = new LinkedHashMap<>();
        if (providerIds != null) {
            for (Map.Entry<String, String> e : providerIds.entrySet()) {
                if (e.getKey() == null || e.getValue() == null) continue;
                copy.put(e.getKey(), e.getValue());
            }
        }
        this.providerIds = Collections.unmodifiableMap(copy);
    }

    public String itemId() { return itemId; }
    public String serverId() { return serverId; }
    public String name() { return name; }
    public Kind kind() { return kind; }
    public int productionYear() { return productionYear; }
    public int indexNumber() { return indexNumber; }
    public int parentIndexNumber() { return parentIndexNumber; }
    public String parentId() { return parentId; }
    public String seriesId() { return seriesId; }
    public long runTimeMillis() { return runTimeMillis; }
    public Map<String, String> providerIds() { return providerIds; }

    /** Look up a provider id by key (e.g. {@code "Tmdb"}, {@code "Imdb"}). */
    public String providerId(String key) { return providerIds.getOrDefault(key, ""); }

    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MediaItemSnapshot that)) return false;
        return itemId.equals(that.itemId) && serverId.equals(that.serverId);
    }

    @Override public int hashCode() { return Objects.hash(itemId, serverId); }

    @Override public String toString() {
        return "MediaItemSnapshot[" + itemId + " '" + name + "']";
    }
}
