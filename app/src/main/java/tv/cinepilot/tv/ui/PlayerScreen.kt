package tv.cinepilot.tv.ui

import android.graphics.Color
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.media3.ui.PlayerView
import androidx.media3.ui.R as Media3UiR
import tv.cinepilot.tv.R

fun ComponentActivity.playerScreen(
    playerView: View,
    debugInfo: String,
): View {
    val root = FrameLayout(this).apply {
        setBackgroundColor(Color.BLACK)
    }
    root.addView(playerView, FrameLayout.LayoutParams(
        FrameLayout.LayoutParams.MATCH_PARENT,
        FrameLayout.LayoutParams.MATCH_PARENT,
    ))
    val infoPanel = playerInfoPanel(debugInfo)
    val infoButton = playerInfoButton {
        infoPanel.visibility = if (infoPanel.visibility == View.VISIBLE) View.GONE else View.VISIBLE
    }
    infoButton.visibility = View.GONE
    if (playerView is PlayerView) {
        playerView.installPlayerInfoButton(infoButton, infoPanel)
        playerView.addView(infoPanel, playerInfoPanelParams())
        playerView.setControllerVisibilityListener(PlayerView.ControllerVisibilityListener { visibility ->
            infoButton.visibility = visibility
            if (visibility != View.VISIBLE) {
                infoPanel.visibility = View.GONE
            }
        })
    } else {
        root.addView(infoButton, playerInfoOverlayButtonParams())
        root.addView(infoPanel, playerInfoPanelParams())
    }
    return root
}

private fun ComponentActivity.playerInfoButton(onClick: () -> Unit): ImageButton {
    return ImageButton(this).apply {
        contentDescription = "视频信息"
        setImageResource(R.drawable.ic_info)
        setColorFilter(Color.WHITE)
        background = glassDrawable(GlassTokens.ControlRadius)
        isFocusable = true
        isClickable = true
        scaleType = ImageView.ScaleType.CENTER
        setPadding(dp(10), dp(10), dp(10), dp(10))
        setOnClickListener { onClick() }
        setOnFocusChangeListener { focusedView, hasFocus ->
            focusedView.background = focusedView.playerInfoButtonBackground(hasFocus)
        }
    }
}

private fun ComponentActivity.playerInfoOverlayButtonParams(): FrameLayout.LayoutParams {
    return FrameLayout.LayoutParams(
        dp(48),
        dp(48),
        Gravity.BOTTOM or Gravity.END,
    ).apply {
        rightMargin = dp(32)
        bottomMargin = dp(96)
    }
}

private fun PlayerView.installPlayerInfoButton(infoButton: View, infoPanel: View) {
    post {
        val settingsButton = findViewById<View>(Media3UiR.id.exo_settings)
        val controls = settingsButton?.parent as? ViewGroup
        if (controls != null) {
            (infoButton.parent as? ViewGroup)?.removeView(infoButton)
            val insertIndex = controls.indexOfChild(settingsButton).takeIf { it >= 0 } ?: controls.childCount
            controls.addView(infoButton, insertIndex, LinearLayout.LayoutParams(dpValue(48), dpValue(48)))
        } else if (infoButton.parent == null) {
            addView(infoButton, FrameLayout.LayoutParams(
                dpValue(48),
                dpValue(48),
                Gravity.BOTTOM or Gravity.END,
            ).apply {
                rightMargin = dpValue(32)
                bottomMargin = dpValue(96)
            })
        }
        infoButton.setOnFocusChangeListener { focusedView, hasFocus ->
            if (!hasFocus) {
                infoPanel.visibility = View.GONE
            }
            focusedView.background = focusedView.playerInfoButtonBackground(hasFocus)
        }
    }
}

private fun View.playerInfoButtonBackground(hasFocus: Boolean): GlassDrawable {
    return GlassDrawable(
        if (hasFocus) TvColors.GlassFocusTint else TvColors.GlassTint,
        dpValue(GlassTokens.ControlRadius).toFloat(),
        if (hasFocus) TvColors.FocusRing else TvColors.GlassBorder,
    )
}

private fun View.dpValue(value: Int): Int {
    return (value * resources.displayMetrics.density + 0.5f).toInt()
}

private fun ComponentActivity.playerInfoPanelParams(): FrameLayout.LayoutParams {
    return FrameLayout.LayoutParams(
        dp(560),
        FrameLayout.LayoutParams.WRAP_CONTENT,
        Gravity.BOTTOM or Gravity.END,
    ).apply {
        rightMargin = dp(32)
        bottomMargin = dp(170)
    }
}

private fun ComponentActivity.playerInfoPanel(debugInfo: String): TextView {
    return TextView(this).apply {
        text = debugInfo
        textSize = TvType.Metadata
        setTextColor(TvColors.TextSecondary)
        setLineSpacing(2f, 1.05f)
        maxLines = 18
        ellipsize = TextUtils.TruncateAt.END
        background = glassDrawable(GlassTokens.PanelRadius)
        setPadding(dp(14), dp(12), dp(14), dp(12))
        visibility = View.GONE
    }
}
