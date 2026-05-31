package tv.cinepilot.tv.ui

import android.graphics.Color
import android.view.View
import android.widget.FrameLayout
import androidx.activity.ComponentActivity

fun ComponentActivity.playerScreen(
    playerView: View,
): View {
    val root = FrameLayout(this).apply {
        setBackgroundColor(Color.BLACK)
    }
    root.addView(playerView, FrameLayout.LayoutParams(
        FrameLayout.LayoutParams.MATCH_PARENT,
        FrameLayout.LayoutParams.MATCH_PARENT,
    ))
    return root
}
