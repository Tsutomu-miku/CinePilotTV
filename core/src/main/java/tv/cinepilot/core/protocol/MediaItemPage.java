package tv.cinepilot.core.protocol;

import java.util.List;
import tv.cinepilot.core.AndroidCollections;

public record MediaItemPage(
        List<MediaItemSummary> items,
        int totalRecordCount,
        int startIndex
) {
    public MediaItemPage {
        items = AndroidCollections.listCopy(items);
        if (totalRecordCount < 0) {
            throw new IllegalArgumentException("totalRecordCount must be zero or greater");
        }
        if (startIndex < 0) {
            throw new IllegalArgumentException("startIndex must be zero or greater");
        }
    }
}
