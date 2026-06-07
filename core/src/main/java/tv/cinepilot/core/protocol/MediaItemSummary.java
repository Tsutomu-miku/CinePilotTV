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
        String premiereDate,
        Double communityRating,
        String officialRating,
        List<MediaPerson> people,
        List<MediaStreamInfo> mediaStreams,
        UserItemData userData,
        Map<String, String> imageTags,
        List<String> backdropImageTags
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
        if (premiereDate == null) {
            premiereDate = "";
        }
        if (officialRating == null) {
            officialRating = "";
        }
        genres = AndroidCollections.listCopy(genres);
        people = AndroidCollections.listCopy(people);
        mediaStreams = AndroidCollections.listCopy(mediaStreams);
        imageTags = AndroidCollections.mapCopy(imageTags);
        backdropImageTags = AndroidCollections.listCopy(backdropImageTags);
    }

    public boolean hasResumePosition() {
        return userData.playbackPositionTicks() > 0;
    }

    public boolean hasBackdropArtwork() {
        return !backdropImageTags.isEmpty() || imageTags.containsKey("Thumb") || imageTags.containsKey("Primary");
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
            String seriesId,
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
                seriesId,
                overview,
                genres,
                "",
                null,
                "",
                AndroidCollections.emptyList(),
                AndroidCollections.emptyList(),
                userData,
                imageTags,
                AndroidCollections.emptyList()
        );
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
                "",
                null,
                "",
                AndroidCollections.emptyList(),
                AndroidCollections.emptyList(),
                userData,
                imageTags,
                AndroidCollections.emptyList()
        );
    }

    private static void require(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " is required");
        }
    }
}
