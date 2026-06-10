package tv.cinepilot.tv.details

import android.view.View
import android.widget.ImageView
import androidx.activity.ComponentActivity
import tv.cinepilot.core.protocol.MediaItemSummary
import tv.cinepilot.core.protocol.MediaPerson
import tv.cinepilot.core.tv.HomeRow
import tv.cinepilot.core.tv.ShowStructure
import tv.cinepilot.tv.runtime.ArtworkTarget
import tv.cinepilot.tv.ui.DetailPresentation
import tv.cinepilot.tv.ui.InfuseAction
import tv.cinepilot.tv.ui.InfuseActionEmphasis
import tv.cinepilot.tv.ui.RowVisualStyle
import tv.cinepilot.tv.ui.TvIcon
import tv.cinepilot.tv.ui.detailsStage
import tv.cinepilot.tv.ui.HomeRowPresentation
import tv.cinepilot.tv.ui.mediaWallRow
import tv.cinepilot.tv.ui.toDetailPresentation

fun ComponentActivity.seriesDetailScreen(
    structure: ShowStructure,
    onOpenSeason: (MediaItemSummary) -> Unit,
    onSelectSeason: (MediaItemSummary) -> Unit = onOpenSeason,
    onExpandFoldedSeasons: () -> Unit = {},
    seasonStartedStatus: Map<String, Boolean> = emptyMap(),
    onOpenEpisode: (MediaItemSummary) -> Unit,
    loadPoster: (ImageView, MediaItemSummary, Int, Int) -> Unit,
    loadBackdrop: (ImageView, MediaItemSummary) -> Unit,
    loadArtwork: (ImageView, MediaItemSummary, ArtworkTarget, Int, Int) -> Unit,
    loadPerson: (ImageView, MediaPerson, Int, Int) -> Unit,
    onToggleFavorite: () -> Unit = {},
    onToggleWatched: () -> Unit = {},
    onProviderBadgeClick: (String) -> Unit = {},
    onPersonClick: (MediaPerson) -> Unit = {},
): View {
    val series = structure.series()
    val featured = structure.resumeEpisode() ?: structure.nextUp() ?: structure.episodes().firstOrNull()
    val presentation: DetailPresentation = series.toDetailPresentation(
        technicalTags = showTechnicalBadges(featured ?: series),
    )
    val actions = mutableListOf<InfuseAction>()
    featured?.let { episode ->
        val label = if (episode.hasResumePosition()) "继续观看" else "播放下一集"
        actions.add(InfuseAction(label, TvIcon.PLAY, InfuseActionEmphasis.PRIMARY) { onOpenEpisode(episode) })
    }
    actions.add(InfuseAction("查看季集", TvIcon.FORWARD, InfuseActionEmphasis.SECONDARY) {
        structure.selectedSeason()?.let(onOpenSeason)
    })
    actions.add(InfuseAction(
        if (series.userData().favorite()) "已收藏" else "收藏",
        TvIcon.HEART,
        InfuseActionEmphasis.SECONDARY,
        onToggleFavorite,
    ))
    actions.add(InfuseAction(
        if (series.userData().played()) "取消已看" else "标记已看",
        TvIcon.CHECK,
        InfuseActionEmphasis.SECONDARY,
        onToggleWatched,
    ))
    structure.nextUp()?.let { episode ->
        if (episode.id() != featured?.id()) {
            actions.add(InfuseAction("下一集", TvIcon.PLAY, InfuseActionEmphasis.QUIET) { onOpenEpisode(episode) })
        }
    }
    return detailsStage(series, loadBackdrop) {
        addView(showHero(
            title = series.name().ifBlank { "剧集详情" },
            meta = showMetaLine(series),
            badges = presentation.qualityBadges,
            overview = series.overview(),
            actions = actions,
            providerBadges = presentation.providerBadges,
            onProviderBadgeClick = onProviderBadgeClick,
        ))
        if (structure.seasons().isNotEmpty()) {
            addView(seasonRail(
                seasons = structure.seasons(),
                selectedSeason = structure.selectedSeason(),
                onOpenSeason = onOpenSeason,
                onSelectSeason = onSelectSeason,
                seasonsStartedStatus = seasonStartedStatus,
                onExpandFoldedSeasons = onExpandFoldedSeasons,
            ))
        }
        if (structure.episodes().isNotEmpty()) {
            addView(groupedEpisodes(
                sectionTitlePrefix = structure.selectedSeason()?.name()?.ifBlank { "当前季" } ?: "当前季",
                items = structure.episodes(),
                onOpenEpisode,
                loadArtwork,
            ))
        }
        peopleStrip(
            "演职员",
            peopleSummary(series).ifEmpty { featured?.let(::peopleSummary) ?: emptyList() },
            loadPerson,
            onPersonClick,
        )?.let(::addView)
    }
}

fun ComponentActivity.seasonDetailScreen(
    structure: ShowStructure,
    onOpenEpisode: (MediaItemSummary) -> Unit,
    loadPoster: (ImageView, MediaItemSummary, Int, Int) -> Unit,
    loadBackdrop: (ImageView, MediaItemSummary) -> Unit,
    loadArtwork: (ImageView, MediaItemSummary, ArtworkTarget, Int, Int) -> Unit,
    loadPerson: (ImageView, MediaPerson, Int, Int) -> Unit,
    onToggleFavorite: () -> Unit = {},
    onToggleWatched: () -> Unit = {},
    onProviderBadgeClick: (String) -> Unit = {},
    onPersonClick: (MediaPerson) -> Unit = {},
): View {
    val season = structure.selectedSeason() ?: structure.series()
    val featured = structure.resumeEpisode() ?: structure.nextUp() ?: structure.episodes().firstOrNull()
    val heroItem = featured ?: season
    val presentation: DetailPresentation = heroItem.toDetailPresentation(
        technicalTags = showTechnicalBadges(heroItem),
    )
    val actions = mutableListOf<InfuseAction>()
    featured?.let { episode ->
        val label = if (episode.hasResumePosition()) "继续播放" else "播放本集"
        actions.add(InfuseAction(label, TvIcon.PLAY, InfuseActionEmphasis.PRIMARY) { onOpenEpisode(episode) })
    }
    actions.add(InfuseAction(
        if (season.userData().favorite()) "已收藏" else "收藏",
        TvIcon.HEART,
        InfuseActionEmphasis.SECONDARY,
        onToggleFavorite,
    ))
    actions.add(InfuseAction(
        if (season.userData().played()) "取消已看" else "标记已看",
        TvIcon.CHECK,
        InfuseActionEmphasis.SECONDARY,
        onToggleWatched,
    ))
    return detailsStage(heroItem, loadBackdrop) {
        addView(showHero(
            title = showHeroTitle(season, featured),
            meta = showMetaLine(heroItem),
            badges = presentation.qualityBadges,
            overview = featured?.overview()?.ifBlank { season.overview() } ?: season.overview(),
            actions,
            providerBadges = presentation.providerBadges,
            onProviderBadgeClick = onProviderBadgeClick,
        ))
        if (structure.episodes().isNotEmpty()) {
            addView(groupedEpisodes("全部集数", structure.episodes(), onOpenEpisode, loadArtwork, wrap = true))
        }
        peopleStrip(
            "演职员",
            peopleSummary(heroItem).ifEmpty { peopleSummary(structure.series()) },
            loadPerson,
            onPersonClick,
        )?.let(::addView)
    }
}

/**
 * Split episodes into "未观看 / 已看过" subsections. If only one group is non-empty we collapse
 * the header and just render a single rail. Both groups are always expandable in-place so the
 * user doesn't have to leave the series detail page to explore a watched section.
 */
private fun ComponentActivity.groupedEpisodes(
    sectionTitlePrefix: String,
    items: List<MediaItemSummary>,
    onOpen: (MediaItemSummary) -> Unit,
    loadArtwork: (ImageView, MediaItemSummary, ArtworkTarget, Int, Int) -> Unit,
    wrap: Boolean = false,
): View {
    val unwatched = items.filterNot { it.userData().played() }
    val watched = items.filter { it.userData().played() }
    val root = android.widget.LinearLayout(this).apply {
        orientation = android.widget.LinearLayout.VERTICAL
    }
    if (unwatched.isNotEmpty()) {
        val title = if (watched.isEmpty()) sectionTitlePrefix else "$sectionTitlePrefix · 未观看"
        root.addView(showEpisodes(title, unwatched, wrap, onOpen, loadArtwork))
    }
    if (watched.isNotEmpty()) {
        val title = if (unwatched.isEmpty()) sectionTitlePrefix else "$sectionTitlePrefix · 已看过"
        root.addView(showEpisodes(title, watched, wrap, onOpen, loadArtwork))
    }
    return root
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
