package tv.cinepilot.tv.compose.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import tv.cinepilot.tv.compose.foundation.FocusSurface as foundationFocusSurface
import tv.cinepilot.tv.compose.foundation.fixedTvCard as foundationFixedTvCard
import tv.cinepilot.tv.compose.foundation.tvLongClickable as foundationTvLongClickable
import tv.cinepilot.tv.compose.theme.CinePilotPalette

// Re-exports — keep the old package working while the codebase migrates.
// New code should import directly from the sub-packages.

// ── Foundation ──────────────────────────────────────────────────────────
@Composable
fun FocusSurface(
    palette: CinePilotPalette,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    enabled: Boolean = true,
    isFocused: Boolean? = null,
    requestInitialFocus: Boolean = false,
    focusedScale: Float = 1.03f,
    glowRadius: Dp = 8.dp,
    radius: Dp = tv.cinepilot.tv.compose.theme.TvDp.ControlRadius,
    padding: PaddingValues = PaddingValues(horizontal = 9.dp, vertical = 6.dp),
    onFocusChanged: (Boolean) -> Unit = {},
    onClick: (() -> Unit)? = null,
    content: @Composable androidx.compose.foundation.layout.BoxScope.(focused: Boolean) -> Unit,
) {
    foundationFocusSurface(
        palette = palette,
        modifier = modifier,
        selected = selected,
        enabled = enabled,
        isFocused = isFocused,
        requestInitialFocus = requestInitialFocus,
        focusedScale = focusedScale,
        glowRadius = glowRadius,
        radius = radius,
        padding = padding,
        onFocusChanged = onFocusChanged,
        onClick = onClick,
        content = content,
    )
}

@OptIn(ExperimentalFoundationApi::class)
fun Modifier.tvLongClickable(
    enabled: Boolean = true,
    onLongClick: () -> Unit,
    onClick: () -> Unit,
): Modifier = with(this) {
    foundationTvLongClickable(
        enabled = enabled,
        onLongClick = onLongClick,
        onClick = onClick,
    )
}

fun Modifier.fixedTvCard(
    width: Dp,
    height: Dp,
    shape: Shape = androidx.compose.foundation.shape.RoundedCornerShape(tv.cinepilot.tv.compose.theme.TvDp.CardRadius),
): Modifier = with(this) {
    foundationFixedTvCard(width, height, shape)
}

// ── Buttons ───────────────────────────────────────────────────────────────
@Composable
fun TvActionButton(
    palette: CinePilotPalette,
    label: String,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    requestInitialFocus: Boolean = false,
    onClick: () -> Unit,
) {
    tv.cinepilot.tv.compose.components.buttons.TvActionButton(
        palette = palette,
        label = label,
        modifier = modifier,
        selected = selected,
        requestInitialFocus = requestInitialFocus,
        onClick = onClick,
    )
}

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
    tv.cinepilot.tv.compose.components.buttons.TvIconButton(
        palette = palette,
        iconRes = iconRes,
        contentDescription = contentDescription,
        modifier = modifier,
        selected = selected,
        requestInitialFocus = requestInitialFocus,
        onClick = onClick,
    )
}

// ── Settings rows ────────────────────────────────────────────────────────
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
    tv.cinepilot.tv.compose.components.settings.TvOptionRow(
        palette = palette,
        label = label,
        value = value,
        description = description,
        selected = selected,
        modifier = modifier,
        requestInitialFocus = requestInitialFocus,
        onClick = onClick,
    )
}

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
    tv.cinepilot.tv.compose.components.settings.TvToggleRow(
        palette = palette,
        label = label,
        checked = checked,
        description = description,
        modifier = modifier,
        requestInitialFocus = requestInitialFocus,
        onCheckedChange = onCheckedChange,
    )
}

// ── Input ────────────────────────────────────────────────────────────────
@Composable
fun TvTextField(
    palette: CinePilotPalette,
    value: String,
    hint: String,
    modifier: Modifier = Modifier,
    password: Boolean = false,
    requestInitialFocus: Boolean = false,
    selectAllOnFocus: Boolean = false,
    onValueChange: (String) -> Unit,
) {
    tv.cinepilot.tv.compose.components.input.TvTextField(
        palette = palette,
        value = value,
        hint = hint,
        modifier = modifier,
        password = password,
        requestInitialFocus = requestInitialFocus,
        selectAllOnFocus = selectAllOnFocus,
        onValueChange = onValueChange,
    )
}
