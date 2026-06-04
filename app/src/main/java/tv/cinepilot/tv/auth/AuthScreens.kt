package tv.cinepilot.tv.auth

import android.text.InputType
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.ComponentActivity
import tv.cinepilot.core.protocol.PublicUserSummary
import tv.cinepilot.core.protocol.QuickConnectSession
import tv.cinepilot.tv.runtime.RecentAccount
import tv.cinepilot.tv.runtime.RecentServer
import tv.cinepilot.tv.ui.action
import tv.cinepilot.tv.ui.dp
import tv.cinepilot.tv.ui.input
import tv.cinepilot.tv.ui.label
import tv.cinepilot.tv.ui.requestInitialFocus
import tv.cinepilot.tv.ui.rounded
import tv.cinepilot.tv.ui.screen
import tv.cinepilot.tv.ui.section
import tv.cinepilot.tv.ui.TvColors
import tv.cinepilot.tv.ui.TvRadius
import tv.cinepilot.tv.ui.TvSpacing
import tv.cinepilot.tv.ui.TvType

fun ComponentActivity.serverEntryScreen(
    recentAccounts: List<RecentAccount>,
    recentServers: List<RecentServer>,
    onContinueAccount: (RecentAccount) -> Unit,
    onOpenServer: (String) -> Unit,
    onClearAccounts: () -> Unit,
): ScrollView {
    val serverInput = input("http://192.168.1.10:8096", InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI)
    return screen("CinePilot TV") {
        var initialFocusSet = false
        recentAccounts.forEach { account ->
            addView(initialFocusIfNeeded(action("继续 ${account.displayName()}") {
                onContinueAccount(account)
            }, initialFocusSet).also { initialFocusSet = true })
        }
        recentServers.forEach { server ->
            addView(initialFocusIfNeeded(action("服务器 ${server.displayName()}") {
                onOpenServer(server.serverAddress)
            }, initialFocusSet).also { initialFocusSet = true })
        }
        if (recentAccounts.isNotEmpty()) {
            addView(action("清除已保存登录", onClearAccounts))
        }
        addView(label("服务器地址"))
        addView(initialFocusIfNeeded(serverInput, initialFocusSet))
        addView(action("连接服务器") {
            onOpenServer(serverInput.text.toString())
        })
    }
}

fun ComponentActivity.loginScreen(
    serverName: String,
    publicUsers: List<PublicUserSummary>,
    quickConnectAvailable: Boolean,
    loadPublicUserImage: (ImageView, PublicUserSummary, Int, Int) -> Unit,
    onLogin: (String, String) -> Unit,
    onQuickConnect: () -> Unit,
    onBackToServer: () -> Unit,
): ScrollView {
    val usernameInput = input("用户名", InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_NORMAL)
    val passwordInput = input("密码", InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD)
    return screen("登录 $serverName") {
        var initialFocusSet = false
        if (publicUsers.isNotEmpty()) {
            addView(section("选择用户"))
            publicUsers.forEach { user ->
                addView(initialFocusIfNeeded(
                    publicUserAction(user, loadPublicUserImage, usernameInput::setText, passwordInput::requestFocus, onLogin),
                    initialFocusSet,
                ).also { initialFocusSet = true })
            }
        }
        addView(label("用户名"))
        addView(initialFocusIfNeeded(usernameInput, initialFocusSet))
        addView(label("密码"))
        addView(passwordInput)
        addView(action("登录") {
            onLogin(usernameInput.text.toString(), passwordInput.text.toString())
        })
        if (quickConnectAvailable) {
            addView(action("Quick Connect", onQuickConnect))
        }
        addView(action("返回服务器输入", onBackToServer))
    }
}

fun ComponentActivity.quickConnectScreen(
    quickConnect: QuickConnectSession,
    onCheckNow: () -> Unit,
    onBackToLogin: () -> Unit,
): QuickConnectViews {
    var statusView: TextView? = null
    val root = screen("Quick Connect") {
        addView(label("授权码：${quickConnect.code()}"))
        addView(label("请在 Jellyfin 中输入授权码，授权后会自动登录"))
        statusView = label("正在等待授权，电视会每 2 秒自动检查一次").also(::addView)
        addView(action("立即检查授权", onCheckNow).requestInitialFocus())
        addView(action("返回登录", onBackToLogin))
    }
    return QuickConnectViews(root, requireNotNull(statusView))
}

data class QuickConnectViews(
    val root: View,
    val status: TextView,
)

private fun ComponentActivity.publicUserAction(
    user: PublicUserSummary,
    loadPublicUserImage: (ImageView, PublicUserSummary, Int, Int) -> Unit,
    setUsername: (String) -> Unit,
    focusPassword: () -> Unit,
    onLogin: (String, String) -> Unit,
): View {
    val userName = user.name().ifBlank { user.id() }
    val status = if (user.passwordRequired()) "需要密码" else "免密码登录"
    val card = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        isFocusable = true
        isClickable = true
        background = rounded(TvColors.SurfaceControl, dp(TvRadius.Control), dp(1), TvColors.FocusRing)
        setPadding(dp(10), dp(8), dp(14), dp(8))
        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(64)).apply {
            bottomMargin = dp(TvSpacing.ControlGap)
        }
    }
    val avatar = ImageView(this).apply {
        scaleType = ImageView.ScaleType.CENTER_CROP
        setBackgroundColor(TvColors.PosterFallback)
        contentDescription = "$userName 头像"
    }
    card.addView(avatar, LinearLayout.LayoutParams(dp(44), dp(44)).apply {
        rightMargin = dp(12)
    })
    card.addView(LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        addView(userText(userName, true))
        addView(userText(status, false))
    }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
    card.setOnClickListener {
        if (!user.passwordRequired()) {
            setUsername(userName)
            onLogin(userName, "")
        } else {
            setUsername(userName)
            focusPassword()
        }
    }
    card.setOnFocusChangeListener { focusedView, hasFocus ->
        focusedView.background = rounded(
            if (hasFocus) TvColors.Focus else TvColors.SurfaceControl,
            dp(TvRadius.Control),
            if (hasFocus) dp(3) else dp(1),
            TvColors.FocusRing,
        )
        setUserCardTextColor(card, hasFocus)
    }
    loadPublicUserImage(avatar, user, 96, 96)
    return card
}

private fun ComponentActivity.userText(textValue: String, primary: Boolean): TextView {
    return TextView(this).apply {
        text = textValue
        textSize = if (primary) TvType.Body else TvType.Metadata
        setTextColor(if (primary) TvColors.TextPrimary else TvColors.TextMuted)
        maxLines = 1
        ellipsize = TextUtils.TruncateAt.END
        includeFontPadding = false
    }
}

private fun setUserCardTextColor(card: LinearLayout, focused: Boolean) {
    val column = card.getChildAt(1) as? LinearLayout ?: return
    for (index in 0 until column.childCount) {
        val textView = column.getChildAt(index) as? TextView ?: continue
        textView.setTextColor(if (focused) TvColors.FocusText else if (index == 0) TvColors.TextPrimary else TvColors.TextMuted)
    }
}

private fun <T : View> initialFocusIfNeeded(view: T, alreadySet: Boolean): T {
    return if (alreadySet) view else view.requestInitialFocus()
}
