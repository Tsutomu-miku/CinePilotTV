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

    /**
     * Returns a copy of this row where every media item whose id matches
     * {@code updated.id()} is replaced with {@code updated}. If no item in
     * this row matches, {@code this} is returned unchanged.
     */
    public HomeRow replaceItem(MediaItemSummary updated) {
        if (updated == null) {
            return this;
        }
        String targetId = updated.id();
        boolean[] hit = {false};
        List<MediaItemSummary> next = new java.util.ArrayList<>(items.size());
        for (MediaItemSummary existing : items) {
            if (existing.id().equals(targetId)) {
                next.add(updated);
                hit[0] = true;
            } else {
                next.add(existing);
            }
        }
        return hit[0] ? new HomeRow(id, title, next) : this;
    }

    private static void require(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " is required");
        }
    }
}
