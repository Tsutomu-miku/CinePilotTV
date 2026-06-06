package tv.cinepilot.tv.ui

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.ComponentActivity

data class TvOptionSelectItem(
    val label: String,
    val selected: Boolean,
    val onSelected: () -> Unit,
)

fun ComponentActivity.optionSelect(
    title: String,
    selectedLabel: String,
    options: List<TvOptionSelectItem>,
    requestFocus: Boolean = false,
): View {
    return LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        isFocusable = true
        isClickable = true
        descendantFocusability = ViewGroup.FOCUS_BLOCK_DESCENDANTS
        contentDescription = "$title，当前 $selectedLabel"
        background = glassDrawable(GlassTokens.ControlRadius)
        setPadding(dp(14), 0, dp(12), 0)
        addView(optionTitle(title))
        addView(optionValue(selectedLabel), LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        addView(optionChevron())
        setOnClickListener { anchor ->
            showOptionPopover(anchor, title, options)
        }
        setOnFocusChangeListener { focusedView, hasFocus ->
            focusedView.applyGlassFocus(hasFocus)
        }
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            dp(44),
        ).apply {
            bottomMargin = dp(8)
        }
        if (requestFocus) {
            post { requestFocus() }
        }
    }
}

private fun ComponentActivity.optionTitle(title: String): TextView {
    return TextView(this).apply {
        text = title
        textSize = TvType.Metadata
        setTextColor(TvColors.AccentStrong)
        gravity = Gravity.CENTER_VERTICAL
        includeFontPadding = false
        layoutParams = LinearLayout.LayoutParams(dp(80), LinearLayout.LayoutParams.WRAP_CONTENT)
    }
}

private fun ComponentActivity.optionValue(value: String): TextView {
    return TextView(this).apply {
        text = value
        textSize = TvType.Metadata
        setTextColor(TvColors.TextSecondary)
        gravity = Gravity.CENTER_VERTICAL
        includeFontPadding = false
        maxLines = 1
        ellipsize = TextUtils.TruncateAt.END
    }
}

private fun ComponentActivity.optionChevron(): TextView {
    return TextView(this).apply {
        text = "⌄"
        textSize = TvType.Metadata
        setTextColor(TvColors.TextMuted)
        gravity = Gravity.CENTER
        includeFontPadding = false
        layoutParams = LinearLayout.LayoutParams(dp(18), LinearLayout.LayoutParams.WRAP_CONTENT)
    }
}

private fun ComponentActivity.showOptionPopover(
    anchor: View,
    title: String,
    options: List<TvOptionSelectItem>,
) {
    val list = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(12), dp(10), dp(12), dp(12))
    }
    list.addView(popoverTitle(title))
    var popup: PopupWindow? = null
    var selectedView: View? = null
    options.forEach { option ->
        val row = popoverOption(option) {
            popup?.dismiss()
            option.onSelected()
        }
        if (option.selected) {
            selectedView = row
        }
        list.addView(row)
    }
    val scroll = ScrollView(this).apply {
        isFillViewport = false
        isFocusable = false
        addView(list)
    }
    val width = minOf(dp(680), resources.displayMetrics.widthPixels - dp(160))
    val height = minOf(dp(74 + options.size * 46), resources.displayMetrics.heightPixels - dp(160))
    popup = PopupWindow(scroll, width, height, true).apply {
        isOutsideTouchable = true
        setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        elevation = dp(10).toFloat()
    }
    scroll.background = glassDrawable(GlassTokens.PanelRadius)
    popup?.showAtLocation(anchor.rootView, Gravity.CENTER, 0, 0)
    (selectedView ?: list.getChildAt(1))?.post {
        (selectedView ?: list.getChildAt(1))?.requestFocus()
    }
}

private fun ComponentActivity.popoverTitle(title: String): TextView {
    return TextView(this).apply {
        text = title
        textSize = TvType.Body
        setTextColor(TvColors.TextPrimary)
        includeFontPadding = false
        setPadding(dp(8), dp(6), dp(8), dp(12))
    }
}

private fun ComponentActivity.popoverOption(option: TvOptionSelectItem, onClick: () -> Unit): TextView {
    return TextView(this).apply {
        text = if (option.selected) "✓ ${option.label}" else "   ${option.label}"
        isSelected = option.selected
        isFocusable = true
        isClickable = true
        maxLines = 1
        ellipsize = TextUtils.TruncateAt.END
        textSize = TvType.Metadata
        setTextColor(if (option.selected) TvColors.AccentStrong else TvColors.TextSecondary)
        gravity = Gravity.CENTER_VERTICAL
        includeFontPadding = false
        background = optionBackground(option.selected, false)
        setPadding(dp(12), 0, dp(12), 0)
        setOnClickListener { onClick() }
        setOnFocusChangeListener { focusedView, hasFocus ->
            focusedView.animate()
                .translationZ(if (hasFocus) dp(6).toFloat() else 0f)
                .setDuration(140L)
                .start()
            focusedView.background = optionBackground(option.selected, hasFocus)
            if (focusedView is TextView) {
                focusedView.setTextColor(when {
                    hasFocus -> TvColors.FocusText
                    option.selected -> TvColors.AccentStrong
                    else -> TvColors.TextSecondary
                })
            }
        }
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            dp(40),
        ).apply {
            bottomMargin = dp(6)
        }
    }
}

private fun ComponentActivity.optionBackground(selected: Boolean, focused: Boolean) = rounded(
    if (focused) TvColors.Focus else if (selected) TvColors.SurfaceControl else TvColors.SurfaceRaised,
    dp(TvRadius.Control),
    if (focused || selected) dp(2) else dp(1),
    when {
        focused -> TvColors.FocusRing
        selected -> TvColors.Accent
        else -> TvColors.PillBorder
    },
)
