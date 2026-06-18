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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import tv.cinepilot.tv.R
import tv.cinepilot.tv.compose.theme.CinePilotPalette
import tv.cinepilot.tv.compose.theme.TvDp
import tv.cinepilot.tv.compose.theme.TvText

@Composable
internal fun ChapterStripBar(
    palette: CinePilotPalette,
    modifier: Modifier = Modifier,
) {
    val chapters = listOf(
        "序幕" to "00:00",
        "黑雨降临" to "08:42",
        "追踪线索" to "17:05",
        "危机四伏" to "26:31",
        "真相浮现" to "35:48",
        "抉择时刻" to "44:12",
    )
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier,
    ) {
        // Chapter label
        Box(
            modifier = Modifier
                .height(48.dp)
                .padding(end = 8.dp),
            contentAlignment = Alignment.Center,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = painterResource(R.drawable.ic_thumbnail),
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(palette.textMuted),
                    modifier = Modifier.size(14.dp),
                )
                Spacer(Modifier.width(4.dp))
                BasicText(
                    text = "章节",
                    style = TextStyle(
                        color = palette.textSecondary,
                        fontSize = TvText.Body,
                    ),
                )
            }
        }
        // Chapter chips
        chapters.forEachIndexed { index, (title, time) ->
            val isCurrent = index == 1
            ChapterChip(
                palette = palette,
                number = (index + 1).toString(),
                title = title,
                time = time,
                selected = isCurrent,
                onClick = { /* TODO */ },
            )
            if (index < chapters.lastIndex) {
                Spacer(Modifier.width(6.dp))
            }
        }
        // More button
        Spacer(Modifier.width(6.dp))
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(palette.glass)
                .border(0.5.dp, palette.glassBorder, RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(R.drawable.ic_forward),
                contentDescription = null,
                colorFilter = ColorFilter.tint(palette.textMuted),
                modifier = Modifier.size(14.dp),
            )
        }
    }
}

@Composable
private fun ChapterChip(
    palette: CinePilotPalette,
    number: String,
    title: String,
    time: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(8.dp)
    Box(
        modifier = Modifier
            .width(96.dp)
            .height(48.dp)
            .clip(shape)
            .background(
                when {
                    focused -> palette.glassFocus
                    selected -> palette.accent.copy(alpha = 0.3f)
                    else -> palette.glass
                },
                shape,
            )
            .border(
                width = if (focused) TvDp.FocusRing else if (selected) 1.dp else 0.5.dp,
                color = when {
                    focused -> palette.focusRing
                    selected -> palette.accentStrong
                    else -> palette.glassBorder
                },
                shape,
            )
            .onFocusChanged { focused = it.isFocused }
            .focusable()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 8.dp, vertical = 6.dp),
    ) {
        Column(verticalArrangement = Arrangement.Center) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                BasicText(
                    text = number,
                    style = TextStyle(
                        color = if (selected) palette.accentStrong else palette.textMuted,
                        fontSize = TvText.Label,
                        fontWeight = FontWeight.SemiBold,
                    ),
                )
                Spacer(Modifier.width(3.dp))
                BasicText(
                    text = title,
                    maxLines = 1,
                    style = TextStyle(
                        color = if (focused) palette.textPrimary else palette.textSecondary,
                        fontSize = TvText.Label,
                    ),
                )
            }
            Spacer(Modifier.height(2.dp))
            BasicText(
                text = time,
                style = TextStyle(
                    color = palette.textMuted,
                    fontSize = TvText.Label,
                ),
            )
        }
    }
}
