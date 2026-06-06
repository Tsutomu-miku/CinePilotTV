package tv.cinepilot.tv.ui

import android.view.View
import androidx.activity.ComponentActivity

fun ComponentActivity.homeTopChrome(title: String, navigation: HomeNavigation): View {
    return infuseTopChrome(
        title = title,
        actions = listOf(
            infuseAction(InfuseAction("搜索", TvIcon.SEARCH, InfuseActionEmphasis.QUIET, navigation.onSearch)),
            infuseAction(InfuseAction("刷新", TvIcon.REFRESH, InfuseActionEmphasis.QUIET, navigation.onRefresh)),
            infuseAction(InfuseAction("账号", TvIcon.ACCOUNT, InfuseActionEmphasis.QUIET, navigation.onSwitchAccount)),
            infuseAction(InfuseAction("设置", TvIcon.SETTINGS, InfuseActionEmphasis.QUIET, navigation.onSettings)),
            infuseAction(InfuseAction("退出", TvIcon.LOGOUT, InfuseActionEmphasis.QUIET, navigation.onLogout)),
        ),
    )
}
