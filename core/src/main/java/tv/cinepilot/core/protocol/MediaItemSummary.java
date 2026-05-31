package tv.cinepilot.core.protocol;

import java.util.List;
import java.util.Map;
import tv.cinepilot.core.AndroidCollections;

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
        String seriesId,
        String overview,
        List<String> genres,
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
        if (seriesId == null) {
            seriesId = "";
        }
        if (overview == null) {
            overview = "";
        }
        genres = AndroidCollections.listCopy(genres);
        imageTags = AndroidCollections.mapCopy(imageTags);
    }

    public boolean hasResumePosition() {
        return userData.playbackPositionTicks() > 0;
    }

    public MediaItemSummary(
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
            String overview,
            List<String> genres,
            UserItemData userData,
            Map<String, String> imageTags
    ) {
        this(
                id,
                parentId,
                name,
                type,
                folder,
                playable,
                runTimeTicks,
                productionYear,
                indexNumber,
                parentIndexNumber,
                seriesName,
                "",
                overview,
                genres,
                userData,
                imageTags
        );
    }

    private static void require(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " is required");
        }
    }
}
