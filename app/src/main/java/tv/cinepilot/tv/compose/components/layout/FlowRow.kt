package tv.cinepilot.tv.compose.components.layout

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * A flow row layout that arranges items in rows, wrapping to a new row
 * when items exceed the available width.
 *
 * Similar to a horizontal Row that automatically wraps, like a flexbox row.
 *
 * @param horizontalGap horizontal spacing between items in the same row
 * @param verticalGap vertical spacing between rows
 */
@Composable
fun FlowRow(
    modifier: Modifier = Modifier,
    horizontalGap: Dp = 8.dp,
    verticalGap: Dp = 8.dp,
    content: @Composable () -> Unit,
) {
    Layout(
        content = content,
        modifier = modifier,
    ) { measurables, constraints ->
        val maxWidth = constraints.maxWidth
        val hGapPx = horizontalGap.roundToPx()
        val vGapPx = verticalGap.roundToPx()

        val rows = mutableListOf<RowMeasurements>()
        var currentRow = RowMeasurements()
        var currentRowWidth = 0

        for (measurable in measurables) {
            val placeable = measurable.measure(Constraints(maxWidth = maxWidth))
            val itemWidth = placeable.width
            val itemHeight = placeable.height

            if (currentRow.isNotEmpty() && currentRowWidth + hGapPx + itemWidth > maxWidth) {
                rows.add(currentRow)
                currentRow = RowMeasurements()
                currentRowWidth = 0
            }
            if (currentRow.isNotEmpty()) {
                currentRowWidth += hGapPx
            }
            currentRowWidth += itemWidth
            currentRow.items.add(PlaceableItem(placeable, itemWidth, itemHeight))
        }
        if (currentRow.isNotEmpty()) {
            rows.add(currentRow)
        }

        val totalHeight = rows.sumOf { it.height } + (rows.size - 1) * vGapPx
        layout(maxWidth, totalHeight.coerceAtLeast(0)) {
            var y = 0
            for (row in rows) {
                var x = 0
                for (item in row.items) {
                    item.placeable.placeRelative(x, y)
                    x += item.width + hGapPx
                }
                y += row.height + vGapPx
            }
        }
    }
}

private class RowMeasurements {
    val items = mutableListOf<PlaceableItem>()
    val height: Int get() = items.maxOfOrNull { it.height } ?: 0
    fun isNotEmpty(): Boolean = items.isNotEmpty()
}

private data class PlaceableItem(
    val placeable: Placeable,
    val width: Int,
    val height: Int,
)
