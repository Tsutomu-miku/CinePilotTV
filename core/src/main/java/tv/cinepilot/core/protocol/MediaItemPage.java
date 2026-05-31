package tv.cinepilot.core.protocol;

import java.util.List;

public record MediaItemPage(
        List<MediaItemSummary> items,
        int totalRecordCount,
        int startIndex
) {
    public MediaItemPage {
        items = List.copyOf(items == null ? List.of() : items);
        if (totalRecordCount < 0) {
            throw new IllegalArgumentException("totalRecordCount must be zero or greater");
        }
        if (startIndex < 0) {
            throw new IllegalArgumentException("startIndex must be zero or greater");
        }
    }
}

