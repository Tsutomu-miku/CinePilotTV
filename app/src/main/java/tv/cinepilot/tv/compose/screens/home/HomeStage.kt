package tv.cinepilot.tv.compose.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import tv.cinepilot.tv.compose.artwork.CinePilotAsyncImage
import tv.cinepilot.tv.compose.components.InfoPanel
import tv.cinepilot.tv.compose.components.TvActionButton
import tv.cinepilot.tv.compose.theme.CinePilotPalette
import tv.cinepilot.tv.compose.theme.TvDp
import tv.cinepilot.tv.runtime.ArtworkRequestSpec

@Composable
internal fun HomeWallStage(
    palette: CinePilotPalette,
    backdrop: ArtworkRequestSpec?,
    content: @Composable ColumnScope.() -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.background),
    ) {
        if (backdrop != null) {
            CinePilotAsyncImage(
                request = backdrop,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
        // 整体暗化遮罩
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(palette.scrim.copy(alpha = 0.78f)),
        )
        // 左侧渐变：更宽、过渡更柔和
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        0.0f to palette.background.copy(alpha = 0.96f),
                        0.35f to palette.background.copy(alpha = 0.82f),
                        0.6f to palette.background.copy(alpha = 0.5f),
                        0.85f to palette.background.copy(alpha = 0.18f),
                        1.0f to palette.background.copy(alpha = 0.05f),
                    ),
                ),
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 40.dp, top = 24.dp, end = 40.dp, bottom = 0.dp),
        ) {
            content()
        }
    }
}

@Composable
internal fun EmptyHome(
    palette: CinePilotPalette,
    searchResults: Boolean,
    navigation: ComposeHomeNavigation,
) {
    InfoPanel(
        palette = palette,
        title = if (searchResults) "没有找到匹配的媒体" else "没有可显示的媒体",
        body = if (searchResults) "可以重新搜索，或返回首页浏览媒体库。" else "可以刷新首页，或切换账号后重试。",
    )
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
        TvActionButton(
            palette = palette,
            label = if (searchResults) "重新搜索" else "刷新",
            selected = true,
            requestInitialFocus = true,
            modifier = Modifier.weight(1f),
            onClick = if (searchResults) navigation.onSearch else navigation.onRefresh,
        )
        if (searchResults && navigation.canGoBack) {
            TvActionButton(
                palette = palette,
                label = "返回首页",
                modifier = Modifier.weight(1f),
                onClick = navigation.onBackInBrowse,
            )
        }
        TvActionButton(
            palette = palette,
            label = "切换账号",
            modifier = Modifier.weight(1f),
            onClick = navigation.onSwitchAccount,
        )
    }
}
