package tv.cinepilot.core.protocol;

/**
 * Describes a Trickplay tileset: a tiled JPEG/WEBP sprite plus the grid size used to
 * decode individual thumbnails. Renderers walk the grid based on the tile duration to
 * paint a scrubber preview.
 *
 * <p>The tile URL is a fully-qualified image path built by the protocol layer and does
 * not include server credentials; callers are expected to pass it through
 * {@link PlaybackUrlAuthorizer} or the equivalent image loader before displaying.
 */
public record TrickplayInfo(
        int tileWidth,
        int tileHeight,
        int tilesPerRow,
        int tilesPerColumn,
        int tileCount,
        long tileIntervalTicks,
        String imageUrl
) {
    public TrickplayInfo {
        if (imageUrl == null) {
            imageUrl = "";
        }
    }

    public static TrickplayInfo empty() {
        return new TrickplayInfo(0, 0, 0, 0, 0, 0L, "");
    }

    public boolean isValid() {
        return tileCount > 0 && tileIntervalTicks > 0 && !imageUrl.isBlank()
                && tilesPerRow > 0 && tilesPerColumn > 0;
    }

    public int tileIndexAtTicks(long positionTicks) {
        if (tileIntervalTicks <= 0 || tileCount <= 0) return -1;
        long index = positionTicks / tileIntervalTicks;
        if (index < 0 || index >= tileCount) return -1;
        return (int) index;
    }
}
