package tv.cinepilot.tv.compose.screens.details

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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import tv.cinepilot.core.protocol.AuthenticatedServer
import tv.cinepilot.core.protocol.MediaItemSummary
import tv.cinepilot.plugin.spi.ItemSyncStatus
import tv.cinepilot.tv.compose.artwork.CinePilotAsyncImage
import tv.cinepilot.tv.compose.artwork.rememberArtworkRequest
import tv.cinepilot.tv.compose.components.layout.FlowRow
import tv.cinepilot.tv.compose.theme.CinePilotPalette
import tv.cinepilot.tv.compose.theme.TvDp
import tv.cinepilot.tv.plugin.PluginHost
import tv.cinepilot.tv.runtime.ArtworkRequestFactory
import tv.cinepilot.tv.runtime.ArtworkTarget

// ── Hero / poster constants ─────────────────────────────────────────────

private val DetailPosterWidth = 200.dp
private val DetailPosterHeight = 300.dp

private val DetailTitleSize = 32.sp
private val DetailMetaSize = 12.sp

private val SyncSuccessGreen = Color(0xFF22C55E)
private val SyncWarnAmber = Color(0xFFF59E0B)
private val SyncErrorRed = Color(0xFFEF4444)
private val SyncMutedBlue = Color(0xFF94A3B8)

// ── Details hero ─────────────────────────────────────────────────────────

@Composable
internal fun DetailsHero(
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
    pluginSyncStates: List<PluginHost.PluginItemSyncState>,
    onProviderBadgeClick: (String) -> Unit,
    onRetryPluginSync: (String) -> Unit,
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
            if (pluginSyncStates.isNotEmpty()) {
                PluginSyncStatusRow(
                    states = pluginSyncStates,
                    palette = palette,
                    onRetry = onRetryPluginSync,
                )
                Spacer(Modifier.height(10.dp))
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

// ── Plugin sync status row (Bangumi / future plugins) ──────────────────

@Composable
private fun PluginSyncStatusRow(
    states: List<PluginHost.PluginItemSyncState>,
    palette: CinePilotPalette,
    onRetry: (String) -> Unit,
) {
    FlowRow(
        horizontalGap = 6.dp,
        verticalGap = 5.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        states.forEach { state ->
            val syncColor = when (state.status) {
                ItemSyncStatus.SYNCED -> SyncSuccessGreen
                ItemSyncStatus.FAILED -> SyncErrorRed
                ItemSyncStatus.UNMATCHED -> SyncMutedBlue
                ItemSyncStatus.AUTH_REQUIRED -> SyncWarnAmber
                ItemSyncStatus.UNSUPPORTED -> SyncMutedBlue
            }
            val label = when (state.status) {
                ItemSyncStatus.SYNCED -> "${state.pluginName} 已同步"
                ItemSyncStatus.FAILED -> "${state.pluginName} 同步失败 · 重试"
                ItemSyncStatus.UNMATCHED -> "${state.pluginName} 未匹配"
                ItemSyncStatus.AUTH_REQUIRED -> "${state.pluginName} 需授权"
                ItemSyncStatus.UNSUPPORTED -> return@forEach
            }
            PluginSyncChip(
                palette = palette,
                label = label,
                color = syncColor,
                clickable = state.status == ItemSyncStatus.FAILED,
                onClick = { onRetry(state.pluginId) },
            )
        }
    }
}

@Composable
private fun PluginSyncChip(
    palette: CinePilotPalette,
    label: String,
    color: Color,
    clickable: Boolean,
    onClick: () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    val bg = when {
        focused -> palette.glassFocus
        else -> palette.glass
    }
    val interaction = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    var modifier: Modifier = Modifier
        .height(26.dp)
        .clip(RoundedCornerShape(8.dp))
        .background(bg)
        .border(
            width = if (focused) TvDp.FocusRing else 0.6.dp,
            color = if (focused) palette.focusRing else color.copy(alpha = 0.75f),
            shape = RoundedCornerShape(8.dp),
        )
        .padding(horizontal = 10.dp)
    if (clickable) {
        modifier = modifier
            .onFocusChanged { focused = it.isFocused }
            .focusable(interactionSource = interaction)
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick,
            )
    }
    Box(modifier, contentAlignment = Alignment.Center) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(color),
            )
            BasicText(
                text = label,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = TextStyle(
                    color = if (focused) palette.textPrimary else palette.textSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                ),
            )
        }
    }
}
