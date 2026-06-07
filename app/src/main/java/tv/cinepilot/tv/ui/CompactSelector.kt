package tv.cinepilot.tv.ui

import android.view.View
import android.view.ViewGroup
import androidx.activity.ComponentActivity

fun ComponentActivity.compactSelectorRow(vararg selectors: View): View {
    return TvFlowLayout(this).apply {
        clipChildren = false
        clipToPadding = false
        selectors.forEach { selector ->
            addView(selector)
            val params = selector.layoutParams as? ViewGroup.MarginLayoutParams
            params?.apply {
                rightMargin = dp(8)
                bottomMargin = dp(8)
            }
        }
    }
}
