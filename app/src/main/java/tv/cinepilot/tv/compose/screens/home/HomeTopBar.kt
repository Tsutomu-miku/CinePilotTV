package tv.cinepilot.tv.compose.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import tv.cinepilot.tv.R
import tv.cinepilot.tv.compose.theme.CinePilotPalette
import tv.cinepilot.tv.compose.theme.TvDp
import tv.cinepilot.tv.compose.theme.TvText

@Composable
internal fun HomeTopBar(palette: CinePilotPalette, title: String, navigation: ComposeHomeNavigation) {
    val topBarHeight = 56.dp
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(topBarHeight)
            .clip(RoundedCornerShape(TvDp.ControlRadius))
            .background(
                Brush.verticalGradient(
                    listOf(
                        palette.background.copy(alpha = 0.72f),
                        palette.background.copy(alpha = 0.56f),
                    ),
                ),
            )
            .border(0.5.dp, palette.glassBorder.copy(alpha = 0.35f), RoundedCornerShape(TvDp.ControlRadius)),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 18.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f),
            ) {
                // Brand: CinePilot TV
                Row(verticalAlignment = Alignment.Bottom) {
                    BasicText(
                        text = "CinePilot",
                        maxLines = 1,
                        style = TextStyle(
                            color = palette.textPrimary,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                        ),
                    )
                    BasicText(
                        text = " TV",
                        maxLines = 1,
                        style = TextStyle(
                            color = palette.accentStrong,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                        ),
                    )
                }
                Spacer(Modifier.width(14.dp))
                BasicText(
                    text = "/",
                    maxLines = 1,
                    style = TextStyle(color = palette.textMuted, fontSize = TvText.Body),
                )
                Spacer(Modifier.width(14.dp))
                BasicText(
                    text = title,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = TextStyle(
                        color = palette.textPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                    ),
                )
                Spacer(Modifier.width(14.dp))
                BasicText(
                    text = "|",
                    maxLines = 1,
                    style = TextStyle(color = palette.textMuted, fontSize = TvText.Metadata),
                )
                Spacer(Modifier.width(14.dp))
                BasicText(
                    text = "媒体库  Cinema Library",
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = TextStyle(color = palette.textSecondary, fontSize = TvText.Body),
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                homeActions(navigation).forEach { action ->
                    HeaderIconButton(
                        palette = palette,
                        iconRes = action.iconRes,
                        contentDescription = action.label,
                        onClick = action.onClick,
                    )
                }
            }
        }
    }
}

private data class HomeAction(val label: String, val iconRes: Int, val onClick: () -> Unit)

private fun homeActions(navigation: ComposeHomeNavigation): List<HomeAction> {
    return buildList {
        if (navigation.canGoBack) add(HomeAction("返回", R.drawable.ic_back, navigation.onBackInBrowse))
        if (navigation.canPageBackward) add(HomeAction("上一页", R.drawable.ic_back, navigation.onPreviousPage))
        if (navigation.canPageForward) add(HomeAction("下一页", R.drawable.ic_forward, navigation.onNextPage))
        add(HomeAction("搜索", R.drawable.ic_search, navigation.onSearch))
        add(HomeAction("刷新", R.drawable.ic_refresh, navigation.onRefresh))
        add(HomeAction("账号", R.drawable.ic_account, navigation.onSwitchAccount))
        add(HomeAction("设置", R.drawable.ic_settings, navigation.onSettings))
        add(HomeAction("退出", R.drawable.ic_logout, navigation.onLogout))
    }
}

@Composable
private fun HeaderIconButton(
    palette: CinePilotPalette,
    iconRes: Int,
    contentDescription: String,
    onClick: () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(TvDp.ControlRadius)
    val glowModifier = if (focused) {
        Modifier.shadow(
            elevation = 8.dp,
            shape = shape,
            spotColor = palette.focusGlow,
            ambientColor = palette.focusGlow,
        )
    } else {
        Modifier
    }
    Box(
        modifier = Modifier
            .size(TvDp.IconButtonSize)
            .then(glowModifier)
            .clip(shape)
            .background(if (focused) palette.glassFocus else palette.glass.copy(alpha = 0.0f), shape)
            .border(
                width = if (focused) TvDp.FocusRing else 0.dp,
                color = if (focused) palette.focusRing else palette.glassBorder.copy(alpha = 0f),
                shape = shape,
            )
            .onFocusChanged { focused = it.isFocused }
            .focusable()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        androidx.compose.foundation.Image(
            painter = painterResource(iconRes),
            contentDescription = contentDescription,
            colorFilter = ColorFilter.tint(if (focused) palette.accentStrong else palette.textPrimary),
            modifier = Modifier.size(20.dp),
        )
    }
}
