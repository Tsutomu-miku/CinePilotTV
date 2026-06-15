package tv.cinepilot.tv.compose.screens.playbacksettings

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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import tv.cinepilot.tv.R
import tv.cinepilot.tv.compose.theme.CinePilotPalette
import tv.cinepilot.tv.compose.theme.TvDp
import tv.cinepilot.tv.compose.theme.TvText

@Composable
internal fun NextUpCountdownCard(
    palette: CinePilotPalette,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(TvDp.CardRadius)
    Box(
        modifier = modifier
            .width(TvDp.NextUpWidth)
            .clip(shape)
            .background(palette.glass)
            .border(0.5.dp, palette.glassBorder, shape)
            .shadow(
                elevation = 8.dp,
                shape = shape,
                spotColor = Color.Black.copy(alpha = 0.5f),
                ambientColor = Color.Black.copy(alpha = 0.3f),
            )
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
            Row(
                verticalAlignment = Alignment.Top,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Box(
                    modifier = Modifier
                        .size(width = TvDp.LandscapeWidth, height = TvDp.LandscapeHeight)
                        .clip(RoundedCornerShape(8.dp))
                        .background(palette.posterFallback)
                        .border(0.5.dp, palette.glassBorder, RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Image(
                        painter = painterResource(R.drawable.ic_play),
                        contentDescription = null,
                        colorFilter = ColorFilter.tint(palette.textMuted),
                        modifier = Modifier.size(24.dp),
                    )
                }
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    BasicText(
                        text = "S1:E3",
                        maxLines = 1,
                        style = TextStyle(
                            color = palette.textMuted,
                            fontSize = TvText.Metadata,
                        ),
                    )
                    Spacer(Modifier.height(3.dp))
                    BasicText(
                        text = "危机四伏",
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
                        text = "将在 8 秒后开始播放",
                        maxLines = 1,
                        style = TextStyle(
                            color = palette.textMuted,
                            fontSize = TvText.Label,
                        ),
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                // 立即播放 button (primary)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(TvDp.ControlHeight)
                        .clip(RoundedCornerShape(TvDp.ControlRadius))
                        .background(palette.accent.copy(alpha = 0.85f))
                        .border(1.dp, palette.accentStrong, RoundedCornerShape(TvDp.ControlRadius))
                        .focusable()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { /* TODO */ },
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(R.drawable.ic_play),
                            contentDescription = null,
                            colorFilter = ColorFilter.tint(palette.focusText),
                            modifier = Modifier.size(12.dp),
                        )
                        Spacer(Modifier.width(4.dp))
                        BasicText(
                            text = "立即播放",
                            style = TextStyle(
                                color = palette.focusText,
                                fontSize = TvText.Body,
                                fontWeight = FontWeight.Medium,
                            ),
                        )
                    }
                }
                // 取消 button (secondary)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(TvDp.ControlHeight)
                        .clip(RoundedCornerShape(TvDp.ControlRadius))
                        .background(palette.glass)
                        .border(0.5.dp, palette.glassBorder, RoundedCornerShape(TvDp.ControlRadius))
                        .focusable()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { /* TODO */ },
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    BasicText(
                        text = "取消",
                        style = TextStyle(
                            color = palette.textSecondary,
                            fontSize = TvText.Body,
                            fontWeight = FontWeight.Medium,
                        ),
                    )
                }
            }
        }
    }
}
