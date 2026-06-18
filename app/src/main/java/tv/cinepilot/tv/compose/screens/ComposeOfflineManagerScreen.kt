package tv.cinepilot.tv.compose.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import tv.cinepilot.core.protocol.OfflineRepository
import tv.cinepilot.tv.compose.components.FocusSurface
import tv.cinepilot.tv.compose.components.TvActionButton
import tv.cinepilot.tv.compose.theme.CinePilotPalette
import tv.cinepilot.tv.compose.theme.TvDp
import tv.cinepilot.tv.compose.theme.TvText

@Composable
fun ComposeOfflineManagerScreen(
    palette: CinePilotPalette,
    entries: List<OfflineRepository.Entry>,
    onClose: () -> Unit,
    onPause: (OfflineRepository.Entry) -> Unit,
    onResume: (OfflineRepository.Entry) -> Unit,
    onDelete: (OfflineRepository.Entry) -> Unit,
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
                    text = "离线管理",
                    maxLines = 1,
                    style = TextStyle(color = palette.accentStrong, fontSize = TvText.Section),
                )
                BasicText(
                    text = "离线内容保存在本机存储中，无需联网即可播放。",
                    style = TextStyle(color = palette.textSecondary, fontSize = TvText.Label),
                    modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
                )
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = TvDp.ScreenBottom),
                ) {
                    items(
                        items = entries,
                        key = { it.quality() },
                    ) { entry ->
                        OfflineEntryRow(
                            palette = palette,
                            entry = entry,
                            onPause = onPause,
                            onResume = onResume,
                            onDelete = onDelete,
                        )
                    }
                }
                TvActionButton(
                    palette = palette,
                    label = "返回详情",
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onClose,
                )
            }
        }
    }
}

@Composable
private fun OfflineEntryRow(
    palette: CinePilotPalette,
    entry: OfflineRepository.Entry,
    onPause: (OfflineRepository.Entry) -> Unit,
    onResume: (OfflineRepository.Entry) -> Unit,
    onDelete: (OfflineRepository.Entry) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BasicText(
                text = qualityLabelFor(entry.quality()),
                maxLines = 1,
                style = TextStyle(
                    color = palette.textPrimary,
                    fontSize = TvText.Body,
                ),
                modifier = Modifier.width(110.dp),
            )
            BasicText(
                text = stateTextFor(entry),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = TextStyle(
                    color = palette.textSecondary,
                    fontSize = TvText.Label,
                ),
                modifier = Modifier.weight(1f),
            )
            BasicText(
                text = sizeTextFor(entry),
                maxLines = 1,
                style = TextStyle(
                    color = palette.textMuted,
                    fontSize = TvText.Label,
                ),
            )
        }

        if (entry.state() == OfflineRepository.State.DOWNLOADING) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp, bottom = 8.dp)
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(palette.glass),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(entry.progressPercent() / 100f)
                        .background(palette.accent),
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            val primaryLabel = primaryActionLabelFor(entry.state())
            if (primaryLabel != null) {
                val isPrimary = entry.state() != OfflineRepository.State.QUEUED &&
                        entry.state() != OfflineRepository.State.DOWNLOADING
                FocusSurface(
                    palette = palette,
                    selected = isPrimary,
                    modifier = Modifier.height(TvDp.ControlHeight),
                    padding = PaddingValues(horizontal = 16.dp),
                    onClick = {
                        when (entry.state()) {
                            OfflineRepository.State.QUEUED,
                            OfflineRepository.State.DOWNLOADING -> onPause(entry)
                            OfflineRepository.State.PAUSED,
                            OfflineRepository.State.FAILED -> onResume(entry)
                            OfflineRepository.State.READY -> {}
                        }
                    },
                ) {
                    BasicText(
                        text = primaryLabel,
                        maxLines = 1,
                        style = TextStyle(
                            color = if (isPrimary) palette.textPrimary else palette.textSecondary,
                            fontSize = TvText.Body,
                        ),
                    )
                }
            }
            FocusSurface(
                palette = palette,
                modifier = Modifier.height(TvDp.ControlHeight),
                padding = PaddingValues(horizontal = 16.dp),
                onClick = { onDelete(entry) },
            ) {
                BasicText(
                    text = "删除",
                    maxLines = 1,
                    style = TextStyle(
                        color = palette.textSecondary,
                        fontSize = TvText.Body,
                    ),
                )
            }
        }
    }
}

private fun primaryActionLabelFor(state: OfflineRepository.State): String? =
    when (state) {
        OfflineRepository.State.QUEUED,
        OfflineRepository.State.DOWNLOADING -> "暂停"
        OfflineRepository.State.PAUSED -> "继续"
        OfflineRepository.State.FAILED -> "重试"
        OfflineRepository.State.READY -> null
    }

private fun qualityLabelFor(q: Int): String = when (q) {
    1 -> "480p"
    2 -> "720p"
    3 -> "1080p"
    4 -> "4K"
    else -> "自动"
}

private fun stateTextFor(entry: OfflineRepository.Entry): String = when (entry.state()) {
    OfflineRepository.State.QUEUED -> "队列中"
    OfflineRepository.State.DOWNLOADING ->
        String.format("下载中 %.0f%%", entry.progressPercent())
    OfflineRepository.State.READY -> "已就绪"
    OfflineRepository.State.FAILED ->
        "失败：${entry.errorMessage().ifBlank { "未知错误" }}"
    OfflineRepository.State.PAUSED -> "已暂停"
}

private fun sizeTextFor(entry: OfflineRepository.Entry): String =
    if (entry.bytesTotal() > 0) {
        "共 ${humanizeBytes(entry.bytesTotal())}"
    } else {
        "估算容量中..."
    }

private fun humanizeBytes(bytes: Long): String = when {
    bytes >= 1024L * 1024 * 1024 * 1024 -> String.format("%.1f TB", bytes / (1024e12))
    bytes >= 1024L * 1024 * 1024 -> String.format("%.1f GB", bytes / (1024e9))
    bytes >= 1024L * 1024 -> String.format("%.1f MB", bytes / (1024e6))
    bytes >= 1024L -> String.format("%.1f KB", bytes / (1024e3))
    else -> "${bytes}B"
}
