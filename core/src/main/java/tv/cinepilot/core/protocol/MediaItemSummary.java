package tv.cinepilot.core.protocol;

import java.util.Map;

public record MediaItemSummary(
        String id,
        String parentId,
        String name,
        MediaItemType type,
        boolean folder,
        boolean playable,
        Long runTimeTicks,
        Integer productionYear,
        Integer indexNumber,
        Integer parentIndexNumber,
        String seriesName,
        UserItemData userData,
        Map<String, String> imageTags
) {
    public MediaItemSummary {
        require(id, "id");
        if (parentId == null) {
            parentId = "";
        }
        if (name == null) {
            name = "";
        }
        if (type == null) {
            type = MediaItemType.UNKNOWN;
        }
        if (userData == null) {
            userData = UserItemData.empty();
        }
        imageTags = Map.copyOf(imageTags == null ? Map.of() : imageTags);
    }

    public boolean hasResumePosition() {
        return userData.playbackPositionTicks() > 0;
    }

    private static void require(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " is required");
        }
    }
}

