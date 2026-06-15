package tv.cinepilot.tv.compose.screens.player

import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout

/**
 * Attaches a player view to this FrameLayout, re-parenting it if necessary.
 *
 * This extension function safely handles the case where the player view may already
 * be attached to a different parent, removing it first before adding it to this layout.
 * It also short-circuits if the player view is already correctly attached as the only child.
 */
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
