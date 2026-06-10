package tv.cinepilot.tv.ui

import tv.cinepilot.core.protocol.MediaItemSummary
import tv.cinepilot.core.protocol.MediaItemType
import tv.cinepilot.core.tv.HomeRow

enum class RowVisualStyle {
    COLLECTION_RAIL,
    LANDSCAPE_RAIL,
    POSTER_RAIL,
}

data class HomeRowPresentation(
    val row: HomeRow,
    val title: String,
    val visualStyle: RowVisualStyle,
    val wrapItems: Boolean,
) {
    val showTitle: Boolean = title.isNotBlank()
}

fun HomeRow.toHomeRowPresentation(): HomeRowPresentation {
    return HomeRowPresentation(
        row = this,
        title = displayTitle(),
        visualStyle = rowVisualStyle(),
        wrapItems = shouldWrapItems(),
    )
}

private fun HomeRow.displayTitle(): String {
    return if (id() == "views" || id().startsWith("overview:title")) {
        ""
    } else {
        title()
    }
}

private fun HomeRow.rowVisualStyle(): RowVisualStyle {
    return when {
        id() == "views" || id() == "collections" -> RowVisualStyle.COLLECTION_RAIL
        id() == "resume" || id() == "next-up" -> RowVisualStyle.LANDSCAPE_RAIL
        id().startsWith("overview:resume") || id().startsWith("overview:next-up") -> RowVisualStyle.LANDSCAPE_RAIL
        id().startsWith("latest:") || id().startsWith("filtered:") || id().startsWith("overview:") -> items().majorityStyle()
        id().startsWith("search:") || id().startsWith("folder:") -> items().majorityStyle()
        else -> items().majorityStyle()
    }
}

private fun HomeRow.shouldWrapItems(): Boolean {
    return id().startsWith("search:") || id().startsWith("folder:") || id().startsWith("overview:unplayed")
}

private fun List<MediaItemSummary>.majorityStyle(): RowVisualStyle {
    val landscape = count { it.prefersLandscapeArtwork() }
    val poster = size - landscape
    return if (landscape > poster) RowVisualStyle.LANDSCAPE_RAIL else RowVisualStyle.POSTER_RAIL
}

private fun MediaItemSummary.prefersLandscapeArtwork(): Boolean {
    return when (type()) {
        MediaItemType.EPISODE,
        MediaItemType.VIDEO -> true
        MediaItemType.MOVIE,
        MediaItemType.SERIES,
        MediaItemType.SEASON,
        MediaItemType.COLLECTION_FOLDER,
        MediaItemType.FOLDER,
        MediaItemType.UNKNOWN -> false
    }
}
