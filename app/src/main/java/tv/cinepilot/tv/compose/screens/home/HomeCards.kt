package tv.cinepilot.tv.compose.screens.home

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import tv.cinepilot.core.protocol.MediaItemSummary
import tv.cinepilot.tv.compose.artwork.CinePilotAsyncImage
import tv.cinepilot.tv.compose.theme.CinePilotPalette
import tv.cinepilot.tv.compose.theme.TvDp
import tv.cinepilot.tv.compose.theme.TvText
import tv.cinepilot.tv.runtime.ArtworkRequestSpec
import tv.cinepilot.tv.ui.cardBadgeLabels

@Composable
internal fun HomePosterCard(
    palette: CinePilotPalette,
    item: MediaItemSummary,
    artwork: ArtworkRequestSpec?,
    focused: Boolean,
    onClick: () -> Unit = {},
) {
    HomeArtworkCard(
        palette = palette,
        item = item,
        artwork = artwork,
        focused = focused,
        width = TvDp.PosterWidth,
        height = TvDp.PosterHeight,
        overlayHeight = 68.dp,
        showProgressPercent = focused || item.hasResumePosition(),
        progressAtBottomEdge = false,
        onClick = onClick,
    )
}

@Composable
internal fun HomeLandscapeCard(
    palette: CinePilotPalette,
    item: MediaItemSummary,
    artwork: ArtworkRequestSpec?,
    focused: Boolean,
    width: Dp,
    height: Dp,
    onClick: () -> Unit = {},
) {
    HomeArtworkCard(
        palette = palette,
        item = item,
        artwork = artwork,
        focused = focused,
        width = width,
        height = height,
        overlayHeight = (height * 0.65f),
        showProgressPercent = item.hasResumePosition(),
        progressAtBottomEdge = true,
        onClick = onClick,
    )
}

@Composable
internal fun HomeArtworkCard(
    palette: CinePilotPalette,
    item: MediaItemSummary,
    artwork: ArtworkRequestSpec?,
    focused: Boolean,
    width: Dp,
    height: Dp,
    overlayHeight: Dp,
    showProgressPercent: Boolean,
    progressAtBottomEdge: Boolean,
    onClick: () -> Unit = {},
) {
    val shape = RoundedCornerShape(TvDp.CardRadius)
    val progress = item.resumeFraction()
    val badges = remember(item) { item.cardBadgeLabels() }

    // 缩放动画：聚焦时轻微放大
    val scale by animateFloatAsState(
        targetValue = if (focused) 1.04f else 1f,
        animationSpec = tween(140),
        label = "cardFocusScale",
    )

    // 光晕效果
    val glowModifier = if (focused) {
        Modifier.shadow(
            elevation = 16.dp,
            shape = shape,
            spotColor = palette.focusGlow,
            ambientColor = palette.focusGlow,
        )
    } else {
        Modifier
    }

    val interaction = remember { MutableInteractionSource() }

    Box(
        modifier = Modifier
            .width(width)
            .height(height)
            .then(glowModifier)
            .scale(scale)
            .clip(shape)
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick,
            )
            .background(palette.posterFallback, shape)
            .border(
                width = if (focused) TvDp.FocusRing else 0.5.dp,
                color = if (focused) palette.focusRing else palette.glassBorder,
                shape = shape,
            ),
    ) {
        CinePilotAsyncImage(
            request = artwork,
            contentDescription = item.name(),
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )

        // 底部渐变遮罩：更自然的过渡
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(overlayHeight)
                .background(
                    Brush.verticalGradient(
                        0.0f to palette.background.copy(alpha = 0.0f),
                        0.35f to palette.background.copy(alpha = 0.35f),
                        0.7f to palette.background.copy(alpha = 0.75f),
                        1.0f to palette.background.copy(alpha = 0.95f),
                    ),
                ),
        )

        // 技术徽章
        if (badges.isNotEmpty()) {
            val badgeAlignment = if (progressAtBottomEdge) Alignment.TopStart else Alignment.BottomStart
            val badgePadding = if (progressAtBottomEdge) {
                PaddingValues(8.dp)
            } else {
                PaddingValues(start = 8.dp, bottom = 8.dp)
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier
                    .align(badgeAlignment)
                    .padding(badgePadding),
            ) {
                badges.take(2).forEach { badge ->
                    CardBadgeChip(text = badge, palette = palette)
                }
            }
        }

        if (progressAtBottomEdge && progress > 0f) {
            // 横版卡片：进度条紧贴底部边缘，百分比在进度条上方右侧
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth(),
            ) {
                // 进度百分比：进度条上方右侧
                if (showProgressPercent) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 2.dp),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        BasicText(
                            text = "${(progress * 100f).toInt()}%",
                            maxLines = 1,
                            style = TextStyle(
                                color = palette.textPrimary.copy(alpha = 0.9f),
                                fontSize = TvText.Label,
                                fontWeight = FontWeight.SemiBold,
                            ),
                        )
                    }
                }
                // 进度条：紧贴最底部边缘，横跨全宽
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.5.dp)
                        .background(palette.textPrimary.copy(alpha = 0.15f)),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progress)
                            .fillMaxHeight()
                            .background(palette.accent),
                    )
                }
            }

            // 文字信息：位于渐变遮罩上方区域（在进度条之上）
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .padding(
                        start = 10.dp,
                        end = 10.dp,
                        bottom = if (progress > 0f) 18.dp else 10.dp,
                    ),
            ) {
                BasicText(
                    text = item.name().ifBlank { item.id() },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = TextStyle(
                        color = palette.textPrimary,
                        fontSize = TvText.CardTitle,
                        fontWeight = FontWeight.SemiBold,
                    ),
                )
                Spacer(Modifier.height(3.dp))
                BasicText(
                    text = item.cardMetaLine(),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = TextStyle(color = palette.textMuted, fontSize = TvText.Metadata),
                )
            }
        } else {
            // 海报卡片 / 默认样式：文字信息在底部，角标在最左下角
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .padding(
                        start = 10.dp,
                        end = 10.dp,
                        bottom = if (badges.isNotEmpty()) 32.dp else 10.dp,
                    ),
            ) {
                BasicText(
                    text = item.name().ifBlank { item.id() },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = TextStyle(
                        color = palette.textPrimary,
                        fontSize = TvText.CardTitle,
                        fontWeight = FontWeight.SemiBold,
                    ),
                )
                Spacer(Modifier.height(3.dp))
                BasicText(
                    text = item.cardMetaLine(),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = TextStyle(color = palette.textMuted, fontSize = TvText.Metadata),
                )
                // 播放进度条
                if (progress > 0f && !progressAtBottomEdge) {
                    Spacer(Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(3.dp)
                                .clip(RoundedCornerShape(1.5.dp))
                                .background(palette.textPrimary.copy(alpha = 0.18f)),
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(progress)
                                    .fillMaxSize()
                                    .background(palette.accent),
                            )
                        }
                        if (showProgressPercent) {
                            Spacer(Modifier.width(6.dp))
                            BasicText(
                                text = "${(progress * 100f).toInt()}%",
                                maxLines = 1,
                                style = TextStyle(
                                    color = palette.textMuted,
                                    fontSize = TvText.Label,
                                    fontWeight = FontWeight.Medium,
                                ),
                            )
                        }
                    }
                }
            }
        }
    }
}
