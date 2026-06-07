package tv.cinepilot.tv.ui

import android.view.View
import android.widget.LinearLayout
import androidx.activity.ComponentActivity

fun ComponentActivity.compactSelectorRow(vararg selectors: View): View {
    return LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        selectors.forEach { selector ->
            addView(selector, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ).apply {
                bottomMargin = dp(8)
            })
        }
    }
}
