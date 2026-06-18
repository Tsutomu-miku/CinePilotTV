package tv.cinepilot.tv.playback

import android.view.View
import androidx.activity.ComponentActivity
import tv.cinepilot.core.protocol.MediaStreamInfo
import tv.cinepilot.tv.ui.TvOptionSelectItem
import tv.cinepilot.tv.ui.settingsActionRow
import tv.cinepilot.tv.ui.settingsGridSection
import tv.cinepilot.tv.ui.settingsOptionRow
import tv.cinepilot.tv.ui.settingsPage
import tv.cinepilot.tv.ui.settingsToggleRow
import tv.cinepilot.tv.ui.streamLabel

enum class PlaybackSettingsFocusGroup {
    AFM,
    AUDIO_TRACK,
    SUBTITLE_TRACK,
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
    audioStreams: List<MediaStreamInfo>? = null,
    currentAudioStreamIndex: Int? = null,
    subtitleStreams: List<MediaStreamInfo>? = null,
    currentSubtitleStreamIndex: Int? = null,
    onChanged: (PlaybackSettings, PlaybackSettingsFocusGroup) -> Unit,
    onAudioStreamChanged: (Int?) -> Unit = {},
    onSubtitleStreamChanged: (Int?) -> Unit = {},
    onBack: () -> Unit,
): View {
    fun update(
        next: PlaybackSettingsFocusGroup,
        fn: (PlaybackSettings) -> PlaybackSettings,
    ): Unit = onChanged(fn(current), next)

    return settingsPage(
        title = "播放设置",
        subtitle = "显示模式 / 音轨字幕 / 连播 / 跳过 / 章节",
    ) {
        addView(settingsGridSection(
            title = "显示模式",
            rows = buildList {
                add(settingsToggleRow(
                    label = "自动匹配刷新率",
                    description = "按片源帧率切换显示模式",
                    checked = current.autoFrameMatching,
                    requestFocus = focusGroup == PlaybackSettingsFocusGroup.AFM,
                ) { checked ->
                    update(PlaybackSettingsFocusGroup.AFM) {
                        it.copy(autoFrameMatching = checked)
                    }
                })
                add(settingsToggleRow(
                    label = "匹配色彩空间",
                    description = "HDR / SDR 自动匹配",
                    checked = current.matchColorSpace,
                ) { checked ->
                    update(PlaybackSettingsFocusGroup.AFM) {
                        it.copy(matchColorSpace = checked)
                    }
                })
                add(settingsToggleRow(
                    label = "切换前确认",
                    description = "切换显示模式前确认",
                    checked = current.confirmBeforeFrameSwitch,
                ) { checked ->
                    update(PlaybackSettingsFocusGroup.AFM) {
                        it.copy(
                            confirmBeforeFrameSwitch = checked,
                            skipFrameSwitchConfirm = if (checked) false else it.skipFrameSwitchConfirm,
                        )
                    }
                })
                if (current.skipFrameSwitchConfirm) {
                    add(settingsActionRow(
                        label = "恢复确认提示",
                        description = "重新显示切换确认",
                        actionLabel = "恢复",
                    ) {
                        update(PlaybackSettingsFocusGroup.AFM) {
                            it.copy(skipFrameSwitchConfirm = false)
                        }
                    })
                }
            },
        ))

        val streamRows = buildList {
            if (!audioStreams.isNullOrEmpty()) {
                val currentAudio = audioStreams.firstOrNull { it.index() == currentAudioStreamIndex }
                add(settingsOptionRow(
                    label = "音轨",
                    description = "当前播放音频流",
                    selectedLabel = currentAudio?.let { streamLabel(it) } ?: "默认",
                    options = audioStreams.map { stream ->
                        TvOptionSelectItem(
                            label = streamLabel(stream),
                            selected = stream.index() == currentAudioStreamIndex,
                        ) {
                            onAudioStreamChanged(stream.index())
                        }
                    },
                    requestFocus = focusGroup == PlaybackSettingsFocusGroup.AUDIO_TRACK,
                ))
            }
            if (!subtitleStreams.isNullOrEmpty()) {
                val currentSubtitle = subtitleStreams.firstOrNull { it.index() == currentSubtitleStreamIndex }
                val subtitlesOff = currentSubtitleStreamIndex == -1
                add(settingsOptionRow(
                    label = "字幕",
                    description = "当前字幕轨道",
                    selectedLabel = when {
                        subtitlesOff -> "关闭"
                        currentSubtitle != null -> streamLabel(currentSubtitle)
                        else -> "默认"
                    },
                    options = listOf(TvOptionSelectItem("关闭", subtitlesOff) {
                        onSubtitleStreamChanged(-1)
                    }) + subtitleStreams.map { stream ->
                        TvOptionSelectItem(
                            label = streamLabel(stream),
                            selected = stream.index() == currentSubtitleStreamIndex && !subtitlesOff,
                        ) {
                            onSubtitleStreamChanged(stream.index())
                        }
                    },
                    requestFocus = focusGroup == PlaybackSettingsFocusGroup.SUBTITLE_TRACK,
                ))
            }
            add(settingsOptionRow(
                label = "字幕编码",
                description = "外挂字幕文本解码",
                selectedLabel = current.subtitleEncoding.label,
                options = SubtitleEncoding.values().map { encoding ->
                    TvOptionSelectItem(
                        label = encoding.label,
                        selected = encoding == current.subtitleEncoding,
                    ) {
                        update(PlaybackSettingsFocusGroup.SUBTITLE_ENCODING) {
                            it.copy(subtitleEncoding = encoding)
                        }
                    }
                },
                requestFocus = focusGroup == PlaybackSettingsFocusGroup.SUBTITLE_ENCODING,
            ))
            add(settingsToggleRow(
                label = "图形字幕烧录",
                description = "转码时由服务器烧录",
                checked = current.burnGraphicSubtitleWhenTranscoding,
                requestFocus = focusGroup == PlaybackSettingsFocusGroup.BURN_GRAPHIC_SUBTITLE,
            ) { checked ->
                update(PlaybackSettingsFocusGroup.BURN_GRAPHIC_SUBTITLE) {
                    it.copy(burnGraphicSubtitleWhenTranscoding = checked)
                }
            })
        }
        addView(settingsGridSection("音轨与字幕", streamRows))

        addView(settingsGridSection(
            title = "连播与预览",
            rows = listOf(
                settingsToggleRow(
                    label = "自动下一集",
                    description = "剧集末段显示下一集",
                    checked = current.autoPlayNext,
                    requestFocus = focusGroup == PlaybackSettingsFocusGroup.AUTO_PLAY,
                ) { checked ->
                    update(PlaybackSettingsFocusGroup.AUTO_PLAY) {
                        it.copy(autoPlayNext = checked)
                    }
                },
                settingsToggleRow(
                    label = "章节列表",
                    description = "底部显示章节 chips",
                    checked = current.showChapterStrip,
                    requestFocus = focusGroup == PlaybackSettingsFocusGroup.CHAPTERS,
                ) { checked ->
                    update(PlaybackSettingsFocusGroup.CHAPTERS) {
                        it.copy(showChapterStrip = checked)
                    }
                },
                settingsToggleRow(
                    label = "缩略图预览",
                    description = "进度条显示 trickplay",
                    checked = current.showTrickplayPreview,
                    requestFocus = focusGroup == PlaybackSettingsFocusGroup.TRICKPLAY,
                ) { checked ->
                    update(PlaybackSettingsFocusGroup.TRICKPLAY) {
                        it.copy(showTrickplayPreview = checked)
                    }
                },
            ),
        ))

        addView(settingsGridSection(
            title = "片头 / 片尾",
            rows = listOf(
                settingsToggleRow(
                    label = "片头按钮",
                    description = "片头区间显示跳过按钮",
                    checked = current.showIntroSkipButton,
                    requestFocus = focusGroup == PlaybackSettingsFocusGroup.INTRO_SKIP,
                ) { checked ->
                    update(PlaybackSettingsFocusGroup.INTRO_SKIP) {
                        it.copy(showIntroSkipButton = checked)
                    }
                },
                settingsToggleRow(
                    label = "自动跳片头",
                    description = "进入片头直接跳过",
                    checked = current.autoSkipIntro,
                ) { checked ->
                    update(PlaybackSettingsFocusGroup.INTRO_SKIP) {
                        it.copy(autoSkipIntro = checked)
                    }
                },
                settingsToggleRow(
                    label = "片尾按钮",
                    description = "片尾区间显示跳过按钮",
                    checked = current.showCreditsSkipButton,
                ) { checked ->
                    update(PlaybackSettingsFocusGroup.INTRO_SKIP) {
                        it.copy(showCreditsSkipButton = checked)
                    }
                },
                settingsToggleRow(
                    label = "自动跳片尾",
                    description = "进入片尾直接跳过",
                    checked = current.autoSkipCredits,
                ) { checked ->
                    update(PlaybackSettingsFocusGroup.INTRO_SKIP) {
                        it.copy(autoSkipCredits = checked)
                    }
                },
            ),
        ))

        addView(settingsGridSection(
            title = "播放器",
            rows = listOf(
                settingsActionRow(
                    label = "返回播放器",
                    description = "关闭设置并恢复播放界面",
                    actionLabel = "返回",
                    onClick = onBack,
                ),
            ),
        ))
    }
}
