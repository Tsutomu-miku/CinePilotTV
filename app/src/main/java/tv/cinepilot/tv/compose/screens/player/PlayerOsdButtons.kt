package tv.cinepilot.tv.compose.screens.player

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import tv.cinepilot.tv.R
import tv.cinepilot.tv.compose.theme.CinePilotPalette
import tv.cinepilot.tv.compose.theme.TvDp

@Composable
internal fun PlayPauseButton(
    palette: CinePilotPalette,
    isPlaying: Boolean,
    onClick: () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    val size = 64.dp
    val innerSize = 52.dp
    val shape = RoundedCornerShape(size / 2)
    val innerShape = RoundedCornerShape(innerSize / 2)
    val glowColor = palette.focusGlow
    val iconColor = Color.White

    Box(
        modifier = Modifier
            .size(size)
            .shadow(
                elevation = if (focused) 20.dp else 12.dp,
                shape = shape,
                spotColor = glowColor,
                ambientColor = glowColor,
            )
            .clip(shape)
            .background(
                if (focused) palette.glassFocus.copy(alpha = 0.9f)
                else palette.glass.copy(alpha = 0.85f),
                shape,
            )
            .border(
                width = if (focused) TvDp.FocusRing else 1.5.dp,
                color = if (focused) palette.focusRing else palette.focusRing.copy(alpha = 0.7f),
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
        Box(
            modifier = Modifier
                .size(innerSize)
                .clip(innerShape)
                .border(1.dp, palette.focusRing.copy(alpha = 0.3f), innerShape),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play),
                contentDescription = if (isPlaying) "Pause" else "Play",
                colorFilter = ColorFilter.tint(iconColor),
                modifier = Modifier.size(24.dp),
            )
        }
    }
}

@Composable
internal fun Seek30Button(
    palette: CinePilotPalette,
    forward: Boolean,
    onClick: () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    val size = 44.dp
    val shape = RoundedCornerShape(size / 2)
    Box(
        modifier = Modifier
            .size(size)
            .shadow(
                elevation = if (focused) 10.dp else 4.dp,
                shape = shape,
                spotColor = if (focused) palette.focusGlow else Color.Transparent,
                ambientColor = if (focused) palette.focusGlow else Color.Transparent,
            )
            .clip(shape)
            .background(
                if (focused) palette.glassFocus else palette.glass,
                shape,
            )
            .border(
                width = if (focused) TvDp.FocusRing else 0.5.dp,
                color = if (focused) palette.focusRing else palette.glassBorder,
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
        Box(contentAlignment = Alignment.Center) {
            Image(
                painter = painterResource(
                    if (forward) R.drawable.ic_forward_30 else R.drawable.ic_rewind_30
                ),
                contentDescription = if (forward) "Forward 30s" else "Rewind 30s",
                colorFilter = ColorFilter.tint(
                    if (focused) palette.accentStrong else palette.textPrimary
                ),
                modifier = Modifier.size(size - 6.dp),
            )
            BasicText(
                text = "30",
                style = TextStyle(
                    color = if (focused) palette.accentStrong else palette.textPrimary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                ),
            )
        }
    }
}

@Composable
internal fun IconOsdButton(
    palette: CinePilotPalette,
    iconRes: Int,
    size: Dp = 40.dp,
    iconSize: Dp = 18.dp,
    onClick: () -> Unit,
    badgeLabel: String? = null,
) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(size / 2)
    val badge = badgeLabel?.takeIf { it.isNotBlank() }
    Box(
        modifier = Modifier
            .size(size)
            .shadow(
                elevation = if (focused) 10.dp else 3.dp,
                shape = shape,
                spotColor = if (focused) palette.focusGlow else Color.Transparent,
                ambientColor = if (focused) palette.focusGlow else Color.Transparent,
            )
            .clip(shape)
            .background(
                if (focused) palette.glassFocus else palette.glass,
                shape,
            )
            .border(
                width = if (focused) TvDp.FocusRing else 0.5.dp,
                color = if (focused) palette.focusRing else palette.glassBorder,
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
        Image(
            painter = painterResource(iconRes),
            contentDescription = null,
            colorFilter = ColorFilter.tint(
                if (focused) palette.accentStrong else palette.textSecondary
            ),
            modifier = Modifier.size(iconSize),
        )
        if (badge != null) {
            BasicText(
                text = badge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = TextStyle(
                    color = palette.textMuted,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Medium,
                ),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 2.dp)
                    .width(size - 4.dp),
            )
        }
    }
}

@Composable
internal fun TextOsdButton(
    palette: CinePilotPalette,
    text: String,
    onClick: () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    val height = 30.dp
    val shape = RoundedCornerShape(8.dp)
    Box(
        modifier = Modifier
            .height(height)
            .shadow(
                elevation = if (focused) 10.dp else 3.dp,
                shape = shape,
                spotColor = if (focused) palette.focusGlow else Color.Transparent,
                ambientColor = if (focused) palette.focusGlow else Color.Transparent,
            )
            .clip(shape)
            .background(
                if (focused) palette.glassFocus else palette.glass,
                shape,
            )
            .border(
                width = if (focused) TvDp.FocusRing else 0.5.dp,
                color = if (focused) palette.focusRing else palette.glassBorder,
                shape = shape,
            )
            .onFocusChanged { focused = it.isFocused }
            .focusable()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        BasicText(
            text = text,
            style = TextStyle(
                color = if (focused) palette.accentStrong else palette.textSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
            ),
        )
    }
}
