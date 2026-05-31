package tv.cinepilot.tv.ui

import android.graphics.Color
import android.graphics.Typeface
import android.text.TextUtils
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
import androidx.annotation.DrawableRes
import tv.cinepilot.tv.R

fun ComponentActivity.screen(title: String, content: LinearLayout.() -> Unit): ScrollView {
    val container = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(TvSpacing.ScreenX), dp(TvSpacing.ScreenTop), dp(TvSpacing.ScreenX), dp(TvSpacing.ScreenBottom))
        setBackgroundColor(TvColors.Background)
    }
    container.addView(TextView(this).apply {
        text = "CinePilot TV"
        textSize = TvType.Brand
        typeface = Typeface.DEFAULT_BOLD
        setTextColor(TvColors.AccentStrong)
        letterSpacing = 0.08f
        setPadding(0, 0, 0, dp(6))
    })
    container.addView(TextView(this).apply {
        text = title
        textSize = TvType.Title
        typeface = Typeface.DEFAULT_BOLD
        setTextColor(TvColors.TextPrimary)
        gravity = Gravity.START
        maxLines = 2
        ellipsize = TextUtils.TruncateAt.END
        setPadding(0, 0, 0, dp(TvSpacing.SectionTop))
    })
    container.content()
    return ScrollView(this).apply {
        setBackgroundColor(TvColors.Background)
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
        textSize = TvType.Body
        setSingleLine(true)
        setTextColor(TvColors.TextPrimary)
        setHintTextColor(TvColors.TextMuted)
        setFocusableColors(this, Color.rgb(31, 78, 91), TvColors.SurfaceInput)
        setPadding(dp(18), 0, dp(18), 0)
    }
}

fun ComponentActivity.action(text: String, onClick: () -> Unit): Button {
    return Button(this).apply {
        this.text = text
        textSize = TvType.Body
        isAllCaps = false
        minHeight = dp(TvSize.ControlHeight)
        minimumHeight = dp(TvSize.ControlHeight)
        setTextColor(TvColors.TextPrimary)
        typeface = Typeface.DEFAULT_BOLD
        setFocusableColors(
            view = this,
            focusedColor = TvColors.Focus,
            normalColor = TvColors.SurfaceControl,
            focusedTextColor = TvColors.FocusText,
            normalTextColor = TvColors.TextPrimary,
        )
        setOnClickListener { onClick() }
        setPadding(dp(18), 0, dp(18), 0)
    }
}

fun ComponentActivity.iconAction(text: String, icon: TvIcon, onClick: () -> Unit): Button {
    return action(text, onClick).apply {
        setCompoundDrawablesRelativeWithIntrinsicBounds(icon.drawableRes, 0, 0, 0)
        compoundDrawablePadding = dp(10)
    }
}

fun ComponentActivity.compactIconAction(text: String, icon: TvIcon, onClick: () -> Unit): Button {
    return iconAction(text, icon, onClick).apply {
        layoutParams = LinearLayout.LayoutParams(dp(142), dp(TvSize.ControlHeight)).apply {
            rightMargin = dp(10)
        }
    }
}

fun ComponentActivity.compactAction(text: String, onClick: () -> Unit): Button {
    return action(text, onClick).apply {
        layoutParams = LinearLayout.LayoutParams(dp(118), dp(TvSize.ControlHeight)).apply {
            rightMargin = dp(10)
        }
    }
}

enum class TvIcon(@DrawableRes val drawableRes: Int) {
    SEARCH(R.drawable.ic_search),
    REFRESH(R.drawable.ic_refresh),
    LOGOUT(R.drawable.ic_logout),
    PLAY(R.drawable.ic_play),
    BACK(R.drawable.ic_back),
    SUBTITLES(R.drawable.ic_subtitles),
    SPEED(R.drawable.ic_speed),
}

fun ComponentActivity.label(text: String): TextView {
    return TextView(this).apply {
        this.text = text
        textSize = TvType.Body
        setTextColor(TvColors.TextSecondary)
        setLineSpacing(0f, 1.08f)
        setPadding(0, dp(8), 0, dp(8))
    }
}

fun ComponentActivity.section(text: String): TextView {
    return TextView(this).apply {
        this.text = text
        textSize = TvType.Section
        typeface = Typeface.DEFAULT_BOLD
        setTextColor(TvColors.Accent)
        setPadding(0, dp(TvSpacing.SectionTop), 0, dp(TvSpacing.SectionBottom))
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
                    dp(TvSize.ControlHeight),
                ).apply {
                    rightMargin = dp(TvSpacing.ControlGap)
                    bottomMargin = dp(TvSpacing.ControlGap)
                },
            )
        }
    }
    return HorizontalScrollView(this).apply {
        isHorizontalScrollBarEnabled = false
        addView(row)
    }
}

fun ComponentActivity.metadataPills(values: List<String>): HorizontalScrollView {
    val row = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        values.filter { it.isNotBlank() }.forEach { value ->
            addView(TextView(this@metadataPills).apply {
                text = value
                textSize = TvType.Metadata
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(TvColors.TextSecondary)
                background = rounded(TvColors.PosterFallback, dp(TvRadius.Control), dp(1), Color.rgb(51, 65, 85))
                setPadding(dp(12), dp(7), dp(12), dp(7))
            }, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ).apply {
                rightMargin = dp(10)
                bottomMargin = dp(10)
            })
        }
    }
    return HorizontalScrollView(this).apply {
        isHorizontalScrollBarEnabled = false
        addView(row)
    }
}

fun ComponentActivity.verticalSpace(height: Int): View {
    return View(this).apply {
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            dp(height),
        )
    }
}

fun <T : View> T.requestInitialFocus(): T {
    post { requestFocus() }
    return this
}

fun ComponentActivity.emptyState(text: String): TextView {
    return label(text).apply {
        gravity = Gravity.CENTER
        textSize = TvType.Section
        setTextColor(TvColors.TextMuted)
        background = rounded(TvColors.Surface, dp(TvRadius.Card), dp(1), TvColors.PosterBorder)
        setPadding(dp(28), dp(42), dp(28), dp(42))
    }
}

fun ComponentActivity.resumeBadge(text: String): TextView {
    return label(text).apply {
        typeface = Typeface.DEFAULT_BOLD
        setTextColor(TvColors.TextPrimary)
        background = rounded(TvColors.Resume, dp(TvRadius.Control))
        setPadding(dp(14), dp(10), dp(14), dp(10))
    }
}

fun ComponentActivity.bodyText(text: String): TextView {
    return label(text).apply {
        textSize = TvType.Body
        setTextColor(TvColors.TextSecondary)
        setLineSpacing(4f, 1.08f)
    }
}

fun ComponentActivity.setFocusableColors(
    view: TextView,
    focusedColor: Int,
    normalColor: Int,
    focusedTextColor: Int? = null,
    normalTextColor: Int? = null,
) {
    view.background = rounded(normalColor, dp(TvRadius.Control))
    view.setOnFocusChangeListener { focusedView, hasFocus ->
        focusedView.scaleX = if (hasFocus) 1.035f else 1f
        focusedView.scaleY = if (hasFocus) 1.035f else 1f
        focusedView.elevation = if (hasFocus) dp(8).toFloat() else 0f
        if (focusedView is TextView && focusedTextColor != null && normalTextColor != null) {
            focusedView.setTextColor(if (hasFocus) focusedTextColor else normalTextColor)
        }
        focusedView.background = rounded(
            if (hasFocus) focusedColor else normalColor,
            dp(TvRadius.Control),
            if (hasFocus) dp(4) else dp(1),
            TvColors.FocusRing,
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
