package tv.cinepilot.tv.compose.screens.home

import androidx.compose.runtime.Composable
import tv.cinepilot.core.protocol.MediaItemSummary
import tv.cinepilot.core.protocol.MediaItemType
import tv.cinepilot.core.protocol.MediaTicks
import tv.cinepilot.tv.compose.components.chips.CardBadgeChip
import tv.cinepilot.tv.compose.theme.CinePilotPalette
import tv.cinepilot.tv.compose.theme.TvText

@Composable
internal fun CardBadgeChip(text: String, palette: CinePilotPalette) {
    tv.cinepilot.tv.compose.components.chips.CardBadgeChip(text, palette)
}

internal fun MediaItemSummary.cardMetaLine(): String {
    if (type() == MediaItemType.EPISODE) {
        val season = parentIndexNumber()?.let { "S$it" }.orEmpty()
        val episode = indexNumber()?.let { "E$it" }.orEmpty()
        return listOf(season, episode).filter { it.isNotBlank() }.joinToString(" · ").ifBlank {
            seriesName().ifBlank { productionYear()?.toString().orEmpty() }
        }
    }
    val year = productionYear()?.toString().orEmpty()
    val duration = runTimeTicks()?.let(::durationLabel).orEmpty()
    return listOf(year, duration).filter { it.isNotBlank() }.joinToString(" · ")
}

private fun durationLabel(ticks: Long): String {
    val minutes = (MediaTicks.toSeconds(ticks) / 60L).coerceAtLeast(0L)
    val hours = minutes / 60L
    val rest = minutes % 60L
    return when {
        hours > 0L && rest > 0L -> "${hours}小时 ${rest}分钟"
        hours > 0L -> "${hours}小时"
        rest > 0L -> "${rest}分钟"
        else -> ""
    }
}

internal fun MediaItemSummary.resumeFraction(): Float {
    val duration = runTimeTicks() ?: return 0f
    if (duration <= 0L || !hasResumePosition()) return 0f
    return (userData().playbackPositionTicks().toFloat() / duration.toFloat()).coerceIn(0f, 1f)
}
