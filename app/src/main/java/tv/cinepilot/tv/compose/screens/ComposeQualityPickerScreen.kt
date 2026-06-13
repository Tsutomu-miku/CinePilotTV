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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import tv.cinepilot.tv.compose.components.FocusSurface
import tv.cinepilot.tv.compose.components.TvActionButton
import tv.cinepilot.tv.compose.theme.CinePilotPalette
import tv.cinepilot.tv.compose.theme.TvDp
import tv.cinepilot.tv.compose.theme.TvText

@Composable
fun ComposeQualityPickerScreen(
    palette: CinePilotPalette,
    labels: List<String>,
    values: List<Int>,
    default: Int,
    onChosen: (Int) -> Unit,
    onClose: () -> Unit,
) {
    require(labels.size == values.size) { "labels / values length mismatch" }

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
                    text = "选择离线画质",
                    maxLines = 1,
                    style = TextStyle(color = palette.accentStrong, fontSize = TvText.Section),
                )
                BasicText(
                    text = "下载的码率越高，画面越清晰，但占用的磁盘空间也越大。",
                    style = TextStyle(
                        color = palette.textSecondary,
                        fontSize = TvText.Label,
                        lineHeight = TvText.Body * 1.05f,
                    ),
                    modifier = Modifier.padding(top = 4.dp, bottom = 14.dp),
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    labels.zip(values).forEachIndexed { index, (label, value) ->
                        val isSelected = value == default
                        QualityOptionCard(
                            palette = palette,
                            label = label,
                            description = descriptionForQuality(value),
                            selected = isSelected,
                            requestInitialFocus = index == 0 && value == default,
                            modifier = Modifier.weight(1f),
                            onClick = { onChosen(value) },
                        )
                    }
                }
                TvActionButton(
                    palette = palette,
                    label = "取消",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 20.dp),
                    onClick = onClose,
                )
            }
        }
    }
}

@Composable
private fun QualityOptionCard(
    palette: CinePilotPalette,
    label: String,
    description: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    requestInitialFocus: Boolean = false,
    onClick: () -> Unit,
) {
    FocusSurface(
        palette = palette,
        selected = selected,
        requestInitialFocus = requestInitialFocus,
        modifier = modifier,
        radius = TvDp.ControlRadius,
        padding = PaddingValues(horizontal = 10.dp, vertical = 14.dp),
        onClick = onClick,
    ) { focused ->
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth(),
        ) {
            BasicText(
                text = label,
                maxLines = 1,
                style = TextStyle(
                    color = when {
                        focused -> palette.accentStrong
                        selected -> palette.textPrimary
                        else -> palette.textSecondary
                    },
                    fontSize = TvText.Body,
                ),
            )
            BasicText(
                text = description,
                maxLines = 2,
                style = TextStyle(
                    color = palette.textMuted,
                    fontSize = TvText.Label,
                ),
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

private fun descriptionForQuality(value: Int): String = when (value) {
    1 -> "约 150 MB / 小时"
    2 -> "约 800 MB / 小时"
    3 -> "约 1.8 GB / 小时"
    4 -> "约 4 GB / 小时"
    else -> "根据片源自动选择"
}
