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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import tv.cinepilot.core.protocol.MediaItemSummary
import tv.cinepilot.tv.compose.artwork.CinePilotAsyncImage
import tv.cinepilot.tv.compose.artwork.rememberArtworkRequest
import tv.cinepilot.tv.compose.theme.CinePilotPalette
import tv.cinepilot.tv.compose.theme.TvDp
import tv.cinepilot.tv.compose.theme.TvText
import tv.cinepilot.tv.runtime.ArtworkRequestFactory
import tv.cinepilot.tv.runtime.ArtworkRequestSpec
import tv.cinepilot.tv.runtime.ArtworkTarget
import tv.cinepilot.core.protocol.AuthenticatedServer

// ── Episode card constants ──────────────────────────────────────────────

private val EpisodeCardWidth = 280.dp
private val EpisodeCardHeight = 158.dp
private val EpisodeCardGap = 14.dp
private val EpisodeProgressHeight = 3.dp

// ── Episode row with custom cards ──────────────────────────────────────

@Composable
internal fun EpisodeRow(
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
