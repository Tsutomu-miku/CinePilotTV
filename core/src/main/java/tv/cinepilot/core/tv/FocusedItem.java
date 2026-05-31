package tv.cinepilot.core.tv;

public record FocusedItem(
        String rowId,
        String itemId
) {
    public FocusedItem {
        require(rowId, "rowId");
        require(itemId, "itemId");
    }

    private static void require(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " is required");
        }
    }
}

