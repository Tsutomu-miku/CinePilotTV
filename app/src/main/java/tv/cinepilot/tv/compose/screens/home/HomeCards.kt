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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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

/** 聚焦时的轻微放大倍率。边界通过外层 2dp 白边表达。 */
private const val FocusedCardScale = 1.025f
/** 外层呼吸空间，给放大 + 聚焦描边留位置。 */
private val CardBleedPadding = 8.dp
/** 聚焦描边的视觉宽度（在卡片之外画一圈）。 */
private val FocusedRingWidth = 2.5.dp
/** 进度条厚度。 */
private val ProgressThickness = 3.5.dp

// ── 9:16 海报（电影/剧集主视图） ────────────────────────────────────────

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
        titleMaxLines = 2,
        onClick = onClick,
    )
}

// ── 16:9 横版（继续观看 / 下一集 / 最近添加 / 收藏夹 / 合集） ─────────────

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
        titleMaxLines = 1,
        onClick = onClick,
    )
}

/**
 * 统一的卡片视觉。
 *
 * 外层结构：
 *   Box(w+2*Bleed, h+2*Bleed)          —— 给放大和描边留呼吸空间
 *     ├─ 聚焦时：白边描边 Box           —— 位于 (w+2*Ring) × (h+2*Ring)，画在卡片外
 *     └─ 内层 content Box (w×h)         —— 真正的卡片（海报、渐变、文字、进度）
 *
 * 内层只做 `.scale(FocusedCardScale)`，不再 clip 外层，这样描边 + 光晕不会被裁切。
 *
 * 文字/徽章/进度统一排版（从上到下）：
 *   Title (1~2 行)
 *   Meta 行
 *   徽章行（如有）
 *   圆角进度条 + 行尾百分比
 */
@Composable
internal fun HomeArtworkCard(
    palette: CinePilotPalette,
    item: MediaItemSummary,
    artwork: ArtworkRequestSpec?,
    focused: Boolean,
    width: Dp,
    height: Dp,
    titleMaxLines: Int,
    onClick: () -> Unit = {},
) {
    val shape = RoundedCornerShape(TvDp.CardRadius)
    val progress = item.resumeFraction()
    val badges = remember(item) { item.cardBadgeLabels() }
    val showProgress = progress > 0f
    val showProgressPercent = focused || showProgress

    val scale by animateFloatAsState(
        targetValue = if (focused) FocusedCardScale else 1f,
        animationSpec = tween(140),
        label = "homeCardScale",
    )
    val interaction = remember { MutableInteractionSource() }

    Box(
        modifier = Modifier
            .width(width + CardBleedPadding * 2)
            .height(height + CardBleedPadding * 2),
        contentAlignment = Alignment.Center,
    ) {
        // ── 聚焦描边：画在 (w + 2*Ring) 外层，不进入卡片 clip ─────────
        if (focused) {
            Box(
                modifier = Modifier
                    .width(width + FocusedRingWidth * 2)
                    .height(height + FocusedRingWidth * 2)
                    .scale(scale)
                    .clip(shape)
                    .background(Color.Transparent, shape)
                    .border(FocusedRingWidth, palette.focusRing, shape),
            )
        }

        // ── 内容卡片 ─────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .width(width)
                .height(height)
                .scale(scale)
                .clip(shape)
                .clickable(
                    interactionSource = interaction,
                    indication = null,
                    onClick = onClick,
                )
                .background(palette.posterFallback, shape)
                .border(
                    width = if (focused) 0.dp else 1.dp,
                    color = if (focused) Color.Transparent
                    else palette.glassBorder.copy(alpha = 0.5f),
                    shape = shape,
                ),
        ) {
            CinePilotAsyncImage(
                request = artwork,
                contentDescription = item.name(),
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )

            // 聚焦时的淡色高光蒙层，配合外描边。
            if (focused) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(Color.White.copy(alpha = 0.10f)),
                )
            }

            // 底部渐变遮罩。
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(height * 0.72f)
                    .background(
                        Brush.verticalGradient(
                            0.0f to Color.Transparent,
                            0.45f to palette.background.copy(alpha = 0.55f),
                            1.0f to palette.background.copy(alpha = 0.92f),
                        ),
                    ),
            )

            // 标题 / 元数据 / 徽章 / 进度 —— 统一一列，贴底缘。
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .padding(
                        start = 10.dp,
                        end = 10.dp,
                        bottom = 10.dp,
                    ),
            ) {
                BasicText(
                    text = item.name().ifBlank { item.id() },
                    maxLines = titleMaxLines,
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
                    style = TextStyle(
                        color = palette.textMuted,
                        fontSize = TvText.Metadata,
                    ),
                )
                if (badges.isNotEmpty()) {
                    Spacer(Modifier.height(5.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        badges.take(2).forEach { badge ->
                            CardBadgeChip(text = badge, palette = palette)
                        }
                    }
                }
                if (showProgress) {
                    Spacer(Modifier.height(6.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(ProgressThickness)
                                .clip(RoundedCornerShape(ProgressThickness / 2))
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

// ── 合集 / 收藏夹 等非媒体卡：16:9 横版但深色半透明罩 + 居中大字号标题
//    （避免 artwork 里的 logo / 背景文字与标题重叠）

@Composable
internal fun HomeCollectionCard(
    palette: CinePilotPalette,
    item: MediaItemSummary,
    artwork: ArtworkRequestSpec?,
    focused: Boolean,
    onClick: () -> Unit = {},
) {
    val width = TvDp.LandscapeWidth
    val height = TvDp.LandscapeHeight
    val shape = RoundedCornerShape(TvDp.CardRadius)

    val scale by animateFloatAsState(
        targetValue = if (focused) FocusedCardScale else 1f,
        animationSpec = tween(140),
        label = "collectionCardScale",
    )
    val interaction = remember { MutableInteractionSource() }

    Box(
        modifier = Modifier
            .width(width + CardBleedPadding * 2)
            .height(height + CardBleedPadding * 2),
        contentAlignment = Alignment.Center,
    ) {
        // 聚焦描边
        if (focused) {
            Box(
                modifier = Modifier
                    .width(width + FocusedRingWidth * 2)
                    .height(height + FocusedRingWidth * 2)
                    .scale(scale)
                    .clip(shape)
                    .background(Color.Transparent, shape)
                    .border(FocusedRingWidth, palette.focusRing, shape),
            )
        }

        Box(
            modifier = Modifier
                .width(width)
                .height(height)
                .scale(scale)
                .clip(shape)
                .clickable(
                    interactionSource = interaction,
                    indication = null,
                    onClick = onClick,
                )
                .background(palette.posterFallback, shape)
                .border(
                    width = if (focused) 0.dp else 1.dp,
                    color = if (focused) Color.Transparent
                    else palette.glassBorder.copy(alpha = 0.55f),
                    shape = shape,
                ),
        ) {
            if (artwork != null) {
                CinePilotAsyncImage(
                    request = artwork,
                    contentDescription = item.name(),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            // 合集罩层：深色 + 统一品牌感（避免 artwork 背景图干扰文字）
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0.0f to Color.Black.copy(alpha = 0.55f),
                            0.5f to Color.Black.copy(alpha = 0.72f),
                            1.0f to Color.Black.copy(alpha = 0.85f),
                        ),
                    ),
            )
            if (focused) {
                Box(Modifier.fillMaxSize().background(Color.White.copy(alpha = 0.06f)))
            }
            // 合集名：居中，加粗，字号比普通横版大
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 14.dp),
                contentAlignment = Alignment.Center,
            ) {
                BasicText(
                    text = item.name().ifBlank { item.id() },
                    maxLines = 2,
                    minLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = TextStyle(
                        color = palette.textPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp,
                    ),
                )
            }
        }
    }
}
