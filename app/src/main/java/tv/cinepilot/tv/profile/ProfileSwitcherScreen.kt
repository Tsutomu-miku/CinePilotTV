package tv.cinepilot.tv.profile

import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.ComponentActivity
import tv.cinepilot.core.protocol.ProfileSummary
import tv.cinepilot.tv.ui.InfuseAction
import tv.cinepilot.tv.ui.InfuseActionEmphasis
import tv.cinepilot.tv.ui.TvIcon
import tv.cinepilot.tv.ui.TvOptionSelectItem
import tv.cinepilot.tv.ui.cinematicStage
import tv.cinepilot.tv.ui.compactPanelSpacing
import tv.cinepilot.tv.ui.dp
import tv.cinepilot.tv.ui.homeHairlineColor
import tv.cinepilot.tv.ui.infuseActions
import tv.cinepilot.tv.ui.infusePanelNote
import tv.cinepilot.tv.ui.infusePanelTitle
import tv.cinepilot.tv.ui.optionSelect
import tv.cinepilot.tv.ui.rounded
import tv.cinepilot.tv.ui.sideSheet
import tv.cinepilot.tv.ui.TvColors

fun ComponentActivity.profileSwitcherScreen(
    profiles: List<ProfileSummary>,
    loadImage: (ImageView, ProfileSummary, Int, Int) -> Unit,
    onSwitch: (String) -> Unit,
    onRemove: (String) -> Unit,
    onClose: () -> Unit,
): View {
    return cinematicStage(scrollable = false) {
        addView(sideSheet {
            addView(infusePanelTitle("切换用户"))
            addView(infusePanelNote("选择用户进入对应的「继续观看 / 收藏夹 / 已看状态」。用户之间 token 和浏览进度完全隔离。"))
            if (profiles.isEmpty()) {
                addView(infusePanelNote("当前服务器下没有已保存的登录账号，请返回登录。").compactPanelSpacing())
                addView(infuseActions(listOf(
                    InfuseAction("返回首页", TvIcon.BACK, InfuseActionEmphasis.SECONDARY, onClose),
                )))
            } else {
                addView(profileList(
                    profiles = profiles,
                    loadImage = loadImage,
                    onSwitch = onSwitch,
                    onRemove = onRemove,
                ).compactPanelSpacing())
                addView(infuseActions(listOf(
                    InfuseAction("关闭", TvIcon.BACK, InfuseActionEmphasis.QUIET, onClose),
                )))
            }
        })
    }
}

private fun ComponentActivity.profileList(
    profiles: List<ProfileSummary>,
    loadImage: (ImageView, ProfileSummary, Int, Int) -> Unit,
    onSwitch: (String) -> Unit,
    onRemove: (String) -> Unit,
): View {
    val container = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
    }
    for ((index, profile) in profiles.withIndex()) {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            isFocusable = true
            isClickable = true
            val pad = dp(12)
            setPadding(pad, pad, pad, pad)
            background = rounded(TvColors.SurfaceRaised, dp(12), dp(1), homeHairlineColor())
            clipChildren = false
            clipToPadding = false
        }
        val avatar = ImageView(this).apply {
            scaleType = ImageView.ScaleType.CENTER_CROP
            clipToOutline = true
            background = rounded(TvColors.PosterFallback, dp(32), 0)
            layoutParams = LinearLayout.LayoutParams(dp(64), dp(64)).apply {
                rightMargin = dp(16)
            }
        }
        loadImage(avatar, profile, dp(64), dp(64))
        val column = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        val name = TextView(this).apply {
            val label = buildString {
                append(profile.userName().ifBlank { profile.userId() })
                if (profile.isActive()) append(" · 当前")
            }
            text = label
            textSize = 16f
            setTextColor(TvColors.TextPrimary)
            maxLines = 1
            includeFontPadding = false
        }
        val note = TextView(this).apply {
            text = if (profile.isActive()) "当前登录账号" else "点击切换到此账号"
            textSize = 12f
            setTextColor(TvColors.TextSecondary)
            maxLines = 1
            includeFontPadding = false
        }
        column.addView(name)
        column.addView(note)
        row.addView(avatar)
        row.addView(column)
        val removeBtn = android.widget.Button(this, null, 0, android.R.style.Widget_Material_Button_Borderless_Colored).apply {
            text = "移除"
            setTextColor(TvColors.Accent)
            textSize = 13f
            isFocusable = true
            isClickable = true
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                leftMargin = dp(12)
            }
            setOnClickListener { onRemove(profile.userId()) }
        }
        row.addView(removeBtn)
        row.setOnClickListener { onSwitch(profile.userId()) }
        container.addView(row, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            if (index < profiles.size - 1) bottomMargin = dp(8)
        })
    }
    return container
}
