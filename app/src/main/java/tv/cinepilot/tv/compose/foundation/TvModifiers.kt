package tv.cinepilot.tv.compose.foundation

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import tv.cinepilot.tv.compose.theme.TvDp

/**
 * Adds combined click + long-click behavior for TV use cases.
 *
 * Use this instead of the standard [clickable] when you need both primary action
 * and a secondary long-press action on the same element.
 */
@OptIn(ExperimentalFoundationApi::class)
fun Modifier.tvLongClickable(
    enabled: Boolean = true,
    onLongClick: () -> Unit,
    onClick: () -> Unit,
): Modifier = this.composed {
    if (!enabled) return@composed Modifier
    val interactionSource = remember { MutableInteractionSource() }
    this
        .clickable(
            interactionSource = interactionSource,
            indication = null,
            onClick = onClick,
        )
}

/**
 * Sizes a modifier to fixed card dimensions with rounded corners.
 *
 * A convenience for the common pattern of `width(w).height(h).clip(shape)`.
 */
fun Modifier.fixedTvCard(
    width: Dp,
    height: Dp,
    shape: Shape = RoundedCornerShape(TvDp.CardRadius),
): Modifier = this
    .width(width)
    .height(height)
    .clip(shape)
