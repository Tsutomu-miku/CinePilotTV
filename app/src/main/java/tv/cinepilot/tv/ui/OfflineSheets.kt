package tv.cinepilot.tv.ui

import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.ComponentActivity
import tv.cinepilot.core.protocol.OfflineRepository

/**
 * Horizontal quality picker used by the "离线下载" flow. Shows a row of
 * focusable labels (自动 / 480p / 720p / 1080p / 4K) inside a side sheet.
 */
fun ComponentActivity.qualityPickerSheet(
    labels: List<String>,
    values: List<Int>,
    default: Int,
    onChosen: (Int) -> Unit,
): View {
    require(labels.size == values.size) { "labels / values length mismatch" }
    return sideSheet {
        addView(infusePanelTitle("选择离线画质"))
        addView(TextView(this@qualityPickerSheet).apply {
            text = "下载的码率越高，画面越清晰，但占用的磁盘空间也越大。"
            textSize = 12f
            setTextColor(TvColors.TextSecondary)
            setPadding(0, dp(4), 0, dp(14))
            setLineSpacing(2f, 1.05f)
        })
        val row = LinearLayout(this@qualityPickerSheet).apply {
            orientation = LinearLayout.HORIZONTAL
            weightSum = labels.size.toFloat()
            layoutParams = ViewGroup.MarginLayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            )
        }
        labels.zip(values).forEach { (label, value) ->
            row.addView(toggleChoice(
                label = label,
                description = when (value) {
                    1 -> "约 150 MB / 小时"
                    2 -> "约 800 MB / 小时"
                    3 -> "约 1.8 GB / 小时"
                    4 -> "约 4 GB / 小时"
                    else -> "根据片源自动选择"
                },
                checked = (value == default),
            ) { _ -> onChosen(value) }.apply {
                layoutParams = LinearLayout.LayoutParams(
                    0,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    1f,
                ).apply {
                    setMargins(0, 0, dp(8), 0)
                }
            })
        }
        addView(row)
    }
}

/**
 * Per-item offline manager side sheet. For each entry in the OfflineRepository
 * matching the currently-open media item, shows quality + progress + state
 * line plus pause / resume / delete actions.
 */
fun ComponentActivity.offlineManagerSheet(
    entries: List<OfflineRepository.Entry>,
    onClose: () -> Unit,
    onPause: (OfflineRepository.Entry) -> Unit,
    onResume: (OfflineRepository.Entry) -> Unit,
    onDelete: (OfflineRepository.Entry) -> Unit,
): View {
    return sideSheet {
        addView(infusePanelTitle("离线管理"))
        addView(TextView(this@offlineManagerSheet).apply {
            text = "离线内容保存在本机存储中，无需联网即可播放。"
            textSize = 12f
            setTextColor(TvColors.TextSecondary)
            setPadding(0, dp(4), 0, dp(12))
        })
        entries.forEach { entry ->
            addView(entryLine(entry, onPause, onResume, onDelete))
        }
        addView(detailsActions(listOf(
            InfuseAction("返回详情", TvIcon.BACK, InfuseActionEmphasis.SECONDARY, onClose),
        )))
    }
}

private fun ComponentActivity.entryLine(
    entry: OfflineRepository.Entry,
    onPause: (OfflineRepository.Entry) -> Unit,
    onResume: (OfflineRepository.Entry) -> Unit,
    onDelete: (OfflineRepository.Entry) -> Unit,
): View {
    val qualityText = qualityLabelFor(entry.quality())
    val stateText = when (entry.state()) {
        OfflineRepository.State.QUEUED -> "队列中"
        OfflineRepository.State.DOWNLOADING ->
            String.format("下载中 %.0f%%", entry.progressPercent())
        OfflineRepository.State.READY -> "已就绪"
        OfflineRepository.State.FAILED -> "失败：${entry.errorMessage().ifBlank { "未知错误" }}"
        OfflineRepository.State.PAUSED -> "已暂停"
    }
    val sizeText = if (entry.bytesTotal() > 0) {
        "共 ${humanizeBytes(entry.bytesTotal())}"
    } else {
        "估算容量中..."
    }
    val container = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(0, dp(8), 0, dp(12))
        isFocusable = false
    }
    val header = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        setPadding(0, 0, 0, dp(6))
    }
    header.addView(TextView(this).apply {
        text = qualityText
        textSize = 14f
        setTextColor(TvColors.TextPrimary)
        includeFontPadding = false
        layoutParams = LinearLayout.LayoutParams(
            dp(110),
            ViewGroup.LayoutParams.WRAP_CONTENT,
        )
    })
    header.addView(TextView(this).apply {
        text = stateText
        textSize = 13f
        setTextColor(TvColors.TextSecondary)
        includeFontPadding = false
        maxLines = 1
        ellipsize = TextUtils.TruncateAt.END
        layoutParams = LinearLayout.LayoutParams(
            0,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            1f,
        )
    })
    header.addView(TextView(this).apply {
        text = sizeText
        textSize = 12f
        setTextColor(TvColors.TextMuted)
        gravity = Gravity.END
        includeFontPadding = false
        layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
        )
    })
    container.addView(header)

    if (entry.state() == OfflineRepository.State.DOWNLOADING) {
        container.addView(ProgressBar(
            this,
            null,
            android.R.attr.progressBarStyleHorizontal,
        ).apply {
            isIndeterminate = false
            max = 10000
            progress = (entry.progressPercent() * 100).toInt()
            layoutParams = ViewGroup.MarginLayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(6),
            ).apply {
                setMargins(0, 0, 0, dp(8))
            }
        })
    }
    val actions = mutableListOf<InfuseAction>()
    when (entry.state()) {
        OfflineRepository.State.QUEUED, OfflineRepository.State.DOWNLOADING -> {
            actions += InfuseAction("暂停", TvIcon.SETTINGS, InfuseActionEmphasis.SECONDARY) { onPause(entry) }
        }
        OfflineRepository.State.PAUSED -> {
            actions += InfuseAction("继续", TvIcon.PLAY, InfuseActionEmphasis.PRIMARY) { onResume(entry) }
        }
        OfflineRepository.State.FAILED -> {
            actions += InfuseAction("重试", TvIcon.REFRESH, InfuseActionEmphasis.PRIMARY) { onResume(entry) }
        }
        OfflineRepository.State.READY -> {
            // No-op: user plays from the offline rail or regular "播放" button.
        }
    }
    actions += InfuseAction("删除", TvIcon.BACK, InfuseActionEmphasis.QUIET) { onDelete(entry) }
    container.addView(detailsActions(actions))
    return container
}

private fun qualityLabelFor(q: Int): String = when (q) {
    1 -> "480p"
    2 -> "720p"
    3 -> "1080p"
    4 -> "4K"
    else -> "自动"
}

private fun humanizeBytes(bytes: Long): String = when {
    bytes >= 1024L * 1024 * 1024 * 1024 -> String.format("%.1f TB", bytes / (1024e12))
    bytes >= 1024L * 1024 * 1024 -> String.format("%.1f GB", bytes / (1024e9))
    bytes >= 1024L * 1024 -> String.format("%.1f MB", bytes / (1024e6))
    bytes >= 1024L -> String.format("%.1f KB", bytes / (1024e3))
    else -> "${bytes}B"
}
