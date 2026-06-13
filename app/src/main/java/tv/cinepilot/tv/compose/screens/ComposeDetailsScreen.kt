package tv.cinepilot.tv.compose.screens

import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.text.TextStyle
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
import tv.cinepilot.tv.compose.components.LandscapeCard
import tv.cinepilot.tv.compose.components.MediaRail
import tv.cinepilot.tv.compose.components.PosterCard
import tv.cinepilot.tv.compose.theme.CinePilotPalette
import tv.cinepilot.tv.compose.theme.TvText
import tv.cinepilot.tv.details.DetailTrackSelection
import tv.cinepilot.tv.runtime.ArtworkRequestFactory
import tv.cinepilot.tv.runtime.ArtworkRequestSpec
import tv.cinepilot.tv.runtime.ArtworkTarget
import tv.cinepilot.tv.ui.MediaWallTokens
import tv.cinepilot.tv.ui.MediaWallType
import tv.cinepilot.tv.ui.browseChildrenLabel
import tv.cinepilot.tv.ui.isEpisode
import tv.cinepilot.tv.ui.mediaTechnicalPills
import tv.cinepilot.tv.ui.sourceLabel
import tv.cinepilot.tv.ui.streamLabel
import tv.cinepilot.tv.ui.toDetailPresentation
import kotlin.math.max

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

    DetailsStage(
        backdrop = backdrop,
        palette = palette,
    ) {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(MediaWallTokens.DetailGap.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = MediaWallTokens.ScreenX.dp,
                top = (MediaWallTokens.DetailTop + 8).dp,
                end = MediaWallTokens.ScreenX.dp,
                bottom = MediaWallTokens.ScreenBottom.dp,
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
                    DetailSection(title = "剧情简介", palette = palette) {
                        BasicText(
                            text = presentation.overview,
                            style = TextStyle(
                                color = palette.textSecondary,
                                fontSize = TvText.Body,
                                lineHeight = (TvText.Body.value * 1.08f).sp,
                            ),
                        )
                    }
                }
            }
            if (item.isEpisode() && siblingEpisodes.isNotEmpty()) {
                item {
                    DetailSection(title = "本季集数", palette = palette) {
                        MediaRail(
                            palette = palette,
                            title = "",
                            items = siblingEpisodes,
                            key = { it.id() },
                        ) { ep ->
                            LandscapeCard(
                                palette = palette,
                                item = ep,
                                artwork = rememberArtworkRequest(
                                    factory = artworkFactory,
                                    authenticated = authenticated,
                                    item = ep,
                                    target = ArtworkTarget.LANDSCAPE,
                                    width = 376,
                                    height = 212,
                                ),
                                onFocus = {},
                                onClick = { onOpenEpisode(ep) },
                            )
                        }
                    }
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
                                    width = 260,
                                    height = 390,
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

// ---- Details stage ----------------------------------------------------------------

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
        // Left-side readable scrim — 1180dp wide gradient, matches original readableDetailsScrim
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(1180.dp)
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 236f / 255f),
                            Color.Black.copy(alpha = 184f / 255f),
                            Color.Black.copy(alpha = 72f / 255f),
                            Color.Transparent,
                        ),
                    ),
                ),
        )
        // Scrollable content
        content()
    }
}

// ---- Section helper -------------------------------------------------------------

@Composable
private fun DetailSection(
    title: String,
    palette: CinePilotPalette,
    content: @Composable () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        BasicText(
            text = title,
            style = TextStyle(
                color = palette.textSecondary,
                fontSize = MediaWallType.RowTitle.sp,
            ),
        )
        content()
    }
}

// ---- Flow Row layout (matches TvFlowLayout behavior) --------------------------

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

// ---- Details hero -----------------------------------------------------------------

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
    onProviderBadgeClick: (String) -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(28.dp),
        verticalAlignment = Alignment.Top,
        modifier = Modifier.fillMaxWidth(),
    ) {
        // Poster — with elevation shadow, matches original
        val poster = rememberArtworkRequest(
            factory = artworkFactory,
            authenticated = authenticated,
            item = item,
            target = ArtworkTarget.POSTER,
            width = 288,
            height = 432,
        )
        Box(
            modifier = Modifier
                .width(MediaWallTokens.DetailPosterWidth.dp)
                .height(MediaWallTokens.DetailPosterHeight.dp)
                .shadow(
                    elevation = 6.dp,
                    shape = RoundedCornerShape(MediaWallTokens.CellRadius.dp),
                    clip = false,
                )
                .clip(RoundedCornerShape(MediaWallTokens.CellRadius.dp))
                .background(palette.posterFallback)
                .border(
                    width = 1.dp,
                    color = palette.glassBorder.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(MediaWallTokens.CellRadius.dp),
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
                .padding(top = 4.dp),
        ) {
            BasicText(
                text = title,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                style = TextStyle(
                    color = palette.textPrimary,
                    fontSize = MediaWallType.DetailTitle.sp,
                    lineHeight = (MediaWallType.DetailTitle * 1.02f).sp,
                ),
            )
            if (contextLine.isNotBlank()) {
                Spacer(Modifier.height(10.dp))
                BasicText(
                    text = contextLine,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = TextStyle(
                        color = palette.textSecondary,
                        fontSize = MediaWallType.DetailMeta.sp,
                    ),
                )
            }
            if (qualityBadges.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                MetadataPills(badges = qualityBadges, palette = palette)
            }
            if (providerBadges.isNotEmpty()) {
                Spacer(Modifier.height(6.dp))
                ProviderBadgeFlow(
                    badges = providerBadges,
                    palette = palette,
                    onClick = onProviderBadgeClick,
                )
            }
            if (actions.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                DetailActionFlow(actions = actions, palette = palette)
            }
        }
    }
}

// ---- Metadata pills ---------------------------------------------------------------

@Composable
private fun MetadataPills(badges: List<String>, palette: CinePilotPalette) {
    FlowRow(
        horizontalGap = 6.dp,
        verticalGap = 4.dp,
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
        verticalGap = 4.dp,
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
        else -> Color.White.copy(alpha = 0.08f)
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
        .height(26.dp)
        .clip(RoundedCornerShape(8.dp))
        .background(bgColor)
        .border(1.dp, borderColor, RoundedCornerShape(8.dp))
        .padding(horizontal = 8.dp)

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
            ),
        )
    }
}

// ---- Detail actions (pill-style, flow layout) ------------------------------------

private data class DetailAction(val label: String, val onClick: () -> Unit)

@Composable
private fun DetailActionFlow(
    actions: List<DetailAction>,
    palette: CinePilotPalette,
) {
    FlowRow(
        horizontalGap = 8.dp,
        verticalGap = 8.dp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp, bottom = 2.dp),
    ) {
        actions.forEachIndexed { index, action ->
            DetailActionButton(
                label = action.label,
                palette = palette,
                primary = index == 0,
                requestInitialFocus = index == 0,
                onClick = action.onClick,
            )
        }
    }
}

@Composable
private fun DetailActionButton(
    label: String,
    palette: CinePilotPalette,
    primary: Boolean,
    requestInitialFocus: Boolean = false,
    onClick: () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val interaction = remember { MutableInteractionSource() }

    val height = if (primary) 38.dp else 34.dp
    val horizontalPadding = if (primary) 18.dp else 10.dp

    val bgColor = when {
        primary -> palette.accentStrong
        focused -> palette.glassFocus
        else -> Color.Black.copy(alpha = 42f / 255f)
    }
    val textColor = when {
        primary -> palette.focusText
        focused -> palette.textPrimary
        else -> palette.textSecondary
    }
    val borderColor = when {
        primary -> palette.accentStrong
        focused -> palette.focusRing
        else -> palette.glassBorder.copy(alpha = 44f / 255f)
    }

    LaunchedEffect(Unit) {
        if (requestInitialFocus) {
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
            .height(height)
            .clip(RoundedCornerShape(16.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(16.dp))
            .padding(horizontal = horizontalPadding),
        contentAlignment = Alignment.Center,
    ) {
        BasicText(
            text = label,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = TextStyle(
                color = textColor,
                fontSize = MediaWallType.Action.sp,
            ),
        )
    }
}

// ---- Detail track options (info-section style, not SettingsGrid) ------------------

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
        FlowRow(
            horizontalGap = 8.dp,
            verticalGap = 8.dp,
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (sources.size > 1) {
                TrackOptionSelector(
                    palette = palette,
                    title = "媒体源",
                    value = sourceValue(activeSource),
                    width = 420.dp,
                    requestInitialFocus = selection.focusKey == FOCUS_SOURCE,
                    onClick = { /* popover TBD */ },
                )
            }
            TrackOptionSelector(
                palette = palette,
                title = "音轨",
                value = audioValue(activeSource, selection),
                width = 360.dp,
                requestInitialFocus = selection.focusKey == FOCUS_AUDIO,
                onClick = { /* popover TBD */ },
            )
            TrackOptionSelector(
                palette = palette,
                title = "字幕",
                value = subtitleValue(activeSource, selection),
                width = 360.dp,
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
    width: Dp = 360.dp,
    requestInitialFocus: Boolean = false,
    onClick: () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val interaction = remember { MutableInteractionSource() }

    val bgColor = if (focused) palette.glassFocus else palette.glass
    val borderColor = if (focused) palette.focusRing else palette.glassBorder
    val valueColor = if (focused) palette.textPrimary else palette.textSecondary

    LaunchedEffect(Unit) {
        if (requestInitialFocus) {
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
            .width(width)
            .height(40.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            BasicText(
                text = title,
                style = TextStyle(
                    color = palette.accentStrong,
                    fontSize = 13.sp,
                ),
                modifier = Modifier.width(58.dp),
            )
            BasicText(
                text = value,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = TextStyle(
                    color = valueColor,
                    fontSize = 13.sp,
                ),
                modifier = Modifier.weight(1f),
            )
            BasicText(
                text = "\u2304",
                style = TextStyle(
                    color = palette.textMuted,
                    fontSize = 13.sp,
                ),
                modifier = Modifier.width(18.dp),
            )
        }
    }
}

// ---- Helper functions -------------------------------------------------------------

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
        return listOf(DetailAction(item.browseChildrenLabel().ifBlank { "打开子项目" }, onOpenFolder))
    }
    return buildList {
        if (item.hasResumePosition()) {
            add(DetailAction("继续播放") { onPreparePlayback(null) })
            add(DetailAction("从头播放") { onPreparePlayback(PlaybackSelectionPreferences.defaults()) })
        } else {
            add(DetailAction("播放") { onPreparePlayback(null) })
        }
        add(DetailAction(if (item.userData().favorite()) "已收藏" else "收藏", onToggleFavorite))
        add(DetailAction(if (item.userData().played()) "取消已看" else "标记已看", onToggleWatched))
        add(DetailAction("省流量") { onPreparePlayback(lowBitratePreferences(item)) })
        add(DetailAction("字幕样式", onSubtitleStyle))
        if (hasSubtitleSearch) add(DetailAction("搜索字幕", onSearchSubtitles))
        add(DetailAction("速度", onPlaybackSpeed))
        if (item.isEpisode() && item.parentId().isNotBlank()) add(DetailAction("选集", onOpenEpisodePicker))
        if (item.seriesId().isNotBlank()) add(DetailAction("剧集", onOpenSeries))
        if (item.seriesId().isNotBlank()) add(DetailAction("本剧下一集", onSeriesNextUp))
        add(DetailAction("修正编号", onOpenProviderIdsEditor))
        offlineActionLabel?.let { label ->
            add(DetailAction(label) {
                if (offlineActionIsReady) onManageOffline() else onChooseDownloadQuality(0)
            })
        }
        add(DetailAction("添加到播放列表", onAddToPlaylist))
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
