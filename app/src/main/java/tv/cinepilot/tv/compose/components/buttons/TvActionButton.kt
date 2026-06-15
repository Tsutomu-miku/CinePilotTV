package tv.cinepilot.tv.compose.components.buttons

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.ColorFilter
import tv.cinepilot.tv.compose.foundation.FocusSurface
import tv.cinepilot.tv.compose.theme.CinePilotPalette
import tv.cinepilot.tv.compose.theme.TvDp
import tv.cinepilot.tv.compose.theme.TvText

/** A standard text action button with TV focus behavior. */
@Composable
fun TvActionButton(
    palette: CinePilotPalette,
    label: String,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    requestInitialFocus: Boolean = false,
    onClick: () -> Unit,
) {
    FocusSurface(
        palette = palette,
        selected = selected,
        requestInitialFocus = requestInitialFocus,
        modifier = modifier.height(TvDp.ControlHeight),
        padding = PaddingValues(horizontal = if (selected) 12.dp else 10.dp),
        onClick = onClick,
    ) { focused ->
        BasicText(
            text = label,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = TextStyle(
                color = when {
                    focused -> palette.accentStrong
                    selected -> palette.textPrimary
                    else -> palette.textSecondary
                },
                fontSize = TvText.Body,
            ),
        )
    }
}

/** An icon-only button with TV focus behavior. */
@Composable
fun TvIconButton(
    palette: CinePilotPalette,
    iconRes: Int,
    contentDescription: String,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    requestInitialFocus: Boolean = false,
    onClick: () -> Unit,
) {
    FocusSurface(
        palette = palette,
        selected = selected,
        requestInitialFocus = requestInitialFocus,
        modifier = modifier.size(TvDp.IconButtonSize),
        padding = PaddingValues(0.dp),
        onClick = onClick,
    ) { focused ->
        Image(
            painter = painterResource(iconRes),
            contentDescription = contentDescription,
            colorFilter = ColorFilter.tint(if (focused || selected) palette.accentStrong else palette.textSecondary),
            modifier = Modifier
                .align(Alignment.Center)
                .size(15.dp),
        )
    }
}
