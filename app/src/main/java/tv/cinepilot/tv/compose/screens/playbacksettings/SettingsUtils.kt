package tv.cinepilot.tv.compose.screens.playbacksettings

import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import tv.cinepilot.tv.R
import tv.cinepilot.tv.compose.theme.CinePilotPalette
import tv.cinepilot.tv.compose.theme.TvDp
import tv.cinepilot.tv.compose.theme.TvText

@Composable
internal fun SettingsBottomButton(
    palette: CinePilotPalette,
    label: String,
    iconRes: Int,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(TvDp.ControlRadius)
    Box(
        modifier = modifier
            .height(TvDp.ControlHeight)
            .clip(shape)
            .background(
                when {
                    focused -> palette.glassFocus
                    selected -> palette.accent.copy(alpha = 0.8f)
                    else -> palette.glass
                },
                shape,
            )
            .border(
                width = if (focused || selected) TvDp.FocusRing else 0.5.dp,
                color = when {
                    focused -> palette.focusRing
                    selected -> palette.accentStrong
                    else -> palette.glassBorder
                },
                shape,
            )
            .shadow(
                elevation = if (focused) 12.dp else if (selected) 6.dp else 0.dp,
                shape = shape,
                spotColor = when {
                    focused -> palette.focusGlow
                    selected -> palette.accent.copy(alpha = 0.4f)
                    else -> Color.Transparent
                },
                ambientColor = when {
                    focused -> palette.focusGlow
                    selected -> palette.accent.copy(alpha = 0.3f)
                    else -> Color.Transparent
                },
            )
            .onFocusChanged { focused = it.isFocused }
            .focusable()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Image(
                painter = painterResource(iconRes),
                contentDescription = null,
                colorFilter = ColorFilter.tint(
                    if (focused || selected) palette.focusText else palette.textSecondary
                ),
                modifier = Modifier.size(14.dp),
            )
            BasicText(
                text = label,
                maxLines = 1,
                style = TextStyle(
                    color = if (focused || selected) palette.focusText else palette.textSecondary,
                    fontSize = TvText.Body,
                    fontWeight = FontWeight.Medium,
                ),
            )
        }
    }
}

internal fun FrameLayout.attachPlayerView(playerView: View) {
    if (playerView.parent === this && childCount == 1 && getChildAt(0) === playerView) {
        return
    }
    (playerView.parent as? ViewGroup)?.removeView(playerView)
    removeAllViews()
    addView(
        playerView,
        FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT,
        ),
    )
}
