package tv.cinepilot.tv.ui

import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import androidx.activity.ComponentActivity
import tv.cinepilot.core.protocol.MediaItemSummary

fun ComponentActivity.detailsScreen(
    item: MediaItemSummary,
    episodeLabel: String,
    formatTicks: (Long) -> String,
    playbackActions: List<View>,
    folderAction: View,
    loadPoster: (LinearLayout, MediaItemSummary) -> Unit,
    onBackHome: () -> Unit,
): View {
    return screen(item.name()) {
        addView(LinearLayout(this@detailsScreen).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.TOP
            loadPoster(this, item)
            addView(
                LinearLayout(this@detailsScreen).apply {
                    orientation = LinearLayout.VERTICAL
                    addView(metaLine(item))
                    if (item.runTimeTicks() != null) {
                        addView(supportingLabel("时长 ${formatTicks(item.runTimeTicks())}"))
                    }
                    if (episodeLabel.isNotBlank()) {
                        addView(supportingLabel(episodeLabel))
                    }
                    if (item.genres().isNotEmpty()) {
                        addView(supportingLabel(item.genres().joinToString(" / ")))
                    }
                    if (item.hasResumePosition()) {
                        addView(resumeBadge("可从 ${formatTicks(item.userData().playbackPositionTicks())} 继续播放"))
                    }
                    if (item.playable()) {
                        addView(actionStrip(playbackActions))
                    } else {
                        addView(folderAction)
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
