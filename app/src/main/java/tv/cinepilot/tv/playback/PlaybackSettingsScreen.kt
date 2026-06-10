package tv.cinepilot.tv.playback

import android.widget.ScrollView
import androidx.activity.ComponentActivity
import tv.cinepilot.tv.ui.TvIcon
import tv.cinepilot.tv.ui.iconAction
import tv.cinepilot.tv.ui.radioChoice
import tv.cinepilot.tv.ui.requestInitialFocus
import tv.cinepilot.tv.ui.screen
import tv.cinepilot.tv.ui.section
import tv.cinepilot.tv.ui.settingChoiceRow
import tv.cinepilot.tv.ui.toggleChoice

enum class PlaybackSettingsFocusGroup {
    AFM,
    AUTO_PLAY,
    INTRO_SKIP,
    TRICKPLAY,
    CHAPTERS,
    SUBTITLE_ENCODING,
    BURN_GRAPHIC_SUBTITLE,
}

fun ComponentActivity.playbackSettingsScreen(
    current: PlaybackSettings,
    focusGroup: PlaybackSettingsFocusGroup = PlaybackSettingsFocusGroup.AFM,
    onChanged: (PlaybackSettings) -> Unit,
    onBack: () -> Unit,
): ScrollView {
    fun update(
        next: PlaybackSettingsFocusGroup,
        fn: (PlaybackSettings) -> PlaybackSettings,
    ): Unit = onChanged(fn(current))

    return screen("播放设置") {
        addView(section("帧率 / 色彩匹配 (AFM)"))
        addView(toggleRow(
            label = "自动匹配刷新率",
            description = "根据视频帧率切换电视 Display.Mode，播放前给出确认通知",
            checked = current.autoFrameMatching,
            focus = focusGroup == PlaybackSettingsFocusGroup.AFM,
        ) { checked ->
            update(PlaybackSettingsFocusGroup.AFM) {
                it.copy(autoFrameMatching = checked)
            }
        })
        addView(toggleRow(
            label = "匹配色彩空间",
            description = "随 HDR / SDR 自动切换颜色范围（需电视支持）",
            checked = current.matchColorSpace,
            focus = false,
        ) { checked ->
            update(PlaybackSettingsFocusGroup.AUTO_PLAY) {
                it.copy(matchColorSpace = checked)
            }
        })
        addView(section("自动连播"))
        addView(toggleRow(
            label = "末 30s 弹出下一集",
            description = "剧集末段显示倒计时卡片，无操作自动播放下一集",
            checked = current.autoPlayNext,
            focus = focusGroup == PlaybackSettingsFocusGroup.AUTO_PLAY,
        ) { checked ->
            update(PlaybackSettingsFocusGroup.AUTO_PLAY) {
                it.copy(autoPlayNext = checked)
            }
        })
        addView(section("片头 / 片尾跳过"))
        addView(toggleRow(
            label = "显示「跳过片头」按钮",
            description = "进入片头区间时在屏幕左下角显示按钮",
            checked = current.showIntroSkipButton,
            focus = focusGroup == PlaybackSettingsFocusGroup.INTRO_SKIP,
        ) { checked ->
            update(PlaybackSettingsFocusGroup.INTRO_SKIP) {
                it.copy(showIntroSkipButton = checked)
            }
        })
        addView(toggleRow(
            label = "进入片头自动跳过",
            description = "不显示按钮，自动跳到片头结束位置",
            checked = current.autoSkipIntro,
            focus = false,
        ) { checked ->
            update(PlaybackSettingsFocusGroup.INTRO_SKIP) {
                it.copy(autoSkipIntro = checked)
            }
        })
        addView(toggleRow(
            label = "显示「跳过片尾」按钮",
            description = "进入片尾区间时在屏幕右下角显示按钮",
            checked = current.showCreditsSkipButton,
            focus = false,
        ) { checked ->
            update(PlaybackSettingsFocusGroup.INTRO_SKIP) {
                it.copy(showCreditsSkipButton = checked)
            }
        })
        addView(toggleRow(
            label = "进入片尾自动跳过",
            description = "不显示按钮，自动跳到片尾结束位置（剧末则触发 Next Up）",
            checked = current.autoSkipCredits,
            focus = false,
        ) { checked ->
            update(PlaybackSettingsFocusGroup.INTRO_SKIP) {
                it.copy(autoSkipCredits = checked)
            }
        })
        addView(section("预览 / 章节"))
        addView(toggleRow(
            label = "拖动进度条显示章节缩略图 (Trickplay)",
            description = "需要服务器生成 320p tiles",
            checked = current.showTrickplayPreview,
            focus = focusGroup == PlaybackSettingsFocusGroup.TRICKPLAY,
        ) { checked ->
            update(PlaybackSettingsFocusGroup.TRICKPLAY) {
                it.copy(showTrickplayPreview = checked)
            }
        })
        addView(toggleRow(
            label = "显示章节列表",
            description = "播放时 OSD 底部展示章节标题 chips，点击跳转",
            checked = current.showChapterStrip,
            focus = focusGroup == PlaybackSettingsFocusGroup.CHAPTERS,
        ) { checked ->
            update(PlaybackSettingsFocusGroup.CHAPTERS) {
                it.copy(showChapterStrip = checked)
            }
        })
        addView(section("字幕编码"))
        addView(settingChoiceRow(SubtitleEncoding.values().map { enc ->
            val option = radioChoice(enc.label, enc == current.subtitleEncoding) {
                update(PlaybackSettingsFocusGroup.SUBTITLE_ENCODING) {
                    it.copy(subtitleEncoding = enc)
                }
            }
            if (focusGroup == PlaybackSettingsFocusGroup.SUBTITLE_ENCODING &&
                enc == current.subtitleEncoding
            ) {
                option.requestInitialFocus()
            }
            option
        }))
        addView(section("图形字幕 (PGS / VobSub)"))
        addView(toggleRow(
            label = "转码时由服务器烧录图形字幕",
            description = "关闭后若本地无法渲染图形字幕，屏幕不会有字幕显示",
            checked = current.burnGraphicSubtitleWhenTranscoding,
            focus = focusGroup == PlaybackSettingsFocusGroup.BURN_GRAPHIC_SUBTITLE,
        ) { checked ->
            update(PlaybackSettingsFocusGroup.BURN_GRAPHIC_SUBTITLE) {
                it.copy(burnGraphicSubtitleWhenTranscoding = checked)
            }
        })
        addView(iconAction("返回播放器", TvIcon.BACK, onBack))
    }
}

private fun ComponentActivity.toggleRow(
    label: String,
    description: String,
    checked: Boolean,
    focus: Boolean = false,
    onChecked: (Boolean) -> Unit,
) = toggleChoice(label, description, checked, focus, onChecked)
