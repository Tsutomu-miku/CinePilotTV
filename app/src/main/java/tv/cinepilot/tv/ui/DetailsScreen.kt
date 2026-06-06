package tv.cinepilot.tv.ui

import android.graphics.Color
import android.graphics.Typeface
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.ComponentActivity
import tv.cinepilot.core.protocol.MediaItemType
import tv.cinepilot.core.protocol.MediaItemSummary
import tv.cinepilot.core.protocol.MediaTicks

fun ComponentActivity.detailsScreen(
    item: MediaItemSummary,
    episodeLabel: String,
    formatTicks: (Long) -> String,
    playbackActions: List<View>,
    trackControls: View?,
    technicalInfo: List<String>,
    folderAction: View,
    loadPoster: (LinearLayout, MediaItemSummary) -> Unit,
    loadBackdrop: (ImageView, MediaItemSummary) -> Unit,
): View {
    return detailsStage(item, loadBackdrop) {
        addView(LinearLayout(this@detailsScreen).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.TOP
            clipChildren = false
            clipToPadding = false
            loadPoster(this, item)
            addView(
                detailDecisionColumn {
                    addView(detailTitle(item.name()))
                    addView(metadataPills(detailMetadata(item, episodeLabel)))
                    if (item.hasResumePosition()) {
                        addView(resumeBadge("可从 ${formatTicks(item.userData().playbackPositionTicks())} 继续播放"))
                    }
                    addView(verticalSpace(8))
                    if (item.playable()) {
                        addView(actionStrip(playbackActions))
                        playbackActions.firstOrNull()?.requestInitialFocus()
                    } else {
                        addView(folderAction)
                        folderAction.requestInitialFocus()
                    }
                },
                LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f),
            )
        })
        trackControls?.let { controls ->
            addView(detailGlassSection { addView(controls) })
        }
        if (technicalInfo.isNotEmpty()) {
            addView(detailGlassSection {
                addView(section("媒体信息"))
                addView(metadataPills(technicalInfo))
            })
        }
        if (item.overview().isNotBlank()) {
            addView(detailGlassSection {
                addView(section("剧情简介"))
                addView(bodyText(item.overview()))
            })
        }
    }
}

private fun ComponentActivity.detailsStage(
    item: MediaItemSummary,
    loadBackdrop: (ImageView, MediaItemSummary) -> Unit,
    content: LinearLayout.() -> Unit,
): FrameLayout {
    val backdrop = ImageView(this).apply {
        scaleType = ImageView.ScaleType.CENTER_CROP
        alpha = 0.74f
        setBackgroundColor(TvColors.Background)
        applyBackdropBlur()
    }
    loadBackdrop(backdrop, item)
    val container = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(TvSpacing.ScreenX), dp(24), dp(TvSpacing.ScreenX), dp(TvSpacing.ScreenBottom))
        content()
    }
    val scroll = ScrollView(this).apply {
        isFillViewport = true
        isFocusable = false
        descendantFocusability = ViewGroup.FOCUS_AFTER_DESCENDANTS
        contentDescription = "详情 ${item.name()}"
        addView(container, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
        ))
    }.bindVerticalDpadScrollFallback()
    return FrameLayout(this).apply {
        setBackgroundColor(TvColors.Background)
        addView(backdrop, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT,
        ))
        addView(View(this@detailsStage).apply {
            background = rounded(Color.argb(186, 0, 0, 0), 0)
        }, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT,
        ))
        addView(scroll)
    }
}

private fun ComponentActivity.detailDecisionColumn(content: LinearLayout.() -> Unit): LinearLayout {
    return LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(4), dp(6), dp(4), dp(16))
        content()
    }
}

private fun ComponentActivity.detailGlassSection(content: LinearLayout.() -> Unit): FrameLayout {
    return glassPanel {
        setPadding(dp(18), dp(14), dp(18), dp(16))
        addView(LinearLayout(this@detailGlassSection).apply {
            orientation = LinearLayout.VERTICAL
            content()
        })
    }.apply {
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
        ).apply {
            topMargin = dp(14)
        }
    }
}

private fun ComponentActivity.detailTitle(title: String): TextView {
    return TextView(this).apply {
        text = title
        textSize = 34f
        typeface = Typeface.DEFAULT
        setTextColor(TvColors.TextPrimary)
        maxLines = 3
        ellipsize = TextUtils.TruncateAt.END
        includeFontPadding = false
        setLineSpacing(2f, 1.02f)
        setPadding(0, 0, 0, dp(10))
    }
}

private fun detailMetadata(item: MediaItemSummary, episodeLabel: String): List<String> {
    val values = mutableListOf<String>()
    val typeLabel = mediaTypeLabel(item.type())
    if (typeLabel.isNotBlank()) {
        values.add(typeLabel)
    }
    if (episodeLabel.isNotBlank()) {
        values.add(episodeLabel.replace(" · ", " / "))
    }
    if (item.type() != MediaItemType.EPISODE && item.productionYear() != null) {
        values.add(item.productionYear().toString())
    }
    if (item.runTimeTicks() != null) {
        values.add("约 ${durationLabel(item.runTimeTicks())}")
    }
    values.addAll(item.genres().take(3))
    return values
}

private fun mediaTypeLabel(type: MediaItemType): String {
    return when (type) {
        MediaItemType.MOVIE -> "电影"
        MediaItemType.SERIES -> "剧集"
        MediaItemType.SEASON -> "季"
        MediaItemType.EPISODE -> "单集"
        MediaItemType.VIDEO -> "视频"
        MediaItemType.COLLECTION_FOLDER,
        MediaItemType.FOLDER -> "目录"
        MediaItemType.UNKNOWN -> ""
    }
}

private fun durationLabel(ticks: Long): String {
    val totalMinutes = (MediaTicks.toMilliseconds(ticks) / 60_000).coerceAtLeast(1)
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return if (hours > 0 && minutes > 0) {
        "${hours} 小时 ${minutes} 分钟"
    } else if (hours > 0) {
        "${hours} 小时"
    } else {
        "${minutes} 分钟"
    }
}
