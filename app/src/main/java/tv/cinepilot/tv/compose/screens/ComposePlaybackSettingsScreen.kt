package tv.cinepilot.tv.compose.screens

import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import tv.cinepilot.core.protocol.MediaStreamInfo
import tv.cinepilot.tv.R
import tv.cinepilot.tv.compose.theme.CinePilotPalette
import tv.cinepilot.tv.compose.theme.TvDp
import tv.cinepilot.tv.compose.theme.TvText
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
            update = { host -> host.attachPlayerView(playerView) },
            modifier = Modifier.fillMaxSize(),
        )
        // Bottom gradient mask
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(180.dp)
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.5f),
                            Color.Black.copy(alpha = 0.8f),
                        ),
                    ),
                ),
        )
        // Right settings panel container
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .fillMaxHeight()
                .width(TvDp.PlayerSettingsPanelWidth + 24.dp)
                .padding(top = TvDp.ScreenTop, bottom = TvDp.ScreenBottom, end = TvDp.ScreenX),
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
            ) {
                // Settings panel card
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(TvDp.PanelRadius))
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    palette.background.copy(alpha = 0.15f),
                                    palette.background.copy(alpha = 0.88f),
                                    palette.background.copy(alpha = 0.95f),
                                ),
                            ),
                        )
                        .border(0.8.dp, palette.glassBorder, RoundedCornerShape(TvDp.PanelRadius)),
                ) {
                    LazyColumn(
                        contentPadding = PaddingValues(
                            horizontal = 18.dp,
                            vertical = 16.dp,
                        ),
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        item {
                            BasicText(
                                text = "播放设置",
                                maxLines = 1,
                                style = TextStyle(
                                    color = palette.textPrimary,
                                    fontSize = TvText.PageTitle,
                                    fontWeight = FontWeight.SemiBold,
                                ),
                            )
                            Spacer(Modifier.height(2.dp))
                            BasicText(
                                text = "显示模式 / 音轨字幕 / 连播 / 跳过 / 章节",
                                maxLines = 1,
                                style = TextStyle(
                                    color = palette.textMuted,
                                    fontSize = TvText.Metadata,
                                ),
                            )
                            Spacer(Modifier.height(14.dp))
                        }
                        item {
                            DisplayModeSection(
                                palette = palette,
                                current = current,
                                focusGroup = focusGroup,
                                onUpdate = { next, fn -> update(next, fn) },
                            )
                            Spacer(Modifier.height(12.dp))
                        }
                        item {
                            StreamSettingsSection(
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
                            Spacer(Modifier.height(12.dp))
                        }
                        item {
                            AutoPlaySection(
                                palette = palette,
                                current = current,
                                focusGroup = focusGroup,
                                onUpdate = { next, fn -> update(next, fn) },
                            )
                            Spacer(Modifier.height(12.dp))
                        }
                        item {
                            IntroCreditsSection(
                                palette = palette,
                                current = current,
                                focusGroup = focusGroup,
                                onUpdate = { next, fn -> update(next, fn) },
                            )
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                // Bottom action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    SettingsBottomButton(
                        palette = palette,
                        label = "视频信息",
                        iconRes = R.drawable.ic_info,
                        selected = false,
                        modifier = Modifier.weight(1f),
                        onClick = { /* TODO: switch to info panel */ },
                    )
                    SettingsBottomButton(
                        palette = palette,
                        label = "播放设置",
                        iconRes = R.drawable.ic_settings,
                        selected = true,
                        modifier = Modifier.weight(1f),
                        onClick = { /* already on settings */ },
                    )
                }
            }
        }
        // Chapter strip at bottom (placeholder UI)
        ChapterStripBar(
            palette = palette,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = TvDp.ScreenX, bottom = 16.dp),
        )
        // Next up countdown card (placeholder UI)
        NextUpCountdownCard(
            palette = palette,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(
                    end = TvDp.PlayerSettingsPanelWidth + TvDp.ScreenX + 32.dp,
                    bottom = 70.dp,
                ),
        )
    }
}

// ==================== Setting Row Components ====================

@Composable
private fun IconSettingRow(
    palette: CinePilotPalette,
    iconRes: Int,
    label: String,
    value: String,
    description: String = "",
    selected: Boolean = false,
    requestInitialFocus: Boolean = false,
    onClick: () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(TvDp.ControlRadius)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(TvDp.SettingsRowHeight)
            .clip(shape)
            .background(
                when {
                    focused -> palette.glassFocus
                    selected -> palette.accent.copy(alpha = 0.25f)
                    else -> palette.glass
                },
                shape,
            )
            .border(
                width = if (focused) TvDp.FocusRing else 0.5.dp,
                color = when {
                    focused -> palette.focusRing
                    selected -> palette.accentStrong
                    else -> palette.glassBorder
                },
                shape,
            )
            .shadow(
                elevation = if (focused) 10.dp else 0.dp,
                shape = shape,
                spotColor = if (focused) palette.focusGlow else Color.Transparent,
                ambientColor = if (focused) palette.focusGlow else Color.Transparent,
            )
            .onFocusChanged { focused = it.isFocused }
            .focusable()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(
                painter = painterResource(iconRes),
                contentDescription = null,
                colorFilter = ColorFilter.tint(
                    if (focused || selected) palette.accentStrong else palette.textSecondary
                ),
                modifier = Modifier.size(16.dp),
            )
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                BasicText(
                    text = label,
                    maxLines = 1,
                    style = TextStyle(
                        color = if (focused) palette.textPrimary else palette.textSecondary,
                        fontSize = TvText.Body,
                    ),
                )
                if (description.isNotBlank()) {
                    Spacer(Modifier.height(3.dp))
                    BasicText(
                        text = description,
                        maxLines = 1,
                        style = TextStyle(
                            color = palette.textMuted,
                            fontSize = TvText.Label,
                        ),
                    )
                }
            }
            Spacer(Modifier.width(8.dp))
            BasicText(
                text = value,
                maxLines = 1,
                style = TextStyle(
                    color = if (focused || selected) palette.accentStrong else palette.textMuted,
                    fontSize = TvText.Metadata,
                ),
            )
            Spacer(Modifier.width(4.dp))
            Image(
                painter = painterResource(R.drawable.ic_forward),
                contentDescription = null,
                colorFilter = ColorFilter.tint(
                    if (focused || selected) palette.accentStrong else palette.textMuted
                ),
                modifier = Modifier.size(12.dp),
            )
        }
    }
}

@Composable
private fun IconToggleRow(
    palette: CinePilotPalette,
    iconRes: Int,
    label: String,
    description: String = "",
    checked: Boolean,
    requestInitialFocus: Boolean = false,
    onCheckedChange: (Boolean) -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(TvDp.ControlRadius)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(TvDp.SettingsRowHeight)
            .clip(shape)
            .background(
                if (focused) palette.glassFocus else palette.glass,
                shape,
            )
            .border(
                width = if (focused) TvDp.FocusRing else 0.5.dp,
                color = if (focused) palette.focusRing else palette.glassBorder,
                shape,
            )
            .shadow(
                elevation = if (focused) 10.dp else 0.dp,
                shape = shape,
                spotColor = if (focused) palette.focusGlow else Color.Transparent,
                ambientColor = if (focused) palette.focusGlow else Color.Transparent,
            )
            .onFocusChanged { focused = it.isFocused }
            .focusable()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = { onCheckedChange(!checked) },
            )
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Image(
                painter = painterResource(iconRes),
                contentDescription = null,
                colorFilter = ColorFilter.tint(
                    if (focused) palette.accentStrong else palette.textSecondary
                ),
                modifier = Modifier.size(16.dp),
            )
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                BasicText(
                    text = label,
                    maxLines = 1,
                    style = TextStyle(
                        color = if (focused) palette.textPrimary else palette.textSecondary,
                        fontSize = TvText.Body,
                    ),
                )
                if (description.isNotBlank()) {
                    Spacer(Modifier.height(3.dp))
                    BasicText(
                        text = description,
                        maxLines = 1,
                        style = TextStyle(
                            color = palette.textMuted,
                            fontSize = TvText.Label,
                        ),
                    )
                }
            }
            // Custom toggle switch
            Box(
                modifier = Modifier
                    .width(36.dp)
                    .height(20.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        if (checked) palette.accent.copy(alpha = 0.7f)
                        else Color.White.copy(alpha = 0.15f),
                    )
                    .border(
                        width = if (focused) 1.dp else 0.5.dp,
                        color = if (checked) palette.accentStrong else palette.glassBorder,
                        shape = RoundedCornerShape(10.dp),
                    ),
                contentAlignment = Alignment.CenterStart,
            ) {
                Box(
                    modifier = Modifier
                        .padding(start = if (checked) 18.dp else 2.dp)
                        .size(16.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (checked) palette.accentStrong else Color.White.copy(alpha = 0.7f),
                        ),
                )
            }
        }
    }
}

@Composable
private fun SectionTitle(
    palette: CinePilotPalette,
    title: String,
) {
    BasicText(
        text = title,
        maxLines = 1,
        style = TextStyle(
            color = palette.accentStrong,
            fontSize = TvText.Section,
            fontWeight = FontWeight.SemiBold,
        ),
    )
    Spacer(Modifier.height(6.dp))
}

// ==================== Sections ====================

@Composable
private fun DisplayModeSection(
    palette: CinePilotPalette,
    current: PlaybackSettings,
    focusGroup: PlaybackSettingsFocusGroup,
    onUpdate: (PlaybackSettingsFocusGroup, (PlaybackSettings) -> PlaybackSettings) -> Unit,
) {
    Column {
        SectionTitle(palette, "显示模式")
        // 画面比例
        IconSettingRow(
            palette = palette,
            iconRes = R.drawable.ic_aspect_ratio,
            label = "画面比例",
            value = "原始比例 16:9",
            onClick = { /* TODO: aspect ratio selection */ },
        )
        Spacer(Modifier.height(6.dp))
        // 画面缩放
        IconSettingRow(
            palette = palette,
            iconRes = R.drawable.ic_zoom,
            label = "画面缩放",
            value = "默认",
            onClick = { /* TODO: zoom selection */ },
        )
        Spacer(Modifier.height(6.dp))
        // 自动匹配刷新率
        IconToggleRow(
            palette = palette,
            iconRes = R.drawable.ic_refresh,
            label = "自动匹配刷新率",
            description = "按片源帧率切换显示模式",
            checked = current.autoFrameMatching,
            requestInitialFocus = focusGroup == PlaybackSettingsFocusGroup.AFM,
            onCheckedChange = { checked ->
                onUpdate(PlaybackSettingsFocusGroup.AFM) {
                    it.copy(autoFrameMatching = checked)
                }
            },
        )
        Spacer(Modifier.height(6.dp))
        // 匹配色彩空间
        IconToggleRow(
            palette = palette,
            iconRes = R.drawable.ic_eye,
            label = "匹配色彩空间",
            description = "HDR / SDR 自动匹配",
            checked = current.matchColorSpace,
            onCheckedChange = { checked ->
                onUpdate(PlaybackSettingsFocusGroup.AFM) {
                    it.copy(matchColorSpace = checked)
                }
            },
        )
        Spacer(Modifier.height(6.dp))
        // 切换前确认
        IconToggleRow(
            palette = palette,
            iconRes = R.drawable.ic_check,
            label = "切换前确认",
            description = "切换显示模式前确认",
            checked = current.confirmBeforeFrameSwitch,
            onCheckedChange = { checked ->
                onUpdate(PlaybackSettingsFocusGroup.AFM) {
                    it.copy(
                        confirmBeforeFrameSwitch = checked,
                        skipFrameSwitchConfirm = if (checked) false else it.skipFrameSwitchConfirm,
                    )
                }
            },
        )
        if (current.skipFrameSwitchConfirm) {
            Spacer(Modifier.height(6.dp))
            IconSettingRow(
                palette = palette,
                iconRes = R.drawable.ic_refresh,
                label = "恢复确认提示",
                description = "重新显示切换确认",
                value = "恢复",
                onClick = {
                    onUpdate(PlaybackSettingsFocusGroup.AFM) {
                        it.copy(skipFrameSwitchConfirm = false)
                    }
                },
            )
        }
    }
}

@Composable
private fun StreamSettingsSection(
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
    Column {
        SectionTitle(palette, "音轨与字幕")
        // Audio streams
        audioStreams.orEmpty().forEach { stream ->
            val selected = stream.index() == currentAudioStreamIndex
            IconSettingRow(
                palette = palette,
                iconRes = R.drawable.ic_audio,
                label = "音轨：${streamLabel(stream)}",
                value = if (selected) "当前" else "切换",
                selected = selected,
                requestInitialFocus = focusGroup == PlaybackSettingsFocusGroup.AUDIO_TRACK && selected,
                onClick = { onAudioStreamChanged(stream.index()) },
            )
            Spacer(Modifier.height(6.dp))
        }
        // Subtitle off option
        val subtitlesOff = currentSubtitleStreamIndex == -1
        IconSettingRow(
            palette = palette,
            iconRes = R.drawable.ic_subtitle_row,
            label = "字幕：关闭",
            value = if (subtitlesOff) "当前" else "切换",
            selected = subtitlesOff,
            requestInitialFocus = focusGroup == PlaybackSettingsFocusGroup.SUBTITLE_TRACK && subtitlesOff,
            onClick = { onSubtitleStreamChanged(-1) },
        )
        Spacer(Modifier.height(6.dp))
        // Subtitle streams
        subtitleStreams.orEmpty().forEach { stream ->
            val selected = stream.index() == currentSubtitleStreamIndex
            IconSettingRow(
                palette = palette,
                iconRes = R.drawable.ic_subtitle_row,
                label = "字幕：${streamLabel(stream)}",
                value = if (selected) "当前" else "切换",
                selected = selected,
                requestInitialFocus = focusGroup == PlaybackSettingsFocusGroup.SUBTITLE_TRACK && selected,
                onClick = { onSubtitleStreamChanged(stream.index()) },
            )
            Spacer(Modifier.height(6.dp))
        }
        // Subtitle encoding
        SubtitleEncoding.values().forEach { encoding ->
            val selected = encoding == current.subtitleEncoding
            IconSettingRow(
                palette = palette,
                iconRes = R.drawable.ic_subtitle_row,
                label = "字幕编码：${encoding.label}",
                value = if (selected) "当前" else "切换",
                selected = selected,
                requestInitialFocus = focusGroup == PlaybackSettingsFocusGroup.SUBTITLE_ENCODING && selected,
                onClick = { onEncoding(encoding) },
            )
            Spacer(Modifier.height(6.dp))
        }
        // Burn graphic subtitle
        IconToggleRow(
            palette = palette,
            iconRes = R.drawable.ic_subtitle_row,
            label = "图形字幕烧录",
            description = "转码时由服务器烧录",
            checked = current.burnGraphicSubtitleWhenTranscoding,
            requestInitialFocus = focusGroup == PlaybackSettingsFocusGroup.BURN_GRAPHIC_SUBTITLE,
            onCheckedChange = onBurnGraphicSubtitle,
        )
    }
}

@Composable
private fun AutoPlaySection(
    palette: CinePilotPalette,
    current: PlaybackSettings,
    focusGroup: PlaybackSettingsFocusGroup,
    onUpdate: (PlaybackSettingsFocusGroup, (PlaybackSettings) -> PlaybackSettings) -> Unit,
) {
    Column {
        SectionTitle(palette, "连播与预览")
        IconToggleRow(
            palette = palette,
            iconRes = R.drawable.ic_play_next_row,
            label = "自动连播",
            description = "剧集末段显示下一集",
            checked = current.autoPlayNext,
            requestInitialFocus = focusGroup == PlaybackSettingsFocusGroup.AUTO_PLAY,
            onCheckedChange = { checked ->
                onUpdate(PlaybackSettingsFocusGroup.AUTO_PLAY) {
                    it.copy(autoPlayNext = checked)
                }
            },
        )
        Spacer(Modifier.height(6.dp))
        IconSettingRow(
            palette = palette,
            iconRes = R.drawable.ic_thumbnail,
            label = "下一集预览",
            value = "30秒",
            onClick = { /* TODO: preview duration selection */ },
        )
        Spacer(Modifier.height(6.dp))
        IconToggleRow(
            palette = palette,
            iconRes = R.drawable.ic_thumbnail,
            label = "章节列表",
            description = "底部显示章节 chips",
            checked = current.showChapterStrip,
            requestInitialFocus = focusGroup == PlaybackSettingsFocusGroup.CHAPTERS,
            onCheckedChange = { checked ->
                onUpdate(PlaybackSettingsFocusGroup.CHAPTERS) {
                    it.copy(showChapterStrip = checked)
                }
            },
        )
        Spacer(Modifier.height(6.dp))
        IconToggleRow(
            palette = palette,
            iconRes = R.drawable.ic_thumbnail,
            label = "缩略图预览",
            description = "保留设置，预览后续接入",
            checked = current.showTrickplayPreview,
            requestInitialFocus = focusGroup == PlaybackSettingsFocusGroup.TRICKPLAY,
            onCheckedChange = { checked ->
                onUpdate(PlaybackSettingsFocusGroup.TRICKPLAY) {
                    it.copy(showTrickplayPreview = checked)
                }
            },
        )
    }
}

@Composable
private fun IntroCreditsSection(
    palette: CinePilotPalette,
    current: PlaybackSettings,
    focusGroup: PlaybackSettingsFocusGroup,
    onUpdate: (PlaybackSettingsFocusGroup, (PlaybackSettings) -> PlaybackSettings) -> Unit,
) {
    Column {
        SectionTitle(palette, "片头 / 片尾")
        IconToggleRow(
            palette = palette,
            iconRes = R.drawable.ic_skip_forward_row,
            label = "跳过片头",
            checked = current.showIntroSkipButton,
            requestInitialFocus = focusGroup == PlaybackSettingsFocusGroup.INTRO_SKIP,
            onCheckedChange = { checked ->
                onUpdate(PlaybackSettingsFocusGroup.INTRO_SKIP) {
                    it.copy(showIntroSkipButton = checked)
                }
            },
        )
        Spacer(Modifier.height(6.dp))
        IconToggleRow(
            palette = palette,
            iconRes = R.drawable.ic_skip_forward_row,
            label = "自动跳片头",
            checked = current.autoSkipIntro,
            onCheckedChange = { checked ->
                onUpdate(PlaybackSettingsFocusGroup.INTRO_SKIP) {
                    it.copy(autoSkipIntro = checked)
                }
            },
        )
        Spacer(Modifier.height(6.dp))
        IconToggleRow(
            palette = palette,
            iconRes = R.drawable.ic_skip_forward_row,
            label = "跳过片尾",
            checked = current.showCreditsSkipButton,
            onCheckedChange = { checked ->
                onUpdate(PlaybackSettingsFocusGroup.INTRO_SKIP) {
                    it.copy(showCreditsSkipButton = checked)
                }
            },
        )
        Spacer(Modifier.height(6.dp))
        IconToggleRow(
            palette = palette,
            iconRes = R.drawable.ic_skip_forward_row,
            label = "自动跳片尾",
            checked = current.autoSkipCredits,
            onCheckedChange = { checked ->
                onUpdate(PlaybackSettingsFocusGroup.INTRO_SKIP) {
                    it.copy(autoSkipCredits = checked)
                }
            },
        )
    }
}

// ==================== Bottom Buttons & Misc ====================

@Composable
private fun SettingsBottomButton(
    palette: CinePilotPalette,
    label: String,
    iconRes: Int,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(TvDp.ControlRadius)
    Box(
        modifier = modifier
            .height(TvDp.ControlHeight)
            .clip(shape)
            .background(
                when {
                    focused -> palette.glassFocus
                    selected -> palette.accent.copy(alpha = 0.8f)
                    else -> palette.glass
                },
                shape,
            )
            .border(
                width = if (focused || selected) TvDp.FocusRing else 0.5.dp,
                color = when {
                    focused -> palette.focusRing
                    selected -> palette.accentStrong
                    else -> palette.glassBorder
                },
                shape,
            )
            .shadow(
                elevation = if (focused) 12.dp else if (selected) 6.dp else 0.dp,
                shape = shape,
                spotColor = when {
                    focused -> palette.focusGlow
                    selected -> palette.accent.copy(alpha = 0.4f)
                    else -> Color.Transparent
                },
                ambientColor = when {
                    focused -> palette.focusGlow
                    selected -> palette.accent.copy(alpha = 0.3f)
                    else -> Color.Transparent
                },
            )
            .onFocusChanged { focused = it.isFocused }
            .focusable()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Image(
                painter = painterResource(iconRes),
                contentDescription = null,
                colorFilter = ColorFilter.tint(
                    if (focused || selected) palette.focusText else palette.textSecondary
                ),
                modifier = Modifier.size(14.dp),
            )
            BasicText(
                text = label,
                maxLines = 1,
                style = TextStyle(
                    color = if (focused || selected) palette.focusText else palette.textSecondary,
                    fontSize = TvText.Body,
                    fontWeight = FontWeight.Medium,
                ),
            )
        }
    }
}

@Composable
private fun ChapterStripBar(
    palette: CinePilotPalette,
    modifier: Modifier = Modifier,
) {
    val chapters = listOf(
        "序幕" to "00:00",
        "黑雨降临" to "08:42",
        "追踪线索" to "17:05",
        "危机四伏" to "26:31",
        "真相浮现" to "35:48",
        "抉择时刻" to "44:12",
    )
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier,
    ) {
        // Chapter label
        Box(
            modifier = Modifier
                .height(48.dp)
                .padding(end = 8.dp),
            contentAlignment = Alignment.Center,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = painterResource(R.drawable.ic_thumbnail),
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(palette.textMuted),
                    modifier = Modifier.size(14.dp),
                )
                Spacer(Modifier.width(4.dp))
                BasicText(
                    text = "章节",
                    style = TextStyle(
                        color = palette.textSecondary,
                        fontSize = TvText.Body,
                    ),
                )
            }
        }
        // Chapter chips
        chapters.forEachIndexed { index, (title, time) ->
            val isCurrent = index == 1
            ChapterChip(
                palette = palette,
                number = (index + 1).toString(),
                title = title,
                time = time,
                selected = isCurrent,
                onClick = { /* TODO */ },
            )
            if (index < chapters.lastIndex) {
                Spacer(Modifier.width(6.dp))
            }
        }
        // More button
        Spacer(Modifier.width(6.dp))
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(palette.glass)
                .border(0.5.dp, palette.glassBorder, RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(R.drawable.ic_forward),
                contentDescription = null,
                colorFilter = ColorFilter.tint(palette.textMuted),
                modifier = Modifier.size(14.dp),
            )
        }
    }
}

@Composable
private fun ChapterChip(
    palette: CinePilotPalette,
    number: String,
    title: String,
    time: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(8.dp)
    Box(
        modifier = Modifier
            .width(96.dp)
            .height(48.dp)
            .clip(shape)
            .background(
                when {
                    focused -> palette.glassFocus
                    selected -> palette.accent.copy(alpha = 0.3f)
                    else -> palette.glass
                },
                shape,
            )
            .border(
                width = if (focused) TvDp.FocusRing else if (selected) 1.dp else 0.5.dp,
                color = when {
                    focused -> palette.focusRing
                    selected -> palette.accentStrong
                    else -> palette.glassBorder
                },
                shape,
            )
            .onFocusChanged { focused = it.isFocused }
            .focusable()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 8.dp, vertical = 6.dp),
    ) {
        Column(verticalArrangement = Arrangement.Center) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                BasicText(
                    text = number,
                    style = TextStyle(
                        color = if (selected) palette.accentStrong else palette.textMuted,
                        fontSize = TvText.Label,
                        fontWeight = FontWeight.SemiBold,
                    ),
                )
                Spacer(Modifier.width(3.dp))
                BasicText(
                    text = title,
                    maxLines = 1,
                    style = TextStyle(
                        color = if (focused) palette.textPrimary else palette.textSecondary,
                        fontSize = TvText.Label,
                    ),
                )
            }
            Spacer(Modifier.height(2.dp))
            BasicText(
                text = time,
                style = TextStyle(
                    color = palette.textMuted,
                    fontSize = TvText.Label,
                ),
            )
        }
    }
}

@Composable
private fun NextUpCountdownCard(
    palette: CinePilotPalette,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(TvDp.CardRadius)
    Box(
        modifier = modifier
            .width(TvDp.NextUpWidth)
            .clip(shape)
            .background(palette.glass)
            .border(0.5.dp, palette.glassBorder, shape)
            .shadow(
                elevation = 8.dp,
                shape = shape,
                spotColor = Color.Black.copy(alpha = 0.5f),
                ambientColor = Color.Black.copy(alpha = 0.3f),
            )
            .padding(12.dp),
    ) {
        Column {
            BasicText(
                text = "接下来播放",
                maxLines = 1,
                style = TextStyle(
                    color = palette.textSecondary,
                    fontSize = TvText.Metadata,
                    fontWeight = FontWeight.Medium,
                ),
            )
            Spacer(Modifier.height(8.dp))
            Row(
                verticalAlignment = Alignment.Top,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Box(
                    modifier = Modifier
                        .size(width = TvDp.LandscapeWidth, height = TvDp.LandscapeHeight)
                        .clip(RoundedCornerShape(8.dp))
                        .background(palette.posterFallback)
                        .border(0.5.dp, palette.glassBorder, RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Image(
                        painter = painterResource(R.drawable.ic_play),
                        contentDescription = null,
                        colorFilter = ColorFilter.tint(palette.textMuted),
                        modifier = Modifier.size(24.dp),
                    )
                }
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    BasicText(
                        text = "S1:E3",
                        maxLines = 1,
                        style = TextStyle(
                            color = palette.textMuted,
                            fontSize = TvText.Metadata,
                        ),
                    )
                    Spacer(Modifier.height(3.dp))
                    BasicText(
                        text = "危机四伏",
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        style = TextStyle(
                            color = palette.textPrimary,
                            fontSize = TvText.Body,
                            fontWeight = FontWeight.SemiBold,
                        ),
                    )
                    Spacer(Modifier.height(5.dp))
                    BasicText(
                        text = "将在 8 秒后开始播放",
                        maxLines = 1,
                        style = TextStyle(
                            color = palette.textMuted,
                            fontSize = TvText.Label,
                        ),
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                // 立即播放 button (primary)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(TvDp.ControlHeight)
                        .clip(RoundedCornerShape(TvDp.ControlRadius))
                        .background(palette.accent.copy(alpha = 0.85f))
                        .border(1.dp, palette.accentStrong, RoundedCornerShape(TvDp.ControlRadius))
                        .focusable()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { /* TODO */ },
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(R.drawable.ic_play),
                            contentDescription = null,
                            colorFilter = ColorFilter.tint(palette.focusText),
                            modifier = Modifier.size(12.dp),
                        )
                        Spacer(Modifier.width(4.dp))
                        BasicText(
                            text = "立即播放",
                            style = TextStyle(
                                color = palette.focusText,
                                fontSize = TvText.Body,
                                fontWeight = FontWeight.Medium,
                            ),
                        )
                    }
                }
                // 取消 button (secondary)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(TvDp.ControlHeight)
                        .clip(RoundedCornerShape(TvDp.ControlRadius))
                        .background(palette.glass)
                        .border(0.5.dp, palette.glassBorder, RoundedCornerShape(TvDp.ControlRadius))
                        .focusable()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { /* TODO */ },
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    BasicText(
                        text = "取消",
                        style = TextStyle(
                            color = palette.textSecondary,
                            fontSize = TvText.Body,
                            fontWeight = FontWeight.Medium,
                        ),
                    )
                }
            }
        }
    }
}

private fun FrameLayout.attachPlayerView(playerView: View) {
    if (playerView.parent === this && childCount == 1 && getChildAt(0) === playerView) {
        return
    }
    (playerView.parent as? ViewGroup)?.removeView(playerView)
    removeAllViews()
    addView(
        playerView,
        FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT,
        ),
    )
}
