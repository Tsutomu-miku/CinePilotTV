package tv.cinepilot.tv.compose.screens.home

import tv.cinepilot.core.protocol.MediaItemSummary
import tv.cinepilot.core.tv.HomeRow
import tv.cinepilot.core.tv.TvAppState
import tv.cinepilot.tv.home.activeSearchTerm

internal data class HomeFocusAddress(val rowIndex: Int, val itemIndex: Int)

internal fun initialFocusAddress(state: TvAppState, rows: List<HomeRow>): HomeFocusAddress {
    val focus = state.focus()
    if (focus != null) {
        rows.forEachIndexed { rowIndex, row ->
            if (row.id() == focus.rowId()) {
                val itemIndex = row.items().indexOfFirst { item -> item.id() == focus.itemId() }
                if (itemIndex >= 0) return HomeFocusAddress(rowIndex, itemIndex)
            }
        }
    }
    return HomeFocusAddress(0, 0)
}

internal fun homeTitle(state: TvAppState): String {
    if (state.activeSearchTerm().isNotBlank()) return "搜索：${state.activeSearchTerm()}"
    val overviewTitle = state.homeRows().firstOrNull { row -> row.id().startsWith("overview:") }?.title().orEmpty()
    return overviewTitle.ifBlank { "首页" }
}

internal fun libraryOverviewTitle(item: MediaItemSummary): String {
    val name = item.name().ifBlank { "媒体库" }
    return "$name · 总览"
}

internal fun MediaItemSummary.isSeriesLibrary(): Boolean {
    val name = name()
    return name.contains("电视") || name.contains("Series", ignoreCase = true) ||
        name.contains("Shows", ignoreCase = true)
}
