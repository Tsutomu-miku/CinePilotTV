package tv.cinepilot.core.protocol;

import java.util.List;
import java.util.LinkedHashMap;
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
        List<String> backdropImageTags,
        Map<String, String> providerIds
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
        providerIds = AndroidCollections.mapCopy(providerIds);
    }

    public boolean hasResumePosition() {
        return userData.playbackPositionTicks() > 0;
    }

    public boolean hasBackdropArtwork() {
        return !backdropImageTags.isEmpty() || imageTags.containsKey("Thumb") || imageTags.containsKey("Primary");
    }

    public String tmdbId() {
        return providerId("Tmdb");
    }

    public String imdbId() {
        return providerId("Imdb");
    }

    public String tvdbId() {
        return providerId("Tvdb");
    }

    public String tmdbCollectionId() {
        return providerId("TmdbCollection");
    }

    public String providerId(String key) {
        if (providerIds == null || key == null) {
            return "";
        }
        String value = providerIds.get(key);
        return value == null ? "" : value;
    }

    public MediaItemSummary withUserData(UserItemData updated) {
        return new MediaItemSummary(
                id, parentId, name, type, folder, playable,
                runTimeTicks, productionYear, indexNumber, parentIndexNumber,
                seriesName, seriesId, overview, genres, premiereDate,
                communityRating, officialRating, people, mediaStreams,
                updated == null ? userData : updated,
                imageTags, backdropImageTags, providerIds
        );
    }

    // ---- backward-compatible constructors -------------------------------------------------

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
                id, parentId, name, type, folder, playable,
                runTimeTicks, productionYear, indexNumber, parentIndexNumber,
                seriesName, seriesId, overview, genres,
                "", null, "",
                AndroidCollections.emptyList(), AndroidCollections.emptyList(),
                userData, imageTags, AndroidCollections.emptyList(), new LinkedHashMap<>()
        );
    }

    /** Backward-compatible 22-arg constructor (without providerIds, added in P1 batch 7). */
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
            String premiereDate,
            Double communityRating,
            String officialRating,
            List<MediaPerson> people,
            List<MediaStreamInfo> mediaStreams,
            UserItemData userData,
            Map<String, String> imageTags,
            List<String> backdropImageTags
    ) {
        this(
                id, parentId, name, type, folder, playable,
                runTimeTicks, productionYear, indexNumber, parentIndexNumber,
                seriesName, seriesId, overview, genres,
                premiereDate, communityRating, officialRating,
                people, mediaStreams,
                userData, imageTags, backdropImageTags, new LinkedHashMap<>()
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
                id, parentId, name, type, folder, playable,
                runTimeTicks, productionYear, indexNumber, parentIndexNumber,
                seriesName, "", overview, genres,
                "", null, "",
                AndroidCollections.emptyList(), AndroidCollections.emptyList(),
                userData, imageTags, AndroidCollections.emptyList(), new LinkedHashMap<>()
        );
    }

    private static void require(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " is required");
        }
    }
}
