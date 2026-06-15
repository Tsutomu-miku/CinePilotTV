package tv.cinepilot.tv.compose.screens.playbacksettings

import android.view.View
import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import tv.cinepilot.core.protocol.MediaStreamInfo
import tv.cinepilot.tv.R
import tv.cinepilot.tv.compose.theme.CinePilotPalette
import tv.cinepilot.tv.compose.theme.TvDp
import tv.cinepilot.tv.compose.theme.TvText
import tv.cinepilot.tv.playback.PlaybackSettings
import tv.cinepilot.tv.playback.PlaybackSettingsFocusGroup
import tv.cinepilot.tv.playback.SubtitleEncoding

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
                        .background(palette.glass.copy(alpha = 0.92f))
                        .border(0.8.dp, palette.glassBorder, RoundedCornerShape(TvDp.PanelRadius)),
                ) {
                    LazyColumn(
                        contentPadding = PaddingValues(
                            horizontal = 16.dp,
                            vertical = 18.dp,
                        ),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
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
                        }
                        item {
                            DisplayModeSection(
                                palette = palette,
                                current = current,
                                focusGroup = focusGroup,
                                onUpdate = { next, fn -> update(next, fn) },
                            )
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
                        }
                        item {
                            AutoPlaySection(
                                palette = palette,
                                current = current,
                                focusGroup = focusGroup,
                                onUpdate = { next, fn -> update(next, fn) },
                            )
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
