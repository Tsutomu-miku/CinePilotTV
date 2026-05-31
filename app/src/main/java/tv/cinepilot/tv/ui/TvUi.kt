package tv.cinepilot.tv.ui

import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.ComponentActivity
import tv.cinepilot.core.protocol.MediaItemSummary

fun ComponentActivity.screen(title: String, content: LinearLayout.() -> Unit): ScrollView {
    val container = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(56), dp(42), dp(56), dp(56))
        setBackgroundColor(Color.rgb(8, 13, 24))
    }
    container.addView(TextView(this).apply {
        text = "CinePilot TV"
        textSize = 14f
        typeface = Typeface.DEFAULT_BOLD
        setTextColor(Color.rgb(94, 234, 212))
        letterSpacing = 0.08f
        setPadding(0, 0, 0, dp(6))
    })
    container.addView(TextView(this).apply {
        text = title
        textSize = 36f
        typeface = Typeface.DEFAULT_BOLD
        setTextColor(Color.WHITE)
        gravity = Gravity.START
        setPadding(0, 0, 0, dp(28))
    })
    container.content()
    return ScrollView(this).apply {
        setBackgroundColor(Color.rgb(8, 13, 24))
        isFillViewport = true
        addView(
            container,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ),
        )
    }
}

fun ComponentActivity.input(hintText: String, inputTypeValue: Int = InputType.TYPE_CLASS_TEXT): EditText {
    return EditText(this).apply {
        hint = hintText
        inputType = inputTypeValue
        textSize = 18f
        setSingleLine(true)
        setTextColor(Color.WHITE)
        setHintTextColor(Color.rgb(148, 163, 184))
        setFocusableColors(this, Color.rgb(31, 78, 91), Color.rgb(16, 24, 39))
        setPadding(dp(18), 0, dp(18), 0)
    }
}

fun ComponentActivity.action(text: String, onClick: () -> Unit): Button {
    return Button(this).apply {
        this.text = text
        textSize = 18f
        isAllCaps = false
        minHeight = dp(54)
        minimumHeight = dp(54)
        setTextColor(Color.WHITE)
        setFocusableColors(this, Color.rgb(20, 184, 166), Color.rgb(26, 36, 52))
        setOnClickListener { onClick() }
        setPadding(dp(18), 0, dp(18), 0)
    }
}

fun ComponentActivity.compactAction(text: String, onClick: () -> Unit): Button {
    return action(text, onClick).apply {
        layoutParams = LinearLayout.LayoutParams(dp(118), dp(56)).apply {
            rightMargin = dp(10)
        }
    }
}

fun ComponentActivity.label(text: String): TextView {
    return TextView(this).apply {
        this.text = text
        textSize = 18f
        setTextColor(Color.rgb(226, 232, 240))
        setLineSpacing(0f, 1.08f)
        setPadding(0, dp(8), 0, dp(8))
    }
}

fun ComponentActivity.section(text: String): TextView {
    return TextView(this).apply {
        this.text = text
        textSize = 22f
        typeface = Typeface.DEFAULT_BOLD
        setTextColor(Color.rgb(45, 212, 191))
        setPadding(0, dp(28), 0, dp(12))
    }
}

fun ComponentActivity.toolbar(content: LinearLayout.() -> Unit): LinearLayout {
    return LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        setPadding(0, 0, 0, dp(18))
        content()
    }
}

fun ComponentActivity.actionStrip(actions: List<View>): HorizontalScrollView {
    val row = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        actions.forEach { actionView ->
            addView(
                actionView,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    dp(56),
                ).apply {
                    rightMargin = dp(12)
                    bottomMargin = dp(12)
                },
            )
        }
    }
    return HorizontalScrollView(this).apply {
        isHorizontalScrollBarEnabled = false
        addView(row)
    }
}

fun ComponentActivity.emptyState(text: String): TextView {
    return label(text).apply {
        gravity = Gravity.CENTER
        textSize = 22f
        setTextColor(Color.rgb(148, 163, 184))
        background = rounded(Color.rgb(13, 20, 33), dp(10), dp(1), Color.rgb(30, 41, 59))
        setPadding(dp(28), dp(42), dp(28), dp(42))
    }
}

fun ComponentActivity.metaLine(item: MediaItemSummary): TextView {
    return supportingLabel("${item.type()}${if (item.productionYear() != null) " · ${item.productionYear()}" else ""}").apply {
        textSize = 18f
        setTextColor(Color.rgb(94, 234, 212))
        typeface = Typeface.DEFAULT_BOLD
    }
}

fun ComponentActivity.supportingLabel(text: String): TextView {
    return label(text).apply {
        textSize = 18f
        setTextColor(Color.rgb(203, 213, 225))
    }
}

fun ComponentActivity.resumeBadge(text: String): TextView {
    return label(text).apply {
        typeface = Typeface.DEFAULT_BOLD
        setTextColor(Color.WHITE)
        background = rounded(Color.rgb(15, 118, 110), dp(8))
        setPadding(dp(14), dp(10), dp(14), dp(10))
    }
}

fun ComponentActivity.bodyText(text: String): TextView {
    return label(text).apply {
        textSize = 18f
        setTextColor(Color.rgb(226, 232, 240))
        setLineSpacing(4f, 1.08f)
    }
}

fun ComponentActivity.setFocusableColors(view: TextView, focusedColor: Int, normalColor: Int) {
    view.background = rounded(normalColor, dp(8))
    view.setOnFocusChangeListener { focusedView, hasFocus ->
        focusedView.background = rounded(
            if (hasFocus) focusedColor else normalColor,
            dp(8),
            if (hasFocus) dp(2) else 0,
            Color.rgb(153, 246, 228),
        )
    }
}

fun ComponentActivity.rounded(
    color: Int,
    radius: Int,
    strokeWidth: Int = 0,
    strokeColor: Int = Color.TRANSPARENT,
): GradientDrawable {
    return GradientDrawable().apply {
        setColor(color)
        cornerRadius = radius.toFloat()
        if (strokeWidth > 0) {
            setStroke(strokeWidth, strokeColor)
        }
    }
}

fun ComponentActivity.dp(value: Int): Int {
    return (value * resources.displayMetrics.density).toInt()
}
