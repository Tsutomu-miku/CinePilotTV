package tv.cinepilot.tv.settings

enum class AppTheme(
    val id: String,
    val label: String,
    val description: String,
) {
    CINEPILOT("cinepilot", "CinePilot 青绿", "默认深色主题"),
    JELLYFIN("jellyfin", "Jellyfin 蓝", "蓝色焦点与强调色"),
    EMBY("emby", "Emby 绿", "绿色焦点与强调色");

    companion object {
        fun fromId(id: String?): AppTheme {
            return values().firstOrNull { it.id == id } ?: CINEPILOT
        }
    }
}
