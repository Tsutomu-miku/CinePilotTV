package tv.cinepilot.tv.compose.screens

import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import tv.cinepilot.core.protocol.MediaStreamInfo
import tv.cinepilot.tv.compose.components.InfoPanel
import tv.cinepilot.tv.compose.components.SettingsGrid
import tv.cinepilot.tv.compose.components.TvActionButton
import tv.cinepilot.tv.compose.components.TvOptionRow
import tv.cinepilot.tv.compose.components.TvToggleRow
import tv.cinepilot.tv.compose.theme.CinePilotPalette
import tv.cinepilot.tv.compose.theme.TvDp
import tv.cinepilot.tv.playback.PlaybackSettings
import tv.cinepilot.tv.playback.PlaybackSettingsFocusGroup
import tv.cinepilot.tv.playback.SubtitleEncoding
import tv.cinepilot.tv.ui.streamLabel

@Composable
fun ComposePlaybackSettingsScreen(
    palette: CinePilotPalette,
    playerView: View,
    current: PlaybackSettings,
    focusGroup: PlaybackSettingsFocusGroup,
    audioStreams: List<MediaStreamInfo>?,
    currentAudioStreamIndex: Int?,
    subtitleStreams: List<MediaStreamInfo>?,
    currentSubtitleStreamIndex: Int?,
    onChanged: (PlaybackSettings, PlaybackSettingsFocusGroup) -> Unit,
    onAudioStreamChanged: (Int?) -> Unit,
    onSubtitleStreamChanged: (Int?) -> Unit,
    onBack: () -> Unit,
) {
    fun update(next: PlaybackSettingsFocusGroup, fn: (PlaybackSettings) -> PlaybackSettings) {
        onChanged(fn(current), next)
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(androidx.compose.ui.graphics.Color.Black),
    ) {
        AndroidView(
            factory = { context -> FrameLayout(context) },
            update = { host ->
                (playerView.parent as? ViewGroup)?.removeView(playerView)
                host.removeAllViews()
                host.addView(playerView, FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT,
                ))
            },
            modifier = Modifier.fillMaxSize(),
        )
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight()
                .width(760.dp)
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            palette.background.copy(alpha = 0.12f),
                            palette.background.copy(alpha = 0.92f),
                            palette.background.copy(alpha = 0.98f),
                        ),
                    ),
                )
                .padding(horizontal = 28.dp, vertical = 24.dp),
        ) {
            LazyColumn(
                contentPadding = PaddingValues(bottom = TvDp.ScreenBottom),
                modifier = Modifier.fillMaxSize(),
            ) {
                item {
                    InfoPanel(
                        palette = palette,
                        title = "播放设置",
                        body = "显示模式 / 音轨字幕 / 连播 / 跳过 / 章节",
                    )
                }
                item {
                    SettingsGrid(
                        palette = palette,
                        title = "显示模式",
                        rows = buildList<@Composable () -> Unit> {
                            add {
                                TvToggleRow(
                                    palette = palette,
                                    label = "自动匹配刷新率",
                                    description = "按片源帧率切换显示模式",
                                    checked = current.autoFrameMatching,
                                    requestInitialFocus = focusGroup == PlaybackSettingsFocusGroup.AFM,
                                    onCheckedChange = { checked ->
                                        update(PlaybackSettingsFocusGroup.AFM) {
                                            it.copy(autoFrameMatching = checked)
                                        }
                                    },
                                )
                            }
                            add {
                                TvToggleRow(
                                    palette = palette,
                                    label = "匹配色彩空间",
                                    description = "HDR / SDR 自动匹配",
                                    checked = current.matchColorSpace,
                                    onCheckedChange = { checked ->
                                        update(PlaybackSettingsFocusGroup.AFM) {
                                            it.copy(matchColorSpace = checked)
                                        }
                                    },
                                )
                            }
                            add {
                                TvToggleRow(
                                    palette = palette,
                                    label = "切换前确认",
                                    description = "切换显示模式前确认",
                                    checked = current.confirmBeforeFrameSwitch,
                                    onCheckedChange = { checked ->
                                        update(PlaybackSettingsFocusGroup.AFM) {
                                            it.copy(
                                                confirmBeforeFrameSwitch = checked,
                                                skipFrameSwitchConfirm = if (checked) false else it.skipFrameSwitchConfirm,
                                            )
                                        }
                                    },
                                )
                            }
                            if (current.skipFrameSwitchConfirm) {
                                add {
                                    TvOptionRow(
                                        palette = palette,
                                        label = "恢复确认提示",
                                        description = "重新显示切换确认",
                                        value = "恢复",
                                        onClick = {
                                            update(PlaybackSettingsFocusGroup.AFM) {
                                                it.copy(skipFrameSwitchConfirm = false)
                                            }
                                        },
                                    )
                                }
                            }
                        },
                    )
                }
                item {
                    StreamSettings(
                        palette = palette,
                        current = current,
                        focusGroup = focusGroup,
                        audioStreams = audioStreams,
                        currentAudioStreamIndex = currentAudioStreamIndex,
                        subtitleStreams = subtitleStreams,
                        currentSubtitleStreamIndex = currentSubtitleStreamIndex,
                        onEncoding = { encoding ->
                            update(PlaybackSettingsFocusGroup.SUBTITLE_ENCODING) {
                                it.copy(subtitleEncoding = encoding)
                            }
                        },
                        onBurnGraphicSubtitle = { checked ->
                            update(PlaybackSettingsFocusGroup.BURN_GRAPHIC_SUBTITLE) {
                                it.copy(burnGraphicSubtitleWhenTranscoding = checked)
                            }
                        },
                        onAudioStreamChanged = onAudioStreamChanged,
                        onSubtitleStreamChanged = onSubtitleStreamChanged,
                    )
                }
                item {
                    SettingsGrid(
                        palette = palette,
                        title = "连播与预览",
                        rows = listOf(
                            {
                                TvToggleRow(
                                    palette = palette,
                                    label = "自动下一集",
                                    description = "剧集末段显示下一集",
                                    checked = current.autoPlayNext,
                                    requestInitialFocus = focusGroup == PlaybackSettingsFocusGroup.AUTO_PLAY,
                                    onCheckedChange = { checked ->
                                        update(PlaybackSettingsFocusGroup.AUTO_PLAY) { it.copy(autoPlayNext = checked) }
                                    },
                                )
                            },
                            {
                                TvToggleRow(
                                    palette = palette,
                                    label = "章节列表",
                                    description = "底部显示章节 chips",
                                    checked = current.showChapterStrip,
                                    requestInitialFocus = focusGroup == PlaybackSettingsFocusGroup.CHAPTERS,
                                    onCheckedChange = { checked ->
                                        update(PlaybackSettingsFocusGroup.CHAPTERS) { it.copy(showChapterStrip = checked) }
                                    },
                                )
                            },
                            {
                                TvToggleRow(
                                    palette = palette,
                                    label = "缩略图预览",
                                    description = "保留设置，预览后续接入",
                                    checked = current.showTrickplayPreview,
                                    requestInitialFocus = focusGroup == PlaybackSettingsFocusGroup.TRICKPLAY,
                                    onCheckedChange = { checked ->
                                        update(PlaybackSettingsFocusGroup.TRICKPLAY) { it.copy(showTrickplayPreview = checked) }
                                    },
                                )
                            },
                        ),
                    )
                }
                item {
                    SettingsGrid(
                        palette = palette,
                        title = "片头 / 片尾",
                        rows = listOf(
                            {
                                TvToggleRow(
                                    palette = palette,
                                    label = "片头按钮",
                                    checked = current.showIntroSkipButton,
                                    requestInitialFocus = focusGroup == PlaybackSettingsFocusGroup.INTRO_SKIP,
                                    onCheckedChange = { checked ->
                                        update(PlaybackSettingsFocusGroup.INTRO_SKIP) { it.copy(showIntroSkipButton = checked) }
                                    },
                                )
                            },
                            {
                                TvToggleRow(
                                    palette = palette,
                                    label = "自动跳片头",
                                    checked = current.autoSkipIntro,
                                    onCheckedChange = { checked ->
                                        update(PlaybackSettingsFocusGroup.INTRO_SKIP) { it.copy(autoSkipIntro = checked) }
                                    },
                                )
                            },
                            {
                                TvToggleRow(
                                    palette = palette,
                                    label = "片尾按钮",
                                    checked = current.showCreditsSkipButton,
                                    onCheckedChange = { checked ->
                                        update(PlaybackSettingsFocusGroup.INTRO_SKIP) { it.copy(showCreditsSkipButton = checked) }
                                    },
                                )
                            },
                            {
                                TvToggleRow(
                                    palette = palette,
                                    label = "自动跳片尾",
                                    checked = current.autoSkipCredits,
                                    onCheckedChange = { checked ->
                                        update(PlaybackSettingsFocusGroup.INTRO_SKIP) { it.copy(autoSkipCredits = checked) }
                                    },
                                )
                            },
                        ),
                    )
                }
                item {
                    SettingsGrid(
                        palette = palette,
                        title = "播放器",
                        rows = listOf(
                            {
                                TvActionButton(
                                    palette = palette,
                                    label = "返回播放器",
                                    modifier = Modifier.fillMaxWidth(),
                                    onClick = onBack,
                                )
                            },
                        ),
                    )
                }
            }
        }
    }
}

@Composable
private fun StreamSettings(
    palette: CinePilotPalette,
    current: PlaybackSettings,
    focusGroup: PlaybackSettingsFocusGroup,
    audioStreams: List<MediaStreamInfo>?,
    currentAudioStreamIndex: Int?,
    subtitleStreams: List<MediaStreamInfo>?,
    currentSubtitleStreamIndex: Int?,
    onEncoding: (SubtitleEncoding) -> Unit,
    onBurnGraphicSubtitle: (Boolean) -> Unit,
    onAudioStreamChanged: (Int?) -> Unit,
    onSubtitleStreamChanged: (Int?) -> Unit,
) {
    SettingsGrid(
        palette = palette,
        title = "音轨与字幕",
        rows = buildList<@Composable () -> Unit> {
            audioStreams.orEmpty().forEach { stream ->
                add {
                    val selected = stream.index() == currentAudioStreamIndex
                    TvOptionRow(
                        palette = palette,
                        label = "音轨：${streamLabel(stream)}",
                        value = if (selected) "当前" else "切换",
                        selected = selected,
                        requestInitialFocus = focusGroup == PlaybackSettingsFocusGroup.AUDIO_TRACK && selected,
                        onClick = { onAudioStreamChanged(stream.index()) },
                    )
                }
            }
            add {
                val subtitlesOff = currentSubtitleStreamIndex == -1
                TvOptionRow(
                    palette = palette,
                    label = "字幕：关闭",
                    value = if (subtitlesOff) "当前" else "切换",
                    selected = subtitlesOff,
                    requestInitialFocus = focusGroup == PlaybackSettingsFocusGroup.SUBTITLE_TRACK && subtitlesOff,
                    onClick = { onSubtitleStreamChanged(-1) },
                )
            }
            subtitleStreams.orEmpty().forEach { stream ->
                add {
                    val selected = stream.index() == currentSubtitleStreamIndex
                    TvOptionRow(
                        palette = palette,
                        label = "字幕：${streamLabel(stream)}",
                        value = if (selected) "当前" else "切换",
                        selected = selected,
                        requestInitialFocus = focusGroup == PlaybackSettingsFocusGroup.SUBTITLE_TRACK && selected,
                        onClick = { onSubtitleStreamChanged(stream.index()) },
                    )
                }
            }
            SubtitleEncoding.values().forEach { encoding ->
                add {
                    val selected = encoding == current.subtitleEncoding
                    TvOptionRow(
                        palette = palette,
                        label = "字幕编码：${encoding.label}",
                        value = if (selected) "当前" else "切换",
                        selected = selected,
                        requestInitialFocus = focusGroup == PlaybackSettingsFocusGroup.SUBTITLE_ENCODING && selected,
                        onClick = { onEncoding(encoding) },
                    )
                }
            }
            add {
                TvToggleRow(
                    palette = palette,
                    label = "图形字幕烧录",
                    description = "转码时由服务器烧录",
                    checked = current.burnGraphicSubtitleWhenTranscoding,
                    requestInitialFocus = focusGroup == PlaybackSettingsFocusGroup.BURN_GRAPHIC_SUBTITLE,
                    onCheckedChange = onBurnGraphicSubtitle,
                )
            }
        },
    )
}
