package tv.cinepilot.tv.ui

import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.ComponentActivity
import tv.cinepilot.plugin.spi.SubtitleSearchResult

/**
 * Side sheet that shows subtitle search results.
 *
 * Displays a list of [SubtitleSearchResult] rows with name, language, format,
 * rating, and download count. Each row is clickable to download and select
 * the subtitle. A loading / empty state is also supported.
 */
fun ComponentActivity.subtitleSearchSheet(
    itemName: String,
    results: List<SubtitleSearchResult>,
    isLoading: Boolean,
    selectedId: String? = null,
    onPick: (SubtitleSearchResult) -> Unit,
    onClose: () -> Unit,
): View {
    return sideSheet {
        addView(infusePanelTitle("搜索在线字幕"))
        addView(TextView(this@subtitleSearchSheet).apply {
            text = itemName
            textSize = 12f
            setTextColor(TvColors.TextSecondary)
            setPadding(0, dp(4), 0, dp(14))
            setLineSpacing(2f, 1.05f)
        })

        if (isLoading) {
            addView(TextView(this@subtitleSearchSheet).apply {
                text = "正在搜索…"
                textSize = 14f
                setTextColor(TvColors.TextMuted)
                setPadding(0, dp(24), 0, dp(24))
                gravity = Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                )
            })
        } else if (results.isEmpty()) {
            addView(TextView(this@subtitleSearchSheet).apply {
                text = "未找到匹配的字幕"
                textSize = 14f
                setTextColor(TvColors.TextMuted)
                setPadding(0, dp(24), 0, dp(24))
                gravity = Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                )
            })
        } else {
            results.forEach { result ->
                addView(subtitleResultRow(result, result.id() == selectedId) { onPick(result) })
            }
        }

        addView(detailsActions(listOf(
            InfuseAction("返回", TvIcon.BACK, InfuseActionEmphasis.SECONDARY, onClose),
        )))
    }
}

/** A single selectable subtitle result row. */
private fun ComponentActivity.subtitleResultRow(
    result: SubtitleSearchResult,
    isSelected: Boolean,
    onClick: () -> Unit,
): View {
    val row = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(16), dp(12), dp(16), dp(12))
        isFocusable = true
        isClickable = true
        background = rounded(
            if (isSelected) TvColors.Accent else TvColors.SurfaceControl,
            dp(TvRadius.Control),
            dp(1),
            TvColors.FocusRing
        )
        setOnClickListener { onClick() }
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
        ).apply {
            bottomMargin = dp(8)
        }
    }

    // Primary line: name
    row.addView(TextView(this).apply {
        text = result.name()
        textSize = 14f
        setTextColor(if (isSelected) TvColors.Accent else TvColors.TextPrimary)
        maxLines = 1
        android.text.TextUtils.TruncateAt.MIDDLE.also { ellipsize = it }
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
        )
    })

    // Secondary line: language, format, rating, downloads
    val secondary = buildString {
        if (result.language().isNotBlank()) {
            append(result.language())
        }
        val formatLabel = formatLabel(result.format())
        if (formatLabel.isNotBlank()) {
            if (isNotEmpty()) append(" · ")
            append(formatLabel)
        }
        if (result.rating().isNotBlank()) {
            if (isNotEmpty()) append(" · ")
            append(result.rating())
        }
        if (result.downloadCount() > 0) {
            if (isNotEmpty()) append(" · ")
            append("${result.downloadCount()} 次下载")
        }
    }

    if (secondary.isNotBlank()) {
        row.addView(TextView(this).apply {
            text = secondary
            textSize = 11f
            setTextColor(TvColors.TextSecondary)
            setPadding(0, dp(2), 0, 0)
            maxLines = 1
            android.text.TextUtils.TruncateAt.END.also { ellipsize = it }
        })
    }

    // Author line (optional)
    if (result.author().isNotBlank()) {
        row.addView(TextView(this).apply {
            text = "上传者：${result.author()}"
            textSize = 11f
            setTextColor(TvColors.TextMuted)
            setPadding(0, dp(2), 0, 0)
            maxLines = 1
            android.text.TextUtils.TruncateAt.END.also { ellipsize = it }
        })
    }

    return row
}

private fun formatLabel(format: SubtitleSearchResult.Format): String =
    when (format) {
        SubtitleSearchResult.Format.SRT -> "SRT"
        SubtitleSearchResult.Format.ASS -> "ASS"
        SubtitleSearchResult.Format.SSA -> "SSA"
        SubtitleSearchResult.Format.VTT -> "WebVTT"
        SubtitleSearchResult.Format.PGS -> "PGS"
        SubtitleSearchResult.Format.UNKNOWN -> ""
    }
