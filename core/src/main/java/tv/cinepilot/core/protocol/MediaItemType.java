package tv.cinepilot.core.protocol;

public enum MediaItemType {
    COLLECTION_FOLDER("collectionfolder"),
    MOVIE("movie"),
    SERIES("series"),
    SEASON("season"),
    EPISODE("episode"),
    VIDEO("video"),
    FOLDER("folder"),
    PLAYLIST("playlist"),
    UNKNOWN("unknown");

    private final String wireName;

    MediaItemType(String wireName) {
        this.wireName = wireName;
    }

    public String wireName() {
        return wireName;
    }

    public static MediaItemType fromWireName(String value) {
        if (value == null || value.isBlank()) {
            return UNKNOWN;
        }
        return switch (value.toLowerCase()) {
            case "collectionfolder" -> COLLECTION_FOLDER;
            case "movie" -> MOVIE;
            case "series" -> SERIES;
            case "season" -> SEASON;
            case "episode" -> EPISODE;
            case "video" -> VIDEO;
            case "folder" -> FOLDER;
            case "playlist" -> PLAYLIST;
            default -> UNKNOWN;
        };
    }
}

