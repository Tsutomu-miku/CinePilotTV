package tv.cinepilot.tv.compose.screens.player

import androidx.activity.ComponentActivity
import androidx.compose.foundation.Image
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import tv.cinepilot.core.protocol.AuthenticatedServer
import tv.cinepilot.tv.R
import tv.cinepilot.tv.compose.artwork.CinePilotAsyncImage
import tv.cinepilot.tv.compose.artwork.rememberArtworkRequest
import tv.cinepilot.tv.compose.components.TvActionButton
import tv.cinepilot.tv.compose.theme.CinePilotPalette
import tv.cinepilot.tv.compose.theme.TvDp
import tv.cinepilot.tv.compose.theme.TvText
import tv.cinepilot.tv.runtime.ArtworkRequestFactory
import tv.cinepilot.tv.runtime.ArtworkTarget

@Composable
internal fun NextUpMiniCard(
    palette: CinePilotPalette,
    owner: ComponentActivity,
    artworkFactory: ArtworkRequestFactory,
    authenticated: AuthenticatedServer?,
    info: ComposeNextUpInfo,
    modifier: Modifier = Modifier,
    onPlayNext: () -> Unit,
) {
    val artwork = rememberArtworkRequest(
        factory = artworkFactory,
        authenticated = authenticated,
        item = info.item,
        target = ArtworkTarget.LANDSCAPE,
        width = 400,
        height = 225,
    )
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(12.dp)
    Box(
        modifier = modifier
            .width(200.dp)
            .height(112.dp)
            .clip(shape)
            .shadow(
                elevation = if (focused) 16.dp else 6.dp,
                shape = shape,
                spotColor = if (focused) palette.focusGlow else Color.Black.copy(alpha = 0.4f),
                ambientColor = if (focused) palette.focusGlow else Color.Black.copy(alpha = 0.3f),
            )
            .border(
                width = if (focused) TvDp.FocusRing else 0.8.dp,
                color = if (focused) palette.focusRing else palette.glassBorder,
                shape = shape,
            )
            .onFocusChanged { focused = it.isFocused }
            .focusable()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onPlayNext,
            ),
    ) {
        CinePilotAsyncImage(
            request = artwork,
            contentDescription = info.item.name(),
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        // Dark gradient overlay
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.Black.copy(alpha = 0.05f),
                            Color.Black.copy(alpha = 0.65f),
                        ),
                    ),
                ),
        )
        // Play icon circle overlay
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .size(32.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color.Black.copy(alpha = 0.5f))
                .border(1.dp, Color.White.copy(alpha = 0.6f), RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(R.drawable.ic_play),
                contentDescription = null,
                colorFilter = ColorFilter.tint(Color.White),
                modifier = Modifier.size(12.dp),
            )
        }
        // Bottom text
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(8.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                BasicText(
                    text = "下一集",
                    maxLines = 1,
                    style = TextStyle(
                        color = palette.textPrimary,
                        fontSize = TvText.Body,
                        fontWeight = FontWeight.SemiBold,
                    ),
                )
            }
            BasicText(
                text = info.episodeLabel,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = TextStyle(
                    color = palette.textMuted,
                    fontSize = TvText.Label,
                ),
            )
        }
    }
}

@Composable
internal fun NextUpCard(
    palette: CinePilotPalette,
    owner: ComponentActivity,
    artworkFactory: ArtworkRequestFactory,
    authenticated: AuthenticatedServer?,
    info: ComposeNextUpInfo,
    modifier: Modifier = Modifier,
    onPlayNext: () -> Unit,
    onCancelNextUp: () -> Unit,
) {
    val artwork = rememberArtworkRequest(
        factory = artworkFactory,
        authenticated = authenticated,
        item = info.item,
        target = ArtworkTarget.LANDSCAPE,
        width = 600,
        height = 338,
    )
    val shape = RoundedCornerShape(TvDp.CardRadius)
    Box(
        modifier = modifier
            .width(TvDp.NextUpWidth)
            .clip(shape)
            .background(palette.glass)
            .border(0.5.dp, palette.glassBorder, shape)
            .padding(12.dp),
    ) {
        Column {
            BasicText(
                text = "接下来播放",
                maxLines = 1,
                style = TextStyle(
                    color = palette.textSecondary,
                    fontSize = TvText.Metadata,
                    fontWeight = FontWeight.Medium,
                ),
            )
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.Top, modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .size(width = TvDp.LandscapeWidth, height = TvDp.LandscapeHeight)
                        .clip(RoundedCornerShape(8.dp))
                        .background(palette.posterFallback)
                        .border(0.5.dp, palette.glassBorder, RoundedCornerShape(8.dp)),
                ) {
                    CinePilotAsyncImage(
                        request = artwork,
                        contentDescription = info.item.name(),
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    BasicText(
                        text = info.episodeLabel,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = TextStyle(
                            color = palette.textMuted,
                            fontSize = TvText.Metadata,
                        ),
                    )
                    Spacer(Modifier.height(3.dp))
                    BasicText(
                        text = info.item.name().ifBlank { "下一集" },
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        style = TextStyle(
                            color = palette.textPrimary,
                            fontSize = TvText.Body,
                            fontWeight = FontWeight.SemiBold,
                        ),
                    )
                    Spacer(Modifier.height(5.dp))
                    BasicText(
                        text = info.item.overview().ifBlank {
                            if (info.autoPlay) "${info.countdownSeconds}s 后自动播放" else "下一集"
                        },
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        style = TextStyle(color = palette.textMuted, fontSize = TvText.Label),
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                TvActionButton(
                    palette = palette,
                    label = "立即播放",
                    selected = true,
                    requestInitialFocus = true,
                    modifier = Modifier.weight(1f),
                    onClick = onPlayNext,
                )
                TvActionButton(
                    palette = palette,
                    label = "取消",
                    modifier = Modifier.weight(1f),
                    onClick = onCancelNextUp,
                )
            }
        }
    }
}
