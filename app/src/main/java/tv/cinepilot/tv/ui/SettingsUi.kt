package tv.cinepilot.tv.ui

import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.ComponentActivity
import tv.cinepilot.tv.R

fun ComponentActivity.settingsPage(
    title: String,
    subtitle: String,
    content: LinearLayout.() -> Unit,
): View {
    val backdrop = cinematicBackdrop(blurred = false).apply { alpha = 0.28f }
    return cinematicStage(backdrop, scrollable = false) {
        addView(settingsHeader(title, subtitle), FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            dp(98),
            Gravity.TOP or Gravity.START,
        ).apply {
            leftMargin = dp(MediaWallTokens.ScreenX)
            rightMargin = dp(MediaWallTokens.ScreenX)
            topMargin = dp(MediaWallTokens.ScreenTop)
        })
        addView(ScrollView(this@settingsPage).apply {
            isFillViewport = false
            isFocusable = false
            descendantFocusability = ViewGroup.FOCUS_AFTER_DESCENDANTS
            clipToPadding = false
            addView(LinearLayout(this@settingsPage).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(
                    dp(MediaWallTokens.ScreenX),
                    0,
                    dp(MediaWallTokens.ScreenX),
                    dp(MediaWallTokens.ScreenBottom),
                )
                content()
            }, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ))
        }, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT,
        ).apply {
            topMargin = dp(128)
        })
    }
}

fun ComponentActivity.settingsGridSection(
    title: String,
    rows: List<View>,
): View {
    return LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        clipChildren = false
        clipToPadding = false
        setPadding(0, 0, 0, dp(16))
        addView(TextView(this@settingsGridSection).apply {
            text = title
            textSize = MediaWallType.RowTitle
            setTextColor(TvColors.AccentStrong)
            includeFontPadding = false
            setPadding(0, 0, 0, dp(8))
        })
        rows.chunked(2).forEach { pair ->
            addView(LinearLayout(this@settingsGridSection).apply {
                orientation = LinearLayout.HORIZONTAL
                clipChildren = false
                clipToPadding = false
                pair.forEachIndexed { index, row ->
                    addView(row, LinearLayout.LayoutParams(
                        0,
                        dp(SettingsTokens.RowHeight),
                        1f,
                    ).apply {
                        rightMargin = if (index == 0 && pair.size > 1) dp(10) else 0
                        bottomMargin = dp(10)
                    })
                }
                if (pair.size == 1) {
                    addView(View(this@settingsGridSection), LinearLayout.LayoutParams(
                        0,
                        dp(SettingsTokens.RowHeight),
                        1f,
                    ).apply {
                        leftMargin = dp(10)
                        bottomMargin = dp(10)
                    })
                }
            })
        }
    }
}

fun ComponentActivity.settingsToggleRow(
    label: String,
    description: String,
    checked: Boolean,
    requestFocus: Boolean = false,
    onChanged: (Boolean) -> Unit,
): View {
    val row = settingsBaseRow(label, description)
    val value = settingsValueText(if (checked) "开" else "关")
    val indicator = CheckBox(this).apply {
        isChecked = checked
        isClickable = false
        isFocusable = false
        buttonTintList = ColorStateList(
            arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf()),
            intArrayOf(TvColors.AccentStrong, TvColors.TextMuted),
        )
    }
    row.setTag(R.id.tag_toggle_value, checked)
    row.addView(value)
    row.addView(indicator, LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.WRAP_CONTENT,
        LinearLayout.LayoutParams.WRAP_CONTENT,
    ).apply {
        marginStart = dp(10)
    })
    fun updateVisual(hasFocus: Boolean) {
        val on = row.getTag(R.id.tag_toggle_value) as? Boolean ?: checked
        row.applySettingsRowFocus(hasFocus, active = on)
        value.text = if (on) "开" else "关"
        value.setTextColor(when {
            hasFocus -> TvColors.FocusText
            on -> TvColors.AccentStrong
            else -> TvColors.TextMuted
        })
    }
    row.setOnClickListener {
        val next = !(row.getTag(R.id.tag_toggle_value) as? Boolean ?: checked)
        row.setTag(R.id.tag_toggle_value, next)
        indicator.isChecked = next
        updateVisual(row.hasFocus())
        onChanged(next)
    }
    row.setOnFocusChangeListener { view, hasFocus ->
        view.applySettingsRowFocus(hasFocus, active = row.getTag(R.id.tag_toggle_value) as? Boolean ?: checked)
        updateVisual(hasFocus)
    }
    updateVisual(false)
    if (requestFocus) row.requestInitialFocus()
    return row
}

fun ComponentActivity.settingsOptionRow(
    label: String,
    description: String,
    selectedLabel: String,
    options: List<TvOptionSelectItem>,
    requestFocus: Boolean = false,
): View {
    val row = settingsBaseRow(label, description)
    val value = settingsValueText(selectedLabel)
    row.addView(value, LinearLayout.LayoutParams(
        dp(210),
        LinearLayout.LayoutParams.WRAP_CONTENT,
    ))
    row.addView(settingsChevron())
    row.setOnClickListener { anchor ->
        showSettingsOptionPopup(anchor, label, options)
    }
    row.setOnFocusChangeListener { view, hasFocus ->
        view.applySettingsRowFocus(hasFocus)
        value.setTextColor(if (hasFocus) TvColors.FocusText else TvColors.TextSecondary)
    }
    row.background = settingsRowBackground(false, false)
    if (requestFocus) row.requestInitialFocus()
    return row
}

fun ComponentActivity.settingsActionRow(
    label: String,
    description: String,
    actionLabel: String,
    requestFocus: Boolean = false,
    onClick: () -> Unit,
): View {
    val row = settingsBaseRow(label, description)
    val value = settingsValueText(actionLabel)
    row.addView(value)
    row.addView(settingsChevron())
    row.setOnClickListener { onClick() }
    row.setOnFocusChangeListener { view, hasFocus ->
        view.applySettingsRowFocus(hasFocus)
        value.setTextColor(if (hasFocus) TvColors.FocusText else TvColors.TextSecondary)
    }
    row.background = settingsRowBackground(false, false)
    if (requestFocus) row.requestInitialFocus()
    return row
}

private fun ComponentActivity.settingsHeader(title: String, subtitle: String): View {
    return LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        gravity = Gravity.CENTER_VERTICAL
        addView(TextView(this@settingsHeader).apply {
            text = title
            textSize = 30f
            setTextColor(TvColors.TextPrimary)
            includeFontPadding = false
        })
        addView(TextView(this@settingsHeader).apply {
            text = subtitle
            textSize = MediaWallType.RowTitle
            setTextColor(TvColors.TextMuted)
            includeFontPadding = false
            setPadding(0, dp(8), 0, 0)
        })
    }
}

private fun ComponentActivity.settingsBaseRow(label: String, description: String): LinearLayout {
    return LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        isFocusable = true
        isClickable = true
        descendantFocusability = ViewGroup.FOCUS_BLOCK_DESCENDANTS
        clipChildren = false
        clipToPadding = false
        setPadding(dp(14), dp(8), dp(12), dp(8))
        addView(LinearLayout(this@settingsBaseRow).apply {
            orientation = LinearLayout.VERTICAL
            addView(TextView(this@settingsBaseRow).apply {
                text = label
                textSize = 13.5f
                setTextColor(TvColors.TextPrimary)
                includeFontPadding = false
                maxLines = 1
                ellipsize = TextUtils.TruncateAt.END
            })
            addView(TextView(this@settingsBaseRow).apply {
                text = description
                textSize = 11.5f
                setTextColor(TvColors.TextMuted)
                includeFontPadding = false
                maxLines = 1
                ellipsize = TextUtils.TruncateAt.END
                setPadding(0, dp(6), 0, 0)
            })
        }, LinearLayout.LayoutParams(
            0,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            1f,
        ).apply {
            rightMargin = dp(12)
        })
    }
}

private fun ComponentActivity.settingsValueText(value: String): TextView {
    return TextView(this).apply {
        text = value
        textSize = 12.5f
        setTextColor(TvColors.TextSecondary)
        gravity = Gravity.CENTER_VERTICAL or Gravity.END
        includeFontPadding = false
        maxLines = 1
        ellipsize = TextUtils.TruncateAt.END
    }
}

private fun ComponentActivity.settingsChevron(): TextView {
    return TextView(this).apply {
        text = "⌄"
        textSize = 15f
        setTextColor(TvColors.TextMuted)
        gravity = Gravity.CENTER
        includeFontPadding = false
        layoutParams = LinearLayout.LayoutParams(dp(18), LinearLayout.LayoutParams.WRAP_CONTENT)
    }
}

private fun ComponentActivity.showSettingsOptionPopup(
    anchor: View,
    title: String,
    options: List<TvOptionSelectItem>,
) {
    val list = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(10), dp(10), dp(10), dp(10))
        addView(TextView(this@showSettingsOptionPopup).apply {
            text = title
            textSize = 14f
            setTextColor(TvColors.TextPrimary)
            includeFontPadding = false
            setPadding(dp(8), dp(2), dp(8), dp(8))
        })
    }
    var popup: PopupWindow? = null
    var selectedView: View? = null
    options.forEach { option ->
        val row = settingsPopupOption(option) {
            popup?.dismiss()
            option.onSelected()
        }
        if (option.selected) selectedView = row
        list.addView(row)
    }
    val scroll = ScrollView(this).apply {
        isFillViewport = false
        isFocusable = false
        background = glassDrawable(8)
        addView(list)
    }
    val width = minOf(maxOf(anchor.width, dp(520)), resources.displayMetrics.widthPixels - dp(140))
    val height = minOf(dp(52 + options.size * 38), resources.displayMetrics.heightPixels - dp(170))
    popup = PopupWindow(scroll, width, height, true).apply {
        isOutsideTouchable = true
        setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        elevation = dp(10).toFloat()
    }
    popup?.showAsDropDown(anchor, 0, dp(6))
    (selectedView ?: list.getChildAt(1))?.post {
        (selectedView ?: list.getChildAt(1))?.requestFocus()
    }
}

private fun ComponentActivity.settingsPopupOption(option: TvOptionSelectItem, onClick: () -> Unit): TextView {
    return TextView(this).apply {
        text = if (option.selected) "✓ ${option.label}" else "   ${option.label}"
        isFocusable = true
        isClickable = true
        maxLines = 1
        ellipsize = TextUtils.TruncateAt.END
        textSize = 12.5f
        setTextColor(if (option.selected) TvColors.AccentStrong else TvColors.TextSecondary)
        gravity = Gravity.CENTER_VERTICAL
        includeFontPadding = false
        background = settingsPopupBackground(option.selected, false)
        setPadding(dp(12), 0, dp(12), 0)
        setOnClickListener { onClick() }
        setOnFocusChangeListener { view, hasFocus ->
            view.background = settingsPopupBackground(option.selected, hasFocus)
            setTextColor(when {
                hasFocus -> TvColors.TextPrimary
                option.selected -> TvColors.AccentStrong
                else -> TvColors.TextSecondary
            })
        }
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            dp(34),
        ).apply {
            bottomMargin = dp(4)
        }
    }
}

private fun ComponentActivity.settingsRowBackground(active: Boolean, focused: Boolean) = GlassDrawable(
    tintColor = when {
        focused -> TvColors.GlassFocusTint
        active -> Color.argb(96, Color.red(TvColors.Accent), Color.green(TvColors.Accent), Color.blue(TvColors.Accent))
        else -> TvColors.GlassTint
    },
    radius = dp(8).toFloat(),
    borderColor = when {
        focused -> TvColors.FocusRing
        active -> TvColors.Accent
        else -> TvColors.GlassBorder
    },
)

private fun View.applySettingsRowFocus(hasFocus: Boolean, active: Boolean = false) {
    animate()
        .translationZ(if (hasFocus) settingsViewDp(10).toFloat() else 0f)
        .alpha(if (hasFocus) 1f else 0.96f)
        .setDuration(160L)
        .start()
    background = GlassDrawable(
        tintColor = when {
            hasFocus -> TvColors.GlassFocusTint
            active -> Color.argb(96, Color.red(TvColors.Accent), Color.green(TvColors.Accent), Color.blue(TvColors.Accent))
            else -> TvColors.GlassTint
        },
        radius = settingsViewDp(8).toFloat(),
        borderColor = when {
            hasFocus -> TvColors.FocusRing
            active -> TvColors.Accent
            else -> TvColors.GlassBorder
        },
    )
}

private fun View.settingsViewDp(value: Int): Int {
    return (value * resources.displayMetrics.density + 0.5f).toInt()
}

private fun ComponentActivity.settingsPopupBackground(selected: Boolean, focused: Boolean) = rounded(
    when {
        focused -> TvColors.GlassFocusTint
        selected -> Color.argb(72, 255, 255, 255)
        else -> Color.argb(32, 0, 0, 0)
    },
    dp(7),
    dp(1),
    when {
        focused -> TvColors.FocusRing
        selected -> homeHairlineColor(92)
        else -> homeHairlineColor(44)
    },
)

private object SettingsTokens {
    const val RowHeight = 64
}
