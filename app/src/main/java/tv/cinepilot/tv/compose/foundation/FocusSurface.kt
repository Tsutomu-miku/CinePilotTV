package tv.cinepilot.tv.compose.foundation

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import tv.cinepilot.tv.compose.theme.CinePilotPalette
import tv.cinepilot.tv.compose.theme.TvDp

/**
 * Core TV focus primitive — the building block for all focusable TV components.
 *
 * Handles focus state, scale animation, glass background, border, and glow shadow
 * in one place so every component gets consistent focus behavior.
 *
 * @param palette The theme palette to use for colors
 * @param selected When true, uses accent color for background/border (persistent selection state)
 * @param isFocused When non-null, overrides the internal focus state (for state-driven focus)
 * @param requestInitialFocus Whether to request focus when first shown
 * @param focusedScale Scale factor when focused (1f = no animation)
 * @param glowRadius Glow / halo radius when focused (0dp disables glow)
 * @param radius Corner radius
 * @param padding Internal padding
 * @param onFocusChanged Callback for focus changes
 * @param onClick Click handler; null makes the component focusable but not clickable
 */
@Composable
fun FocusSurface(
    palette: CinePilotPalette,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    enabled: Boolean = true,
    /** When non-null, overrides the visual focus state. Use for state-driven focus. */
    isFocused: Boolean? = null,
    requestInitialFocus: Boolean = false,
    focusedScale: Float = 1.03f,
    /** Glow / halo effect when focused. 0dp disables glow. */
    glowRadius: Dp = 8.dp,
    radius: Dp = TvDp.ControlRadius,
    padding: PaddingValues = PaddingValues(horizontal = 9.dp, vertical = 6.dp),
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

    // Glow shadow — applied outside scale so the glow stays crisp
    val glowModifier = if (glowRadius > 0.dp && focused) {
        Modifier.shadow(
            elevation = glowRadius,
            shape = shape,
            spotColor = palette.focusGlow,
            ambientColor = palette.focusGlow,
        )
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
            .then(glowModifier)
            .scale(scale)
            .clip(shape)
            .background(fill, shape)
            .border(BorderStroke(if (focused) TvDp.FocusRing else 0.5.dp, border), shape)
            .padding(padding),
        contentAlignment = Alignment.CenterStart,
    ) {
        content(focused)
    }
}
