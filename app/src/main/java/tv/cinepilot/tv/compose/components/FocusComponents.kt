package tv.cinepilot.tv.compose.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.Image
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import tv.cinepilot.tv.compose.theme.CinePilotPalette
import tv.cinepilot.tv.compose.theme.TvDp
import tv.cinepilot.tv.compose.theme.TvText

@Composable
fun FocusSurface(
    palette: CinePilotPalette,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    enabled: Boolean = true,
    /** When non-null, overrides the visual focus state. Use for state-driven focus. */
    isFocused: Boolean? = null,
    requestInitialFocus: Boolean = false,
    focusedScale: Float = 1.025f,
    radius: Dp = TvDp.ControlRadius,
    padding: PaddingValues = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
    onFocusChanged: (Boolean) -> Unit = {},
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.(focused: Boolean) -> Unit,
) {
    val internalFocused = remember { mutableStateOf(false) }
    val focused = isFocused ?: internalFocused.value
    val scale = if (focusedScale == 1f) {
        1f
    } else {
        val animatedScale by animateFloatAsState(
            targetValue = if (focused) focusedScale else 1f,
            animationSpec = tween(140),
            label = "tvFocusScale",
        )
        animatedScale
    }
    val shape = RoundedCornerShape(radius)
    val fill = when {
        focused -> palette.glassFocus
        selected -> palette.accent.copy(alpha = 0.36f)
        else -> palette.glass
    }
    val border = when {
        focused -> palette.focusRing
        selected -> palette.accentStrong
        else -> palette.glassBorder
    }
    val interaction = remember { MutableInteractionSource() }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        if (requestInitialFocus && enabled) {
            runCatching { focusRequester.requestFocus() }
        }
    }

    // Single source of truth for focus + click.
    // clickable() provides its own focus node; focusable() is used only when there is no click action.
    val interactionModifier = if (onClick != null && enabled) {
        Modifier.clickable(
            interactionSource = interaction,
            indication = null,
            onClick = onClick,
        )
    } else if (enabled) {
        Modifier.focusable(interactionSource = interaction)
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .focusRequester(focusRequester)
            .onFocusChanged {
                internalFocused.value = it.isFocused
                onFocusChanged(it.isFocused)
            }
            .focusProperties {
                canFocus = enabled
            }
            .then(interactionModifier)
            .scale(scale)
            .clip(shape)
            .background(fill, shape)
            .border(BorderStroke(if (focused) 2.dp else 1.dp, border), shape)
            .padding(padding),
        contentAlignment = Alignment.CenterStart,
    ) {
        content(focused)
    }
}

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
        padding = PaddingValues(horizontal = if (selected) 18.dp else 14.dp),
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
                .size(22.dp),
        )
    }
}

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
            Spacer(Modifier.width(14.dp))
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

@OptIn(ExperimentalFoundationApi::class)
fun Modifier.tvLongClickable(
    enabled: Boolean = true,
    onLongClick: () -> Unit,
    onClick: () -> Unit,
): Modifier {
    if (!enabled) return this
    return combinedClickable(
        interactionSource = MutableInteractionSource(),
        indication = null,
        onClick = onClick,
        onLongClick = onLongClick,
    )
}

fun Modifier.fixedTvCard(width: Dp, height: Dp, shape: Shape = RoundedCornerShape(TvDp.CardRadius)): Modifier {
    return size(width, height).clip(shape)
}

@Composable
fun TvTextField(
    palette: CinePilotPalette,
    value: String,
    hint: String,
    modifier: Modifier = Modifier,
    password: Boolean = false,
    onValueChange: (String) -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(TvDp.ControlRadius)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(shape)
            .background(if (focused) palette.glassFocus else palette.glass, shape)
            .border(BorderStroke(if (focused) 2.dp else 1.dp, if (focused) palette.focusRing else palette.glassBorder), shape)
            .onFocusChanged { focused = it.isFocused }
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        if (value.isBlank()) {
            BasicText(
                text = hint,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = TextStyle(color = palette.textMuted, fontSize = TvText.Body),
            )
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = TextStyle(color = palette.textPrimary, fontSize = TvText.Body),
            visualTransformation = if (password) PasswordVisualTransformation() else VisualTransformation.None,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
