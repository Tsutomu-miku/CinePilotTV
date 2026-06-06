package tv.cinepilot.tv.ui

import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.content.res.ColorStateList
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.ComponentActivity
import java.util.WeakHashMap

fun ComponentActivity.choiceAction(text: String, selected: Boolean, onClick: () -> Unit): android.widget.Button {
    return action(if (selected) "已选 $text" else text, onClick).apply {
        val normalColor = if (selected) TvColors.AccentStrong else TvColors.SurfaceControl
        setTextColor(if (selected) TvColors.FocusText else TvColors.TextPrimary)
        background = rounded(normalColor, dp(TvRadius.Control), dp(1), TvColors.FocusRing)
        setOnFocusChangeListener { focusedView, hasFocus ->
            applyFocusState(focusedView, hasFocus)
            if (focusedView is TextView) {
                focusedView.setTextColor(if (hasFocus || selected) TvColors.FocusText else TvColors.TextPrimary)
            }
            animateFocusBackground(
                view = focusedView,
                hasFocus = hasFocus,
                focusedColor = TvColors.Focus,
                normalColor = normalColor,
            )
        }
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            dp(TvSize.ControlHeight),
        ).apply {
            rightMargin = dp(TvSpacing.ControlGap)
            bottomMargin = dp(TvSpacing.ControlGap)
        }
    }
}

fun ComponentActivity.radioChoice(text: String, selected: Boolean, onClick: () -> Unit): RadioButton {
    return RadioButton(this).apply {
        this.text = text
        isChecked = selected
        textSize = TvType.Metadata
        minHeight = dp(40)
        minimumHeight = dp(40)
        setTextColor(if (selected) TvColors.FocusText else TvColors.TextSecondary)
        buttonTintList = ColorStateList(
            arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf()),
            intArrayOf(TvColors.FocusText, TvColors.TextMuted),
        )
        background = rounded(if (selected) TvColors.AccentStrong else TvColors.SurfaceControl, dp(TvRadius.Control), dp(1), TvColors.FocusRing)
        setPadding(dp(10), 0, dp(12), 0)
        setOnClickListener { onClick() }
        setOnFocusChangeListener { focusedView, hasFocus ->
            applyFocusState(focusedView, hasFocus)
            setTextColor(if (hasFocus || selected) TvColors.FocusText else TvColors.TextSecondary)
            animateFocusBackground(
                view = focusedView,
                hasFocus = hasFocus,
                focusedColor = TvColors.Focus,
                normalColor = if (selected) TvColors.AccentStrong else TvColors.SurfaceControl,
            )
        }
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            dp(40),
        ).apply {
            rightMargin = dp(8)
            bottomMargin = dp(8)
        }
    }
}

fun <T : View> T.requestInitialFocus(): T {
    post { requestFocus() }
    return this
}

fun <T : View> T.keepFocusOnVerticalDpad(
    consumeUp: Boolean = true,
    consumeDown: Boolean = true,
): T {
    setOnKeyListener { _, keyCode, event ->
        event.action == KeyEvent.ACTION_DOWN &&
            ((consumeUp && keyCode == KeyEvent.KEYCODE_DPAD_UP) ||
                (consumeDown && keyCode == KeyEvent.KEYCODE_DPAD_DOWN))
    }
    return this
}

fun <T : View> T.scrollOnVerticalDpad(
    scrollView: ScrollView,
    scrollUp: Boolean = true,
    scrollDown: Boolean = true,
): T {
    setOnKeyListener { _, keyCode, event ->
        if (event.action != KeyEvent.ACTION_DOWN) {
            false
        } else if (scrollUp && keyCode == KeyEvent.KEYCODE_DPAD_UP) {
            scrollView.dpadScrollBy(-1)
        } else if (scrollDown && keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
            scrollView.dpadScrollBy(1)
        } else {
            false
        }
    }
    return this
}

fun <T : ScrollView> T.bindVerticalDpadScrollFallback(): T {
    post {
        getChildAt(0)?.bindVerticalDpadScrollFallback(this)
    }
    return this
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
        applyFocusState(focusedView, hasFocus)
        if (focusedView is TextView && focusedTextColor != null && normalTextColor != null) {
            focusedView.setTextColor(if (hasFocus) focusedTextColor else normalTextColor)
        }
        animateFocusBackground(
            view = focusedView,
            hasFocus = hasFocus,
            focusedColor = focusedColor,
            normalColor = normalColor,
        )
    }
}

private fun ComponentActivity.applyFocusState(view: View, hasFocus: Boolean) {
    view.animate()
        .alpha(if (hasFocus) 1f else 0.96f)
        .translationZ(if (hasFocus) dp(6).toFloat() else 0f)
        .setDuration(FOCUS_ANIMATION_MS)
        .start()
}

private fun ComponentActivity.animateFocusBackground(
    view: View,
    hasFocus: Boolean,
    focusedColor: Int,
    normalColor: Int,
) {
    focusAnimators[view]?.cancel()
    val startColor = if (hasFocus) normalColor else focusedColor
    val endColor = if (hasFocus) focusedColor else normalColor
    focusAnimators[view] = ValueAnimator.ofObject(ArgbEvaluator(), startColor, endColor).apply {
        duration = FOCUS_ANIMATION_MS
        addUpdateListener { animator ->
            view.background = rounded(
                animator.animatedValue as Int,
                dp(TvRadius.Control),
                if (hasFocus) dp(3) else dp(1),
                TvColors.FocusRing,
            )
        }
        start()
    }
}

private fun ScrollView.dpadScrollBy(direction: Int): Boolean {
    val child = getChildAt(0)
    val maxScroll = ((child?.height ?: 0) - height).coerceAtLeast(0)
    if (maxScroll == 0) {
        return true
    }
    val delta = (height * DPAD_SCROLL_FRACTION).toInt().coerceAtLeast(DPAD_SCROLL_MIN_PX)
    val target = (scrollY + delta * direction).coerceIn(0, maxScroll)
    if (target != scrollY) {
        smoothScrollTo(0, target)
    }
    return true
}

private fun View.bindVerticalDpadScrollFallback(scrollView: ScrollView) {
    if (isFocusable) {
        setOnKeyListener { focusedView, keyCode, event ->
            val focusDirection = keyCode.toFocusDirection() ?: return@setOnKeyListener false
            if (event.action != KeyEvent.ACTION_DOWN) {
                return@setOnKeyListener false
            }
            val nextFocus = focusedView.focusSearch(focusDirection)
            if (nextFocus != null && nextFocus != focusedView && scrollView.containsDescendant(nextFocus)) {
                return@setOnKeyListener false
            }
            scrollView.dpadScrollBy(if (focusDirection == View.FOCUS_UP) -1 else 1)
        }
    }
    if (this is ViewGroup) {
        for (index in 0 until childCount) {
            getChildAt(index).bindVerticalDpadScrollFallback(scrollView)
        }
    }
}

private fun Int.toFocusDirection(): Int? {
    return when (this) {
        KeyEvent.KEYCODE_DPAD_UP -> View.FOCUS_UP
        KeyEvent.KEYCODE_DPAD_DOWN -> View.FOCUS_DOWN
        else -> null
    }
}

private fun ViewGroup.containsDescendant(target: View): Boolean {
    var current: View? = target
    while (current != null) {
        if (current == this) {
            return true
        }
        current = current.parent as? View
    }
    return false
}

private val focusAnimators = WeakHashMap<View, ValueAnimator>()
private const val FOCUS_ANIMATION_MS = 160L
private const val DPAD_SCROLL_FRACTION = 0.42f
private const val DPAD_SCROLL_MIN_PX = 80
