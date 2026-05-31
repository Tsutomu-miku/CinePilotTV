package tv.cinepilot.core.protocol;

public enum MediaItemType {
    COLLECTION_FOLDER,
    MOVIE,
    SERIES,
    SEASON,
    EPISODE,
    VIDEO,
    FOLDER,
    UNKNOWN;

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
            default -> UNKNOWN;
        };
    }
}

