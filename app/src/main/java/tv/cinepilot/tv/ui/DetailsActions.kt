package tv.cinepilot.tv.ui

import android.view.View
import androidx.activity.ComponentActivity

fun ComponentActivity.detailsActions(actions: List<InfuseAction>): View {
    return infuseActions(actions, requestFirstFocus = actions.firstOrNull()?.emphasis == InfuseActionEmphasis.PRIMARY)
}
