package tv.cinepilot.tv.ui

import android.graphics.Color
import android.graphics.drawable.Drawable
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.media3.ui.PlayerView
import androidx.media3.ui.R as Media3UiR
import tv.cinepilot.core.protocol.MediaTicks
import tv.cinepilot.tv.R

fun ComponentActivity.playerScreen(
    playerView: View,
    debugInfo: String,
): View = playerScreen(
    playerView = playerView,
    debugInfo = debugInfo,
    overlays = PlayerOverrides(),
)

data class PlayerOverrides(
    val chapterTitles: List<Pair<Long, String>> = emptyList(),
    val onChapterClick: (Long) -> Unit = {},
    val introSegmentTicks: LongRange? = null,
    val creditsSegmentTicks: LongRange? = null,
    val onSkipIntro: () -> Unit = {},
    val onSkipCredits: () -> Unit = {},
    val nextUp: PlayerNextUpInfo? = null,
    val onPlayNext: () -> Unit = {},
    val onCancelNextUp: () -> Unit = {},
    val trickplayTileUrl: String = "",
    val trickplayGrid: TrickplayGridSpec = TrickplayGridSpec(0, 0, 0, 0L, 0),
    val trickplayImageLoader: ((ImageView, String) -> Unit)? = null,
    val playbackSettingsButton: Boolean = true,
    val onOpenPlaybackSettings: () -> Unit = {},
)

data class PlayerNextUpInfo(
    val episodeLabel: String,
    val title: String,
    val overview: String,
    val artworkUrl: String,
    val countdownSeconds: Int,
    val autoPlay: Boolean,
)

data class TrickplayGridSpec(
    val tileWidth: Int,
    val tileHeight: Int,
    val tilesPerRow: Int,
    val tileIntervalTicks: Long,
    val tileCount: Int,
)

fun ComponentActivity.playerScreen(
    playerView: View,
    debugInfo: String,
    overlays: PlayerOverrides,
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
        if (overlays.playbackSettingsButton) {
            playerView.installPlaybackSettingsButton { overlays.onOpenPlaybackSettings() }
        }
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
    overlays.introSegmentTicks?.let { range ->
        root.addView(
            skipBanner("跳过片头", overlays.onSkipIntro, range),
            skipBannerParams(Gravity.BOTTOM or Gravity.START),
        )
    }
    overlays.creditsSegmentTicks?.let { range ->
        root.addView(
            skipBanner("跳过片尾", overlays.onSkipCredits, range),
            skipBannerParams(Gravity.BOTTOM or Gravity.END),
        )
    }
    overlays.nextUp?.let { nextUp ->
        root.addView(
            nextUpCard(nextUp, overlays.onPlayNext, overlays.onCancelNextUp),
            nextUpCardParams(),
        )
    }
    if (overlays.chapterTitles.isNotEmpty()) {
        root.addView(chapterStrip(overlays.chapterTitles, overlays.onChapterClick),
            chapterStripParams())
    }
    return root
}

// ---- Skip banner --------------------------------------------------------------

private fun ComponentActivity.skipBanner(
    label: String,
    onClick: () -> Unit,
    ticks: LongRange,
): View {
    val remaining = (ticks.last - ticks.first).coerceAtLeast(0L)
    val seconds = (remaining / MediaTicks.TICKS_PER_SECOND).toInt().coerceAtLeast(1)
    val container = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        background = GlassDrawable(
            TvColors.GlassFocusTint,
            dp(14).toFloat(),
            TvColors.FocusRing,
        )
        elevation = dp(12).toFloat()
        setPadding(dp(16), dp(10), dp(16), dp(10))
        isFocusable = true
        isClickable = true
        setOnClickListener { onClick() }
        setOnFocusChangeListener { v, hasFocus ->
            v.background = GlassDrawable(
                if (hasFocus) TvColors.GlassFocusTint else TvColors.GlassTint,
                dp(14).toFloat(),
                if (hasFocus) TvColors.FocusRing else TvColors.GlassBorder,
            )
        }
    }
    container.addView(ImageView(this).apply {
        setImageResource(R.drawable.ic_fast_forward)
        setColorFilter(Color.WHITE)
        scaleType = ImageView.ScaleType.CENTER
        layoutParams = LinearLayout.LayoutParams(dp(22), dp(22)).apply {
            rightMargin = dp(10)
        }
    })
    container.addView(TextView(this).apply {
        this.text = "$label ${seconds}s"
        textSize = InfuseTypeTokens.Secondary
        setTextColor(TvColors.TextPrimary)
        includeFontPadding = false
        maxLines = 1
    })
    return container
}

private fun ComponentActivity.skipBannerParams(gravity: Int): FrameLayout.LayoutParams {
    return FrameLayout.LayoutParams(
        FrameLayout.LayoutParams.WRAP_CONTENT,
        FrameLayout.LayoutParams.WRAP_CONTENT,
        gravity,
    ).apply {
        when {
            gravity and Gravity.START != 0 -> leftMargin = dp(40)
            else -> rightMargin = dp(40)
        }
        bottomMargin = dp(140)
    }
}

// ---- Next Up card -------------------------------------------------------------

private fun ComponentActivity.nextUpCard(
    info: PlayerNextUpInfo,
    onPlayNow: () -> Unit,
    onCancel: () -> Unit,
): View {
    val card = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        background = glassDrawable(GlassTokens.PanelRadius)
        elevation = dp(16).toFloat()
        setPadding(dp(16), dp(14), dp(16), dp(14))
    }
    card.addView(ImageView(this).apply {
        scaleType = ImageView.ScaleType.CENTER_CROP
        background = roundRectDrawable(dp(10), Color.argb(255, 20, 20, 22))
        layoutParams = LinearLayout.LayoutParams(dp(196), dp(110)).apply {
            rightMargin = dp(16)
        }
    })
    val textStack = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
    }
    val countdown = if (info.autoPlay) "${info.countdownSeconds}s 后自动播放" else "下一集"
    textStack.addView(TextView(this).apply {
        this.text = info.episodeLabel
        textSize = InfuseTypeTokens.Label
        setTextColor(TvColors.TextSecondary)
        maxLines = 1
        includeFontPadding = false
    })
    textStack.addView(TextView(this).apply {
        this.text = info.title
        textSize = InfuseTypeTokens.Primary
        setTextColor(TvColors.TextPrimary)
        maxLines = 1
        ellipsize = TextUtils.TruncateAt.END
        includeFontPadding = false
        setPadding(0, dp(4), 0, dp(6))
    })
    textStack.addView(TextView(this).apply {
        this.text = info.overview.ifBlank { countdown }
        textSize = InfuseTypeTokens.Secondary
        setTextColor(TvColors.TextTertiary)
        maxLines = 2
        ellipsize = TextUtils.TruncateAt.END
        includeFontPadding = false
    })
    card.addView(textStack)
    val actions = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
        ).apply { leftMargin = dp(12) }
    }
    val playAction = TextView(this).apply {
        this.text = "立即播放"
        textSize = InfuseTypeTokens.Secondary
        setTextColor(TvColors.TextPrimary)
        background = GlassDrawable(
            TvColors.GlassFocusTint,
            dp(12).toFloat(),
            TvColors.FocusRing,
        )
        isFocusable = true
        isClickable = true
        setPadding(dp(18), dp(10), dp(18), dp(10))
        setOnClickListener { onPlayNow() }
        setOnFocusChangeListener { v, hasFocus ->
            v.background = GlassDrawable(
                if (hasFocus) TvColors.GlassFocusTint else TvColors.GlassTint,
                dp(12).toFloat(),
                if (hasFocus) TvColors.FocusRing else TvColors.GlassBorder,
            )
        }
    }
    val cancelAction = TextView(this).apply {
        this.text = "取消"
        textSize = InfuseTypeTokens.Secondary
        setTextColor(TvColors.TextSecondary)
        background = glassDrawable(12)
        isFocusable = true
        isClickable = true
        setPadding(dp(18), dp(10), dp(18), dp(10))
        setOnClickListener { onCancel() }
        setOnFocusChangeListener { v, hasFocus ->
            v.background = GlassDrawable(
                if (hasFocus) TvColors.GlassFocusTint else TvColors.GlassTint,
                dp(12).toFloat(),
                if (hasFocus) TvColors.FocusRing else TvColors.GlassBorder,
            )
        }
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
        ).apply { topMargin = dp(10) }
    }
    actions.addView(playAction)
    actions.addView(cancelAction)
    card.addView(actions)
    playAction.requestFocus()
    return card
}

private fun ComponentActivity.nextUpCardParams(): FrameLayout.LayoutParams = FrameLayout.LayoutParams(
    dp(620),
    FrameLayout.LayoutParams.WRAP_CONTENT,
    Gravity.BOTTOM or Gravity.END,
).apply {
    rightMargin = dp(40)
    bottomMargin = dp(140)
}

// ---- Chapter strip -----------------------------------------------------------

private fun ComponentActivity.chapterStrip(
    chapters: List<Pair<Long, String>>,
    onClick: (Long) -> Unit,
): View {
    val row = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        setPadding(dp(40), dp(8), dp(40), dp(8))
    }
    chapters.forEach { (ticks, title) ->
        val chip = TextView(this).apply {
            this.text = title.ifBlank { MediaTicks.formatShort(ticks) }
            textSize = InfuseTypeTokens.Secondary
            setTextColor(TvColors.TextSecondary)
            includeFontPadding = false
            background = glassDrawable(24)
            isFocusable = true
            isClickable = true
            setPadding(dp(14), dp(8), dp(14), dp(8))
            setOnClickListener { onClick(ticks) }
            setOnFocusChangeListener { v, hasFocus ->
                setTextColor(if (hasFocus) TvColors.TextPrimary else TvColors.TextSecondary)
                v.background = GlassDrawable(
                    if (hasFocus) TvColors.GlassFocusTint else TvColors.GlassTint,
                    dp(24).toFloat(),
                    if (hasFocus) TvColors.FocusRing else TvColors.GlassBorder,
                )
            }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ).apply { rightMargin = dp(10) }
        }
        row.addView(chip)
    }
    val scroller = HorizontalScrollView(this).apply {
        isHorizontalScrollBarEnabled = false
        setPadding(0, dp(10), 0, dp(10))
        addView(row)
    }
    return scroller
}

private fun ComponentActivity.chapterStripParams(): FrameLayout.LayoutParams = FrameLayout.LayoutParams(
    FrameLayout.LayoutParams.MATCH_PARENT,
    FrameLayout.LayoutParams.WRAP_CONTENT,
    Gravity.BOTTOM,
).apply { bottomMargin = dp(210) }

// ---- Playback settings button on the controller bar -------------------------

private fun PlayerView.installPlaybackSettingsButton(onClick: () -> Unit) {
    post {
        val existing = findViewById<View>(R.id.exo_playback_settings_button)
        if (existing != null) return@post
        val settingsButton = findViewById<View>(Media3UiR.id.exo_settings)
        val controls = settingsButton?.parent as? ViewGroup ?: return@post
        val button = ImageButton(context).apply {
            id = R.id.exo_playback_settings_button
            contentDescription = "播放设置"
            setImageResource(R.drawable.ic_settings)
            setColorFilter(Color.WHITE)
            scaleType = ImageView.ScaleType.CENTER
            setPadding(dpValue(10), dpValue(10), dpValue(10), dpValue(10))
            background = null
            isFocusable = true
            isClickable = true
            setOnClickListener { onClick() }
        }
        val insertIndex = controls.indexOfChild(settingsButton).takeIf { it >= 0 }
            ?: controls.childCount
        controls.addView(button, insertIndex,
            LinearLayout.LayoutParams(dpValue(48), dpValue(48)))
    }
}

// ---- Reused helpers ----------------------------------------------------------

private fun ComponentActivity.roundRectDrawable(radiusDp: Int, fill: Int): Drawable {
    return GradientDrawableBuilder()
        .corner(dp(radiusDp).toFloat())
        .fill(fill)
        .build()
}

private class GradientDrawableBuilder {
    private var corner = 0f
    private var fill = Color.TRANSPARENT
    fun corner(value: Float) = apply { corner = value }
    fun fill(value: Int) = apply { fill = value }
    fun build(): Drawable = android.graphics.drawable.GradientDrawable().apply {
        shape = android.graphics.drawable.GradientDrawable.RECTANGLE
        cornerRadius = corner
        setColor(fill)
    }
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
        dp(MediaWallTokens.SheetWidth),
        FrameLayout.LayoutParams.WRAP_CONTENT,
        Gravity.CENTER_VERTICAL or Gravity.END,
    ).apply {
        rightMargin = dp(MediaWallTokens.ScreenX)
    }
}

private fun ComponentActivity.playerInfoPanel(debugInfo: String): View {
    return sideSheet {
        addView(TextView(this@playerInfoPanel).apply {
                text = "视频信息"
                textSize = InfuseTypeTokens.ShelfTitle
                setTextColor(TvColors.TextPrimary)
                includeFontPadding = false
                setPadding(0, 0, 0, dp(8))
        })
        debugInfo.lineSequence()
            .filter { it.isNotBlank() }
            .take(14)
            .forEach { line -> addView(playerInfoLine(line)) }
    }.apply {
        visibility = View.GONE
    }
}

private fun ComponentActivity.playerInfoLine(line: String): View {
    val label = line.substringBefore('：', "")
    val value = line.substringAfter('：', line)
    return TextView(this).apply {
        text = if (label.isBlank()) value else "$label  $value"
        textSize = InfuseTypeTokens.Metadata
        setTextColor(TvColors.TextSecondary)
        maxLines = if (label == "请求路径" || label == "请求参数") 2 else 1
        ellipsize = TextUtils.TruncateAt.END
        includeFontPadding = false
        setLineSpacing(2f, 1.04f)
        setPadding(0, dp(3), 0, dp(3))
    }
}
