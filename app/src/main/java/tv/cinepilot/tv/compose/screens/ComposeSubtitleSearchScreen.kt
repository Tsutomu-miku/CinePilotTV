package tv.cinepilot.tv.compose.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import tv.cinepilot.plugin.spi.SubtitleSearchResult
import tv.cinepilot.tv.compose.components.FocusSurface
import tv.cinepilot.tv.compose.components.TvActionButton
import tv.cinepilot.tv.compose.theme.CinePilotPalette
import tv.cinepilot.tv.compose.theme.TvDp
import tv.cinepilot.tv.compose.theme.TvText

@Composable
fun ComposeSubtitleSearchScreen(
    palette: CinePilotPalette,
    itemName: String,
    results: List<SubtitleSearchResult>,
    isLoading: Boolean,
    selectedId: String? = null,
    onPick: (SubtitleSearchResult) -> Unit,
    onClose: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(androidx.compose.ui.graphics.Color.Black),
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight()
                .width(420.dp)
                .background(
                    Brush.horizontalGradient(
                    listOf(
                        palette.background.copy(alpha = 0.12f),
                        palette.background.copy(alpha = 0.92f),
                        palette.background.copy(alpha = 0.98f),
                    ),
                ),
            )
                .padding(horizontal = 28.dp, vertical = 24.dp),
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                BasicText(
                    text = "搜索在线字幕",
                    maxLines = 1,
                    style = TextStyle(color = palette.accentStrong, fontSize = TvText.Section),
                )
                BasicText(
                    text = itemName,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = TextStyle(color = palette.textSecondary, fontSize = TvText.Body),
                    modifier = Modifier.padding(top = 4.dp, bottom = 14.dp),
                )
                when {
                    isLoading -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            BasicText(
                                text = "正在搜索…",
                                style = TextStyle(color = palette.textMuted, fontSize = TvText.Body),
                            )
                        }
                    }
                    results.isEmpty() -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            BasicText(
                                text = "未找到匹配的字幕",
                                style = TextStyle(color = palette.textMuted, fontSize = TvText.Body),
                            )
                        }
                    }
                    else -> {
                        LazyColumn(
                            contentPadding = PaddingValues(bottom = TvDp.ScreenBottom),
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            items(
                                items = results,
                                key = { it.id() },
                            ) { result ->
                                val isSelected = result.id() == selectedId
                                SubtitleResultRow(
                                    palette = palette,
                                    result = result,
                                    isSelected = isSelected,
                                    onClick = { onPick(result) },
                                )
                            }
                        }
                    }
                }
                TvActionButton(
                    palette = palette,
                    label = "返回",
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onClose,
                )
            }
        }
    }
}

@Composable
private fun SubtitleResultRow(
    palette: CinePilotPalette,
    result: SubtitleSearchResult,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    FocusSurface(
        palette = palette,
        selected = isSelected,
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        radius = TvDp.ControlRadius.dp,
        padding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
    ) { _ ->
        Column {
            BasicText(
                text = result.name(),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = TextStyle(
                    color = if (isSelected) palette.accentStrong else palette.textPrimary,
                    fontSize = TvText.Body,
                ),
            )
            val secondary = buildString {
                if (result.language().isNotBlank()) {
                    append(result.language())
                }
                val formatLabel = formatLabel(result.format())
                if (formatLabel.isNotBlank()) {
                    if (isNotEmpty()) append(" · ")
                    append(formatLabel)
                }
                if (result.rating().isNotBlank()) {
                    if (isNotEmpty()) append(" · ")
                    append(result.rating())
                }
                if (result.downloadCount() > 0) {
                    if (isNotEmpty()) append(" · ")
                    append("${result.downloadCount()} 次下载")
                }
            }
            if (secondary.isNotBlank()) {
                BasicText(
                    text = secondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = TextStyle(color = palette.textSecondary, fontSize = TvText.Label),
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
            if (result.author().isNotBlank()) {
                BasicText(
                    text = "上传者：${result.author()}",
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = TextStyle(color = palette.textMuted, fontSize = TvText.Label),
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
    }
}

private fun formatLabel(format: SubtitleSearchResult.Format): String =
    when (format) {
        SubtitleSearchResult.Format.SRT -> "SRT"
        SubtitleSearchResult.Format.ASS -> "ASS"
        SubtitleSearchResult.Format.SSA -> "SSA"
        SubtitleSearchResult.Format.VTT -> "WebVTT"
        SubtitleSearchResult.Format.PGS -> "PGS"
        SubtitleSearchResult.Format.UNKNOWN -> ""
    }
