package tv.cinepilot.tv.details

import android.view.View
import android.widget.ImageView
import androidx.activity.ComponentActivity
import tv.cinepilot.core.protocol.MediaItemSummary
import tv.cinepilot.core.tv.HomeRow
import tv.cinepilot.core.tv.ShowStructure
import tv.cinepilot.tv.runtime.ArtworkTarget
import tv.cinepilot.tv.ui.InfuseAction
import tv.cinepilot.tv.ui.InfuseActionEmphasis
import tv.cinepilot.tv.ui.RowVisualStyle
import tv.cinepilot.tv.ui.TvIcon
import tv.cinepilot.tv.ui.detailsInfoSections
import tv.cinepilot.tv.ui.detailsHero
import tv.cinepilot.tv.ui.detailsStage
import tv.cinepilot.tv.ui.mediaWallGrid
import tv.cinepilot.tv.ui.toDetailPresentation
import tv.cinepilot.tv.ui.HomeRowPresentation

fun ComponentActivity.seriesDetailScreen(
    structure: ShowStructure,
    onOpenSeason: (MediaItemSummary) -> Unit,
    onOpenEpisode: (MediaItemSummary) -> Unit,
    loadPoster: (ImageView, MediaItemSummary, Int, Int) -> Unit,
    loadBackdrop: (ImageView, MediaItemSummary) -> Unit,
    loadArtwork: (ImageView, MediaItemSummary, ArtworkTarget, Int, Int) -> Unit,
): View {
    val series = structure.series()
    val actions = mutableListOf(
        InfuseAction("查看季集", TvIcon.FORWARD, InfuseActionEmphasis.PRIMARY) {
            structure.selectedSeason()?.let(onOpenSeason)
        },
    )
    structure.nextUp()?.let { episode ->
        actions.add(InfuseAction("下一集", TvIcon.PLAY, InfuseActionEmphasis.QUIET) { onOpenEpisode(episode) })
    }
    return detailsStage(series, loadBackdrop) {
        addView(detailsHero(series, series.toDetailPresentation(emptyList()), actions, actions.first(), loadPoster))
        if (structure.seasons().isNotEmpty()) {
            addView(showGrid("季", structure.seasons(), RowVisualStyle.POSTER_RAIL, onOpenSeason, loadArtwork))
        }
        if (structure.episodes().isNotEmpty()) {
            addView(showGrid(
                structure.selectedSeason()?.name()?.ifBlank { "集数" } ?: "集数",
                structure.episodes(),
                RowVisualStyle.LANDSCAPE_RAIL,
                onOpenEpisode,
                loadArtwork,
            ))
        }
        detailsInfoSections(null, series.toDetailPresentation(emptyList())).forEach(::addView)
    }
}

fun ComponentActivity.seasonDetailScreen(
    structure: ShowStructure,
    onOpenEpisode: (MediaItemSummary) -> Unit,
    loadPoster: (ImageView, MediaItemSummary, Int, Int) -> Unit,
    loadBackdrop: (ImageView, MediaItemSummary) -> Unit,
    loadArtwork: (ImageView, MediaItemSummary, ArtworkTarget, Int, Int) -> Unit,
): View {
    val season = structure.selectedSeason() ?: structure.series()
    val firstEpisode = structure.resumeEpisode() ?: structure.episodes().firstOrNull()
    val actions = listOfNotNull(
        firstEpisode?.let { episode ->
            InfuseAction("播放本季", TvIcon.PLAY, InfuseActionEmphasis.PRIMARY) { onOpenEpisode(episode) }
        },
    )
    return detailsStage(season, loadBackdrop) {
        addView(detailsHero(
            season,
            season.toDetailPresentation(emptyList()),
            actions,
            InfuseAction("选集", TvIcon.FORWARD, InfuseActionEmphasis.PRIMARY) {},
            loadPoster,
        ))
        if (structure.episodes().isNotEmpty()) {
            addView(showGrid("选集", structure.episodes(), RowVisualStyle.LANDSCAPE_RAIL, onOpenEpisode, loadArtwork))
        }
        detailsInfoSections(null, season.toDetailPresentation(emptyList())).forEach(::addView)
    }
}

private fun ComponentActivity.showGrid(
    title: String,
    items: List<MediaItemSummary>,
    style: RowVisualStyle,
    onOpen: (MediaItemSummary) -> Unit,
    loadArtwork: (ImageView, MediaItemSummary, ArtworkTarget, Int, Int) -> Unit,
): View {
    return mediaWallGrid(
        presentation = HomeRowPresentation(
            row = HomeRow("detail:${title}", title, items),
            title = title,
            visualStyle = style,
            wrapItems = true,
        ),
        onCell = { _, _ -> },
        onFocus = { _, _ -> },
        onOpen = { _, item -> onOpen(item) },
        loadArtwork = loadArtwork,
    )
}
