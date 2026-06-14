package tv.cinepilot.tv.compose.screens

import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.Image
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.DataSaverOff
import androidx.compose.material.icons.outlined.Title
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.ViewList
import androidx.compose.material.icons.outlined.Tv
import androidx.compose.material.icons.outlined.SkipNext
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.PlaylistAdd
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import tv.cinepilot.core.protocol.AuthenticatedServer
import tv.cinepilot.core.protocol.MediaItemSummary
import tv.cinepilot.core.protocol.MediaSourceInfo
import tv.cinepilot.core.protocol.MediaStreamInfo
import tv.cinepilot.core.protocol.MediaStreamType
import tv.cinepilot.core.protocol.PlaybackInfo
import tv.cinepilot.core.protocol.PlaybackSelectionPreferences
import tv.cinepilot.tv.compose.artwork.CinePilotAsyncImage
import tv.cinepilot.tv.compose.artwork.rememberArtworkRequest
import tv.cinepilot.tv.compose.components.FocusSurface
import tv.cinepilot.tv.compose.components.MediaRail
import tv.cinepilot.tv.compose.components.PosterCard
import tv.cinepilot.tv.compose.components.TvActionButton
import tv.cinepilot.tv.compose.theme.CinePilotPalette
import tv.cinepilot.tv.compose.theme.TvDp
import tv.cinepilot.tv.compose.theme.TvText
import tv.cinepilot.tv.details.DetailTrackSelection
import tv.cinepilot.tv.runtime.ArtworkRequestFactory
import tv.cinepilot.tv.runtime.ArtworkRequestSpec
import tv.cinepilot.tv.runtime.ArtworkTarget
import tv.cinepilot.tv.ui.browseChildrenLabel
import tv.cinepilot.tv.ui.isEpisode
import tv.cinepilot.tv.ui.mediaTechnicalPills
import tv.cinepilot.tv.ui.sourceLabel
import tv.cinepilot.tv.ui.streamLabel
import tv.cinepilot.tv.ui.toDetailPresentation

// ── Local design constants ────────────────────────────────────────────────

private val DetailPosterWidth = 210.dp
private val DetailPosterHeight = 315.dp

private val DoubanGreen = Color(0xFF2ECC71)
private val ImdbGold = Color(0xFFF5C518)

private val ActionButtonWidth = 80.dp
private val ActionButtonHeight = 100.dp
private val ActionButtonIconSize = 28.dp
private val ActionButtonGap = 12.dp
private val ActionButtonProgressHeight = 3.dp

private val EpisodeCardWidth = 280.dp
private val EpisodeCardHeight = 158.dp
private val EpisodeCardGap = 14.dp
private val EpisodeProgressHeight = 3.dp

private val PillChipHeight = 26.dp
private val PillChipRadius = 8.dp

private val RatingBadgeHeight = 34.dp
private val RatingBadgeIconWidth = 58.dp

private val TrackSelectorHeight = 48.dp
private val TrackSelectorIconSize = 20.dp

private val DetailTitleSize = 34.sp
private val DetailMetaSize = 13.sp
private val OverviewTextSize = 12.sp

// ── Top-level screen ────────────────────────────────────────────────────

@Composable
fun ComposeDetailsScreen(
    palette: CinePilotPalette,
    owner: ComponentActivity,
    artworkFactory: ArtworkRequestFactory,
    authenticated: AuthenticatedServer?,
    item: MediaItemSummary,
    playbackInfo: PlaybackInfo?,
    siblingEpisodes: List<MediaItemSummary>,
    sameCollectionItems: List<MediaItemSummary>,
    trackSelection: DetailTrackSelection,
    supportedHdrTypes: Set<String>,
    supportedPassthroughCodecs: Set<String>,
    onPreparePlayback: (PlaybackSelectionPreferences?) -> Unit,
    onTrackSelection: (DetailTrackSelection) -> Unit,
    onSubtitleStyle: () -> Unit,
    onPlaybackSpeed: () -> Unit,
    onSeriesNextUp: () -> Unit,
    onOpenEpisodePicker: () -> Unit,
    onOpenSeries: () -> Unit,
    onOpenEpisode: (MediaItemSummary) -> Unit,
    onOpenCollectionItem: (MediaItemSummary) -> Unit,
    onOpenFolder: () -> Unit,
    onToggleFavorite: () -> Unit,
    onToggleWatched: () -> Unit,
    onSetUserRating: (Double?) -> Unit,
    onOpenProviderIdsEditor: () -> Unit,
    onProviderBadgeClick: (String) -> Unit,
    onChooseDownloadQuality: (Int) -> Unit,
    onManageOffline: () -> Unit,
    offlineActionLabel: String?,
    offlineActionIsReady: Boolean,
    onAddToPlaylist: () -> Unit,
    onSearchSubtitles: () -> Unit,
    hasSubtitleSearch: Boolean,
) {
    val technicalInfo = mediaTechnicalPills(playbackInfo, supportedHdrTypes, supportedPassthroughCodecs)
    val presentation = item.toDetailPresentation(technicalTags = technicalInfo)
    val backdrop = rememberArtworkRequest(
        factory = artworkFactory,
        authenticated = authenticated,
        item = item,
        target = ArtworkTarget.BACKDROP,
        width = 1280,
        height = 720,
    )
    val actions = rememberDetailActions(
        item = item,
        onPreparePlayback = onPreparePlayback,
        onSubtitleStyle = onSubtitleStyle,
        onPlaybackSpeed = onPlaybackSpeed,
        onSeriesNextUp = onSeriesNextUp,
        onOpenEpisodePicker = onOpenEpisodePicker,
        onOpenSeries = onOpenSeries,
        onOpenFolder = onOpenFolder,
        onToggleFavorite = onToggleFavorite,
        onToggleWatched = onToggleWatched,
        onOpenProviderIdsEditor = onOpenProviderIdsEditor,
        onChooseDownloadQuality = onChooseDownloadQuality,
        onManageOffline = onManageOffline,
        offlineActionLabel = offlineActionLabel,
        offlineActionIsReady = offlineActionIsReady,
        onAddToPlaylist = onAddToPlaylist,
        onSearchSubtitles = onSearchSubtitles,
        hasSubtitleSearch = hasSubtitleSearch,
    )

    val playbackProgress = remember(item) {
        calculateProgress(item)
    }

    DetailsStage(
        backdrop = backdrop,
        palette = palette,
    ) {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(18.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = TvDp.ScreenX,
                top = TvDp.ScreenTop,
                end = TvDp.ScreenX,
                bottom = TvDp.ScreenBottom,
            ),
            modifier = Modifier.fillMaxSize(),
        ) {
            item {
                DetailsHero(
                    palette = palette,
                    artworkFactory = artworkFactory,
                    authenticated = authenticated,
                    item = item,
                    title = presentation.title,
                    contextLine = presentation.contextLine,
                    qualityBadges = presentation.qualityBadges,
                    providerBadges = presentation.providerBadges.map { it.label to it.externalUrl },
                    actions = actions,
                    playbackProgress = playbackProgress,
                    onProviderBadgeClick = onProviderBadgeClick,
                )
            }
            val hasTracks = playbackInfo?.mediaSources().orEmpty().isNotEmpty()
            if (hasTracks) {
                item {
                    DetailTrackOptions(
                        palette = palette,
                        playbackInfo = playbackInfo,
                        selection = trackSelection,
                        onSelection = onTrackSelection,
                    )
                }
            }
            if (presentation.overview.isNotBlank()) {
                item {
                    BasicText(
                        text = presentation.overview,
                        style = TextStyle(
                            color = palette.textMuted,
                            fontSize = OverviewTextSize,
                            lineHeight = (OverviewTextSize.value * 1.4f).sp,
                        ),
                    )
                }
            }
            if (item.isEpisode() && siblingEpisodes.isNotEmpty()) {
                item {
                    EpisodeRow(
                        palette = palette,
                        artworkFactory = artworkFactory,
                        authenticated = authenticated,
                        episodes = siblingEpisodes,
                        currentItem = item,
                        onEpisodeClick = { ep -> onOpenEpisode(ep) },
                    )
                }
            }
            val related = sameCollectionItems.filterNot { it.id() == item.id() }
            if (related.isNotEmpty()) {
                item {
                    DetailSection(title = "同系列其他", palette = palette) {
                        MediaRail(
                            palette = palette,
                            title = "",
                            items = related,
                            key = { it.id() },
                        ) { rel ->
                            PosterCard(
                                palette = palette,
                                item = rel,
                                artwork = rememberArtworkRequest(
                                    factory = artworkFactory,
                                    authenticated = authenticated,
                                    item = rel,
                                    target = ArtworkTarget.POSTER,
                                    width = 300,
                                    height = 450,
                                ),
                                onFocus = {},
                                onClick = { onOpenCollectionItem(rel) },
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun calculateProgress(item: MediaItemSummary): Float {
    if (!item.hasResumePosition()) return 0f
    val runtime = item.runTimeTicks() ?: return 0f
    if (runtime <= 0L) return 0f
    val position = item.userData().playbackPositionTicks()
    return (position.toFloat() / runtime.toFloat()).coerceIn(0f, 1f)
}

// ── Details stage ────────────────────────────────────────────────────────

@Composable
private fun DetailsStage(
    backdrop: ArtworkRequestSpec?,
    palette: CinePilotPalette,
    content: @Composable () -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        // Backdrop image at 84% opacity — matches original
        if (backdrop != null) {
            CinePilotAsyncImage(
                request = backdrop,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(0.84f),
            )
        }
        // Dark base overlay — matches original Color.argb(42, 0, 0, 0) (16.5% black)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 42f / 255f)),
        )
        // Left-side readable scrim — wide gradient, softer transition
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(900.dp)
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 250f / 255f),
                            Color.Black.copy(alpha = 220f / 255f),
                            Color.Black.copy(alpha = 150f / 255f),
                            Color.Black.copy(alpha = 60f / 255f),
                            Color.Transparent,
                        ),
                    ),
                ),
        )
        // Scrollable content
        content()
    }
}

// ── Section helper ───────────────────────────────────────────────────────

@Composable
private fun DetailSection(
    title: String,
    palette: CinePilotPalette,
    content: @Composable () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        BasicText(
            text = title,
            style = TextStyle(
                color = palette.textSecondary,
                fontSize = TvText.Section,
                fontWeight = FontWeight.Medium,
            ),
        )
        content()
    }
}

// ── Flow Row layout (matches TvFlowLayout behavior) ────────────────────

@Composable
private fun FlowRow(
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
    val placeable: androidx.compose.ui.layout.Placeable,
    val width: Int,
    val height: Int,
)

// ── Details hero ─────────────────────────────────────────────────────────

@Composable
private fun DetailsHero(
    palette: CinePilotPalette,
    artworkFactory: ArtworkRequestFactory,
    authenticated: AuthenticatedServer?,
    item: MediaItemSummary,
    title: String,
    contextLine: String,
    qualityBadges: List<String>,
    providerBadges: List<Pair<String, String>>,
    actions: List<DetailAction>,
    playbackProgress: Float,
    onProviderBadgeClick: (String) -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(20.dp),
        verticalAlignment = Alignment.Top,
        modifier = Modifier.fillMaxWidth(),
    ) {
        // Poster — full artwork without overlay button
        val poster = rememberArtworkRequest(
            factory = artworkFactory,
            authenticated = authenticated,
            item = item,
            target = ArtworkTarget.POSTER,
            width = 420,
            height = 630,
        )
        Box(
            modifier = Modifier
                .width(DetailPosterWidth)
                .height(DetailPosterHeight)
                .shadow(
                    elevation = 6.dp,
                    shape = RoundedCornerShape(TvDp.CardRadius),
                    clip = false,
                )
                .clip(RoundedCornerShape(TvDp.CardRadius))
                .background(palette.posterFallback)
                .border(
                    width = 0.5.dp,
                    color = palette.glassBorder.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(TvDp.CardRadius),
                ),
        ) {
            CinePilotAsyncImage(
                request = poster,
                contentDescription = "$title 海报",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }

        // Right column: title, meta, badges, actions
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(top = 2.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp),
        ) {
            BasicText(
                text = title,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                style = TextStyle(
                    color = palette.textPrimary,
                    fontSize = DetailTitleSize,
                    fontWeight = FontWeight.Bold,
                    lineHeight = (DetailTitleSize.value * 1.15f).sp,
                ),
            )
            Spacer(Modifier.height(8.dp))
            if (contextLine.isNotBlank()) {
                BasicText(
                    text = contextLine,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = TextStyle(
                        color = palette.textSecondary,
                        fontSize = DetailMetaSize,
                    ),
                )
                Spacer(Modifier.height(10.dp))
            }
            if (qualityBadges.isNotEmpty()) {
                MetadataPills(badges = qualityBadges, palette = palette)
                Spacer(Modifier.height(10.dp))
            }
            // 评分徽章行（TMDB / 豆瓣 / IMDB）
            item.communityRating()?.let { rating ->
                RatingBadgeRow(
                    palette = palette,
                    communityRating = rating,
                    hasTmdb = item.tmdbId().isNotBlank(),
                    hasImdb = item.imdbId().isNotBlank(),
                )
                Spacer(Modifier.height(12.dp))
            }
            if (providerBadges.isNotEmpty()) {
                ProviderBadgeFlow(
                    badges = providerBadges,
                    palette = palette,
                    onClick = onProviderBadgeClick,
                )
                Spacer(Modifier.height(8.dp))
            }
            if (actions.isNotEmpty()) {
                DetailActionFlow(
                    actions = actions,
                    palette = palette,
                    playbackProgress = playbackProgress,
                )
            }
        }
    }
}

// ── Metadata pills ───────────────────────────────────────────────────────

@Composable
private fun MetadataPills(badges: List<String>, palette: CinePilotPalette) {
    FlowRow(
        horizontalGap = 6.dp,
        verticalGap = 5.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        badges.take(8).forEach { badge ->
            PillChip(
                text = badge,
                palette = palette,
                primary = false,
            )
        }
    }
}

@Composable
private fun ProviderBadgeFlow(
    badges: List<Pair<String, String>>,
    palette: CinePilotPalette,
    onClick: (String) -> Unit,
) {
    FlowRow(
        horizontalGap = 6.dp,
        verticalGap = 5.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        badges.take(5).forEach { (label, url) ->
            var focused by remember { mutableStateOf(false) }
            PillChip(
                text = label,
                palette = palette,
                primary = false,
                clickable = true,
                focused = focused,
                onFocusChange = { focused = it },
                onClick = { onClick(url) },
            )
        }
    }
}

@Composable
private fun PillChip(
    text: String,
    palette: CinePilotPalette,
    primary: Boolean = false,
    clickable: Boolean = false,
    focused: Boolean = false,
    onFocusChange: (Boolean) -> Unit = {},
    onClick: () -> Unit = {},
) {
    val bgColor = when {
        primary -> palette.accentStrong
        focused -> palette.glassFocus
        else -> palette.glass
    }
    val textColor = when {
        primary -> palette.focusText
        focused -> palette.textPrimary
        else -> palette.textSecondary
    }
    val borderColor = when {
        primary -> palette.accentStrong
        focused -> palette.focusRing
        else -> palette.glassBorder.copy(alpha = 0.4f)
    }
    val interaction = remember { MutableInteractionSource() }
    val baseModifier = Modifier
        .height(PillChipHeight)
        .clip(RoundedCornerShape(PillChipRadius))
        .background(bgColor)
        .border(if (focused) TvDp.FocusRing else 0.5.dp, borderColor, RoundedCornerShape(PillChipRadius))
        .padding(horizontal = 10.dp)

    val finalModifier = if (clickable) {
        baseModifier
            .onFocusChanged { onFocusChange(it.isFocused) }
            .focusable(interactionSource = interaction)
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick,
            )
    } else {
        baseModifier
    }

    Box(finalModifier, contentAlignment = Alignment.Center) {
        BasicText(
            text = text,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = TextStyle(
                color = textColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
            ),
        )
    }
}

// ── Rating badges (with colored icon backgrounds) ───────────────────────

@Composable
private fun RatingBadgeRow(
    palette: CinePilotPalette,
    communityRating: Double,
    hasTmdb: Boolean,
    hasImdb: Boolean,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        // TMDB / 社区评分 — teal accent style
        IconRatingBadge(
            palette = palette,
            iconLabel = if (hasTmdb) "TMDb" else "评分",
            value = "%.0f%%".format(communityRating * 10.0),
            iconBgColor = palette.accent,
            iconTextColor = palette.focusText,
        )
        // 豆瓣 — green style
        IconRatingBadge(
            palette = palette,
            iconLabel = "豆瓣",
            value = "%.1f".format(communityRating),
            iconBgColor = DoubanGreen,
            iconTextColor = Color.White,
        )
        // IMDb — gold style
        if (hasImdb) {
            IconRatingBadge(
                palette = palette,
                iconLabel = "IMDb",
                value = "—",
                iconBgColor = ImdbGold,
                iconTextColor = Color.Black,
            )
        }
    }
}

@Composable
private fun IconRatingBadge(
    palette: CinePilotPalette,
    iconLabel: String,
    value: String,
    iconBgColor: Color,
    iconTextColor: Color,
) {
    val height = RatingBadgeHeight
    val iconWidth = RatingBadgeIconWidth

    Box(
        modifier = Modifier
            .height(height)
            .clip(RoundedCornerShape(8.dp))
            .background(palette.glass)
            .border(0.5.dp, palette.glassBorder, RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.CenterStart,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Icon area — colored background
            Box(
                modifier = Modifier
                    .width(iconWidth)
                    .fillMaxHeight()
                    .background(iconBgColor),
                contentAlignment = Alignment.Center,
            ) {
                BasicText(
                    text = iconLabel,
                    maxLines = 1,
                    style = TextStyle(
                        color = iconTextColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                )
            }
            // Value area
            Box(
                modifier = Modifier
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                BasicText(
                    text = value,
                    maxLines = 1,
                    style = TextStyle(
                        color = palette.textPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                )
            }
        }
    }
}

// ── Detail actions (icon + label, flow layout) ──────────────────────────

private data class DetailAction(
    val label: String,
    val icon: ImageVector,
    val onClick: () -> Unit,
)

@Composable
private fun DetailActionFlow(
    actions: List<DetailAction>,
    palette: CinePilotPalette,
    playbackProgress: Float = 0f,
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(ActionButtonGap),
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 2.dp, bottom = 2.dp),
    ) {
        itemsIndexed(actions.take(9)) { index, action ->
            val isPrimary = index == 0
            val progress = if (isPrimary) playbackProgress else 0f
            DetailActionButton(
                label = action.label,
                icon = action.icon,
                palette = palette,
                primary = isPrimary,
                progress = progress,
                requestInitialFocus = isPrimary,
                onClick = action.onClick,
            )
        }
    }
}

@Composable
private fun DetailActionButton(
    label: String,
    icon: ImageVector,
    palette: CinePilotPalette,
    primary: Boolean,
    progress: Float = 0f,
    requestInitialFocus: Boolean = false,
    onClick: () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val interaction = remember { MutableInteractionSource() }

    val bgColor = when {
        primary -> palette.accentStrong
        focused -> palette.glassFocus
        else -> palette.glass
    }
    val textColor = when {
        primary -> Color.White
        focused -> palette.textPrimary
        else -> palette.textSecondary
    }
    val borderColor = when {
        primary -> palette.accentStrong
        focused -> palette.focusRing
        else -> palette.glassBorder
    }
    val iconColor = when {
        primary -> Color.White
        focused -> palette.accentStrong
        else -> palette.textSecondary
    }

    LaunchedEffect(Unit) {
        if (requestInitialFocus) {
            runCatching { focusRequester.requestFocus() }
        }
    }

    val glowModifier = if (focused) {
        Modifier.shadow(
            elevation = 8.dp,
            shape = RoundedCornerShape(TvDp.ControlRadius),
            spotColor = palette.focusGlow,
            ambientColor = palette.focusGlow,
        )
    } else {
        Modifier
    }

    Box(
        modifier = Modifier
            .focusRequester(focusRequester)
            .onFocusChanged { focused = it.isFocused }
            .focusable(interactionSource = interaction)
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick,
            )
            .width(ActionButtonWidth)
            .height(ActionButtonHeight)
            .then(glowModifier)
            .clip(RoundedCornerShape(TvDp.ControlRadius))
            .background(bgColor)
            .border(
                if (focused) TvDp.FocusRing else 0.5.dp,
                borderColor,
                RoundedCornerShape(TvDp.ControlRadius),
            ),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 10.dp),
        ) {
            Image(
                imageVector = icon,
                contentDescription = label,
                colorFilter = ColorFilter.tint(iconColor),
                modifier = Modifier
                    .size(ActionButtonIconSize),
            )
            Spacer(Modifier.height(6.dp))
            BasicText(
                text = label,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = TextStyle(
                    color = textColor,
                    fontSize = 11.sp,
                    fontWeight = if (primary) FontWeight.Bold else FontWeight.Medium,
                ),
            )
        }
        // Progress bar (only for primary button with progress > 0)
        if (primary && progress > 0f) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .height(ActionButtonProgressHeight)
                    .background(Color.Black.copy(alpha = 0.25f)),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(progress)
                        .background(
                            if (primary) Color.White else palette.accentStrong
                        ),
                )
            }
        }
    }
}

// ── Detail track options (with icons) ────────────────────────────────────

@Composable
private fun DetailTrackOptions(
    palette: CinePilotPalette,
    playbackInfo: PlaybackInfo?,
    selection: DetailTrackSelection,
    onSelection: (DetailTrackSelection) -> Unit,
) {
    val sources = playbackInfo?.mediaSources().orEmpty()
    if (sources.isEmpty()) return

    val activeSource = sources.firstOrNull { it.id() == selection.mediaSourceId } ?: sources.first()

    Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (sources.size > 1) {
                TrackOptionSelector(
                    palette = palette,
                    title = "媒体源",
                    value = sourceValue(activeSource),
                    iconRes = tv.cinepilot.tv.R.drawable.ic_media_source,
                    modifier = Modifier.weight(1f),
                    requestInitialFocus = selection.focusKey == FOCUS_SOURCE,
                    onClick = { /* popover TBD */ },
                )
            }
            TrackOptionSelector(
                palette = palette,
                title = "音轨",
                value = audioValue(activeSource, selection),
                iconRes = tv.cinepilot.tv.R.drawable.ic_volume,
                modifier = Modifier.weight(1f),
                requestInitialFocus = selection.focusKey == FOCUS_AUDIO,
                onClick = { /* popover TBD */ },
            )
            TrackOptionSelector(
                palette = palette,
                title = "字幕",
                value = subtitleValue(activeSource, selection),
                iconRes = tv.cinepilot.tv.R.drawable.ic_subtitles,
                modifier = Modifier.weight(1f),
                requestInitialFocus = selection.focusKey == FOCUS_SUBTITLE,
                onClick = { /* popover TBD */ },
            )
        }
    }
}

@Composable
private fun TrackOptionSelector(
    palette: CinePilotPalette,
    title: String,
    value: String,
    @androidx.annotation.DrawableRes iconRes: Int,
    modifier: Modifier = Modifier,
    requestInitialFocus: Boolean = false,
    onClick: () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val interaction = remember { MutableInteractionSource() }

    val bgColor = if (focused) palette.glassFocus else palette.glass
    val borderColor = if (focused) palette.focusRing else palette.glassBorder
    val valueColor = if (focused) palette.textPrimary else palette.textSecondary
    val iconTint = if (focused) palette.accentStrong else palette.textSecondary

    val glowModifier = if (focused) {
        Modifier.shadow(
            elevation = 6.dp,
            shape = RoundedCornerShape(TvDp.ControlRadius),
            spotColor = palette.focusGlow,
            ambientColor = palette.focusGlow,
        )
    } else {
        Modifier
    }

    LaunchedEffect(Unit) {
        if (requestInitialFocus) {
            runCatching { focusRequester.requestFocus() }
        }
    }

    Box(
        modifier = modifier
            .focusRequester(focusRequester)
            .onFocusChanged { focused = it.isFocused }
            .focusable(interactionSource = interaction)
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick,
            )
            .height(TrackSelectorHeight)
            .then(glowModifier)
            .clip(RoundedCornerShape(TvDp.ControlRadius))
            .background(bgColor)
            .border(
                if (focused) TvDp.FocusRing else 0.5.dp,
                borderColor,
                RoundedCornerShape(TvDp.ControlRadius),
            )
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Image(
                painter = painterResource(iconRes),
                contentDescription = title,
                colorFilter = ColorFilter.tint(iconTint),
                modifier = Modifier
                    .size(TrackSelectorIconSize),
            )
            Spacer(Modifier.width(10.dp))
            BasicText(
                text = title,
                style = TextStyle(
                    color = palette.textMuted,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                ),
            )
            Spacer(Modifier.width(8.dp))
            BasicText(
                text = value,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = TextStyle(
                    color = valueColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                ),
                modifier = Modifier.weight(1f),
            )
            Image(
                painter = painterResource(tv.cinepilot.tv.R.drawable.ic_chevron_down),
                contentDescription = null,
                colorFilter = ColorFilter.tint(palette.textMuted),
                modifier = Modifier.size(14.dp),
            )
        }
    }
}

// ── Episode row with custom cards ───────────────────────────────────────

@Composable
private fun EpisodeRow(
    palette: CinePilotPalette,
    artworkFactory: ArtworkRequestFactory,
    authenticated: AuthenticatedServer?,
    episodes: List<MediaItemSummary>,
    currentItem: MediaItemSummary,
    onEpisodeClick: (MediaItemSummary) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // Section header: "剧集 第N季 >"
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.padding(bottom = 8.dp),
        ) {
            BasicText(
                text = "剧集",
                style = TextStyle(
                    color = palette.textSecondary,
                    fontSize = TvText.Section,
                    fontWeight = FontWeight.Medium,
                ),
            )
            val seasonNum = currentItem.parentIndexNumber()
            if (seasonNum != null) {
                BasicText(
                    text = "第 ${seasonNum} 季",
                    style = TextStyle(
                        color = palette.accentStrong,
                        fontSize = TvText.Section,
                        fontWeight = FontWeight.Medium,
                    ),
                )
                BasicText(
                    text = ">",
                    style = TextStyle(
                        color = palette.accentStrong,
                        fontSize = TvText.Section,
                        fontWeight = FontWeight.Medium,
                    ),
                )
            }
        }
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(EpisodeCardGap),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = 3.dp,
                top = 3.dp,
                end = 21.dp,
                bottom = 4.dp,
            ),
        ) {
            itemsIndexed(
                items = episodes,
                key = { _, ep -> ep.id() },
            ) { index, ep ->
                val progress = remember(ep) { calculateProgress(ep) }
                DetailEpisodeCard(
                    palette = palette,
                    episode = ep,
                    episodeNumber = ep.indexNumber() ?: (index + 1),
                    artwork = rememberArtworkRequest(
                        factory = artworkFactory,
                        authenticated = authenticated,
                        item = ep,
                        target = ArtworkTarget.LANDSCAPE,
                        width = 400,
                        height = 225,
                    ),
                    progress = progress,
                    isCurrent = ep.id() == currentItem.id(),
                    onClick = { onEpisodeClick(ep) },
                )
            }
        }
    }
}

@Composable
private fun DetailEpisodeCard(
    palette: CinePilotPalette,
    episode: MediaItemSummary,
    episodeNumber: Int,
    artwork: ArtworkRequestSpec?,
    progress: Float,
    isCurrent: Boolean = false,
    onClick: () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val interaction = remember { MutableInteractionSource() }

    val cardWidth = EpisodeCardWidth
    val cardHeight = EpisodeCardHeight
    val cardRadius = 16.dp

    val glowModifier = if (focused) {
        Modifier.shadow(
            elevation = 10.dp,
            shape = RoundedCornerShape(cardRadius),
            spotColor = palette.focusGlow,
            ambientColor = palette.focusGlow,
        )
    } else {
        Modifier
    }

    LaunchedEffect(Unit) {
        if (isCurrent) {
            runCatching { focusRequester.requestFocus() }
        }
    }

    Box(
        modifier = Modifier
            .focusRequester(focusRequester)
            .onFocusChanged { focused = it.isFocused }
            .focusable(interactionSource = interaction)
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick,
            )
            .width(cardWidth)
            .height(cardHeight)
            .then(glowModifier)
            .clip(RoundedCornerShape(cardRadius))
            .border(
                if (focused) TvDp.FocusRing else 0.5.dp,
                if (focused) palette.focusRing else palette.glassBorder,
                RoundedCornerShape(cardRadius),
            ),
    ) {
        // Thumbnail image
        CinePilotAsyncImage(
            request = artwork,
            contentDescription = episode.name(),
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        // Bottom gradient overlay for readability
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(70.dp)
                .background(
                    brush = Brush.verticalGradient(
                        listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.85f),
                        ),
                    ),
                ),
        )
        // Bottom content: episode number + title + progress
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(start = 12.dp, end = 12.dp, bottom = 10.dp),
        ) {
            Column {
                // Episode number and title row
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    BasicText(
                        text = episodeNumber.toString(),
                        style = TextStyle(
                            color = Color.White,
                            fontSize = 36.sp,
                            fontWeight = FontWeight.Bold,
                            lineHeight = 36.sp,
                        ),
                    )
                    BasicText(
                        text = episode.name().ifBlank { "第 $episodeNumber 集" },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = TextStyle(
                            color = palette.textPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                        ),
                        modifier = Modifier.weight(1f).padding(bottom = 4.dp),
                    )
                }
                Spacer(Modifier.height(6.dp))
                // Progress bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(EpisodeProgressHeight)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color.White.copy(alpha = 0.2f)),
                ) {
                    if (progress > 0f) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(progress)
                                .background(palette.accentStrong),
                        )
                    }
                }
            }
        }
    }
}

// ── Helper functions ─────────────────────────────────────────────────────

private fun sourceValue(source: MediaSourceInfo): String {
    return sourceLabel(source).removePrefix("媒体源：")
}

private fun audioValue(source: MediaSourceInfo, selection: DetailTrackSelection): String {
    val streams = source.streamsOf(MediaStreamType.AUDIO)
    val selectedStream = streams.firstOrNull {
        selection.audioSelected && selection.audioStreamIndex == it.index()
    }
    return selectedStream?.let { streamLabel(it) } ?: serverDefaultLabel(streams)
}

private fun subtitleValue(source: MediaSourceInfo, selection: DetailTrackSelection): String {
    val streams = source.streamsOf(MediaStreamType.SUBTITLE)
    return when {
        selection.subtitleSelected && selection.subtitleStreamIndex == SUBTITLES_OFF_INDEX ->
            "关闭字幕"
        else -> {
            val selectedStream = streams.firstOrNull {
                selection.subtitleSelected && selection.subtitleStreamIndex == it.index()
            }
            selectedStream?.let { streamLabel(it) } ?: serverDefaultLabel(streams)
        }
    }
}

@Composable
private fun rememberDetailActions(
    item: MediaItemSummary,
    onPreparePlayback: (PlaybackSelectionPreferences?) -> Unit,
    onSubtitleStyle: () -> Unit,
    onPlaybackSpeed: () -> Unit,
    onSeriesNextUp: () -> Unit,
    onOpenEpisodePicker: () -> Unit,
    onOpenSeries: () -> Unit,
    onOpenFolder: () -> Unit,
    onToggleFavorite: () -> Unit,
    onToggleWatched: () -> Unit,
    onOpenProviderIdsEditor: () -> Unit,
    onChooseDownloadQuality: (Int) -> Unit,
    onManageOffline: () -> Unit,
    offlineActionLabel: String?,
    offlineActionIsReady: Boolean,
    onAddToPlaylist: () -> Unit,
    onSearchSubtitles: () -> Unit,
    hasSubtitleSearch: Boolean,
): List<DetailAction> {
    if (!item.playable()) {
        return listOf(DetailAction(item.browseChildrenLabel().ifBlank { "打开子项目" }, Icons.Outlined.Folder, onOpenFolder))
    }
    return buildList {
        if (item.hasResumePosition()) {
            add(DetailAction("继续播放", Icons.Outlined.PlayArrow) { onPreparePlayback(null) })
            add(DetailAction("从头播放", Icons.Outlined.PlayArrow) { onPreparePlayback(PlaybackSelectionPreferences.defaults()) })
        } else {
            add(DetailAction("播放", Icons.Outlined.PlayArrow) { onPreparePlayback(null) })
        }
        val isFavorite = item.userData().favorite()
        add(DetailAction(if (isFavorite) "已收藏" else "收藏", if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder, onToggleFavorite))
        add(DetailAction(if (item.userData().played()) "取消已看" else "标记已看", Icons.Outlined.Check, onToggleWatched))
        add(DetailAction("省流量", Icons.Outlined.DataSaverOff) { onPreparePlayback(lowBitratePreferences(item)) })
        add(DetailAction("字幕样式", Icons.Outlined.Title, onSubtitleStyle))
        if (hasSubtitleSearch) add(DetailAction("搜索字幕", Icons.Outlined.Search, onSearchSubtitles))
        add(DetailAction("速度", Icons.Outlined.Speed, onPlaybackSpeed))
        if (item.isEpisode() && item.parentId().isNotBlank()) add(DetailAction("选集", Icons.Outlined.ViewList, onOpenEpisodePicker))
        if (item.seriesId().isNotBlank()) add(DetailAction("剧集", Icons.Outlined.Tv, onOpenSeries))
        if (item.seriesId().isNotBlank()) add(DetailAction("下一集", Icons.Outlined.SkipNext, onSeriesNextUp))
        add(DetailAction("修正编号", Icons.Outlined.Edit, onOpenProviderIdsEditor))
        offlineActionLabel?.let { label ->
            add(DetailAction(label, Icons.Outlined.Download) {
                if (offlineActionIsReady) onManageOffline() else onChooseDownloadQuality(0)
            })
        }
        add(DetailAction("播放列表", Icons.Outlined.PlaylistAdd, onAddToPlaylist))
    }
}

private fun DetailTrackSelection.forSource(sourceId: String): DetailTrackSelection {
    return if (mediaSourceId == sourceId) this else DetailTrackSelection(mediaSourceId = sourceId)
}

private fun MediaSourceInfo.streamsOf(type: MediaStreamType): List<MediaStreamInfo> {
    return mediaStreams().filter { stream -> stream.type() == type }
}

private fun serverDefaultLabel(streams: List<MediaStreamInfo>): String {
    val defaultStream = streams.firstOrNull { it.defaultStream() } ?: return "服务器默认"
    return "服务器默认 · ${streamLabel(defaultStream)}"
}

private fun MediaStreamInfo.requiresBurnInWhenTranscoding(): Boolean {
    val value = listOf(codec(), displayTitle()).joinToString(" ").lowercase()
    return value.contains("pgs") ||
        value.contains("dvdsub") ||
        value.contains("dvd_subtitle") ||
        value.contains("vobsub")
}

private fun lowBitratePreferences(item: MediaItemSummary): PlaybackSelectionPreferences {
    val startTimeTicks = if (item.hasResumePosition()) item.userData().playbackPositionTicks() else 0L
    return PlaybackSelectionPreferences.lowBitrate(startTimeTicks)
}

private const val FOCUS_SOURCE = "source"
private const val FOCUS_AUDIO = "audio"
private const val FOCUS_SUBTITLE = "subtitle"
private const val SUBTITLES_OFF_INDEX = -1
