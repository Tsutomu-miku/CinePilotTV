package tv.cinepilot.tv.ui

import android.graphics.Color
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.ComponentActivity

/**
 * 0-10 用户评分输入行（详情页 Hero 下方）。
 *
 * 用 5 颗可聚焦的 ★ 图标承载 0–10 的评分刻度（每颗 ★ = 2 分），
 * 点击第 n 颗即为 n×2 分；点击「清除」写回 null；
 * 数值区回显服务端的精确 Double（例如 7.5 / 10）。
 */
fun ComponentActivity.userRatingRow(
    currentRating: Double?,
    onChange: (Double?) -> Unit,
    communityRating: Double? = null,
): View {
    val row = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        clipChildren = false
        clipToPadding = false
        setPadding(0, dp(10), 0, dp(12))
    }
    val value = currentRating ?: 0.0
    val clampedZeroToTen = value.coerceIn(0.0, 10.0)
    val starsToFill = (clampedZeroToTen / 2.0).coerceIn(0.0, 5.0)
    (0 until 5).forEach { idx ->
        val filled = (idx + 1) <= starsToFill
        val text = if (filled) "★" else "☆"
        val color = if (filled) TvColors.Accent else TvColors.TextMuted
        row.addView(TextView(this).apply {
            this.text = text
            textSize = 20f
            gravity = Gravity.CENTER
            setTextColor(color)
            includeFontPadding = false
            isFocusable = true
            isClickable = true
            setPadding(dp(4), dp(4), dp(4), dp(4))
            setOnClickListener {
                onChange(((idx + 1) * 2).toDouble())
            }
            setOnLongClickListener {
                // 长按半星评分：整数 + 0.5
                onChange((idx * 2 + 1).toDouble())
                true
            }
            setOnFocusChangeListener { view, focused ->
                view.applyFocusOutline(focused, 8)
                (view as? TextView)?.setTextColor(when {
                    filled && focused -> Color.WHITE
                    filled -> TvColors.Accent
                    focused -> TvColors.TextPrimary
                    else -> TvColors.TextMuted
                })
            }
            background = rounded(
                Color.TRANSPARENT,
                dp(8),
                dp(1),
                Color.TRANSPARENT,
            )
            layoutParams = ViewGroup.MarginLayoutParams(
                dp(32),
                dp(32),
            ).apply {
                rightMargin = dp(2)
            }
        })
    }
    row.addView(TextView(this).apply {
        val exact = currentRating?.let { String.format("%.1f / 10", it) } ?: "未评分"
        val communityHint = communityRating?.takeIf { it > 0.0 }
            ?.let { String.format(" · 社区 %.1f", it) } ?: ""
        text = exact + communityHint
        textSize = 12f
        setTextColor(TvColors.TextSecondary)
        includeFontPadding = false
        maxLines = 1
        ellipsize = TextUtils.TruncateAt.END
        setPadding(dp(10), 0, dp(10), 0)
    })
    if (currentRating != null) {
        row.addView(TextView(this).apply {
            text = "清除评分"
            textSize = 12f
            gravity = Gravity.CENTER
            setTextColor(TvColors.TextMuted)
            includeFontPadding = false
            isFocusable = true
            isClickable = true
            maxLines = 1
            ellipsize = TextUtils.TruncateAt.END
            background = rounded(
                Color.argb(20, 0, 0, 0),
                dp(10),
                dp(1),
                homeHairlineColor(38),
            )
            setPadding(dp(12), 0, dp(12), 0)
            setOnClickListener { onChange(null) }
            setOnFocusChangeListener { view, focused ->
                view.applyFocusOutline(focused, 10)
                (view as? TextView)?.setTextColor(
                    if (focused) TvColors.TextPrimary else TvColors.TextMuted
                )
            }
            layoutParams = ViewGroup.MarginLayoutParams(
                ViewGroup.MarginLayoutParams.WRAP_CONTENT,
                dp(28),
            )
        })
    }
    return row
}
