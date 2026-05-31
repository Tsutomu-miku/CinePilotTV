package tv.cinepilot.tv.error

import android.widget.ScrollView
import androidx.activity.ComponentActivity
import tv.cinepilot.core.tv.TvAppState
import tv.cinepilot.tv.ui.TvIcon
import tv.cinepilot.tv.ui.action
import tv.cinepilot.tv.ui.iconAction
import tv.cinepilot.tv.ui.label
import tv.cinepilot.tv.ui.screen

fun ComponentActivity.errorRouteScreen(
    state: TvAppState,
    message: String,
    authenticationExpired: Boolean,
    onReturnDetails: () -> Unit,
    onRetryLowBitrate: () -> Unit,
    onPlaybackOptions: () -> Unit,
    onDiagnostics: () -> Unit,
    onHome: () -> Unit,
    onLogin: () -> Unit,
    onServerEntry: () -> Unit,
): ScrollView {
    return screen("出错了") {
        addView(label(message))
        if (state.selectedItem() != null && !authenticationExpired) {
            addView(iconAction("返回详情", TvIcon.BACK, onReturnDetails))
        }
        if (state.playableMedia() != null && !authenticationExpired) {
            addView(iconAction("低码率重试", TvIcon.SPEED, onRetryLowBitrate))
            addView(iconAction("切换音轨 / 字幕", TvIcon.SUBTITLES, onPlaybackOptions))
            addView(action("诊断信息", onDiagnostics))
        }
        if (state.homeRows().isNotEmpty() && !authenticationExpired) {
            addView(iconAction("返回首页", TvIcon.BACK, onHome))
        }
        if (state.server() != null) {
            addView(action("重新登录", onLogin))
        }
        addView(iconAction("返回服务器输入", TvIcon.BACK, onServerEntry))
    }
}
