package tv.cinepilot.tv.auth

import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.graphics.Typeface
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
import tv.cinepilot.tv.ui.dp
import tv.cinepilot.tv.ui.iconAction
import tv.cinepilot.tv.ui.input
import tv.cinepilot.tv.ui.label
import tv.cinepilot.tv.ui.primaryIconAction
import tv.cinepilot.tv.ui.requestInitialFocus
import tv.cinepilot.tv.ui.rounded
import tv.cinepilot.tv.ui.screen
import tv.cinepilot.tv.ui.section
import tv.cinepilot.tv.ui.TvColors
import tv.cinepilot.tv.ui.TvIcon
import tv.cinepilot.tv.ui.TvRadius
import tv.cinepilot.tv.ui.TvSpacing
import tv.cinepilot.tv.ui.TvType
import java.util.WeakHashMap

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
            addView(initialFocusIfNeeded(iconAction("继续 ${account.displayName()}", TvIcon.ACCOUNT) {
                onContinueAccount(account)
            }, initialFocusSet).also { initialFocusSet = true })
        }
        recentServers.forEach { server ->
            addView(initialFocusIfNeeded(iconAction("服务器 ${server.displayName()}", TvIcon.FORWARD) {
                onOpenServer(server.serverAddress)
            }, initialFocusSet).also { initialFocusSet = true })
        }
        if (recentAccounts.isNotEmpty()) {
            addView(iconAction("清除已保存登录", TvIcon.LOGOUT, onClearAccounts))
        }
        addView(label("服务器地址"))
        addView(initialFocusIfNeeded(serverInput, initialFocusSet))
        addView(primaryIconAction("连接服务器", TvIcon.FORWARD) {
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
        addView(primaryIconAction("登录", TvIcon.ACCOUNT) {
            onLogin(usernameInput.text.toString(), passwordInput.text.toString())
        })
        if (quickConnectAvailable) {
            addView(iconAction("Quick Connect", TvIcon.FORWARD, onQuickConnect))
        }
        addView(iconAction("返回服务器输入", TvIcon.BACK, onBackToServer))
    }
}

fun ComponentActivity.quickConnectScreen(
    quickConnect: QuickConnectSession,
    onCheckNow: () -> Unit,
    onBackToLogin: () -> Unit,
): QuickConnectViews {
    var statusView: TextView? = null
    val root = screen("Quick Connect") {
        addView(label("授权码"))
        addView(quickConnectCode(quickConnect.code()))
        addView(label("请在 Jellyfin 中输入授权码，授权后会自动登录"))
        statusView = label("正在等待授权，电视会每 2 秒自动检查一次").also(::addView)
        addView(primaryIconAction("立即检查授权", TvIcon.REFRESH, onCheckNow).requestInitialFocus())
        addView(iconAction("返回登录", TvIcon.BACK, onBackToLogin))
    }
    return QuickConnectViews(root, requireNotNull(statusView))
}

data class QuickConnectViews(
    val root: View,
    val status: TextView,
)

private fun ComponentActivity.quickConnectCode(code: String): TextView {
    return TextView(this).apply {
        text = code
        textSize = QUICK_CONNECT_CODE_TEXT_SIZE
        typeface = Typeface.MONOSPACE
        gravity = Gravity.CENTER
        includeFontPadding = false
        setTextColor(TvColors.AccentStrong)
        background = rounded(TvColors.SurfaceRaised, dp(TvRadius.Control), dp(2), TvColors.FocusRing)
        setPadding(dp(22), dp(16), dp(22), dp(16))
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
        ).apply {
            bottomMargin = dp(TvSpacing.ControlGap)
        }
    }
}

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
        animatePublicUserFocus(focusedView as LinearLayout, hasFocus)
        setUserCardTextColor(card, hasFocus)
    }
    loadPublicUserImage(avatar, user, 96, 96)
    return card
}

private fun ComponentActivity.animatePublicUserFocus(card: LinearLayout, hasFocus: Boolean) {
    publicUserFocusAnimators[card]?.cancel()
    publicUserFocusAnimators[card] = ValueAnimator.ofFloat(if (hasFocus) 0f else 1f, if (hasFocus) 1f else 0f).apply {
        duration = PUBLIC_USER_FOCUS_ANIMATION_MS
        addUpdateListener { animator ->
            val progress = animator.animatedValue as Float
            val color = publicUserFocusColorEvaluator.evaluate(progress, TvColors.SurfaceControl, TvColors.Focus) as Int
            val strokeWidth = (dp(1) + (dp(3) - dp(1)) * progress).toInt()
            card.background = rounded(color, dp(TvRadius.Control), strokeWidth, TvColors.FocusRing)
            card.translationZ = dp(4).toFloat() * progress
            card.alpha = 0.96f + 0.04f * progress
        }
        start()
    }
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

private val publicUserFocusAnimators = WeakHashMap<LinearLayout, ValueAnimator>()
private val publicUserFocusColorEvaluator = ArgbEvaluator()
private const val PUBLIC_USER_FOCUS_ANIMATION_MS = 160L
private const val QUICK_CONNECT_CODE_TEXT_SIZE = 36f
