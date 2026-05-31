package tv.cinepilot.core.tv;

import java.util.List;
import tv.cinepilot.core.AndroidCollections;
import tv.cinepilot.core.protocol.MediaItemSummary;

public record HomeRow(
        String id,
        String title,
        List<MediaItemSummary> items
) {
    public HomeRow {
        require(id, "id");
        if (title == null) {
            title = "";
        }
        items = AndroidCollections.listCopy(items);
    }

    public boolean containsItem(String itemId) {
        return items.stream().anyMatch(item -> item.id().equals(itemId));
    }

    private static void require(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " is required");
        }
    }
}
