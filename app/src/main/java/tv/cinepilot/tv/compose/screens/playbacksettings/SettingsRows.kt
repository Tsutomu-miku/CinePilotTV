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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import tv.cinepilot.tv.R
import tv.cinepilot.tv.compose.theme.CinePilotPalette
import tv.cinepilot.tv.compose.theme.TvDp
import tv.cinepilot.tv.compose.theme.TvText

@Composable
internal fun IconSettingRow(
    palette: CinePilotPalette,
    iconRes: Int,
    label: String,
    value: String,
    description: String = "",
    selected: Boolean = false,
    requestInitialFocus: Boolean = false,
    onClick: () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(TvDp.ControlRadius)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(TvDp.SettingsRowHeight)
            .clip(shape)
            .background(
                when {
                    focused -> palette.glassFocus
                    selected -> palette.accent.copy(alpha = 0.25f)
                    else -> palette.glass
                },
                shape,
            )
            .border(
                width = if (focused) TvDp.FocusRing else 0.5.dp,
                color = when {
                    focused -> palette.focusRing
                    selected -> palette.accentStrong
                    else -> palette.glassBorder
                },
                shape,
            )
            .shadow(
                elevation = if (focused) 10.dp else 0.dp,
                shape = shape,
                spotColor = if (focused) palette.focusGlow else Color.Transparent,
                ambientColor = if (focused) palette.focusGlow else Color.Transparent,
            )
            .onFocusChanged { focused = it.isFocused }
            .focusable()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(
                painter = painterResource(iconRes),
                contentDescription = null,
                colorFilter = ColorFilter.tint(
                    if (focused || selected) palette.accentStrong else palette.textSecondary
                ),
                modifier = Modifier.size(16.dp),
            )
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                BasicText(
                    text = label,
                    maxLines = 1,
                    style = TextStyle(
                        color = if (focused) palette.textPrimary else palette.textSecondary,
                        fontSize = TvText.Body,
                    ),
                )
                if (description.isNotBlank()) {
                    Spacer(Modifier.height(3.dp))
                    BasicText(
                        text = description,
                        maxLines = 1,
                        style = TextStyle(
                            color = palette.textMuted,
                            fontSize = TvText.Label,
                        ),
                    )
                }
            }
            Spacer(Modifier.width(8.dp))
            BasicText(
                text = value,
                maxLines = 1,
                style = TextStyle(
                    color = if (focused || selected) palette.accentStrong else palette.textMuted,
                    fontSize = TvText.Metadata,
                ),
            )
            Spacer(Modifier.width(4.dp))
            Image(
                painter = painterResource(R.drawable.ic_forward),
                contentDescription = null,
                colorFilter = ColorFilter.tint(
                    if (focused || selected) palette.accentStrong else palette.textMuted
                ),
                modifier = Modifier.size(12.dp),
            )
        }
    }
}

@Composable
internal fun IconToggleRow(
    palette: CinePilotPalette,
    iconRes: Int,
    label: String,
    description: String = "",
    checked: Boolean,
    requestInitialFocus: Boolean = false,
    onCheckedChange: (Boolean) -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(TvDp.ControlRadius)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(TvDp.SettingsRowHeight)
            .clip(shape)
            .background(
                if (focused) palette.glassFocus else palette.glass,
                shape,
            )
            .border(
                width = if (focused) TvDp.FocusRing else 0.5.dp,
                color = if (focused) palette.focusRing else palette.glassBorder,
                shape,
            )
            .shadow(
                elevation = if (focused) 10.dp else 0.dp,
                shape = shape,
                spotColor = if (focused) palette.focusGlow else Color.Transparent,
                ambientColor = if (focused) palette.focusGlow else Color.Transparent,
            )
            .onFocusChanged { focused = it.isFocused }
            .focusable()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = { onCheckedChange(!checked) },
            )
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Image(
                painter = painterResource(iconRes),
                contentDescription = null,
                colorFilter = ColorFilter.tint(
                    if (focused) palette.accentStrong else palette.textSecondary
                ),
                modifier = Modifier.size(16.dp),
            )
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                BasicText(
                    text = label,
                    maxLines = 1,
                    style = TextStyle(
                        color = if (focused) palette.textPrimary else palette.textSecondary,
                        fontSize = TvText.Body,
                    ),
                )
                if (description.isNotBlank()) {
                    Spacer(Modifier.height(3.dp))
                    BasicText(
                        text = description,
                        maxLines = 1,
                        style = TextStyle(
                            color = palette.textMuted,
                            fontSize = TvText.Label,
                        ),
                    )
                }
            }
            // Custom toggle switch
            Box(
                modifier = Modifier
                    .width(36.dp)
                    .height(20.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        if (checked) palette.accent.copy(alpha = 0.7f)
                        else Color.White.copy(alpha = 0.15f),
                    )
                    .border(
                        width = if (focused) 1.dp else 0.5.dp,
                        color = if (checked) palette.accentStrong else palette.glassBorder,
                        shape = RoundedCornerShape(10.dp),
                    ),
                contentAlignment = Alignment.CenterStart,
            ) {
                Box(
                    modifier = Modifier
                        .padding(start = if (checked) 18.dp else 2.dp)
                        .size(16.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (checked) palette.accentStrong else Color.White.copy(alpha = 0.7f),
                        ),
                )
            }
        }
    }
}

@Composable
internal fun SectionTitle(
    palette: CinePilotPalette,
    title: String,
) {
    BasicText(
        text = title,
        maxLines = 1,
        style = TextStyle(
            color = palette.textSecondary,
            fontSize = TvText.Section,
            fontWeight = FontWeight.Medium,
        ),
    )
    Spacer(Modifier.height(8.dp))
}
