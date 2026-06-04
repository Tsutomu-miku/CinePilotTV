package tv.cinepilot.core.tv;

public enum SearchFilter {
    ALL("全部", "Movie,Episode,Series,Video"),
    MOVIES("电影", "Movie"),
    SERIES("剧集", "Series"),
    EPISODES("单集", "Episode"),
    VIDEOS("视频", "Video");

    private final String label;
    private final String includeItemTypes;

    SearchFilter(String label, String includeItemTypes) {
        this.label = label;
        this.includeItemTypes = includeItemTypes;
    }

    public String label() {
        return label;
    }

    public String includeItemTypes() {
        return includeItemTypes;
    }

    public static SearchFilter safe(SearchFilter value) {
        return value == null ? ALL : value;
    }
}
