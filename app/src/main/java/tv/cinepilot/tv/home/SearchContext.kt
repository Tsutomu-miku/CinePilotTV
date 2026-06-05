package tv.cinepilot.tv.home

import tv.cinepilot.core.tv.TvAppState

fun TvAppState.activeSearchTerm(): String {
    val rowId = homeRows().firstOrNull { row -> row.id().startsWith(SEARCH_ROW_PREFIX) }?.id() ?: return ""
    val filterSeparator = rowId.indexOf(':', SEARCH_ROW_PREFIX.length)
    return if (filterSeparator >= 0) rowId.substring(filterSeparator + 1) else ""
}

private const val SEARCH_ROW_PREFIX = "search:"
