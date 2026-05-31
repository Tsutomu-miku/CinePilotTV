package tv.cinepilot.tv.ui

import android.graphics.Typeface
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
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
    technicalInfo: List<String>,
    folderAction: View,
    loadPoster: (LinearLayout, MediaItemSummary) -> Unit,
    onBackHome: () -> Unit,
): View {
    return screen("详情") {
        addView(LinearLayout(this@detailsScreen).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.TOP
            loadPoster(this, item)
            addView(
                LinearLayout(this@detailsScreen).apply {
                    orientation = LinearLayout.VERTICAL
                    addView(detailTitle(item.name()))
                    addView(metadataPills(detailMetadata(item, episodeLabel)))
                    if (item.hasResumePosition()) {
                        addView(resumeBadge("可从 ${formatTicks(item.userData().playbackPositionTicks())} 继续播放"))
                    }
                    addView(verticalSpace(14))
                    if (item.playable()) {
                        addView(actionStrip(playbackActions))
                        playbackActions.firstOrNull()?.requestInitialFocus()
                    } else {
                        addView(folderAction)
                        folderAction.requestInitialFocus()
                    }
                    if (technicalInfo.isNotEmpty()) {
                        addView(section("媒体信息"))
                        addView(metadataPills(technicalInfo))
                    }
                    if (item.overview().isNotBlank()) {
                        addView(section("剧情简介"))
                        addView(bodyText(item.overview()))
                    }
                    addView(iconAction("返回首页", TvIcon.BACK, onBackHome))
                },
                LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f),
            )
        })
    }
}

private fun ComponentActivity.detailTitle(title: String): TextView {
    return TextView(this).apply {
        text = title
        textSize = TvType.Title
        typeface = Typeface.DEFAULT_BOLD
        setTextColor(TvColors.TextPrimary)
        maxLines = 3
        ellipsize = TextUtils.TruncateAt.END
        includeFontPadding = false
        setLineSpacing(2f, 1.02f)
        setPadding(0, 0, 0, dp(14))
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
