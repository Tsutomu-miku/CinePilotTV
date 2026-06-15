package tv.cinepilot.tv.compose.components.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import tv.cinepilot.tv.compose.foundation.FocusSurface
import tv.cinepilot.tv.compose.theme.CinePilotPalette
import tv.cinepilot.tv.compose.theme.TvDp
import tv.cinepilot.tv.compose.theme.TvText

/**
 * A settings row with label + value text.
 *
 * @param label The setting name
 * @param value The current value / summary
 * @param description Optional secondary description
 * @param selected Whether this row is in a selected state
 */
@Composable
fun TvOptionRow(
    palette: CinePilotPalette,
    label: String,
    value: String,
    description: String = "",
    selected: Boolean = false,
    modifier: Modifier = Modifier,
    requestInitialFocus: Boolean = false,
    onClick: () -> Unit,
) {
    FocusSurface(
        palette = palette,
        selected = selected,
        requestInitialFocus = requestInitialFocus,
        modifier = modifier
            .fillMaxWidth()
            .height(TvDp.SettingsRowHeight),
        onClick = onClick,
    ) { focused ->
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center,
            ) {
                BasicText(
                    text = label,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = TextStyle(
                        color = if (focused) palette.textPrimary else palette.textSecondary,
                        fontSize = TvText.Body,
                    ),
                )
                if (description.isNotBlank()) {
                    Spacer(Modifier.height(5.dp))
                    BasicText(
                        text = description,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = TextStyle(color = palette.textMuted, fontSize = TvText.Label),
                    )
                }
            }
            Spacer(Modifier.width(9.dp))
            BasicText(
                text = value,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = TextStyle(
                    color = if (focused || selected) palette.accentStrong else palette.textMuted,
                    fontSize = TvText.Metadata,
                ),
            )
        }
    }
}

/**
 * A toggle row — label + on/off state.
 *
 * Implemented as a TvOptionRow with "开"/"关" as the value.
 */
@Composable
fun TvToggleRow(
    palette: CinePilotPalette,
    label: String,
    checked: Boolean,
    description: String = "",
    modifier: Modifier = Modifier,
    requestInitialFocus: Boolean = false,
    onCheckedChange: (Boolean) -> Unit,
) {
    TvOptionRow(
        palette = palette,
        label = label,
        description = description,
        value = if (checked) "开" else "关",
        selected = checked,
        modifier = modifier,
        requestInitialFocus = requestInitialFocus,
        onClick = { onCheckedChange(!checked) },
    )
}
