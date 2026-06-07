package tv.cinepilot.tv.details

import android.view.View
import android.widget.ImageView
import androidx.activity.ComponentActivity
import tv.cinepilot.core.protocol.MediaItemSummary
import tv.cinepilot.core.protocol.MediaPerson
import tv.cinepilot.core.tv.HomeRow
import tv.cinepilot.core.tv.ShowStructure
import tv.cinepilot.tv.runtime.ArtworkTarget
import tv.cinepilot.tv.ui.InfuseAction
import tv.cinepilot.tv.ui.InfuseActionEmphasis
import tv.cinepilot.tv.ui.RowVisualStyle
import tv.cinepilot.tv.ui.TvIcon
import tv.cinepilot.tv.ui.detailsStage
import tv.cinepilot.tv.ui.HomeRowPresentation
import tv.cinepilot.tv.ui.mediaWallRow

fun ComponentActivity.seriesDetailScreen(
    structure: ShowStructure,
    onOpenSeason: (MediaItemSummary) -> Unit,
    onOpenEpisode: (MediaItemSummary) -> Unit,
    loadPoster: (ImageView, MediaItemSummary, Int, Int) -> Unit,
    loadBackdrop: (ImageView, MediaItemSummary) -> Unit,
    loadArtwork: (ImageView, MediaItemSummary, ArtworkTarget, Int, Int) -> Unit,
    loadPerson: (ImageView, MediaPerson, Int, Int) -> Unit,
): View {
    val series = structure.series()
    val featured = structure.resumeEpisode() ?: structure.nextUp() ?: structure.episodes().firstOrNull()
    val actions = mutableListOf<InfuseAction>()
    featured?.let { episode ->
        val label = if (episode.hasResumePosition()) "继续观看" else "播放下一集"
        actions.add(InfuseAction(label, TvIcon.PLAY, InfuseActionEmphasis.PRIMARY) { onOpenEpisode(episode) })
    }
    actions.add(InfuseAction("查看季集", TvIcon.FORWARD, InfuseActionEmphasis.SECONDARY) {
        structure.selectedSeason()?.let(onOpenSeason)
    })
    structure.nextUp()?.let { episode ->
        if (episode.id() != featured?.id()) {
            actions.add(InfuseAction("下一集", TvIcon.PLAY, InfuseActionEmphasis.QUIET) { onOpenEpisode(episode) })
        }
    }
    return detailsStage(series, loadBackdrop) {
        addView(showHero(
            title = series.name().ifBlank { "剧集详情" },
            meta = showMetaLine(series),
            badges = featured?.let(::showTechnicalBadges) ?: emptyList(),
            overview = series.overview(),
            actions = actions,
        ))
        if (structure.seasons().isNotEmpty()) {
            addView(seasonRail(structure.seasons(), structure.selectedSeason(), onOpenSeason))
        }
        if (structure.episodes().isNotEmpty()) {
            addView(showEpisodes(
                structure.selectedSeason()?.name()?.ifBlank { "当前季" } ?: "当前季",
                structure.episodes(),
                wrap = false,
                onOpenEpisode,
                loadArtwork,
            ))
        }
        peopleStrip("演职员", peopleSummary(series).ifEmpty { featured?.let(::peopleSummary) ?: emptyList() }, loadPerson)
            ?.let(::addView)
    }
}

fun ComponentActivity.seasonDetailScreen(
    structure: ShowStructure,
    onOpenEpisode: (MediaItemSummary) -> Unit,
    loadPoster: (ImageView, MediaItemSummary, Int, Int) -> Unit,
    loadBackdrop: (ImageView, MediaItemSummary) -> Unit,
    loadArtwork: (ImageView, MediaItemSummary, ArtworkTarget, Int, Int) -> Unit,
    loadPerson: (ImageView, MediaPerson, Int, Int) -> Unit,
): View {
    val season = structure.selectedSeason() ?: structure.series()
    val featured = structure.resumeEpisode() ?: structure.nextUp() ?: structure.episodes().firstOrNull()
    val heroItem = featured ?: season
    val actions = listOfNotNull(featured?.let { episode ->
        val label = if (episode.hasResumePosition()) "继续播放" else "播放本集"
        InfuseAction(label, TvIcon.PLAY, InfuseActionEmphasis.PRIMARY) { onOpenEpisode(episode) }
    })
    return detailsStage(heroItem, loadBackdrop) {
        addView(showHero(
            title = showHeroTitle(season, featured),
            meta = showMetaLine(heroItem),
            badges = showTechnicalBadges(heroItem),
            overview = featured?.overview()?.ifBlank { season.overview() } ?: season.overview(),
            actions,
        ))
        if (structure.episodes().isNotEmpty()) {
            addView(showEpisodes("全部集数", structure.episodes(), true, onOpenEpisode, loadArtwork))
        }
        peopleStrip("演职员", peopleSummary(heroItem).ifEmpty { peopleSummary(structure.series()) }, loadPerson)
            ?.let(::addView)
    }
}

private fun ComponentActivity.showEpisodes(
    title: String,
    items: List<MediaItemSummary>,
    wrap: Boolean,
    onOpen: (MediaItemSummary) -> Unit,
    loadArtwork: (ImageView, MediaItemSummary, ArtworkTarget, Int, Int) -> Unit,
): View {
    return mediaWallRow(
        presentation = HomeRowPresentation(
            row = HomeRow("detail:${title}", title, items),
            title = title,
            visualStyle = RowVisualStyle.LANDSCAPE_RAIL,
            wrapItems = wrap,
        ),
        onCell = { _, _ -> },
        onFocus = { _, _ -> },
        onOpen = { _, item -> onOpen(item) },
        loadArtwork = loadArtwork,
    )
}
